package com.ruoyi.system.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.TalentCenterOperationsMapper;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;

/** 临时只读授权与正式身份绑定分离；Redis 丢失状态时关闭测试视角。 */
@Service
public class TalentCenterTestViewService
{
    private static final Logger LOG = LoggerFactory.getLogger(TalentCenterTestViewService.class);
    private final RedisCache redis;
    private final TalentCenterOperationsMapper mapper;
    private final TalentCenterResourceMapper actors;
    private final String tester;
    private final Long consultant;
    private final Instant until;

    public TalentCenterTestViewService(RedisCache redis, TalentCenterOperationsMapper mapper,
            TalentCenterResourceMapper actors,
            @Value("${TALENT_TEST_VIEW_ACTOR:}") String tester,
            @Value("${TALENT_TEST_VIEW_CONSULTANT:0}") Long consultant,
            @Value("${TALENT_TEST_VIEW_UNTIL:1970-01-01T00:00:00Z}") String until)
    {
        this.redis = redis; this.mapper = mapper; this.actors = actors;
        this.tester = tester; this.consultant = consultant; this.until = Instant.parse(until);
    }

    private boolean available(String actor)
    {
        return !tester.isEmpty() && tester.equals(actor) && consultant > 0 && Instant.now().isBefore(until)
                && actors.selectEnabledActorByActorId(actor) != null;
    }

    private String key()
    {
        return "talent:center:test-view:" + tester + ":" + consultant + ":" + until;
    }

    public Long activeConsultantId(String actor)
    {
        if (!available(actor)) return null;
        Object value = redis.getCacheObject(key());
        if (value != null && !"true".equals(value) && !"false".equals(value))
            throw new TalentCenterApiException(503, "测试视角状态异常，请联系开发者");
        return "true".equals(value) ? consultant : null;
    }

    public Map<String, Object> target(Long id)
    {
        Map<String, Object> target = mapper.selectTestConsultant(id);
        if (target == null) throw new TalentCenterApiException(403, "测试管家不存在或未审核");
        return target;
    }

    public Map<String, Object> status(String actor)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean allowed = available(actor);
        result.put("available", allowed);
        result.put("enabled", allowed && activeConsultantId(actor) != null);
        if (allowed)
        {
            result.put("name", target(consultant).get("name"));
            result.put("expiresAt", until.toString());
        }
        return result;
    }

    public Map<String, Object> setEnabled(String actor, boolean enabled)
    {
        if (!available(actor)) throw new TalentCenterApiException(403, "当前账号没有有效的测试视角授权");
        if (enabled) target(consultant);
        long seconds = Duration.between(Instant.now(), until).getSeconds();
        if (seconds < 1) throw new TalentCenterApiException(403, "测试授权已到期");
        redis.setCacheObject(key(), String.valueOf(enabled), (int) Math.min(seconds, Integer.MAX_VALUE), TimeUnit.SECONDS);
        LOG.info("Talent test view actor={} consultant={} enabled={} until={}", actor, consultant, enabled, until);
        return status(actor);
    }
}
