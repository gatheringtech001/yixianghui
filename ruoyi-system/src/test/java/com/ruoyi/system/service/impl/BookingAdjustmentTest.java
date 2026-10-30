package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.IAppGoldService;
import com.ruoyi.system.service.*;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated transactional database tests. WeChat is always mocked; never pays or refunds real money. */
class BookingAdjustmentTest {
    private JdbcTemplate db;
    private BookingAdjustmentGateway gateway;
    private IAppGoldService gold;
    private BookingAdjustmentService service;
    private GoodsRefundGateway refunds;
    private GoodsRefundReviewService reviews;
    private javax.sql.DataSource source;
    private IAppGoodsOrderService goodsOrders;

    @BeforeEach void setup() {
        // MySQL CAST(... AS BINARY) has variable length; H2 otherwise defaults to BINARY(1).
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;VARIABLE_BINARY=TRUE;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        db = new JdbcTemplate(source);
        db.execute("CREATE TABLE app_activity(activity_id BIGINT PRIMARY KEY,status VARCHAR(1),sign_end_time VARCHAR(40),max_count VARCHAR(20),sign_count VARCHAR(20))");
        db.execute("CREATE TABLE app_activity_order(order_id BIGINT PRIMARY KEY,user_id BIGINT,activity_id BIGINT,sign_count INT,status VARCHAR(1),pay_status VARCHAR(1),pay_money DECIMAL(10,2),money_payable DECIMAL(10,2),order_no VARCHAR(100))");
        db.execute("CREATE TABLE app_user_card(record_id BIGINT PRIMARY KEY,user_id BIGINT,status VARCHAR(1),update_time DATETIME)");
        db.execute("CREATE TABLE app_pay_log(log_id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,order_id BIGINT,order_type VARCHAR(10),pay_no VARCHAR(100),pay_name VARCHAR(50),pay_description VARCHAR(100),pay_money DECIMAL(10,2),pay_method VARCHAR(20),status CHAR(1),create_time DATETIME,update_time DATETIME)");
        db.execute("CREATE TABLE app_pay_refund_log(log_id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,order_id BIGINT,order_type VARCHAR(10),pay_method VARCHAR(20),agent_name VARCHAR(50),agent_refund_no VARCHAR(100),refund_money DECIMAL(10,2),status CHAR(1),pay_no VARCHAR(100),agent_pay_no VARCHAR(100),create_time DATETIME,update_time DATETIME)");
        db.execute("CREATE TABLE app_goods_order_after(after_id BIGINT AUTO_INCREMENT PRIMARY KEY,after_type CHAR(1),user_id BIGINT,order_id BIGINT,out_order_no VARCHAR(100),order_money DECIMAL(10,2),goods_id BIGINT,goods_count INT,goods_money DECIMAL(10,2),express_money DECIMAL(10,2),app_refund_money DECIMAL(10,2),refund_money DECIMAL(10,2),status CHAR(1),reason_description VARCHAR(255),remark VARCHAR(255),back_express_name VARCHAR(100),back_express_no VARCHAR(100),resend_express_name VARCHAR(100),resend_express_no VARCHAR(100),refund_reason BIGINT)");
        db.execute("CREATE TABLE app_goods(goods_id BIGINT PRIMARY KEY,goods_name VARCHAR(255),goods_type VARCHAR(50))");
        db.execute("CREATE TABLE app_goods_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR(100),user_id BIGINT,pay_status CHAR(1),order_origin VARCHAR(50),status CHAR(1),travel_status CHAR(1),travel_status_before_refund CHAR(1),update_time DATETIME)");
        db.execute("CREATE TABLE talent_center_workflow_audit(operation_id VARCHAR(36) PRIMARY KEY,actor_id VARCHAR(128),action_kind VARCHAR(50),input_hash VARCHAR(64),input_json TEXT,changes_json TEXT,result_json TEXT)");
        new ResourceDatabasePopulator(new FileSystemResource(Paths.get("..", "sql", "app_booking_adjustment.sql"))).execute(source);
        db.update("INSERT INTO app_activity VALUES (7,'1',NULL,'10','2')");
        db.execute("ALTER TABLE app_activity ADD COLUMN activity_name VARCHAR(100)");
        db.update("UPDATE app_activity SET activity_name='隔离测试活动'");
        db.update("INSERT INTO app_activity_order VALUES (1,5,7,2,'1','1',20.00,20.00,'3000000000000000000001')");
        db.update("INSERT INTO app_pay_log(user_id,order_id,order_type,pay_no,pay_money,pay_method,status) VALUES (5,1,'3','3000000000000000000001',2000,'wxpay','1')");
        gateway = mock(BookingAdjustmentGateway.class); gold = mock(IAppGoldService.class);
        ReflectionTestUtils.setField(gateway, "appId", "test-app"); ReflectionTestUtils.setField(gateway, "merchantId", "test-merchant");
        doCallRealMethod().when(gateway).validatePayment(anyMap(), any());
        refunds=mock(GoodsRefundGateway.class);
        DataSourceTransactionManager manager=new DataSourceTransactionManager(source);
        service = new BookingAdjustmentService(db, manager, gateway, gold,refunds);
        goodsOrders=mock(IAppGoodsOrderService.class);
        reviews=new GoodsRefundReviewService(new GoodsRefundStore(db,manager,service),refunds,goodsOrders,service);
        when(refunds.createAllocation(anyMap())).thenAnswer(call -> refund(call.getArgument(0), Status.PROCESSING));
    }

    @AfterEach void close() { db.execute("SHUTDOWN"); }

    @Test void pendingReservationsMatchExactPaymentReferenceAndBusinessType() {
        db.update("INSERT INTO app_activity_order VALUES (2,5,7,3,'0','0',0,30,'Pending-AbC')");
        // No payment, a different reference, another business, and a closed payment
        // must not reserve seats. In particular, casting must not truncate IDs.
        assertEquals(0, service.reservedSeats(7));
        db.update("INSERT INTO app_pay_log(order_id,order_type,pay_no,status) VALUES (2,'3','Pending-AbD','0'),(2,'3','Pending-abc','0'),(2,'3','Pending-AbC ','0'),(2,'2','Pending-AbC','0'),(2,'3','Pending-AbC','2')");
        assertEquals(0, service.reservedSeats(7));
        db.update("INSERT INTO app_pay_log(order_id,order_type,pay_no,status) VALUES (2,'3','Pending-AbC','0'),(2,'3','Pending-AbC','0')");
        assertEquals(3, service.reservedSeats(7));
        assertEquals(0, service.reservedSeats(7, 2L));
        db.update("UPDATE app_activity_order SET pay_status='1',status='1' WHERE order_id=2");
        assertEquals(0, service.reservedSeats(7));
    }

    @Test void activityApplicationsAreReadByTheOriginalAfterSalesMapperAndGoodsStayIsolated() throws Exception {
        Map<String,Object> request=service.submitActivity(1,5,1);
        assertEquals(1,count("app_goods_order_after"));
        db.update("INSERT INTO app_goods_order_after(order_id,user_id,goods_id,status) VALUES(1,5,9,'0')");
        org.apache.ibatis.session.Configuration config=new org.apache.ibatis.session.Configuration(new org.apache.ibatis.mapping.Environment("test",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),source));
        config.getTypeAliasRegistry().registerAlias("AppGoodsOrderAfter",com.ruoyi.system.domain.AppGoodsOrderAfter.class);
        try(java.io.InputStream xml=getClass().getResourceAsStream("/mapper/system/AppGoodsOrderAfterMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/system/AppGoodsOrderAfterMapper.xml",config.getSqlFragments()).parse();
        }
        try(org.apache.ibatis.session.SqlSession session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession()) {
            com.ruoyi.system.mapper.AppGoodsOrderAfterMapper mapper=session.getMapper(com.ruoyi.system.mapper.AppGoodsOrderAfterMapper.class);
            com.ruoyi.system.domain.AppGoodsOrderAfter activity=mapper.selectAppGoodsOrderAfterByAfterId(((Number)request.get("afterId")).longValue());
            assertEquals("3",activity.getBusinessType()); assertEquals("0",activity.getStatus()); assertEquals("隔离测试活动",activity.getGoodsName());
            com.ruoyi.system.domain.AppGoodsOrderAfter filter=new com.ruoyi.system.domain.AppGoodsOrderAfter();filter.setOrderId(1L);filter.setBusinessType("2");
            assertEquals(1,mapper.selectAppGoodsOrderAfterList(filter).size());
            assertNull(mapper.selectAppGoodsOrderAfterList(filter).get(0).getBookingAdjustmentId());
        }
        verifyNoInteractions(refunds,gateway);
    }
    @Test void existingTravelAndCourseRefundsStillUseTheirOriginalReviewAndGateway() {
        for(String type:Arrays.asList("hotel","education")) {
            long id="hotel".equals(type)?20:21;
            db.update("INSERT INTO app_goods VALUES(?,?,?)",id,"隔离测试"+type,type);
            db.update("INSERT INTO app_goods_order(order_id,order_no,user_id,pay_status,order_origin,status) VALUES(?,? ,5,'1','mini_program','3')",id,"TEST"+id);
            db.update("INSERT INTO app_goods_order_after(after_id,order_id,goods_id,user_id,out_order_no,status,after_type,app_refund_money,goods_money) VALUES(?,?,?,5,?,'0','2',1,1)",id,id,id,"TEST"+id);
            db.update("INSERT INTO app_pay_log(order_id,user_id,order_type,pay_no,pay_money,status,pay_method) VALUES(?,5,'2',?,100,'1','wxpay')",id,"TEST"+id);
            Refund evidence=new Refund();evidence.setOutRefundNo("YXHAF"+id);evidence.setOutTradeNo("TEST"+id);evidence.setRefundId("WX"+id);evidence.setStatus(Status.SUCCESS);
            Amount amount=new Amount();amount.setTotal(100L);amount.setRefund(100L);amount.setCurrency("CNY");evidence.setAmount(amount);
            doReturn(evidence).when(refunds).create(any(),any());
            doAnswer(call -> {com.ruoyi.system.domain.AppPayRefundLog log=call.getArgument(0);db.update("UPDATE app_goods_order_after SET status='6' WHERE order_id=? AND business_type='2'",log.getOrderId());return null;}).when(goodsOrders).completeReviewedRefund(any());
            com.ruoyi.system.domain.AppGoodsOrderAfter approval=new com.ruoyi.system.domain.AppGoodsOrderAfter();approval.setAfterId(id);approval.setStatus("1");approval.setRefundMoney(new BigDecimal("1.00"));approval.setRemark("隔离测试审核");approval.setUpdateBy("99");
            assertEquals(200,reviews.nativeReview(approval).get("code"));
            assertEquals("6",db.queryForObject("SELECT status FROM app_goods_order_after WHERE after_id=?",String.class,id));
        }
        verify(refunds,times(2)).create(any(),any());verify(refunds,never()).createAllocation(anyMap());
    }

    @Test void applyingNeverCallsWechatOrChangesConfirmedBooking() {
        Map<String, Object> row = service.submitActivity(1,5,1);
        assertEquals("REVIEW", row.get("state")); assertBooking(2,"20.00");
        assertEquals("1", db.queryForObject("SELECT pay_status FROM app_activity_order", String.class));
        assertEquals(0, count("app_pay_refund_log")); verifyNoInteractions(gateway, gold);
        assertEquals(row.get("adjustmentId"), service.submitActivity(1,5,1).get("adjustmentId"));
        assertThrows(ServiceException.class, () -> service.submitActivity(1,5,3));
    }
    @Test void rejectedRefundLeavesOriginalBookingAndCapacity() {
        long id = id(service.submitActivity(1,5,0));
        assertEquals("REJECTED", review(id,99,"reject","不符合取消条件").get("state"));
        assertBooking(2,"20.00"); assertEquals("2", db.queryForObject("SELECT sign_count FROM app_activity", String.class));
        assertEquals(0, count("app_pay_refund_log")); verifyNoInteractions(gateway, gold);
    }
    @Test void approvalIsCommittedBeforeWechatAndDoesNotPrematurelyReleaseSeats() {
        long id = id(service.submitActivity(1,5,1));
        doAnswer(call -> {
            assertEquals("PROCESSING", state(id)); assertEquals("backend:99", db.queryForObject("SELECT actor_id FROM talent_center_workflow_audit", String.class));
            assertEquals("1",db.queryForObject("SELECT status FROM app_goods_order_after",String.class));
            assertEquals(1,count("app_pay_refund_log")); assertBooking(2,"20.00");
            return refund(call.getArgument(0),Status.PROCESSING);
        }).when(refunds).createAllocation(anyMap());
        assertEquals("PROCESSING",review(id,99,"approve","核对通过").get("state"));
        assertBooking(2,"20.00"); assertThrows(TalentCenterApiException.class, () -> review(id,98,"approve","重复点击"));
        verify(refunds,times(1)).createAllocation(anyMap());
    }
    @Test void successfulRefundAppliesHeadcountExactlyOnceAndBadEvidenceDoesNothing() {
        long id = id(service.submitActivity(1,5,1)); review(id,99,"approve","核对通过");
        Map<String,Object> item = item(); Refund invalid = refund(item,Status.SUCCESS); invalid.getAmount().setRefund(999L);
        assertThrows(ServiceException.class, () -> service.recordRefund(invalid)); assertBooking(2,"20.00");
        Refund valid=refund(item,Status.SUCCESS); service.recordRefund(valid); service.recordRefund(valid);
        assertBooking(1,"10.00"); assertEquals("1",db.queryForObject("SELECT sign_count FROM app_activity",String.class));
        assertEquals("COMPLETED",state(id)); verify(gold,times(1)).reverseOnRefund(anyLong(),anyString(),anyLong(),any(),anyString());
    }
    @Test void timeoutKeepsApprovedIntentAndSyncReusesSameReference() {
        long id=id(service.submitActivity(1,5,0)); doThrow(new IllegalStateException("timeout")).when(refunds).createAllocation(anyMap());
        review(id,99,"approve","核对通过"); assertBooking(2,"20.00");
        Map<String,Object> item=item(); when(refunds.query(String.valueOf(item.get("out_refund_no")))).thenReturn(refund(item,Status.SUCCESS));
        review(id,99,"sync","核对到账"); assertEquals("COMPLETED",state(id));
        assertBooking(0,"0.00"); assertEquals("4",db.queryForObject("SELECT pay_status FROM app_activity_order",String.class));
        verify(refunds,times(1)).createAllocation(anyMap()); assertEquals(1,count("app_pay_refund_log"));
    }
    @Test void approvalOrRetryWithoutPendingRequestIsRejected() {
        long id=id(service.submitActivity(1,5,1));
        assertThrows(TalentCenterApiException.class, () -> review(id,99,"retry","未审核先退"));
        assertThrows(TalentCenterApiException.class, () -> review(id,99,"approve"," "));
        assertThrows(ServiceException.class, () -> service.dispatchApprovedRefunds(id,false));
        verifyNoInteractions(gateway,refunds); assertEquals(0,count("app_pay_refund_log"));
    }
    @Test void topupIsAnIntentAndCanBeCancelledWithoutSendingAnything() {
        long id=id(service.submitActivity(1,5,4)); assertBooking(2,"20.00"); assertEquals(2,service.reservedSeats(7));
        assertEquals("CANCELLED",service.syncPayment(id,5,true).get("state")); assertEquals(0,service.reservedSeats(7));
        assertBooking(2,"20.00"); verifyNoInteractions(gateway,gold);
    }
    @Test void topupOnlyAppliesOnVerifiedPaymentAndDuplicateCallbacksDoNotDoubleCount() {
        long id=id(service.submitActivity(1,5,3)); service.prepay(id,5); Transaction paid=payment(id);
        when(paid.getAmount().getTotal()).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.recordPayment(paid)); assertBooking(2,"20.00");
        when(paid.getAmount().getTotal()).thenReturn(1000);
        service.recordPayment(paid); service.recordPayment(paid);
        assertBooking(3,"30.00"); assertEquals(0,service.reservedSeats(7)); assertEquals("3",db.queryForObject("SELECT sign_count FROM app_activity",String.class));
        verify(gold,times(1)).grantOnPay(anyLong(),anyString(),anyLong(),any(),anyString());
    }
    @Test void cancelledOrFailedPaymentNeverModifiesOriginalBooking() {
        long id=id(service.submitActivity(1,5,3)); service.prepay(id,5);
        Transaction unpaid=payment(id); when(unpaid.getTradeState()).thenReturn(Transaction.TradeStateEnum.NOTPAY);
        when(gateway.queryPayment(anyString())).thenReturn(unpaid);
        assertEquals("PAYMENT",service.syncPayment(id,5,false).get("state")); assertBooking(2,"20.00");
        doThrow(new IllegalStateException("close timeout")).when(gateway).closePayment(anyString());
        assertThrows(IllegalStateException.class, () -> service.syncPayment(id,5,true)); assertEquals("PAYMENT",state(id));
        doNothing().when(gateway).closePayment(anyString()); service.syncPayment(id,5,true); assertBooking(2,"20.00");
    }
    @Test void cancellationOfAlreadyPaidTopupSynchronizesItInsteadOfDroppingMoney() {
        long id=id(service.submitActivity(1,5,3)); service.prepay(id,5); Transaction paid=payment(id); when(gateway.queryPayment(anyString())).thenReturn(paid);
        assertEquals("COMPLETED",service.syncPayment(id,5,true).get("state")); assertBooking(3,"30.00"); verify(gateway,never()).closePayment(anyString());
    }
    @Test void unknownPaymentResultNeverReleasesReservedSeats() {
        long id=id(service.submitActivity(1,5,3)); service.prepay(id,5);
        assertThrows(ServiceException.class, () -> service.syncPayment(id,5,true));
        assertEquals("PAYMENT",state(id)); assertEquals(1,service.reservedSeats(7)); assertBooking(2,"20.00");
        verify(gateway,never()).closePayment(anyString());
    }
    @Test void explicitlyMissingWechatOrderCanBeCancelledAndCannotBeRecreated() {
        long id=id(service.submitActivity(1,5,3)); service.prepay(id,5);
        doThrow(new com.wechat.pay.java.core.exception.ServiceException(null,404,"{\"code\":\"ORDER_NOT_EXIST\",\"message\":\"not found\"}"))
            .when(gateway).queryPayment(anyString());
        assertEquals("CANCELLED",service.syncPayment(id,5,true).get("state"));
        assertThrows(ServiceException.class, () -> service.prepay(id,5));
        assertEquals(0,service.reservedSeats(7)); assertBooking(2,"20.00");
        verify(gateway,times(1)).prepay(anyMap()); verify(gateway,never()).closePayment(anyString());
    }
    @Test void cancellingWaitsForInFlightWechatCreationBeforeClosing() throws Exception {
        long id=id(service.submitActivity(1,5,3));
        CountDownLatch creating=new CountDownLatch(1),release=new CountDownLatch(1);
        doAnswer(call -> {creating.countDown(); assertTrue(release.await(5,TimeUnit.SECONDS)); return null;}).when(gateway).prepay(anyMap());
        Transaction unpaid=payment(id); when(unpaid.getTradeState()).thenReturn(Transaction.TradeStateEnum.NOTPAY);
        when(gateway.queryPayment(anyString())).thenReturn(unpaid);
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            Future<?> creation=workers.submit(() -> service.prepay(id,5)); assertTrue(creating.await(5,TimeUnit.SECONDS));
            Future<?> cancellation=workers.submit(() -> service.syncPayment(id,5,true));
            assertThrows(TimeoutException.class, () -> cancellation.get(150,TimeUnit.MILLISECONDS));
            verify(gateway,never()).queryPayment(anyString()); release.countDown();
            creation.get(5,TimeUnit.SECONDS); cancellation.get(5,TimeUnit.SECONDS);
            assertEquals("CANCELLED",state(id)); assertBooking(2,"20.00"); verify(gateway).closePayment(anyString());
        } finally {release.countDown(); workers.shutdownNow();}
    }
    @Test void refundsAfterTopupAreSplitAcrossOriginalWechatTransactions() {
        long topup=id(service.submitActivity(1,5,3)); service.prepay(topup,5); service.recordPayment(payment(topup));
        long refund=id(service.submitActivity(1,5,1));
        doAnswer(call -> refund(call.getArgument(0),Status.SUCCESS)).when(refunds).createAllocation(anyMap());
        assertEquals("COMPLETED",review(refund,99,"approve","多笔付款原路退回").get("state"));
        assertEquals(2,count("app_pay_refund_log")); assertBooking(1,"10.00");
        assertEquals(2000,db.queryForObject("SELECT SUM(refund_money) FROM app_pay_refund_log",Integer.class));
    }
    @Test void splitRefundRecoveryPreviewShowsTheFullApprovedAmount() {
        long topup=id(service.submitActivity(1,5,3)); service.prepay(topup,5); service.recordPayment(payment(topup));
        Map<String,Object> request=service.submitActivity(1,5,1);
        doThrow(new IllegalStateException("timeout")).when(refunds).createAllocation(anyMap());
        review(id(request),99,"approve","核对完整差价");
        Map<String,Object> input=new LinkedHashMap<>();
        input.put("kind","retry-refund"); input.put("afterId",String.valueOf(request.get("afterId")));input.put("note","核对原退款");
        Map<String,Object> preview=reviews.preview(GoodsRefundRequest.from("backend:99",input));
        assertTrue(com.alibaba.fastjson2.JSON.toJSONString(preview.get("changes")).contains("20.00 元"));
        assertEquals(2,count("app_pay_refund_log")); assertBooking(3,"30.00");
    }
    @Test void cardRefundUsesActualReceiptAndDoesNotRevokeBenefitsBeforeApproval() {
        db.update("INSERT INTO app_user_card(record_id,user_id,status) VALUES(8,5,'1')");
        db.update("INSERT INTO app_pay_log(user_id,order_id,order_type,pay_no,pay_money,pay_method,status) VALUES (5,8,'1','1000000000000000000008',199,'wxpay','1')");
        Map<String,Object> row=service.submitCard(8,5); assertEquals(new BigDecimal("1.99"),row.get("amount"));
        assertEquals("1",db.queryForObject("SELECT status FROM app_user_card",String.class)); verifyNoInteractions(gateway);
        doAnswer(call -> refund(call.getArgument(0),Status.SUCCESS)).when(refunds).createAllocation(anyMap());
        review(id(row),99,"approve","核对会员卡退款"); assertEquals("3",db.queryForObject("SELECT status FROM app_user_card",String.class));
    }
    @Test void capacityDeadlineOwnershipAndFreeLimitsAreEnforcedOnTheServer() {
        assertThrows(ServiceException.class, () -> service.submitActivity(1,6,3));
        assertThrows(ServiceException.class, () -> service.submitActivity(1,5,11));
        db.update("UPDATE app_activity SET sign_end_time='2020-01-01 00:00:00'");
        assertThrows(ServiceException.class, () -> service.submitActivity(1,5,3));
        assertEquals("REVIEW",service.submitActivity(1,5,0).get("state"));
        assertThrows(ServiceException.class, () -> service.latest("3",1,6));
        db.update("UPDATE app_activity_order SET pay_money=0");
        assertThrows(ServiceException.class, () -> service.submitActivity(1,5,3));
    }
    @Test void simultaneousChangesCannotCreateTwoActiveIntents() throws Exception {
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results=workers.invokeAll(Arrays.asList(() -> submit(3), () -> submit(4)));
            int accepted=0; for(Future<Boolean> result:results) if(result.get(10,TimeUnit.SECONDS)) accepted++;
            assertEquals(1,accepted); assertEquals(1,count("app_booking_adjustment")); assertBooking(2,"20.00");
        } finally { workers.shutdownNow(); }
    }

    private Map<String,Object> review(long id,long reviewer,String decision,String note) {
        Map<String,Object> input=new LinkedHashMap<>();
        input.put("kind",Arrays.asList("sync","retry").contains(decision)?decision+"-refund":"review-refund");
        input.put("afterId",String.valueOf(db.queryForObject("SELECT after_id FROM app_goods_order_after WHERE booking_adjustment_id=?",Long.class,id)));
        input.put("note",note);
        if(Arrays.asList("approve","reject").contains(decision)) {
            input.put("decision",decision);input.put("refundAmount","approve".equals(decision)?db.queryForObject("SELECT app_refund_money FROM app_goods_order_after WHERE booking_adjustment_id=?",BigDecimal.class,id).toPlainString():null);
        }
        GoodsRefundRequest command=GoodsRefundRequest.from("backend:"+reviewer,input);
        reviews.execute(command.confirmation(UUID.randomUUID().toString(),reviews.preview(command).get("fingerprint")));
        Map<String,Object> row=db.queryForMap("SELECT business_type,order_id,user_id FROM app_booking_adjustment WHERE adjustment_id=?",id);
        return service.latest(String.valueOf(row.get("business_type")),((Number)row.get("order_id")).longValue(),((Number)row.get("user_id")).longValue());
    }
    private boolean submit(int target) { try {service.submitActivity(1,5,target);return true;} catch(ServiceException error){return false;} }
    private long id(Map<String,Object> row) { return ((Number)row.get("adjustmentId")).longValue(); }
    private int count(String table) { return db.queryForObject("SELECT COUNT(*) FROM " + table,Integer.class); }
    private String state(long id) { return db.queryForObject("SELECT state FROM app_booking_adjustment WHERE adjustment_id=?",String.class,id); }
    private Map<String,Object> item() { return db.queryForMap("SELECT * FROM app_booking_adjustment_refund"); }
    private void assertBooking(int people,String amount) {
        assertEquals(people,db.queryForObject("SELECT sign_count FROM app_activity_order",Integer.class));
        assertEquals(new BigDecimal(amount),db.queryForObject("SELECT pay_money FROM app_activity_order",BigDecimal.class));
    }
    private Transaction payment(long id) {
        Transaction value=mock(Transaction.class,RETURNS_DEEP_STUBS);
        when(value.getOutTradeNo()).thenReturn(db.queryForObject("SELECT pay_no FROM app_booking_adjustment WHERE adjustment_id=?",String.class,id));
        when(value.getAppid()).thenReturn("test-app"); when(value.getMchid()).thenReturn("test-merchant");
        when(value.getTradeState()).thenReturn(Transaction.TradeStateEnum.SUCCESS);
        when(value.getAmount().getCurrency()).thenReturn("CNY"); when(value.getAmount().getTotal()).thenReturn(1000);
        return value;
    }
    private Refund refund(Map<String,Object> item,Status status) {
        Refund value=new Refund(); value.setStatus(status); value.setOutTradeNo(String.valueOf(item.get("out_trade_no")));
        value.setOutRefundNo(String.valueOf(item.get("out_refund_no"))); value.setRefundId("WX"+item.get("item_id"));
        Amount amount=new Amount(); amount.setCurrency("CNY"); amount.setTotal(((Number)item.get("total_fen")).longValue());
        amount.setRefund(((Number)item.get("refund_fen")).longValue()); value.setAmount(amount); return value;
    }
}
