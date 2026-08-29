package com.ruoyi.system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.domain.talent.TalentCenterAudit;
import com.ruoyi.system.domain.talent.TalentCenterResource;
import com.ruoyi.system.domain.talent.TalentCenterResourceType;
import com.ruoyi.system.domain.talent.TalentCenterStatusRequest;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;

@Service
public class TalentCenterAdminService
{
    private static final int MAX_PAGE_SIZE = 100;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
    private final TalentCenterResourceMapper mapper;
    private final SysUserMapper userMapper;
    private final ISysMenuService menuService;
    private final RedisCache redisCache;

    public TalentCenterAdminService(TalentCenterResourceMapper mapper, SysUserMapper userMapper,
            ISysMenuService menuService, RedisCache redisCache)
    {
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.menuService = menuService;
        this.redisCache = redisCache;
    }

    public List<TalentCenterResource> list(String typeValue, String actorUnionid, int pageNum, int pageSize)
    {
        TalentCenterResourceType type = TalentCenterResourceType.from(typeValue);
        validatePage(pageNum, pageSize);
        requirePermission(actorUnionid, type.getListPermission());
        int offset = (pageNum - 1) * pageSize;
        switch (type)
        {
            case GOODS: return mapper.listGoods(offset, pageSize);
            case ACTIVITY: return mapper.listActivities(offset, pageSize);
            case ARTICLE: return mapper.listArticles(offset, pageSize);
            case AD: return mapper.listAds(offset, pageSize);
            default: throw new IllegalStateException("未处理的资源类型");
        }
    }

    public TalentCenterResource get(String typeValue, Long id, String actorUnionid)
    {
        TalentCenterResourceType type = TalentCenterResourceType.from(typeValue);
        validateId(id);
        requirePermission(actorUnionid, type.getQueryPermission());
        TalentCenterResource resource = select(type, id);
        if (resource == null)
        {
            throw new TalentCenterApiException(404, "资源不存在");
        }
        return resource;
    }

    @Transactional(noRollbackFor = TalentCenterApiException.class)
    public TalentCenterResource updateStatus(String typeValue, Long id, TalentCenterStatusRequest request,
            String idempotencyKey, String serviceId, String ip)
    {
        TalentCenterResourceType type = TalentCenterResourceType.from(typeValue);
        validateId(id);
        validateRequest(request, idempotencyKey);
        String keyHash = sha256(serviceId + ":" + idempotencyKey);
        reserveIdempotency(keyHash);

        TalentCenterAudit audit = newAudit(type, id, request, serviceId, keyHash, ip);
        try
        {
            mapper.insertAudit(audit);
        }
        catch (DuplicateKeyException e)
        {
            throw new TalentCenterApiException(409, "confirmationId 或 Idempotency-Key 已使用");
        }

        SysUser actor = findActor(request.getActorUnionid(), audit);
        requirePermission(actor, type.getEditPermission(), audit);
        TalentCenterResource before = select(type, id);
        if (before == null)
        {
            fail(audit, null, "NOT_FOUND", 404, "资源不存在");
        }
        if (!request.getExpectedStatus().equals(before.getStatus()))
        {
            fail(audit, before.getStatus(), "STATUS_CONFLICT", 409, "当前状态与 expectedStatus 不一致");
        }
        if (update(type, id, request.getExpectedStatus(), request.getStatus()) != 1)
        {
            TalentCenterResource current = select(type, id);
            String currentStatus = current == null ? null : current.getStatus();
            fail(audit, currentStatus, "STATUS_CONFLICT", 409, "资源状态已被其他请求修改");
        }
        mapper.finishAudit(audit.getAuditId(), before.getStatus(), request.getStatus(), "SUCCESS");
        return select(type, id);
    }

    private SysUser findActor(String actorUnionid, TalentCenterAudit audit)
    {
        SysUser actor = userMapper.selectEnabledUserByUnionId(actorUnionid);
        if (actor == null)
        {
            fail(audit, null, "ACTOR_NOT_FOUND", 403, "操作人不存在或已停用");
        }
        mapper.updateAuditActor(audit.getAuditId(), actor.getUserId());
        return actor;
    }

    private void requirePermission(String actorUnionid, String permission)
    {
        validateActor(actorUnionid);
        SysUser actor = userMapper.selectEnabledUserByUnionId(actorUnionid);
        if (actor == null)
        {
            throw new TalentCenterApiException(403, "操作人不存在或已停用");
        }
        requirePermission(actor, permission, null);
    }

    private void requirePermission(SysUser actor, String permission, TalentCenterAudit audit)
    {
        Set<String> permissions = menuService.selectMenuPermsByUserId(actor.getUserId());
        if (!actor.isAdmin() && (permissions == null || !permissions.contains(permission)))
        {
            if (audit != null)
            {
                fail(audit, null, "PERMISSION_DENIED", 403, "操作人缺少权限");
            }
            throw new TalentCenterApiException(403, "操作人缺少权限");
        }
    }

    private TalentCenterResource select(TalentCenterResourceType type, Long id)
    {
        switch (type)
        {
            case GOODS: return mapper.getGoods(id);
            case ACTIVITY: return mapper.getActivity(id);
            case ARTICLE: return mapper.getArticle(id);
            case AD: return mapper.getAd(id);
            default: throw new IllegalStateException("未处理的资源类型");
        }
    }

    private int update(TalentCenterResourceType type, Long id, String expected, String status)
    {
        switch (type)
        {
            case GOODS: return mapper.updateGoodsStatus(id, expected, status);
            case ACTIVITY: return mapper.updateActivityStatus(id, expected, status);
            case ARTICLE: return mapper.updateArticleStatus(id, expected, status);
            case AD: return mapper.updateAdStatus(id, expected, status);
            default: throw new IllegalStateException("未处理的资源类型");
        }
    }

    private void reserveIdempotency(String keyHash)
    {
        boolean reserved = redisCache.setCacheObjectIfAbsent("talent:center:idempotency:" + keyHash,
                "accepted", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        if (!reserved)
        {
            throw new TalentCenterApiException(409, "Idempotency-Key 已使用");
        }
    }

    private TalentCenterAudit newAudit(TalentCenterResourceType type, Long id, TalentCenterStatusRequest request,
            String serviceId, String keyHash, String ip)
    {
        TalentCenterAudit audit = new TalentCenterAudit();
        audit.setServiceId(serviceId);
        audit.setResourceType(type.getValue());
        audit.setResourceId(id);
        audit.setAfterStatus(request.getStatus());
        audit.setConfirmationId(request.getConfirmationId());
        audit.setIdempotencyKeyHash(keyHash);
        audit.setRequestTime(new Date());
        audit.setResult("PROCESSING");
        audit.setIp(ip);
        return audit;
    }

    private void validateRequest(TalentCenterStatusRequest request, String idempotencyKey)
    {
        if (request == null)
        {
            throw new TalentCenterApiException(400, "请求体不能为空");
        }
        validateActor(request.getActorUnionid());
        if (!isStatus(request.getExpectedStatus()) || !isStatus(request.getStatus())
                || request.getExpectedStatus().equals(request.getStatus()))
        {
            throw new TalentCenterApiException(400, "expectedStatus 和 status 必须为不同的 0 或 1");
        }
        if (!isSafeId(request.getConfirmationId()))
        {
            throw new TalentCenterApiException(400, "confirmationId 格式不正确");
        }
        if (!isSafeId(idempotencyKey))
        {
            throw new TalentCenterApiException(400, "Idempotency-Key 格式不正确");
        }
        validateConfirmedAt(request.getConfirmedAt());
    }

    private void validateConfirmedAt(String value)
    {
        try
        {
            Instant confirmedAt = OffsetDateTime.parse(value).toInstant();
            if (Duration.between(confirmedAt, Instant.now()).abs().getSeconds() > 300)
            {
                throw new TalentCenterApiException(400, "confirmedAt 与服务器时间偏差不能超过5分钟");
            }
        }
        catch (DateTimeParseException | NullPointerException e)
        {
            throw new TalentCenterApiException(400, "confirmedAt 必须为 ISO 时间");
        }
    }

    private void validateActor(String actorUnionid)
    {
        if (actorUnionid == null || actorUnionid.length() < 8 || actorUnionid.length() > 128)
        {
            throw new TalentCenterApiException(400, "actorUnionid 格式不正确");
        }
    }

    private void validatePage(int pageNum, int pageSize)
    {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE)
        {
            throw new TalentCenterApiException(400, "pageNum 必须大于0且 pageSize 不超过100");
        }
    }

    private void validateId(Long id)
    {
        if (id == null || id <= 0)
        {
            throw new TalentCenterApiException(400, "资源ID必须为正整数");
        }
    }

    private void fail(TalentCenterAudit audit, String beforeStatus, String result, int status, String message)
    {
        mapper.finishAudit(audit.getAuditId(), beforeStatus, audit.getAfterStatus(), result);
        throw new TalentCenterApiException(status, message);
    }

    private boolean isStatus(String status) { return "0".equals(status) || "1".equals(status); }
    private boolean isSafeId(String value) { return value != null && SAFE_ID.matcher(value).matches(); }

    private String sha256(String value)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
