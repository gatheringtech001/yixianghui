package com.ruoyi.web.internal.talent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.service.*;
import com.ruoyi.system.mapper.TalentCenterOperationsMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Explicit opt-in; never accepts a remote or an ordinary business database. */
@EnabledIfEnvironmentVariable(named = "WORKFLOW_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1.*")
class TalentWorkflowMysqlTest {
    private static final String ADMIN = "00000000-0000-4000-8000-000000000001";
    private static final String SELF = "00000000-0000-4000-8000-000000000002";
    private static final AtomicLong IDS = new AtomicLong(2000);
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transaction;
    private static TalentBindingService binding;
    private static TalentInboxService inbox;
    private static TalentWorkflowAccess access;
    private static SqlSessionFactory mapperFactory;

    @BeforeAll static void setup() {
        String url = System.getenv("WORKFLOW_MYSQL_URL");
        if (!url.matches("jdbc:mysql://127\\.0\\.0\\.1(:[0-9]+)?/yxh_workflow_test_[a-z0-9_]+(\\?.*)?"))
            throw new IllegalStateException("A dedicated local workflow test database is required");
        DataSource source = new DriverManagerDataSource(url, System.getenv().getOrDefault("WORKFLOW_MYSQL_USER", "root"), System.getenv().getOrDefault("WORKFLOW_MYSQL_PASSWORD", ""));
        jdbc = new JdbcTemplate(source);
        new ResourceDatabasePopulator(new ClassPathResource("talent-workflow-fixture.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource(Objects.requireNonNull(System.getenv("WORKFLOW_MIGRATION_FILE")))).execute(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        access = new TalentWorkflowAccess(jdbc);
        TalentBindingStore store = new TalentBindingStore(jdbc);
        binding = new TalentBindingService(store, access, mock(RedisCache.class));
        inbox = new TalentInboxService(jdbc, access);
        jdbc.update("INSERT INTO sys_user(user_id,nick_name) VALUES (1,'测试管理员'),(2,'测试负责人'),(3,'其他负责人')");
        jdbc.update("INSERT INTO talent_center_admin_actor(actor_id,user_id) VALUES (?,1),(?,2)", ADMIN, SELF);
        jdbc.update("INSERT INTO app_goods(goods_id,goods_name,goods_type) VALUES (1,'测试旅居','hotel')");
        Configuration configuration = new Configuration(new Environment("workflow-test", new JdbcTransactionFactory(), source));
        new XMLMapperBuilder(TalentWorkflowMysqlTest.class.getResourceAsStream("/mapper/system/TalentCenterOperationsMapper.xml"), configuration, "workflow-operations", configuration.getSqlFragments()).parse();
        mapperFactory = new SqlSessionFactoryBuilder().build(configuration);
    }
    @Test void migrationMakesApprovalTablesTransactional() {
        String engine = jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='app_consultant'", String.class);
        assertEquals("InnoDB", engine);
    }
    @Test void bindingIsAtomicAndRepeatedConfirmationIsIdempotent() {
        long user = user(); String actor = UUID.randomUUID().toString();
        Map<String, Object> input = identity(actor, user);
        Map<String, Object> request = prepare(input);
        assertEquals("completed", execute(request).get("status"));
        assertEquals("completed", execute(request).get("status"));
        assertEquals(1, count("SELECT COUNT(*) FROM app_consultant WHERE user_id=?", user));
        assertEquals(1, count("SELECT COUNT(*) FROM talent_center_workflow_audit WHERE operation_id=?", request.get("operationId")));
        Map<String, Object> linked = jdbc.queryForMap("SELECT user_id,consultant_id FROM talent_center_admin_actor WHERE actor_id=?", actor);
        assertEquals(user, ((Number) linked.get("user_id")).longValue()); assertNotNull(linked.get("consultant_id"));
    }
    @Test void aStaleBindingPreviewRollsBackBothAuditAndIdentity() {
        long user = user(); String actor = UUID.randomUUID().toString(); Map<String, Object> request = prepare(identity(actor, user));
        jdbc.update("UPDATE sys_user SET nick_name='变化后的测试账号' WHERE user_id=?", user);
        assertEquals(409, assertThrows(TalentCenterApiException.class, () -> execute(request)).getHttpStatus());
        assertEquals(0, count("SELECT COUNT(*) FROM talent_center_admin_actor WHERE actor_id=?", actor));
        assertEquals(0, count("SELECT COUNT(*) FROM talent_center_workflow_audit WHERE operation_id=?", request.get("operationId")));
    }
    @Test void twoApplicantsCannotTakeTheSameBackendAccount() {
        long user = user(); String first = UUID.randomUUID().toString(), second = UUID.randomUUID().toString();
        Map<String, Object> a = prepare(identity(first, user)), b = prepare(identity(second, user)); execute(a);
        assertEquals(409, assertThrows(TalentCenterApiException.class, () -> execute(b)).getHttpStatus());
        assertEquals(1, count("SELECT COUNT(*) FROM talent_center_admin_actor WHERE user_id=?", user));
        assertEquals(0, count("SELECT COUNT(*) FROM talent_center_workflow_audit WHERE operation_id=?", b.get("operationId")));
    }
    @Test void concurrentConfirmationUsesOneAuditAndOneConsultant() throws Exception {
        long user = user(); Map<String, Object> request = prepare(identity(UUID.randomUUID().toString(), user));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Map<String, Object>> first = pool.submit(() -> execute(request));
            Future<Map<String, Object>> second = pool.submit(() -> execute(request));
            assertEquals("completed", first.get(10, TimeUnit.SECONDS).get("status"));
            assertEquals("completed", second.get(10, TimeUnit.SECONDS).get("status"));
        } finally { pool.shutdownNow(); }
        assertEquals(1, count("SELECT COUNT(*) FROM app_consultant WHERE user_id=?", user));
    }
    @Test void nativeAdvisorApprovalChecksStateAndDoesNotGrantSystemAdmin() {
        long id = IDS.incrementAndGet();
        jdbc.update("INSERT INTO app_consultant(consultant_id,consultant_name,status) VALUES (?,'测试申请人','00')", id);
        Map<String, Object> input = new LinkedHashMap<>(); input.put("kind", "review-advisor"); input.put("consultantId", String.valueOf(id)); input.put("decision", "approve"); input.put("note", "已核实资料");
        execute(prepare(input));
        assertEquals("01", jdbc.queryForObject("SELECT status FROM app_consultant WHERE consultant_id=?", String.class, id));
        assertEquals(0, count("SELECT COUNT(*) FROM talent_center_admin_actor WHERE consultant_id=?", id));
        assertEquals(409, assertThrows(TalentCenterApiException.class, () -> prepare(input)).getHttpStatus());
    }
    @Test void aChangedRequestCannotReuseAConfirmationId() {
        long user = user(); Map<String, Object> input = identity(UUID.randomUUID().toString(), user), request = prepare(input); execute(request);
        input.put("note", "不同的操作说明");
        assertEquals(409, assertThrows(TalentCenterApiException.class, () -> execute(request)).getHttpStatus());
    }
    @Test void ownershipAssignmentChangesNoMoneyOrFulfilmentState() {
        long user = user(), order = IDS.incrementAndGet();
        execute(prepare(identity(UUID.randomUUID().toString(), user)));
        jdbc.update("INSERT INTO app_goods_order(order_id,order_no,money_payable,travel_status) VALUES (?,'TEST',99.00,'1')", order);
        Map<String, Object> input = new LinkedHashMap<>(); input.put("kind", "assign-owner"); input.put("orderId", String.valueOf(order)); input.put("backendUserId", String.valueOf(user)); input.put("note", "已核对负责关系");
        execute(prepare(input));
        Map<String, Object> row = jdbc.queryForMap("SELECT service_owner_user_id,money_payable,travel_status FROM app_goods_order WHERE order_id=?", order);
        assertEquals(user, ((Number)row.get("service_owner_user_id")).longValue()); assertEquals("99.00", row.get("money_payable").toString()); assertEquals("1", row.get("travel_status"));
        assertEquals(403, assertThrows(TalentCenterApiException.class, () -> binding.preview(SELF, "self", input)).getHttpStatus());
    }
    @Test void inboxUsesRealOwnershipAndExcludesUnpaidNativeOrders() {
        long paid = order(2L, "1", "1", "native"), unpaid = order(2L, "0", "0", "native"), other = order(3L, "1", "1", "native"), history = order(2L, "0", null, "feishu_history");
        List<Map<String, Object>> items = items(inbox.page(SELF, "self", "my", "travel", "first"));
        Set<String> ids = new HashSet<>(); for (Map<String, Object> item : items) ids.add(String.valueOf(item.get("targetId")));
        assertTrue(ids.contains(String.valueOf(paid))); assertTrue(ids.contains(String.valueOf(history)));
        assertFalse(ids.contains(String.valueOf(unpaid))); assertFalse(ids.contains(String.valueOf(other)));
        assertEquals(403, assertThrows(TalentCenterApiException.class, () -> inbox.page(SELF, "self", "all", "travel", "first")).getHttpStatus());
    }
    @Test void inboxPaginatesWithoutTurningFirstPageSizeIntoTotal() {
        long owner = user(); String actor = UUID.randomUUID().toString(); execute(prepare(identity(actor, owner)));
        for (int i = 0; i < 51; i++) order(owner, "1", "1", "native");
        Map<String, Object> first = inbox.page(actor, "self", "my", "travel", "first");
        assertEquals(51L, first.get("total")); assertEquals(50, items(first).size()); assertNotNull(first.get("nextCursor"));
        Map<String, Object> second = inbox.page(actor, "self", "my", "travel", String.valueOf(first.get("nextCursor")));
        assertEquals(1, items(second).size()); assertNull(second.get("nextCursor")); assertEquals(51L, second.get("total"));
    }
    @Test void absentRefundAmountIsNotPresentedAsZero() {
        long id = IDS.incrementAndGet(); jdbc.update("INSERT INTO app_goods_order_after(after_id,goods_id,status) VALUES (?,1,'0')", id);
        Map<String, Object> row = items(inbox.page(ADMIN, "admin", "my", "refunds", "first")).stream().filter(item -> String.valueOf(id).equals(item.get("targetId"))).findFirst().orElseThrow();
        assertTrue(String.valueOf(row.get("description")).contains("金额待核对"));
    }
    @Test void aReferrerCanReadButOnlyTheActualOwnerCanModifyAnOrder() {
        long id = order(3L, "1", "1", "native");
        jdbc.update("UPDATE app_goods_order SET user_id=99 WHERE order_id=?", id);
        jdbc.update("INSERT INTO app_user_inviter(user_id,new_user_id,status) VALUES (2,99,'1')");
        try (SqlSession session = mapperFactory.openSession(true)) {
            TalentCenterOperationsMapper mapper = session.getMapper(TalentCenterOperationsMapper.class);
            assertNotNull(mapper.selectOrder(id, 2L, false));
            assertEquals(0, mapper.updateOrderStatus(id, 2L, false, "1", "3"));
            assertEquals(1, mapper.updateOrderStatus(id, 3L, false, "1", "3"));
            assertNull(mapper.selectOrder(id, 100L, false));
            assertEquals("order:" + id, mapper.selectOrder(id, 1L, true).get("sourceRecordId"));
        }
    }
    @Test void unpaidNativeOrdersCannotBeManuallyConfirmedEvenByAnAdmin() {
        long id = order(2L, "0", "0", "native");
        try (SqlSession session = mapperFactory.openSession(true)) {
            assertEquals(0, session.getMapper(TalentCenterOperationsMapper.class).updateOrderStatus(id, 1L, true, "0", "1"));
        }
    }
    @Test void explicitConsultantBindingLimitsTheCustomerInbox() {
        long user = user(); String actor = UUID.randomUUID().toString(); execute(prepare(identity(actor, user)));
        Long consultant = jdbc.queryForObject("SELECT consultant_id FROM talent_center_admin_actor WHERE actor_id=?", Long.class, actor);
        long legacy = IDS.incrementAndGet(), ownCustomer = IDS.incrementAndGet(), otherCustomer = IDS.incrementAndGet();
        jdbc.update("INSERT INTO app_consultant(consultant_id,user_id,consultant_name,status) VALUES (?,?,'历史重复档案','01')", legacy, user);
        jdbc.update("INSERT INTO app_customer(customer_id,consultant_id,customer_name,return_visit,del_flag) VALUES (?,?,'归属客户',0,'0'),(?,?,'待核实客户',0,'0')", ownCustomer, consultant, otherCustomer, legacy);
        Set<String> ids = new HashSet<>(); for (Map<String,Object> item : items(inbox.page(actor,"self","my","followup","first"))) ids.add(String.valueOf(item.get("targetId")));
        assertTrue(ids.contains(String.valueOf(ownCustomer))); assertFalse(ids.contains(String.valueOf(otherCustomer)));
        try (SqlSession session = mapperFactory.openSession(true)) {
            assertEquals(consultant, session.getMapper(TalentCenterOperationsMapper.class).selectConsultantId(user));
        }
    }
    private static long user() { long id = IDS.incrementAndGet(); jdbc.update("INSERT INTO sys_user(user_id,nick_name) VALUES (?,'测试后台账号')", id); return id; }
    private static long order(Long owner, String state, String paid, String origin) {
        long id = IDS.incrementAndGet(); String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        jdbc.update("INSERT INTO app_goods_order(order_id,order_no,goods_id,service_owner_user_id,travel_status,pay_status,order_origin,check_in_date,check_out_date) VALUES (?,'TEST',1,?,?,?,?,?,?)", id, owner, state, paid, origin, today, today); return id;
    }
    private static Map<String, Object> identity(String actor, long user) {
        Map<String, Object> input = new LinkedHashMap<>(); input.put("kind", "bind-identity"); input.put("targetActorId", actor); input.put("displayName", "测试管家"); input.put("backendUserId", String.valueOf(user)); input.put("consultantId", null); input.put("roles", Collections.singletonList("butler")); input.put("note", "已核对身份"); return input;
    }
    private static Map<String, Object> prepare(Map<String, Object> input) {
        Map<String, Object> preview = binding.preview(ADMIN, "admin", input), request = new LinkedHashMap<>();
        request.put("operationId", UUID.randomUUID().toString()); request.put("input", input); request.put("fingerprint", preview.get("fingerprint")); return request;
    }
    private static Map<String, Object> execute(Map<String, Object> request) { return transaction.execute(status -> binding.execute(ADMIN, "admin", request)); }
    private static int count(String sql, Object id) { return Objects.requireNonNull(jdbc.queryForObject(sql, Integer.class, id)); }
    @SuppressWarnings("unchecked") private static List<Map<String, Object>> items(Map<String, Object> page) { return (List<Map<String, Object>>)page.get("items"); }
}
