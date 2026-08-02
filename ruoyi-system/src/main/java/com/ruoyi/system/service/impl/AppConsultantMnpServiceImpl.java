package com.ruoyi.system.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.domain.AppUserInviter;
import com.ruoyi.system.domain.vo.ConsultantInviteVo;
import com.ruoyi.system.domain.vo.ConsultantStatVo;
import com.ruoyi.system.mapper.AppCustomerIncomeMapper;
import com.ruoyi.system.mapper.AppCustomerMapper;
import com.ruoyi.system.mapper.AppUserCashMapper;
import com.ruoyi.system.mapper.AppUserInviterMapper;
import com.ruoyi.system.service.IAppConsultantMnpService;
import com.ruoyi.system.service.IAppConsultantService;
import com.ruoyi.system.service.IAppGoldService;
import com.ruoyi.system.service.IAppUserInfoService;
import com.ruoyi.system.service.IWeChatMiniProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppConsultantMnpServiceImpl implements IAppConsultantMnpService
{
    /** invite register reward gold */
    private static final long INVITE_REGISTER_GOLD = 10L;

    @Autowired
    private IAppConsultantService consultantService;
    @Autowired
    private AppCustomerIncomeMapper customerIncomeMapper;
    @Autowired
    private AppUserCashMapper userCashMapper;
    @Autowired
    private AppCustomerMapper customerMapper;
    @Autowired
    private AppUserInviterMapper userInviterMapper;
    @Autowired
    private IAppUserInfoService userInfoService;
    @Autowired
    private IWeChatMiniProgramService weChatMiniProgramService;
    @Autowired
    private IAppGoldService appGoldService;

    @Override
    public AppConsultant requireApprovedConsultant(Long userId, String mobile)
    {
        AppConsultant consultant = consultantService.getOrClaimConsultantByUser(userId, mobile);
        if (consultant == null)
        {
            throw new ServiceException("顾问身份异常！");
        }
        if (!"01".equals(consultant.getStatus()))
        {
            throw new ServiceException("顾问未审核通过！");
        }
        return consultant;
    }

    @Override
    public ConsultantStatVo getConsultantStat(Long userId, String mobile)
    {
        AppConsultant consultant = requireApprovedConsultant(userId, mobile);
        ConsultantStatVo stat = new ConsultantStatVo();
        Long consultantId = consultant.getConsultantId();
        stat.setTotalIncome(defaultAmount(customerIncomeMapper.sumConsultantIncome(consultantId, null)));
        stat.setPendingAmount(defaultAmount(customerIncomeMapper.sumConsultantIncome(consultantId, 0)));
        stat.setWithdrawnAmount(defaultAmount(userCashMapper.sumWithdrawnAmount(userId)));
        stat.setCustomerCount(defaultCount(customerMapper.countByConsultantId(consultantId)));
        stat.setInviteCount(defaultCount(userInviterMapper.countByInviterUserId(userId)));
        return stat;
    }

    @Override
    public Map<String, BigDecimal> getIncomeSummary(Long consultantId)
    {
        Map<String, BigDecimal> summary = new HashMap<>(2);
        summary.put("totalIncome", defaultAmount(customerIncomeMapper.sumConsultantIncome(consultantId, null)));
        summary.put("pendingAmount", defaultAmount(customerIncomeMapper.sumConsultantIncome(consultantId, 0)));
        return summary;
    }

    @Override
    public List<ConsultantInviteVo> selectInviteUserList(Long userId)
    {
        return userInviterMapper.selectInviteUserList(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindInviterIfAbsent(Long newUserId, Long parentUserId)
    {
        if (newUserId == null || parentUserId == null || newUserId.equals(parentUserId))
        {
            return 0;
        }
        AppUserInviter existsWhere = new AppUserInviter();
        existsWhere.setNewUserId(newUserId);
        if (!userInviterMapper.selectAppUserInviterList(existsWhere).isEmpty())
        {
            return 0;
        }
        AppUserInviter inviter = new AppUserInviter();
        inviter.setUserId(parentUserId);
        inviter.setNewUserId(newUserId);
        inviter.setCreateTime(DateUtils.getNowDate());
        inviter.setStatus("1");
        int rows = userInviterMapper.insertAppUserInviter(inviter);
        if (rows > 0)
        {
            // 被邀请人通过邀请链接登录成功并完成绑定后，给邀请人发奖励（幂等）
            appGoldService.grantInviteRegister(parentUserId, newUserId, INVITE_REGISTER_GOLD);
        }
        return rows;
    }

    @Override
    public String getOrCreateInviteQrcodeUrl(Long userId)
    {
        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(userId);
        if (userInfo != null && StringUtils.isNotEmpty(userInfo.getQrcodeUrl()))
        {
            return userInfo.getQrcodeUrl();
        }
        String qrcodeUrl = weChatMiniProgramService.createInviteQrcode(userId);
        if (userInfo == null)
        {
            userInfo = userInfoService.initUserInfo(userId);
        }
        AppUserInfo update = new AppUserInfo();
        update.setUserId(userId);
        update.setQrcodeUrl(qrcodeUrl);
        userInfoService.updateAppUserInfo(update);
        return qrcodeUrl;
    }

    private BigDecimal defaultAmount(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long defaultCount(Long value)
    {
        return value == null ? 0L : value;
    }
}
