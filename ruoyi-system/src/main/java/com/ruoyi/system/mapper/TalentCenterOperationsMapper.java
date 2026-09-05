package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface TalentCenterOperationsMapper
{
    Long selectConsultantId(@Param("userId") Long userId);
    Map<String, Object> selectCommissionSummary(Map<String, Object> filters);
    List<Map<String, Object>> selectCommissionPeople(Map<String, Object> filters);
    List<Map<String, Object>> selectCommissionRecords(Map<String, Object> filters);
    List<Map<String, Object>> selectCustomers(@Param("actorUserId") Long actorUserId,
            @Param("consultantId") Long consultantId, @Param("admin") boolean admin);
    List<Map<String, Object>> selectOrders(@Param("actorUserId") Long actorUserId,
            @Param("admin") boolean admin);
    List<Map<String, Object>> selectSettlements(@Param("actorUserId") Long actorUserId,
            @Param("consultantId") Long consultantId, @Param("admin") boolean admin);
    List<String> selectCustomerStatuses();
    int updateCustomer(@Param("id") Long id, @Param("consultantId") Long consultantId,
            @Param("admin") boolean admin, @Param("expectedStatus") String expectedStatus,
            @Param("status") String status, @Param("preference") String preference,
            @Param("followUpNote") String followUpNote, @Param("updateStatus") boolean updateStatus,
            @Param("updatePreference") boolean updatePreference, @Param("updateFollowUpNote") boolean updateFollowUpNote);
    int updateOrderStatus(@Param("id") Long id, @Param("actorUserId") Long actorUserId,
            @Param("admin") boolean admin, @Param("expectedStatus") String expectedStatus,
            @Param("status") String status);
    int updateTravelSettlement(@Param("id") Long id, @Param("actorUserId") Long actorUserId,
            @Param("admin") boolean admin, @Param("expected") Integer expected, @Param("settled") Integer settled);
    int updateEldercareSettlement(@Param("id") Long id, @Param("consultantId") Long consultantId,
            @Param("admin") boolean admin, @Param("expected") Integer expected, @Param("settled") Integer settled);
}
