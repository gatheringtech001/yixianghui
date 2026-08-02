package com.ruoyi.system.service;

import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.vo.ConsultantInviteVo;
import com.ruoyi.system.domain.vo.ConsultantStatVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 小程序顾问中心业务
 */
public interface IAppConsultantMnpService
{
    AppConsultant requireApprovedConsultant(Long userId, String mobile);

    ConsultantStatVo getConsultantStat(Long userId, String mobile);

    Map<String, BigDecimal> getIncomeSummary(Long consultantId);

    List<ConsultantInviteVo> selectInviteUserList(Long userId);

    int bindInviterIfAbsent(Long newUserId, Long parentUserId);

    String getOrCreateInviteQrcodeUrl(Long userId);
}
