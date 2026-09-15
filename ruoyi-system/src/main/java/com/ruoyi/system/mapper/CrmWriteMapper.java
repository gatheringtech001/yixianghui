package com.ruoyi.system.mapper;
import java.util.Map;
public interface CrmWriteMapper
{
    Long lockCustomer(Map<String,Object> values);
    Map<String,Object> receipt(Map<String,Object> values);
    int event(Map<String,Object> values);
    int createTask(Map<String,Object> values);
    int updateTask(Map<String,Object> values);
    int completeTask(Map<String,Object> values);
    int createNeed(Map<String,Object> values);
    int updateNeed(Map<String,Object> values);
}
