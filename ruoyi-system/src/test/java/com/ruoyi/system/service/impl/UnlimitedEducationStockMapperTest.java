package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.mapper.AppGoodsMapper;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Execute the actual mapper against isolated MySQL-mode tables, never production orders. */
class UnlimitedEducationStockMapperTest {
    private SqlSessionFactory factory;

    @BeforeEach void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection c = source.getConnection(); Statement s = c.createStatement()) {
            s.execute("create table app_goods(goods_id bigint primary key, goods_type varchar(20), stock bigint, sale_count bigint, update_time timestamp)");
            s.execute("create table app_goods_education_ext(goods_id bigint primary key, class_size_max int)");
            s.execute("insert into app_goods(goods_id,goods_type,stock,sale_count) values (1,'education',0,6),(2,'education',2,4),(3,'education',null,null),(4,'online',0,0),(5,'education',0,0)");
            s.execute("insert into app_goods_education_ext values (1,0),(2,6),(3,0),(4,0),(5,null)");
        }
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), source));
        config.getTypeAliasRegistry().registerAlias("AppGoods", AppGoods.class);
        try (InputStream xml = getClass().getResourceAsStream("/mapper/system/AppGoodsMapper.xml")) {
            assertNotNull(xml);
            new XMLMapperBuilder(xml, config, "mapper/system/AppGoodsMapper.xml", config.getSqlFragments()).parse();
        }
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    private void totals(SqlSession session, long id, Long stock, long sales) throws Exception {
        try (Statement s = session.getConnection().createStatement(); ResultSet r = s.executeQuery("select stock,sale_count from app_goods where goods_id=" + id)) {
            assertTrue(r.next());
            assertEquals(stock, r.getObject(1) == null ? null : Long.valueOf(r.getLong(1)));
            assertEquals(sales, r.getLong(2));
        }
    }

    @Test void unlimitedReservationsAndReleasesKeepStockAndAccountForSales() throws Exception {
        try (SqlSession session = factory.openSession()) {
            AppGoodsMapper mapper = session.getMapper(AppGoodsMapper.class);
            for (int i = 0; i < 12; i++) assertEquals(1, mapper.reserveStock(1L, 1L));
            totals(session, 1, 0L, 18);
            assertEquals(1, mapper.releaseStock(1L, 1L));
            totals(session, 1, 0L, 17);
        }
    }

    @Test void unlimitedNullStockIsNotFabricated() throws Exception {
        try (SqlSession session = factory.openSession()) {
            AppGoodsMapper mapper = session.getMapper(AppGoodsMapper.class);
            assertEquals(1, mapper.reserveStock(3L, 1L)); totals(session, 3, null, 1);
            assertEquals(1, mapper.releaseStock(3L, 1L)); totals(session, 3, null, 0);
        }
    }

    @Test void finiteCourseStillDepletesAndRestoresStock() throws Exception {
        try (SqlSession session = factory.openSession()) {
            AppGoodsMapper mapper = session.getMapper(AppGoodsMapper.class);
            assertEquals(1, mapper.reserveStock(2L, 2L)); totals(session, 2, 0L, 6);
            assertEquals(0, mapper.reserveStock(2L, 1L));
            assertEquals(1, mapper.releaseStock(2L, 1L)); totals(session, 2, 1L, 5);
        }
    }

    @Test void retailAndUnspecifiedCoursesDoNotBecomeUnlimited() throws Exception {
        try (SqlSession session = factory.openSession()) {
            AppGoodsMapper mapper = session.getMapper(AppGoodsMapper.class);
            assertEquals(0, mapper.reserveStock(4L, 1L)); totals(session, 4, 0L, 0);
            assertEquals(0, mapper.reserveStock(5L, 1L)); totals(session, 5, 0L, 0);
        }
    }
}
