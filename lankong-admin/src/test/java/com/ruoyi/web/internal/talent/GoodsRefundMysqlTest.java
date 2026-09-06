package com.ruoyi.web.internal.talent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import com.ruoyi.system.domain.AppPayRefundLog;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.service.*;
import com.wechat.pay.java.service.refund.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real isolated MySQL transactions; the payment gateway is always mocked. */
@EnabledIfEnvironmentVariable(named = "REFUND_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1.*")
class GoodsRefundMysqlTest {
    private static final AtomicLong IDS = new AtomicLong(100);
    private static JdbcTemplate jdbc;
    private static GoodsRefundStore store;
    private GoodsRefundGateway gateway;
    private GoodsRefundReviewService service;
    private IAppGoodsOrderService effects;
    private long id;

    @BeforeAll static void database() {
        String url = System.getenv("REFUND_MYSQL_URL");
        if (!url.matches("jdbc:mysql://127\\.0\\.0\\.1(:[0-9]+)?/yxh_refund_test_[a-z0-9_]+(\\?.*)?"))
            throw new IllegalStateException("Only an isolated local refund database is allowed");
        DataSource source = new DriverManagerDataSource(url, System.getenv().getOrDefault("REFUND_MYSQL_USER", "root"), System.getenv().getOrDefault("REFUND_MYSQL_PASSWORD", ""));
        jdbc = new JdbcTemplate(source);
        new ResourceDatabasePopulator(new ClassPathResource("talent-workflow-fixture.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource(Objects.requireNonNull(System.getenv("WORKFLOW_MIGRATION_FILE")))).execute(source);
        store = new GoodsRefundStore(jdbc, new DataSourceTransactionManager(source));
        jdbc.update("INSERT INTO app_goods(goods_id,goods_name,goods_type) VALUES (1,'隔离退款测试课程','education')");
    }
    @BeforeEach void fixture() {
        id = IDS.incrementAndGet(); gateway = mock(GoodsRefundGateway.class); effects = mock(IAppGoodsOrderService.class);
        service = new GoodsRefundReviewService(store, gateway, effects);
        jdbc.update("INSERT INTO app_goods_order(order_id,order_no,goods_id,user_id,status,pay_status,order_origin,travel_status,travel_status_before_refund) VALUES (?,?,1,5,'3','1','mini_program','6','1')", id, "TEST" + id);
        jdbc.update("INSERT INTO app_goods_order_after(after_id,order_id,goods_id,user_id,out_order_no,status,after_type,app_refund_money,goods_money) VALUES (?,?,1,5,?,'0','2',1.00,1.00)", id, id, "TEST" + id);
        jdbc.update("INSERT INTO app_pay_log(order_id,user_id,pay_no,pay_money,status,pay_method) VALUES (?,5,?,100,'1','wxpay')", id, "TEST" + id);
        doAnswer(call -> {
            AppPayRefundLog log = call.getArgument(0);
            jdbc.update("UPDATE app_goods_order_after SET status='6' WHERE after_id=?", log.getOrderId());
            jdbc.update("UPDATE app_goods_order SET status='4',pay_status='4',travel_status='7' WHERE order_id=?", log.getOrderId());
            return null;
        }).when(effects).completeReviewedRefund(any());
        when(gateway.create(any(), any())).thenAnswer(call -> evidence(Status.PROCESSING));
    }
    @Test void previewIsReadOnly() {
        service.preview(request("approve"));
        assertEquals("0", state()); assertEquals(0, logs()); verifyNoInteractions(gateway, effects);
    }
    @Test void approvalCommitsIntentAndStableReferenceBeforeCallingWechat() {
        when(gateway.create(any(), any())).thenAnswer(call -> {
            assertEquals("1", state()); assertEquals(1, logs());
            assertEquals("YXHAF" + id, jdbc.queryForObject("SELECT agent_refund_no FROM app_pay_refund_log WHERE order_id=?", String.class, id));
            assertEquals(1, count("SELECT COUNT(*) FROM talent_center_workflow_audit WHERE input_json->>'$.afterId'=?", String.valueOf(id)));
            return evidence(Status.PROCESSING);
        });
        GoodsRefundRequest request = confirmed("approve");
        assertEquals("pending", service.execute(request).get("status"));
        assertEquals("pending", service.execute(request).get("status"));
        assertEquals(1, logs()); verify(gateway, times(1)).create(any(), any()); verifyNoInteractions(effects);
    }
    @Test void staleMoneyDoesNotReserveOrCallWechat() {
        GoodsRefundRequest request = confirmed("approve");
        jdbc.update("UPDATE app_goods_order_after SET app_refund_money=0.50 WHERE after_id=?", id);
        assertEquals(409, assertThrows(TalentCenterApiException.class, () -> service.execute(request)).getHttpStatus());
        assertEquals(0, logs()); assertEquals("0", state()); verifyNoInteractions(gateway);
        assertEquals(0, count("SELECT COUNT(*) FROM talent_center_workflow_audit WHERE operation_id=?", request.operationId));
    }
    @Test void concurrentAdministratorsCannotRefundTheSameApplicationTwice() throws Exception {
        GoodsRefundRequest first = confirmed("approve"), second = confirmed("approve");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = pool.invokeAll(Arrays.asList(() -> approve(first), () -> approve(second)));
            int successful = 0; for (Future<Boolean> result : results) if (result.get(10, TimeUnit.SECONDS)) successful++;
            assertEquals(1, successful);
        } finally { pool.shutdownNow(); }
        assertEquals(1, logs()); verify(gateway, times(1)).create(any(), any());
    }
    @Test void uncertainPaymentIsNotRetriedAndCanBeReconciledByQuery() {
        when(gateway.create(any(), any())).thenThrow(new IllegalStateException("simulated timeout"));
        GoodsRefundRequest request = confirmed("approve");
        assertEquals("uncertain", service.execute(request).get("refundStatus"));
        assertEquals("uncertain", service.execute(request).get("refundStatus"));
        assertEquals("1", state()); assertEquals(1, logs());
        when(gateway.query("YXHAF" + id)).thenReturn(evidence(Status.SUCCESS));
        assertEquals("succeeded", service.execute(confirmed("sync")).get("refundStatus"));
        assertEquals("6", state()); verify(gateway, times(1)).create(any(), any()); verify(effects, times(1)).completeReviewedRefund(any());
    }
    @Test void crashAfterReservationStillPreventsANewRefund() {
        store.prepare(confirmed("approve"));
        assertThrows(TalentCenterApiException.class, () -> confirmed("approve"));
        when(gateway.query(anyString())).thenThrow(new IllegalStateException("not yet found"));
        assertEquals("uncertain", service.execute(confirmed("sync")).get("refundStatus"));
        verify(gateway, never()).create(any(), any()); assertEquals(1, logs());
        assertEquals("processing", service.execute(confirmed("retry")).get("refundStatus"));
        assertEquals(1, logs()); verify(gateway, times(1)).create(any(), any());
        assertThrows(TalentCenterApiException.class, () -> confirmed("retry"));
    }
    @Test void duplicateVerifiedCallbacksApplyCompletionOnlyOnce() {
        service.execute(confirmed("approve"));
        Refund evidence = evidence(Status.SUCCESS);
        RefundNotification notification = new RefundNotification(); notification.setOutRefundNo(evidence.getOutRefundNo());
        notification.setOutTradeNo(evidence.getOutTradeNo()); notification.setRefundId(evidence.getRefundId());
        notification.setAmount(evidence.getAmount()); notification.setRefundStatus(Status.SUCCESS);
        service.notification(notification); service.notification(notification);
        assertEquals("6", state()); verify(effects, times(1)).completeReviewedRefund(any());
    }
    @Test void paymentEvidenceMustMatchRefundAmountAndOrder() {
        Refund evidence = evidence(Status.SUCCESS); evidence.getAmount().setRefund(101L);
        when(gateway.create(any(), any())).thenReturn(evidence);
        assertEquals("uncertain", service.execute(confirmed("approve")).get("refundStatus"));
        assertEquals("1", state()); verifyNoInteractions(effects);
        assertEquals("0", jdbc.queryForObject("SELECT status FROM app_pay_refund_log WHERE order_id=?", String.class, id));
    }
    @Test void localCompletionFailureRollsBackSuccessAndCanBeSafelyReconciled() {
        when(gateway.create(any(), any())).thenReturn(evidence(Status.SUCCESS));
        doAnswer(call -> { jdbc.update("UPDATE app_goods_order_after SET status='6' WHERE after_id=?", id); throw new IllegalStateException("simulated accounting failure"); }).when(effects).completeReviewedRefund(any());
        assertEquals("uncertain", service.execute(confirmed("approve")).get("refundStatus"));
        assertEquals("1", state());
        assertEquals("0", jdbc.queryForObject("SELECT status FROM app_pay_refund_log WHERE order_id=?", String.class, id));
    }
    @Test void terminalFailureCannotBeOverwrittenByAnOlderProcessingResponse() {
        when(gateway.create(any(), any())).thenReturn(evidence(Status.CLOSED));
        assertEquals("failed", service.execute(confirmed("approve")).get("refundStatus"));
        assertEquals("5", state());
        when(gateway.query(anyString())).thenReturn(evidence(Status.PROCESSING));
        assertEquals("failed", service.execute(confirmed("sync")).get("refundStatus"));
        assertEquals("1", jdbc.queryForObject("SELECT travel_status FROM app_goods_order WHERE order_id=?", String.class, id));
    }
    @Test void rejectionIsAuditedAndNeverCallsPaymentGateway() {
        GoodsRefundRequest request = confirmed("reject");
        assertEquals("rejected", service.execute(request).get("refundStatus"));
        assertEquals("rejected", service.execute(request).get("refundStatus"));
        assertEquals("2", state()); assertEquals(0, logs()); verifyNoInteractions(gateway, effects);
        assertEquals("1", jdbc.queryForObject("SELECT travel_status FROM app_goods_order WHERE order_id=?", String.class, id));
    }
    @Test void replayCannotChangeTheDecisionOrActor() {
        GoodsRefundRequest first = confirmed("approve"); service.execute(first);
        GoodsRefundRequest tampered = request("reject").confirmation(first.operationId, first.expected);
        assertEquals(409, assertThrows(TalentCenterApiException.class, () -> service.execute(tampered)).getHttpStatus());
        verify(gateway, times(1)).create(any(), any()); assertEquals("1", state());
    }
    private boolean approve(GoodsRefundRequest request) {
        try { service.execute(request); return true; }
        catch (TalentCenterApiException error) { assertEquals(409, error.getHttpStatus()); return false; }
    }
    private GoodsRefundRequest confirmed(String decision) {
        GoodsRefundRequest request = request(decision); return request.confirmation(UUID.randomUUID().toString(), service.preview(request).get("fingerprint"));
    }
    private GoodsRefundRequest request(String decision) {
        boolean existing = Arrays.asList("sync", "retry").contains(decision);
        Map<String, Object> input = new LinkedHashMap<>(); input.put("kind", existing ? decision + "-refund" : "review-refund");
        input.put("afterId", String.valueOf(id)); input.put("note", "隔离测试核验");
        if (!existing) { input.put("decision", decision); input.put("refundAmount", "approve".equals(decision) ? "1.00" : null); input.put("returnReceived", false); }
        return GoodsRefundRequest.from("test-admin", input);
    }
    private Refund evidence(Status state) {
        Refund result = new Refund(); result.setOutRefundNo("YXHAF" + id); result.setOutTradeNo("TEST" + id); result.setRefundId("TEST-REFUND-" + id); result.setStatus(state);
        Amount amount = new Amount(); amount.setTotal(100L); amount.setRefund(100L); amount.setCurrency("CNY"); result.setAmount(amount); return result;
    }
    private String state() { return jdbc.queryForObject("SELECT status FROM app_goods_order_after WHERE after_id=?", String.class, id); }
    private int logs() { return count("SELECT COUNT(*) FROM app_pay_refund_log WHERE order_id=?", id); }
    private static int count(String sql, Object value) { return Objects.requireNonNull(jdbc.queryForObject(sql, Integer.class, value)); }
}
