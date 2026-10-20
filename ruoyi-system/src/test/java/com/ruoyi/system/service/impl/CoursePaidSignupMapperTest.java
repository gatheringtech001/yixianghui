package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.AppGoods;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoursePaidSignupMapperTest {
    @Test void countsPaidSeatsNotPendingCancelledRefundedOrOtherCourses() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), source));
        config.getTypeAliasRegistry().registerAlias("AppGoods", AppGoods.class);
        try (InputStream xml = getClass().getResourceAsStream("/mapper/system/AppGoodsMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/system/AppGoodsMapper.xml", config.getSqlFragments()).parse();
        }
        // Build fixture columns from the actual list projection so the real mapper is executed.
        String projection = config.getMappedStatement("com.ruoyi.system.mapper.AppGoodsMapper.selectAppGoodsList")
                .getBoundSql(new AppGoods()).getSql();
        String columnList = projection.substring(projection.indexOf("select ") + 7, projection.indexOf(", case"));
        try (Connection c = source.getConnection(); Statement s = c.createStatement()) {
            StringBuilder ddl = new StringBuilder("create table app_goods (");
            for (String column : columnList.split(",")) {
                String name = column.trim();
                if (!name.startsWith("g.")) continue;
                name = name.substring(2);
                ddl.append(name).append(name.equals("goods_id") || name.equals("dept_id") ? " bigint," : " varchar(200),");
            }
            s.execute(ddl.substring(0, ddl.length() - 1) + ")");
            s.execute("create table sys_dept(dept_id bigint,dept_name varchar(100))");
            s.execute("create table app_goods_order(goods_id bigint,goods_count bigint,pay_status varchar(1),status varchar(1))");
            s.execute("insert into app_goods(goods_id,goods_type,sale_count) values (1,'education','99'),(2,'education','0'),(3,'online','15')");
            s.execute("insert into app_goods_order values (1,2,'1','1'),(1,3,'1','3'),(1,9,'0','0'),(1,8,'1','2'),(1,7,'4','4'),(1,6,'3','1'),(3,10,'1','1')");
        }
        try (SqlSession session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            List<AppGoods> rows = session.selectList("com.ruoyi.system.mapper.AppGoodsMapper.selectAppGoodsList", new AppGoods());
            AppGoods course = rows.stream().filter(g -> g.getGoodsId() == 1L).findFirst().get();
            assertEquals(5L, course.getPaidSignupCount());
            assertEquals(99L, course.getSaleCount());
            assertEquals(0L, rows.stream().filter(g -> g.getGoodsId() == 2L).findFirst().get().getPaidSignupCount());
            assertNull(rows.stream().filter(g -> g.getGoodsId() == 3L).findFirst().get().getPaidSignupCount());
        }
    }
}
