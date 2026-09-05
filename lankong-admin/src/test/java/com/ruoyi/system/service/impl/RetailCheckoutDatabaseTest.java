package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.RetailCheckout.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IAppPayLogService;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Opt-in: run bash scripts/e2e/retail-db.sh first. No production connections. */
class RetailCheckoutDatabaseTest {
    static JdbcTemplate jdbc;
    static TransactionTemplate tx;
    static SqlSessionTemplate sql;
    RetailCheckoutService checkout;
    RetailOrderStore store;
    SupplierFulfillmentService fulfillment;

    @BeforeAll static void database() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("RETAIL_DB_TEST")));
        DataSource ds = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/yixianghui_retail_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai", "root", "");
        jdbc = new JdbcTemplate(ds); tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean(); factory.setDataSource(ds);
        factory.setTypeAliasesPackage("com.ruoyi.system.domain;com.ruoyi.common.core.domain.entity");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<org.springframework.core.io.Resource> resources = new ArrayList<>();
        for (String name : Arrays.asList("AppGoods","AppGoodsCart","AppGoodsOrder","AppGoodsOrderDetail","AppUserAddress","AppGoodsCoupon","AppGoodsCouponGot")) {
            resources.add(resolver.getResource("classpath:mapper/system/"+name+"Mapper.xml"));
        }
        factory.setMapperLocations(resources.toArray(new org.springframework.core.io.Resource[0]));
        sql = new SqlSessionTemplate(factory.getObject());
    }
    @BeforeEach void setup() {
        // This class writes only the explicitly named isolated test database.
        for (String table : Arrays.asList("app_supplier_order","app_goods_order_detail","app_goods_order","app_goods_cart","app_supplier_goods","app_goods_coupon_got","app_goods_coupon","app_user_address","app_goods","app_supplier","sys_user")) jdbc.update("DELETE FROM "+table);
        jdbc.update("INSERT INTO sys_user(user_id,user_name,nick_name) VALUES(7,'retail-test','测试账号')");
        jdbc.update("INSERT INTO app_supplier(supplier_id,supplier_name,supplier_code,status) VALUES(1,'云野集','YUNYE','1'),(2,'测试其他供应商','OTHER','1')");
        jdbc.update("INSERT INTO app_goods(goods_id,dept_id,category_id,goods_name,goods_type,price,vip_price,stock,is_sku,status,express_fee) VALUES(1,100,83,'测试玉米','online',29.90,1,10,0,'1',5),(2,100,83,'测试茶叶','online',60,1,10,0,'1',0)");
        jdbc.update("INSERT INTO app_supplier_goods(goods_id,supplier_id) VALUES(1,1),(2,1)");
        jdbc.update("INSERT INTO app_goods_cart(cart_id,user_id,goods_id,goods_count,is_sku,data_id,status) VALUES(1,7,1,2,0,0,'1'),(2,7,2,1,0,0,'1'),(3,99,1,1,0,0,'1')");
        jdbc.update("INSERT INTO app_user_address(address_id,user_id,link_person,link_mobile,address_detail,is_default) VALUES(1,7,'测试收货人','00000000000','测试地址',1),(2,99,'其他测试用户','00000000000','测试地址',1)");
        store = new RetailOrderStore(); set(store,"jdbc",jdbc); set(store,"orders",mapper(AppGoodsOrderMapper.class));
        RetailCouponService coupons = new RetailCouponService(); set(coupons,"gotMapper",mapper(AppGoodsCouponGotMapper.class)); set(coupons,"couponMapper",mapper(AppGoodsCouponMapper.class));
        checkout = new RetailCheckoutService(); set(checkout,"goodsMapper",mapper(AppGoodsMapper.class)); set(checkout,"cartMapper",mapper(AppGoodsCartMapper.class));
        set(checkout,"addressMapper",mapper(AppUserAddressMapper.class)); set(checkout,"orderMapper",mapper(AppGoodsOrderMapper.class)); set(checkout,"detailMapper",mapper(AppGoodsOrderDetailMapper.class));
        set(checkout,"coupons",coupons); set(checkout,"store",store);
        fulfillment = new SupplierFulfillmentService(); set(fulfillment,"jdbc",jdbc); set(fulfillment,"enabled",false); set(fulfillment,"webhook","");
    }
    @AfterAll static void uiFixture() {
        if (!"1".equals(System.getenv("RETAIL_UI_FIXTURE")) || jdbc == null) return;
        new RetailCheckoutDatabaseTest().setup();
        jdbc.update("INSERT INTO sys_user(user_id,user_name,nick_name,password,status,del_flag) VALUES(1,'retail-admin','本地测试管理员',?,'0','0')",
                com.ruoyi.common.utils.SecurityUtils.encryptPassword("Retail-Test-2026"));
        jdbc.update("UPDATE app_goods_cart SET user_id=1 WHERE user_id=7");
        jdbc.update("UPDATE app_user_address SET user_id=1 WHERE user_id=7");
        jdbc.update("INSERT INTO sys_config(config_id,config_name,config_key,config_value,config_type) VALUES(9001,'测试关闭验证码','sys.account.captchaEnabled','false','N') ON DUPLICATE KEY UPDATE config_value='false'");
        jdbc.update("INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,visible,status,perms) VALUES(9001,'供应商管理',0,1,'retail',NULL,'M','0','0',''),(9002,'供应商',9001,1,'supplier','system/app_supplier/index','C','0','0','system:app_supplier:list') ON DUPLICATE KEY UPDATE status='0'");
    }
    @Test void multiItemOrderHasCorrectAmountsSnapshotsAndCartCleanup() {
        Request r = request(); AppGoodsOrder order = submit(r);
        assertEquals(new BigDecimal("124.80"),order.getMoneyPayable());
        assertEquals(3L,order.getGoodsCount()); assertEquals(2,order.getGoodsList().size());
        assertEquals(2L,order.getGoodsList().get(0).getOrderQuantity());
        assertEquals(8,stock(1)); assertEquals(9,stock(2)); assertEquals(1,count("app_goods_cart"));
        assertEquals(2,count("app_goods_order_detail"));
        jdbc.update("UPDATE app_goods SET goods_name='已改名',price=200 WHERE goods_id=1");
        jdbc.update("UPDATE app_user_address SET address_detail='已修改' WHERE address_id=1");
        AppGoodsOrder read = store.get(order.getOrderId());
        assertEquals("测试玉米",read.getGoodsList().get(0).getGoodsName());
        assertEquals("测试地址",read.getAddressInfo().getAddressDetail());
    }
    @Test void retryReturnsOneOrderAndDoesNotReserveTwice() {
        Request r=request(); AppGoodsOrder first=submit(r); AppGoodsOrder second=submit(r);
        assertEquals(first.getOrderId(),second.getOrderId()); assertEquals(1,count("app_goods_order")); assertEquals(8,stock(1));
    }
    @Test void concurrentSameKeyCreatesOnlyOneOrder() throws Exception {
        Request r=request(); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<AppGoodsOrder> a=pool.submit(()->submit(r)),b=pool.submit(()->submit(r));
            assertEquals(a.get(15,TimeUnit.SECONDS).getOrderId(),b.get(15,TimeUnit.SECONDS).getOrderId());
            assertEquals(1,count("app_goods_order")); assertEquals(8,stock(1));
        } finally {pool.shutdownNow();}
    }
    @Test void anotherUsersCartAndAddressAreRejected() {
        Request r=request(); r.setCartIds(Arrays.asList(3L)); assertThrows(ServiceException.class,()->checkout.quote(7L,r));
        Request otherAddress=request(); otherAddress.setAddressId(2L); assertThrows(ServiceException.class,()->submit(otherAddress));
        assertEquals(0,count("app_goods_order")); assertEquals(10,stock(1));
    }
    @Test void priceChangeAndInsufficientStockDoNotCreatePartialOrders() {
        Request r=request(); jdbc.update("UPDATE app_goods SET price=30 WHERE goods_id=1");
        assertThrows(ServiceException.class,()->submit(r)); assertEquals(10,stock(1)); assertEquals(0,count("app_goods_order"));
        Request fresh=request(); jdbc.update("UPDATE app_goods SET stock=0 WHERE goods_id=2");
        assertThrows(ServiceException.class,()->submit(fresh)); assertEquals(10,stock(1)); assertEquals(3,count("app_goods_cart"));
    }
    @Test void failureAfterReservationRollsBackStockOrdersAndCart() {
        AppGoodsOrderDetailMapper broken=mock(AppGoodsOrderDetailMapper.class); set(checkout,"detailMapper",broken);
        assertThrows(ServiceException.class,()->submit(request()));
        assertEquals(10,stock(1)); assertEquals(10,stock(2)); assertEquals(0,count("app_goods_order")); assertEquals(3,count("app_goods_cart"));
    }
    @Test void supplierMismatchCannotBeSubmitted() {
        jdbc.update("UPDATE app_supplier_goods SET supplier_id=2 WHERE goods_id=2");
        assertThrows(ServiceException.class,()->checkout.quote(7L,baseRequest()));
    }
    @Test void couponIsAllocatedConsumedOnceAndShippingStaysPayable() {
        jdbc.update("INSERT INTO app_goods_coupon(coupon_id,coupon_name,goods_id,min_price,discount_type,discount_price,status) VALUES(1,'测试玉米券',1,0,'1',10,'1')");
        jdbc.update("INSERT INTO app_goods_coupon_got(got_id,user_id,coupon_id,is_used,status) VALUES(1,7,1,0,'1')");
        Request r=baseRequest();r.setCouponGotId(1L);r.setFingerprint(checkout.quote(7L,r).getFingerprint());
        AppGoodsOrder order=submit(r);assertEquals(new BigDecimal("114.80"),order.getMoneyPayable());
        assertEquals(new BigDecimal("10.00"),jdbc.queryForObject("SELECT SUM(discount_money) FROM app_goods_order_detail",BigDecimal.class));
        assertEquals(1,jdbc.queryForObject("SELECT is_used FROM app_goods_coupon_got WHERE got_id=1",Integer.class));
        assertEquals(order.getOrderId(),submit(r).getOrderId());
    }
    @Test void notificationsAreOffByDefaultAndUnpaidCannotShip() {
        AppGoodsOrder order=submit(request()); fulfillment.dispatchPending();
        assertEquals(0,jdbc.queryForObject("SELECT attempts FROM app_supplier_order",Integer.class));
        assertThrows(ServiceException.class,()->tx.execute(s->{fulfillment.confirm(order.getOrderId());return null;}));
        jdbc.update("UPDATE app_goods_order SET pay_status='1',status='1' WHERE order_id=?",order.getOrderId());
        jdbc.update("UPDATE app_goods_order SET is_apply_cancel=1 WHERE order_id=?",order.getOrderId());
        assertThrows(ServiceException.class,()->tx.execute(s->{fulfillment.confirm(order.getOrderId());return null;}));
        assertThrows(ServiceException.class,()->fulfillment.exportRows(Collections.singletonList(order.getOrderId())));
        jdbc.update("UPDATE app_goods_order SET is_apply_cancel=0 WHERE order_id=?",order.getOrderId());
        tx.execute(s->{fulfillment.confirm(order.getOrderId());return null;});
        assertEquals(2,fulfillment.exportRows(Collections.singletonList(order.getOrderId())).size());
        assertEquals("confirmed",jdbc.queryForObject("SELECT notice_status FROM app_supplier_order",String.class));
        assertNull(jdbc.queryForObject("SELECT send_express_no FROM app_goods_order",String.class));
        Map<String,String> shipping=new HashMap<>();shipping.put("expressName","测试快递");shipping.put("expressNo","TEST12345");
        tx.execute(s->{fulfillment.ship(order.getOrderId(),shipping);return null;});
        assertEquals("TEST12345",jdbc.queryForObject("SELECT send_express_no FROM app_goods_order",String.class));
        assertThrows(ServiceException.class,()->tx.execute(s->{fulfillment.ship(order.getOrderId(),shipping);return null;}));
    }
    @Test void timeoutReleasesAllItemsOnlyOnce() {
        AppGoodsOrder order=submit(request());jdbc.update("UPDATE app_goods_order SET create_time=DATE_SUB(NOW(),INTERVAL 31 MINUTE) WHERE order_id=?",order.getOrderId());
        AppGoodsOrderServiceImpl service=new AppGoodsOrderServiceImpl();
        set(service,"appGoodsOrderMapper",mapper(AppGoodsOrderMapper.class));set(service,"appGoodsMapper",mapper(AppGoodsMapper.class));
        set(service,"orderDetailMapper",mapper(AppGoodsOrderDetailMapper.class));set(service,"payLogService",mock(IAppPayLogService.class));
        set(service,"couponGotMapper",mapper(AppGoodsCouponGotMapper.class));set(service,"couponMapper",mapper(AppGoodsCouponMapper.class));
        assertEquals(1,tx.execute(s->service.closeExpiredUnpaidOrders(30)).intValue());
        assertEquals(10,stock(1));assertEquals(10,stock(2));
        assertEquals(0,tx.execute(s->service.closeExpiredUnpaidOrders(30)).intValue());
        assertEquals(10,stock(1));
    }
    @Test void successfulNotificationIsNotSentAgainAndDoesNotMarkShipped() {
        AppGoodsOrder order=submit(request());
        jdbc.update("UPDATE app_goods_order SET pay_status='1',status='1' WHERE order_id=?",order.getOrderId());
        org.springframework.web.client.RestTemplate client=mock(org.springframework.web.client.RestTemplate.class);
        when(client.postForObject(anyString(),any(),eq(String.class))).thenReturn("{\"errcode\":0}");
        set(fulfillment,"client",client);set(fulfillment,"enabled",true);set(fulfillment,"webhook","https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=local-test-only");
        fulfillment.dispatchPending();fulfillment.dispatchPending();
        verify(client,times(1)).postForObject(anyString(),any(),eq(String.class));
        assertEquals("sent",jdbc.queryForObject("SELECT notice_status FROM app_supplier_order",String.class));
        assertNull(jdbc.queryForObject("SELECT send_express_no FROM app_goods_order",String.class));
    }
    @Test void uncertainNotificationNeedsExplicitRetryAndCancelledOrdersAreExcluded() {
        AppGoodsOrder order=submit(request());
        jdbc.update("UPDATE app_goods_order SET pay_status='1',status='1' WHERE order_id=?",order.getOrderId());
        org.springframework.web.client.RestTemplate client=mock(org.springframework.web.client.RestTemplate.class);
        when(client.postForObject(anyString(),any(),eq(String.class))).thenThrow(new org.springframework.web.client.ResourceAccessException("test timeout"));
        set(fulfillment,"client",client);set(fulfillment,"enabled",true);set(fulfillment,"webhook","https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=local-test-only");
        fulfillment.dispatchPending();fulfillment.dispatchPending();
        verify(client,times(1)).postForObject(anyString(),any(),eq(String.class));
        assertEquals("uncertain",jdbc.queryForObject("SELECT notice_status FROM app_supplier_order",String.class));
        fulfillment.retry(order.getOrderId());
        jdbc.update("UPDATE app_goods_order SET status='3' WHERE order_id=?",order.getOrderId());
        fulfillment.dispatchPending();verify(client,times(1)).postForObject(anyString(),any(),eq(String.class));
    }
    Request baseRequest() { Request r=new Request();r.setCartIds(Arrays.asList(1L,2L));r.setAddressId(1L);r.setCheckoutKey(UUID.randomUUID().toString());return r; }
    Request request() { Request r=baseRequest();r.setFingerprint(checkout.quote(7L,r).getFingerprint());return r; }
    AppGoodsOrder submit(Request r) { return tx.execute(s->checkout.submit(7L,r)); }
    int stock(long id) {return jdbc.queryForObject("SELECT stock FROM app_goods WHERE goods_id=?",Integer.class,id);}
    int count(String table) {return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    static <T>T mapper(Class<T> type) {return sql.getMapper(type);}
    static void set(Object target,String field,Object value) {ReflectionTestUtils.setField(target,field,value);}
}
