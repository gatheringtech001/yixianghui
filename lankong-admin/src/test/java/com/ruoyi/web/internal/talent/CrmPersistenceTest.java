package com.ruoyi.web.internal.talent;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.system.mapper.CrmReadMapper;
import com.ruoyi.system.mapper.CrmWriteMapper;
import com.ruoyi.system.service.CrmAccess;
import com.ruoyi.system.service.CrmReadService;
import com.ruoyi.system.service.CrmWriteService;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="CRM_INTEGRATION", matches="1")
class CrmPersistenceTest
{
    @Test void actualMybatisPersistenceReplayCasRollbackAndScopedReads() throws Exception
    {
        // 只创建独立空测试库，从本地 E2E 复制结构，不复制客户数据；禁止连接生产地址。
        String database="crm_it_"+System.currentTimeMillis();
        String base="jdbc:mysql://127.0.0.1:3306/";
        try(Connection admin=DriverManager.getConnection(base+"?useSSL=false&serverTimezone=UTC","root",""); Statement statement=admin.createStatement()) {
            statement.execute("CREATE DATABASE "+database+" CHARACTER SET utf8mb4");
        }
        DriverManagerDataSource ds=new DriverManagerDataSource(base+database+"?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true","root","");
        JdbcTemplate jdbc=new JdbcTemplate(ds);
        String[] tables={"app_customer","app_customer_feishu_source","app_travel_customer_profile","app_eldercare_customer_profile",
                "app_feishu_business_relation","app_customer_income","app_goods_order","app_user_inviter","app_consultant","app_consultant_feishu","app_feishu_business_user"};
        for(String table:tables) jdbc.execute("CREATE TABLE "+table+" LIKE yixianghui_e2e."+table);
        jdbc.execute("ALTER TABLE app_customer ENGINE=InnoDB");
        jdbc.execute("ALTER TABLE app_goods_order ADD COLUMN service_owner_user_id bigint DEFAULT NULL");
        String ddl=new String(Files.readAllBytes(Paths.get("../sql/talent-center-crm.sql")),java.nio.charset.StandardCharsets.UTF_8);
        for(String sql:ddl.split(";")) if(!sql.trim().isEmpty()) jdbc.execute(sql);
        jdbc.update("INSERT INTO app_customer(customer_id,customer_name,consultant_id,del_flag,link_mobile,link1_name,link1_mobile,customer_goods) VALUES (1,'测试客户',24,'0','13800000000','测试联系人','13900000000','旅居咨询'),(2,'其他客户',25,'0',null,null,null,null)");
        jdbc.update("INSERT INTO app_customer_income(income_id,customer_id,consultant_id,purchase_amount,charge_amount,income_no) VALUES(1,1,24,120,1000,'LOCAL-1'),(2,1,25,900,0,'OTHER-OWNER'),(3,null,24,700,0,'FS-shared')");
        jdbc.update("INSERT INTO app_customer_feishu_source(source_table_id,source_record_id,customer_id,business_line,match_method,match_status) VALUES('tblXVuYCto8OOmAz','travel-customer',1,'travel','test','matched'),('tblHDwb0OdfcH1Ib','elder-customer',1,'eldercare','test','matched')");
        jdbc.update("INSERT INTO app_travel_customer_profile(source_table_id,feishu_record_id,canonical_table,canonical_id,canonical_status,fs_fldlivquuw,fs_fldrbddvel,fs_fldqgctoqi) VALUES('tblXVuYCto8OOmAz','travel-customer','app_customer',1,'linked','温泉旅居','历史旅居回访','视频号')");
        jdbc.update("INSERT INTO app_eldercare_customer_profile(source_table_id,feishu_record_id,canonical_table,canonical_id,canonical_status,fs_fldyh1qjlb,fs_fldwjfcdov,fs_fldkhlyxv9,fs_fldj8jvvqz) VALUES('tblHDwb0OdfcH1Ib','elder-customer','app_customer',1,'linked','[\"居家照护\"]','历史养老回访','测试家属','13700000000')");
        jdbc.update("INSERT INTO app_feishu_business_relation(source_table_id,source_record_id,source_field_id,target_business_table,target_business_id,relation_status,target_source_record_id) VALUES('tblA33x9gGWM1b51','shared','fldL0xSls8','app_customer',1,'resolved','first'),('tblA33x9gGWM1b51','shared','fldL0xSls8','app_customer',2,'resolved','second')");
        SqlSessionFactoryBean factory=new SqlSessionFactoryBean(); factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("mapper/system/CrmReadMapper.xml"),new ClassPathResource("mapper/system/CrmWriteMapper.xml"),new ClassPathResource("mapper/system/TalentCenterOperationsMapper.xml"));
        SqlSessionTemplate session=new SqlSessionTemplate(factory.getObject());
        CrmAccess access=mock(CrmAccess.class);
        when(access.resolve(anyString(),eq("self"),anyBoolean())).thenAnswer(invocation->map("consultantId",24L,"actorUserId",108L,"auditUserId",108L,"actorName","tester","admin",false,"readOnly",false));
        CrmWriteService writes=new CrmWriteService(access,session.getMapper(CrmWriteMapper.class));
        CrmReadService reads=new CrmReadService(access,session.getMapper(CrmReadMapper.class));
        TransactionTemplate tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        Map<String,Object> input=map("requestId",UUID.randomUUID().toString(),"content","已沟通","method","phone","occurredAt","2026-09-15T08:00:00Z","nextAction","发方案","nextDueAt","2026-09-16T08:00:00Z");
        assertEquals(false,tx.execute(s->writes.save("actor","self","customer:1","followups",null,input)).get("replayed"));
        assertEquals(true,tx.execute(s->writes.save("actor","self","customer:1","followups",null,input)).get("replayed"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_crm_event",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_crm_task",Integer.class));
        Map<String,Object> changed=new LinkedHashMap<>(input); changed.put("content","不同内容");
        assertThrows(TalentCenterApiException.class,()->tx.execute(s->writes.save("actor","self","customer:1","followups",null,changed)));
        Map<String,Object> stale=new LinkedHashMap<>(input); stale.put("requestId",UUID.randomUUID().toString()); stale.put("completeTaskId",UUID.randomUUID().toString()); stale.put("taskVersion",1);
        assertThrows(TalentCenterApiException.class,()->tx.execute(s->writes.save("actor","self","customer:1","followups",null,stale)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_crm_task",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_crm_event",Integer.class));
        assertThrows(TalentCenterApiException.class,()->tx.execute(s->writes.save("actor","self","customer:2","followups",null,input)));
        String taskId=jdbc.queryForObject("SELECT task_id FROM app_crm_task",String.class);
        Map<String,Object> task=map("requestId",UUID.randomUUID().toString(),"title","发方案","dueAt","2026-09-17T08:00:00Z","version",1,"status","done");
        tx.execute(s->writes.save("actor","self","customer:1","tasks",taskId,task));
        task.put("requestId",UUID.randomUUID().toString());
        assertThrows(TalentCenterApiException.class,()->tx.execute(s->writes.save("actor","self","customer:1","tasks",taskId,task)));
        assertEquals(2,jdbc.queryForObject("SELECT version FROM app_crm_task",Integer.class));
        Map<String,Object> need=map("requestId",UUID.randomUUID().toString(),"title","云南旅居","businessLine","travel");
        tx.execute(s->writes.save("actor","self","customer:1","needs",null,need));
        String needId=jdbc.queryForObject("SELECT opportunity_id FROM app_crm_opportunity",String.class);
        Map<String,Object> advance=map("requestId",UUID.randomUUID().toString(),"title","云南旅居","version",1,"stage","proposal");
        tx.execute(s->writes.save("actor","self","customer:1","needs",needId,advance));
        assertEquals("proposal",jdbc.queryForObject("SELECT stage FROM app_crm_opportunity",String.class));
        CrmWriteMapper broken=mock(CrmWriteMapper.class,org.mockito.AdditionalAnswers.delegatesTo(session.getMapper(CrmWriteMapper.class)));
        doThrow(new IllegalStateException("test event failure")).when(broken).event(anyMap());
        CrmWriteService failing=new CrmWriteService(access,broken);
        Map<String,Object> rollback=map("requestId",UUID.randomUUID().toString(),"title","应回滚的待办","dueAt","2026-09-17T08:00:00Z");
        assertThrows(IllegalStateException.class,()->tx.execute(s->failing.save("actor","self","customer:1","tasks",null,rollback)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_crm_task",Integer.class));
        concurrentReplay(writes,tx,jdbc);
        Map<String,Object> detail=tx.execute(s->reads.detail("actor","self","customer:1"));
        assertEquals("测试联系人",((Map<?,?>)((java.util.List<?>)detail.get("contacts")).get(0)).get("name"));
        assertEquals(2,((java.util.List<?>)detail.get("origins")).size());
        assertEquals(2,((java.util.List<?>)detail.get("historicalNotes")).size());
        assertEquals(2,((java.util.List<?>)detail.get("contacts")).size());
        Map<?,?> amount=(Map<?,?>)((Map<?,?>)detail.get("consumption")).get("eldercare");
        assertEquals(12000L,amount.get("totalCents")); assertEquals(2,amount.get("recordCount")); assertEquals(1,amount.get("sharedCount"));
        assertEquals(1,((java.util.List<?>)reads.workspace("actor","self").get("customers")).size());
        assertThrows(TalentCenterApiException.class,()->reads.detail("actor","self","customer:2"));
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(Paths.get("/tmp/crm-integration-readback.json").toFile(),map("detail",detail,"workspace",reads.workspace("actor","self")));
        System.out.println("CRM isolated database verified: "+database+"; synthetic fixtures retained for inspection.");
    }
    private static Map<String,Object> map(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]); return result;
    }
    private void concurrentReplay(CrmWriteService writes,TransactionTemplate tx,JdbcTemplate jdbc) throws Exception {
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        Map<String,Object> input=map("requestId",UUID.randomUUID().toString(),"content","并发重试","method","phone","occurredAt","2026-09-15T08:00:00Z");
        java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Map<String,Object>> action=()->{ start.await(); return tx.execute(s->writes.save("actor","self","customer:1","followups",null,input)); };
        try {
            java.util.concurrent.Future<Map<String,Object>> first=pool.submit(action),second=pool.submit(action); start.countDown();
            Object a=first.get(10,java.util.concurrent.TimeUnit.SECONDS).get("replayed"),b=second.get(10,java.util.concurrent.TimeUnit.SECONDS).get("replayed");
            assertNotEquals(a,b);
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_crm_event WHERE request_id=?",Integer.class,input.get("requestId")));
        } finally { pool.shutdownNow(); }
    }
}
