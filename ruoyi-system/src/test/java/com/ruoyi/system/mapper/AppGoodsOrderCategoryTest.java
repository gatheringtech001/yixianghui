package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AppGoodsOrder;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real MyBatis parameter binding, not textual SQL substitution. */
public class AppGoodsOrderCategoryTest {
    private Configuration config;
    private static final String NS = "com.ruoyi.system.mapper.AppGoodsOrderMapper.";

    @BeforeEach void mapper() throws Exception {
        config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("AppGoodsOrder", AppGoodsOrder.class);
        try (InputStream in = getClass().getResourceAsStream("/mapper/system/AppGoodsOrderMapper.xml")) {
            assertNotNull(in);
            new XMLMapperBuilder(in, config, "orders", config.getSqlFragments()).parse();
        }
    }
    private BoundSql list(AppGoodsOrder query) {
        return config.getMappedStatement(NS + "selectAppGoodsOrderList").getBoundSql(query);
    }
    @Test void allPreservesUnlinkedOrdersAndDoesNotInferTypes() {
        String sql = list(new AppGoodsOrder()).getSql();
        assertTrue(sql.contains("LEFT JOIN app_goods goods"));
        assertTrue(sql.contains("LEFT JOIN app_goods_category category"));
        assertFalse(sql.contains("goods.goods_type ="));
        assertFalse(sql.contains("travel_status is not null"));
        assertFalse(sql.contains("find_in_set"));
    }
    @Test void listingTypeCategoryAndUnpaidStateAreBoundTogether() {
        AppGoodsOrder query = new AppGoodsOrder();
        query.setGoodsType("education"); query.setCategoryId(10L); query.setPayStatus("0");
        BoundSql sql = list(query);
        assertTrue(sql.getSql().contains("goods.goods_type = ?"));
        assertTrue(sql.getSql().contains("goods.category_id = ?"));
        assertTrue(sql.getSql().contains("find_in_set(?, parent_ids)"));
        List<String> properties = sql.getParameterMappings().stream().map(p -> p.getProperty()).collect(Collectors.toList());
        assertEquals(java.util.Arrays.asList("goodsType", "categoryId", "categoryId", "payStatus"), properties);
    }
    @Test void unknownTypesAreExactFiltersAndLegacyClientsStillWork() {
        AppGoodsOrder query = new AppGoodsOrder(); query.setGoodsType("future-type");
        assertTrue(list(query).getSql().contains("goods.goods_type = ?"));
        query.setGoodsType(""); query.setBusinessType("travel");
        assertTrue(list(query).getSql().contains("travel_status is not null"));
        query.setBusinessType("education");
        assertTrue(list(query).getSql().contains("goods.goods_type = 'education'"));
    }
    @Test void categoryFieldsAreNeverWrittenToOrders() {
        AppGoodsOrder query = new AppGoodsOrder(); query.setOrderId(1L); query.setGoodsId(1L);
        query.setCategoryId(10L); query.setCategoryName("Synthetic");
        for (String id : new String[]{"insertAppGoodsOrder", "updateAppGoodsOrder"}) {
            String sql = config.getMappedStatement(NS + id).getBoundSql(query).getSql();
            assertFalse(sql.contains("category_id")); assertFalse(sql.contains("category_name"));
        }
    }
    public static boolean ancestor(Long id, String path) {
        return path != null && java.util.Arrays.asList(path.split(",")).contains(String.valueOf(id));
    }
    @Test void realMapperSeparatesAllListingTypesAndIncludesCategoryDescendantsWithoutDuplicates() throws Exception {
        org.h2.jdbcx.JdbcDataSource source = new org.h2.jdbcx.JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setEnvironment(new org.apache.ibatis.mapping.Environment("test", new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), source));
        try (java.sql.Connection c = source.getConnection(); java.sql.Statement s = c.createStatement()) {
            // Build all order columns from the real result map, without private production data.
            String columns = config.getResultMap(NS + "AppGoodsOrderResult").getResultMappings().stream()
                .map(m -> m.getColumn() + " varchar(200)").distinct().collect(Collectors.joining(","));
            s.execute("create table app_goods_order(" + columns + ")");
            s.execute("create table sys_dept(dept_id bigint,dept_name varchar(100))");
            s.execute("create table app_goods(goods_id bigint,goods_name varchar(100),goods_type varchar(40),category_id bigint)");
            s.execute("create table app_goods_category(category_id bigint primary key,category_name varchar(100),parent_ids varchar(200))");
            s.execute("create table app_goods_order_detail(order_id bigint,goods_count bigint,self_goods_count bigint,inter_count bigint,sku_id bigint)");
            s.execute("create table app_goods_sku(sku_id bigint,sku_name varchar(100))");
            s.execute("create alias find_in_set for 'com.ruoyi.system.mapper.AppGoodsOrderCategoryTest.ancestor'");
            s.execute("insert into app_goods_category values (10,'Parent','0'),(11,'Child','0,10'),(12,'Training','0,10'),(20,'Travel','0')");
            s.execute("insert into app_goods values (1,'Education','education',11),(2,'Training','course',12),(3,'Travel','hotel',20),(4,'No category','education',null)");
            s.execute("insert into app_goods_order(order_id,goods_id,pay_status,travel_status) values ('1','1','0',null),('2','2','1',null),('3','3','0',null),('4','999','0','1'),('5','1','1',null),('6','4','0',null)");
            s.execute("insert into app_goods_order_detail values (1,1,1,1,1),(1,1,1,1,2)");
        }
        try (org.apache.ibatis.session.SqlSession session = new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession()) {
            AppGoodsOrder query = new AppGoodsOrder();
            List<AppGoodsOrder> all = session.selectList(NS + "selectAppGoodsOrderList", query);
            assertEquals(6, all.size());
            assertEquals(6, all.stream().map(AppGoodsOrder::getOrderId).distinct().count());
            query.setGoodsType("hotel");
            List<AppGoodsOrder> travel = session.selectList(NS + "selectAppGoodsOrderList", query);
            assertEquals(1, travel.size()); assertEquals(Long.valueOf(3), travel.get(0).getOrderId());
            query.setGoodsType("course");
            assertEquals(1, session.selectList(NS + "selectAppGoodsOrderList", query).size());
            query.setGoodsType(null); query.setCategoryId(10L);
            assertEquals(3, session.selectList(NS + "selectAppGoodsOrderList", query).size());
            query.setGoodsType("education"); query.setPayStatus("0");
            List<AppGoodsOrder> unpaid = session.selectList(NS + "selectAppGoodsOrderList", query);
            assertEquals(1, unpaid.size()); assertEquals("Child", unpaid.get(0).getCategoryName());
            query.setCategoryId(11L);
            assertEquals(1, session.selectList(NS + "selectAppGoodsOrderList", query).size());
            query.setCategoryId(999L);
            assertTrue(session.selectList(NS + "selectAppGoodsOrderList", query).isEmpty());
            query.setCategoryId(null); query.setGoodsType("unknown");
            assertTrue(session.selectList(NS + "selectAppGoodsOrderList", query).isEmpty());
        }
    }

}
