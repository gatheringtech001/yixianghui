package com.ruoyi.system.service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.apache.commons.codec.digest.DigestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.CrmWriteMapper;

@Service
public class CrmWriteService
{
    private final CrmAccess access;
    private final CrmWriteMapper mapper;
    public CrmWriteService(CrmAccess access, CrmWriteMapper mapper) { this.access=access; this.mapper=mapper; }

    @Transactional
    public Map<String,Object> save(String actor,String scope,String customer,String action,String entityId,Map<String,Object> input)
    {
        Map<String,Object> values=access.resolve(actor,scope,true);
        CrmInput.validate(action,entityId!=null,input);
        if(entityId!=null) CrmInput.uuid(entityId);
        values.put("customerId",CrmValues.customer(customer));
        if(mapper.lockCustomer(values)==null) throw new TalentCenterApiException(404,"客户不存在或不属于本人");
        values.put("requestId",input.get("requestId"));
        String hash=hash(CrmValues.map("actor",actor,"customer",customer,"action",action,"entity",entityId,"input",new TreeMap<>(input)));
        values.put("payloadHash",hash);
        Map<String,Object> receipt=mapper.receipt(values);
        if(receipt!=null) {
            if(!hash.equals(receipt.get("payloadHash")) || !sameId(values.get("auditUserId"),receipt.get("actorUserId"))
                    || !sameId(values.get("customerId"),receipt.get("customerId"))) throw new TalentCenterApiException(409,"请求编号已用于其他内容");
            return CrmValues.map("requestId",input.get("requestId"),"customerId",customer,"replayed",true);
        }
        values.putAll(input); values.put("now",LocalDateTime.now(ZoneOffset.UTC));
        values.put("entityId",entityId==null ? UUID.randomUUID().toString() : entityId);
        values.put("payloadJson",json(CrmValues.map("action",action,"entityId",values.get("entityId"),"input",new TreeMap<>(input))));
        values.put("occurredAt",values.get("now")); values.put("method",null);
        if("followups".equals(action)) followup(values,input);
        else if("tasks".equals(action)) task(values,entityId!=null);
        else need(values,entityId!=null);
        try { mapper.event(values); }
        catch(DuplicateKeyException e) { throw new TalentCenterApiException(409,"请求编号冲突，请核对本次记录"); }
        return CrmValues.map("requestId",input.get("requestId"),"customerId",customer,"replayed",false);
    }
    private void followup(Map<String,Object> values,Map<String,Object> input)
    {
        values.put("kind","followup"); values.put("method",input.get("method"));
        values.put("occurredAt",utc(input.get("occurredAt")));
        if(input.containsKey("completeTaskId")) changed(mapper.completeTask(values));
        if(input.containsKey("nextAction")) {
            Map<String,Object> task=new LinkedHashMap<>(values);
            task.put("title",input.get("nextAction")); task.put("dueAt",utc(input.get("nextDueAt")));
            mapper.createTask(task);
        }
    }
    private void task(Map<String,Object> values,boolean update)
    {
        values.put("kind",update ? "task_changed" : "task_created");
        values.put("content",(update ? "更新待办" : "安排跟进")+" · "+values.get("title")+" · "+values.get("dueAt")
                +(update ? " · "+values.get("status") : ""));
        values.put("dueAt",utc(values.get("dueAt")));
        if(update) changed(mapper.updateTask(values)); else mapper.createTask(values);
    }
    private void need(Map<String,Object> values,boolean update)
    {
        values.put("kind",update ? "need_changed" : "need_created");
        values.put("content",(update ? "更新需求" : "登记需求")+" · "+values.get("title")+(update ? " · "+values.get("stage") : ""));
        if(update) changed(mapper.updateNeed(values)); else mapper.createNeed(values);
    }
    private LocalDateTime utc(Object value) { return LocalDateTime.ofInstant(Instant.parse(CrmValues.instant(value)),ZoneOffset.UTC); }
    private void changed(int rows) { if(rows!=1) throw new TalentCenterApiException(409,"记录已变化，请刷新后核对"); }
    private boolean sameId(Object expected,Object actual) {
        return expected instanceof Number && actual instanceof Number && ((Number)expected).longValue()==((Number)actual).longValue();
    }
    private String hash(Map<String,Object> value) {
        return DigestUtils.sha256Hex(json(value));
    }
    private String json(Map<String,Object> value) {
        try { return new ObjectMapper().writeValueAsString(new TreeMap<>(value)); }
        catch(JsonProcessingException e) { throw new TalentCenterApiException(400,"提交格式不正确"); }
    }
}
