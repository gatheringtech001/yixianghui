package com.ruoyi.system.mapper;
import java.util.List;
import java.util.Map;
public interface CrmReadMapper
{
    List<Map<String, Object>> customers(Map<String, Object> filters);
    Map<String, Object> profile(Map<String, Object> filters);
    List<Map<String, Object>> origins(Map<String, Object> filters);
    List<Map<String, Object>> tasks(Map<String, Object> filters);
    List<Map<String, Object>> needs(Map<String, Object> filters);
    List<Map<String, Object>> events(Map<String, Object> filters);
    List<Map<String, Object>> travel(Map<String, Object> filters);
    List<Map<String, Object>> eldercare(Map<String, Object> filters);
}
