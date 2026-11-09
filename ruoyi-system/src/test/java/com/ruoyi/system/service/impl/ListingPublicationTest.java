package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoodsOrderAfter;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class ListingPublicationTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private AppListingPublicationService service;
    private final List<String> evicted = new ArrayList<>();

    @BeforeEach void setup() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        service = new AppListingPublicationService();
        setField("jdbc", jdbc);
        setField("cache", new RedisCache() { @Override public boolean deleteObject(String key) { evicted.add(key); return true; } });
        jdbc.execute("create table app_goods(goods_id bigint primary key,goods_name varchar,goods_cover varchar,dept_id bigint,category_id bigint,price decimal,stock bigint,is_sku int,goods_type varchar,status varchar,update_time timestamp,content varchar)");
        jdbc.execute("create table app_goods_sku(goods_id bigint,status varchar,price decimal,stock int,sku_id bigint,sku_type varchar,par_sku_id varchar)");
        jdbc.execute("create table app_goods_education_ext(goods_id bigint,class_size_max int)");
        jdbc.execute("create table app_goods_sku_option(goods_id bigint,sku_id bigint,option_type varchar,option_value varchar)");
        jdbc.execute("create table app_goods_related(goods_id bigint,content varchar)");
        jdbc.execute("create table app_activity(activity_id bigint primary key,activity_name varchar,activity_cover varchar,address varchar,activity_time varchar,dept_id bigint,is_free int,vip_price decimal,sign_end_time timestamp,activity_end_time timestamp,max_count varchar,sign_count varchar,status varchar,update_time timestamp)");
        jdbc.update("insert into app_goods values(1,'合成课程','/cover.jpg',105,63,250,10,0,'education','0',null,'原正文')");
        jdbc.update("insert into app_goods_related values(1,'原特色')");
        jdbc.update("insert into app_activity values(2,'合成活动','/cover.jpg','测试地点','周末',105,0,50,null,null,'10','0','0',null)");
    }
    private void setField(String name, Object value) throws Exception {
        Field field = AppListingPublicationService.class.getDeclaredField(name); field.setAccessible(true); field.set(service, value);
    }

    @Test void publicationAndUnpublicationPreserveAllContentAndStock() {
        assertEquals(1, tx.execute(s -> service.changeGoods(1L, "0", "1")));
        assertEquals("1", jdbc.queryForObject("select status from app_goods where goods_id=1", String.class));
        assertEquals("原正文", jdbc.queryForObject("select content from app_goods where goods_id=1", String.class));
        assertEquals("原特色", jdbc.queryForObject("select content from app_goods_related where goods_id=1", String.class));
        assertEquals(10, jdbc.queryForObject("select stock from app_goods where goods_id=1", Integer.class));
        assertEquals(1, evicted.size());
        assertEquals(1, tx.execute(s -> service.changeGoods(1L, "1", "0")));
        assertEquals("0", jdbc.queryForObject("select status from app_goods where goods_id=1", String.class));
    }

    @Test void staleRequestAndInvalidStatusNeverWrite() {
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(1L, "1", "0")));
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(1L, "0", "6")));
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(999L, "0", "1")));
        assertEquals("0", jdbc.queryForObject("select status from app_goods where goods_id=1", String.class));
        assertTrue(evicted.isEmpty());
    }

    @Test void unavailableGoodsCannotBePublishedButCanAlwaysBeTakenDown() {
        jdbc.update("update app_goods set stock=0");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(1L, "0", "1")));
        jdbc.update("update app_goods set status='1',goods_cover=''");
        assertEquals(1, tx.execute(s -> service.changeGoods(1L, "1", "0")));
    }

    @Test void multispecRequiresAnActivePricedStockedSku() {
        jdbc.update("update app_goods set is_sku=1,goods_type='travel'");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(1L, "0", "1")));
        jdbc.update("insert into app_goods_sku(goods_id,status,price,stock) values(1,'1',100,2)");
        assertEquals(1, tx.execute(s -> service.changeGoods(1L, "0", "1")));
    }

    @Test void paidAndExpiredOrFullActivitiesAreCheckedBeforePublication() {
        jdbc.update("update app_activity set vip_price=0");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeActivity(2L, "0", "1")));
        jdbc.update("update app_activity set vip_price=50,activity_end_time='2020-01-01 10:00:00'");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeActivity(2L, "0", "1")));
        jdbc.update("update app_activity set activity_end_time=null,sign_count='10'");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeActivity(2L, "0", "1")));
        jdbc.update("update app_activity set max_count='0'");
        assertEquals(1, tx.execute(s -> service.changeActivity(2L, "0", "1")));
    }

    @Test void travelOptionPricesDoNotRequireSkuStockAndDisabledParentsNeverQualify() {
        jdbc.update("update app_goods set goods_type='hotel',is_sku=1");
        jdbc.update("insert into app_goods_sku values(1,'1',0,0,10,'200','0')");
        jdbc.update("insert into app_goods_sku values(1,'1',0,0,11,'202','10')");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(1L, "0", "1")));
        jdbc.update("insert into app_goods_sku_option values(1,11,'302','745')");
        assertEquals(1, tx.execute(s -> service.changeGoods(1L, "0", "1")));
        jdbc.update("update app_goods set status='0'");
        jdbc.update("update app_goods_sku set status='0' where sku_id=10");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.changeGoods(1L, "0", "1")));
    }

    @Test void explicitlyUnlimitedCoursesDoNotNeedFabricatedStock() {
        jdbc.update("update app_goods set stock=0");
        jdbc.update("insert into app_goods_education_ext values(1,0)");
        assertEquals(1, tx.execute(s -> service.changeGoods(1L, "0", "1")));
        assertEquals(0, jdbc.queryForObject("select stock from app_goods where goods_id=1", Integer.class));
    }

    @Test void rolledBackPublicationDoesNotInvalidateCacheOrPersistStatus() {
        assertThrows(ServiceException.class, () -> tx.execute(s -> { service.changeGoods(1L, "0", "1"); throw new ServiceException("rollback"); }));
        assertEquals("0", jdbc.queryForObject("select status from app_goods where goods_id=1", String.class));
        assertTrue(evicted.isEmpty());
    }

    @Test void reviewQueuesAreFilteredAndDeterministicallyOrderedBeforePagination() throws Exception {
        String path = System.getProperty("review.mapper", "src/main/resources/mapper/system/AppGoodsOrderAfterMapper.xml");
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("AppGoodsOrderAfter", AppGoodsOrderAfter.class);
        try (FileInputStream input = new FileInputStream(path)) {
            new XMLMapperBuilder(input, config, path, config.getSqlFragments()).parse();
        }
        AppGoodsOrderAfter filter = new AppGoodsOrderAfter(); filter.setStatus("0");
        String statement = "com.ruoyi.system.mapper.AppGoodsOrderAfterMapper.selectAppGoodsOrderAfterList";
        String pending = config.getMappedStatement(statement).getBoundSql(filter).getSql();
        assertTrue(pending.contains("status = ?")); assertTrue(pending.contains("order by after_id asc"));
        filter.setStatus(null); filter.getParams().put("reviewBucket", "processed");
        String processed = config.getMappedStatement(statement).getBoundSql(filter).getSql();
        assertTrue(processed.contains("status in ('1', '2', '6')")); assertTrue(processed.contains("order by after_id desc"));
    }
}
