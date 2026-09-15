package com.ruoyi.system.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.TalentCenterOperationsMapper;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;

@Service
public class CrmAccess
{
    private final TalentCenterResourceMapper actors;
    private final TalentCenterOperationsMapper operations;
    private final TalentCenterTestViewService testView;
    public CrmAccess(TalentCenterResourceMapper actors, TalentCenterOperationsMapper operations, TalentCenterTestViewService testView)
    { this.actors = actors; this.operations = operations; this.testView = testView; }

    public Map<String, Object> resolve(String actorId, String scope, boolean write)
    {
        if (!"self".equals(scope)) throw new TalentCenterApiException(403, "客户服务仅支持本人范围");
        SysUser actor = actors.selectEnabledActorByActorId(actorId);
        if (actor == null) throw new TalentCenterApiException(403, "后台身份尚未关联");
        Long testConsultant = testView.activeConsultantId(actorId);
        if (write && testConsultant != null) throw new TalentCenterApiException(403, "测试视角只读");
        Long consultantId = testConsultant == null ? operations.selectConsultantId(actor.getUserId()) : testConsultant;
        if (consultantId == null) throw new TalentCenterApiException(403, "尚未关联管家身份");
        return CrmValues.map("consultantId", consultantId, "actorUserId", testConsultant == null ? actor.getUserId() : null,
                "auditUserId", actor.getUserId(), "actorName", actor.getUserName(), "admin", false, "readOnly", testConsultant != null);
    }
}
