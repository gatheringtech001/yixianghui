package com.ruoyi.system.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.domain.talent.TalentCenterOperationUpdateRequest;
import com.ruoyi.system.mapper.TalentCenterOperationsMapper;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;

@Service
public class TalentCenterOperationsService
{
    private static final Pattern SAFE_CONFIRMATION = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
    private static final List<String> ORDER_STATUSES = Collections.unmodifiableList(Arrays.asList(
            "待确认", "已确认", "已取消", "已入住", "已离店", "已结算", "退款中", "已退款"));
    private final TalentCenterOperationsMapper mapper;
    private final TalentCenterResourceMapper resourceMapper;
    private final RedisCache redisCache;

    public TalentCenterOperationsService(TalentCenterOperationsMapper mapper,
            TalentCenterResourceMapper resourceMapper, RedisCache redisCache)
    {
        this.mapper = mapper;
        this.resourceMapper = resourceMapper;
        this.redisCache = redisCache;
    }

    public Map<String, Object> snapshot(String actorId, String actorScope)
    {
        Access access = access(actorId, actorScope);
        List<Map<String, Object>> customers = mapper.selectCustomers(access.userId, access.consultantId, access.admin);
        for (Map<String, Object> customer : customers) customer.put("timeline", Collections.emptyList());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scope", access.admin ? "admin" : "self");
        payload.put("customers", customers);
        payload.put("orders", mapper.selectOrders(access.userId, access.admin));
        payload.put("settlements", mapper.selectSettlements(access.userId, access.consultantId, access.admin));
        payload.put("schedules", Collections.emptyList());
        payload.put("fieldOptions", fieldOptions());
        payload.put("readAt", Instant.now().toString());
        return payload;
    }

    @Transactional
    public Map<String, Object> update(String actorId, String actorScope, String businessLine, String resource, String recordId,
            TalentCenterOperationUpdateRequest request, String idempotencyKey)
    {
        validateConfirmation(request, idempotencyKey);
        reserve(idempotencyKey);
        Access access = access(actorId, actorScope);
        Long id = parseId(recordId, resource);
        int changed;
        Map<String, Object> values = new LinkedHashMap<>();
        if ("customers".equals(resource))
        {
            if (!"eldercare".equals(businessLine)) throw new TalentCenterApiException(404, "经营记录不存在");
            boolean updateStatus = request.getStatus() != null;
            if (updateStatus && !mapper.selectCustomerStatuses().contains(request.getStatus()))
                throw new TalentCenterApiException(400, "客户状态不在后台数据库可选范围内");
            changed = mapper.updateCustomer(id, access.consultantId, access.admin, request.getExpectedStatus(),
                    request.getStatus(), request.getPreference(), request.getFollowUpNote(), updateStatus,
                    request.getPreference() != null, request.getFollowUpNote() != null);
            values.put("status", request.getStatus() == null ? request.getExpectedStatus() : request.getStatus());
            values.put("preference", request.getPreference());
            values.put("followUpNote", request.getFollowUpNote());
        }
        else if ("orders".equals(resource))
        {
            if (!"travel".equals(businessLine) || request.getStatus() == null)
                throw new TalentCenterApiException(400, "当前订单状态不可修改");
            changed = mapper.updateOrderStatus(id, access.userId, access.admin,
                    orderStatusCode(request.getExpectedStatus()), orderStatusCode(request.getStatus()));
            values.put("status", request.getStatus());
        }
        else if ("settlements".equals(resource))
        {
            boolean travelRecord = "travel".equals(businessLine) && recordId != null && recordId.startsWith("order:");
            boolean eldercareRecord = "eldercare".equals(businessLine) && recordId != null && recordId.startsWith("income:");
            if (!travelRecord && !eldercareRecord) throw new TalentCenterApiException(404, "经营记录不存在");
            if (request.getSettled() == null) throw new TalentCenterApiException(400, "缺少结算状态");
            Integer expected = booleanCode(request.getExpectedStatus());
            Integer settled = request.getSettled() ? 1 : 0;
            changed = "travel".equals(businessLine)
                    ? mapper.updateTravelSettlement(id, access.userId, access.admin, expected, settled)
                    : mapper.updateEldercareSettlement(id, access.consultantId, access.admin, expected, settled);
            values.put("settled", request.getSettled());
        }
        else throw new TalentCenterApiException(400, "未知经营资源");
        if (changed != 1) throw new TalentCenterApiException(409, "记录已变化或不属于当前操作人，请刷新后重试");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", recordId);
        result.put("businessLine", businessLine);
        result.put("kind", resource.substring(0, resource.length() - 1));
        result.put("updatedAt", Instant.now().toString());
        result.put("values", values);
        return result;
    }

    private Access access(String actorId, String actorScope)
    {
        SysUser actor = resourceMapper.selectEnabledActorByActorId(actorId);
        if (actor == null) throw new TalentCenterApiException(403, "达人账号尚未绑定小程序后台身份");
        boolean admin = "admin".equals(actorScope);
        return new Access(actor.getUserId(), mapper.selectConsultantId(actor.getUserId()), admin);
    }

    private Map<String, Object> fieldOptions()
    {
        Map<String, Object> customerStatus = new LinkedHashMap<>();
        customerStatus.put("travel", Collections.emptyList());
        customerStatus.put("eldercare", mapper.selectCustomerStatuses());
        Map<String, Object> orderStatus = new LinkedHashMap<>();
        orderStatus.put("travel", ORDER_STATUSES);
        orderStatus.put("eldercare", Collections.emptyList());
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("customerStatus", customerStatus);
        options.put("orderStatus", orderStatus);
        return options;
    }

    private void validateConfirmation(TalentCenterOperationUpdateRequest request, String idempotencyKey)
    {
        if (request == null || !safe(request.getConfirmationId()) || !safe(idempotencyKey))
            throw new TalentCenterApiException(400, "缺少有效的二次确认编号");
        try
        {
            Instant confirmed = OffsetDateTime.parse(request.getConfirmedAt()).toInstant();
            if (Duration.between(confirmed, Instant.now()).abs().getSeconds() > 300)
                throw new TalentCenterApiException(400, "二次确认已过期");
        }
        catch (TalentCenterApiException e) { throw e; }
        catch (Exception e) { throw new TalentCenterApiException(400, "二次确认时间格式不正确"); }
    }

    private void reserve(String key)
    {
        if (!redisCache.setCacheObjectIfAbsent("talent:center:operation:" + key, "used", 24, TimeUnit.HOURS))
            throw new TalentCenterApiException(409, "本次操作已经执行");
    }

    private boolean safe(String value) { return value != null && SAFE_CONFIRMATION.matcher(value).matches(); }

    private Long parseId(String recordId, String resource)
    {
        String prefix = "customers".equals(resource) ? "customer:" : "orders".equals(resource) ? "order:" : null;
        if ("settlements".equals(resource))
            prefix = recordId != null && recordId.startsWith("income:") ? "income:" : "order:";
        if (recordId == null || prefix == null || !recordId.startsWith(prefix))
            throw new TalentCenterApiException(404, "经营记录不存在");
        try { return Long.valueOf(recordId.substring(prefix.length())); }
        catch (Exception e) { throw new TalentCenterApiException(404, "经营记录不存在"); }
    }

    private String orderStatusCode(String label)
    {
        int index = ORDER_STATUSES.indexOf(label);
        if (index < 0) throw new TalentCenterApiException(400, "订单状态不在后台可选范围内");
        return String.valueOf(index);
    }

    private Integer booleanCode(String value)
    {
        if (value == null || value.isEmpty()) return null;
        if ("true".equals(value)) return 1;
        if ("false".equals(value)) return 0;
        throw new TalentCenterApiException(400, "原结算状态不正确");
    }

    private static class Access
    {
        final Long userId;
        final Long consultantId;
        final boolean admin;
        Access(Long userId, Long consultantId, boolean admin)
        {
            this.userId = userId;
            this.consultantId = consultantId;
            this.admin = admin;
        }
    }
}
