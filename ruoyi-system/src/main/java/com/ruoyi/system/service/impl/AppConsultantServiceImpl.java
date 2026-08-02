package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysDeptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppConsultantMapper;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.service.IAppConsultantService;

import javax.validation.Validator;

/**
 * 康养顾问Service业务层处理
 * 
 * @author lankong
 * @date 2025-05-14
 */
@Service
public class AppConsultantServiceImpl implements IAppConsultantService 
{
    private static final Logger log = LoggerFactory.getLogger(AppConsultantServiceImpl.class);
    @Autowired
    private AppConsultantMapper appConsultantMapper;
    @Autowired
    private ISysDeptService deptService;
    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    protected Validator validator;


    /**
     * 查询康养顾问
     * 
     * @param consultantId 康养顾问主键
     * @return 康养顾问
     */
    @Override
    public AppConsultant selectAppConsultantByConsultantId(Long consultantId)
    {
        return appConsultantMapper.selectAppConsultantByConsultantId(consultantId);
    }

    /**
     * 查询康养顾问列表
     * 
     * @param appConsultant 康养顾问
     * @return 康养顾问
     */
    @Override
    public List<AppConsultant> selectAppConsultantList(AppConsultant appConsultant)
    {
        return appConsultantMapper.selectAppConsultantList(appConsultant);
    }

    /**
     * 新增康养顾问
     * 
     * @param appConsultant 康养顾问
     * @return 结果
     */
    @Override
    public int insertAppConsultant(AppConsultant appConsultant)
    {
        prepareConsultantForSave(appConsultant);
        appConsultant.setCreateTime(DateUtils.getNowDate());
        int rows = appConsultantMapper.insertAppConsultant(appConsultant);
        clearConsultantCache(appConsultant, null);
        return rows;
    }

    /**
     * 修改康养顾问
     * 
     * @param appConsultant 康养顾问
     * @return 结果
     */
    @Override
    public int updateAppConsultant(AppConsultant appConsultant)
    {
        AppConsultant oldConsultant = appConsultantMapper.selectAppConsultantByConsultantId(appConsultant.getConsultantId());
        prepareConsultantForSave(appConsultant);
        appConsultant.setUpdateTime(DateUtils.getNowDate());
        int rows = appConsultantMapper.updateAppConsultant(appConsultant);
        clearConsultantCache(appConsultant, oldConsultant);
        return rows;
    }

    /**
     * 批量删除康养顾问
     * 
     * @param consultantIds 需要删除的康养顾问主键
     * @return 结果
     */
    @Override
    public int deleteAppConsultantByConsultantIds(Long[] consultantIds)
    {
        return appConsultantMapper.deleteAppConsultantByConsultantIds(consultantIds);
    }

    /**
     * 删除康养顾问信息
     * 
     * @param consultantId 康养顾问主键
     * @return 结果
     */
    @Override
    public int deleteAppConsultantByConsultantId(Long consultantId)
    {
        return appConsultantMapper.deleteAppConsultantByConsultantId(consultantId);
    }

    /**
     * 根据用户ID获取顾问信息
     * @param userId
     * @return
     */
    @Override
    public AppConsultant selectAppConsultantByUserId(Long userId) {
        return appConsultantMapper.selectAppConsultantByUserId(userId);
    }

    /**
     * 根据ID获取顾问名称
     * @param consultantId
     * @return
     */
    @Override
    public String selectAppConsultantNameById(Long consultantId) {
        AppConsultant consultant = appConsultantMapper.selectAppConsultantByConsultantId(consultantId);
        return consultant == null ? null : consultant.getConsultantName();
    }

    /**
     * 根据顾问姓名查找顾问
     * @param consultantName
     * @return
     */
    @Override
    public AppConsultant selectAppConsultantByConsultantName(String consultantName) {
        AppConsultant consultant = appConsultantMapper.selectAppConsultantByConsultantName(consultantName);
        return consultant;
    }

    @Override
    public String importConsultant(List<AppConsultant> consultantList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(consultantList) || consultantList.size() == 0)
        {
            throw new ServiceException("导入顾问数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (AppConsultant consultant : consultantList)
        {
            try
            {
                //匹配关联ID-顾问姓名
                SysDept dept = deptService.selectDeptByName(consultant.getDeptName());
                if (dept != null) {
                    consultant.setDeptId(dept.getDeptId());
                }
                // 验证是否存在这个顾问
                AppConsultant u = appConsultantMapper.selectConsultantByConsultantNo(consultant.getConsultantNo());
                if (StringUtils.isNull(u))
                {
                    BeanValidators.validateWithException(validator, consultant);
                    consultant.setCreateBy(operName);
                    consultant.setUserId(null);
                    prepareConsultantForSave(consultant);
                    appConsultantMapper.insertAppConsultant(consultant);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、顾问 " + consultant.getConsultantName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, consultant);
                    deptService.checkDeptDataScope(consultant.getDeptId());
                    AppConsultant lastConsultant = appConsultantMapper.selectAppConsultantByConsultantName(u.getConsultantName());
                    consultant.setConsultantId(lastConsultant.getConsultantId());
                    consultant.setUserId(lastConsultant.getUserId());
                    consultant.setUpdateBy(operName);
                    prepareConsultantForSave(consultant);
                    appConsultantMapper.updateAppConsultant(consultant);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、顾问 " + consultant.getConsultantName() + " 更新成功");
                }
                else if (StringUtils.isEmpty(u.getConsultantName()))
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、顾问 " + consultant.getConsultantName() + " 姓名为空");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、顾问 " + consultant.getConsultantName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、顾问 " + consultant.getConsultantName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    public AppConsultant getCacheConsultant(Long consultantId) {
        AppConsultant consultant = SpringUtils.getBean(RedisCache.class).getCacheObject(CacheConstants.CONSULTANT_KEY +"id:" + consultantId);
        if (consultant == null) {
            consultant = appConsultantMapper.selectAppConsultantByConsultantId(consultantId);
            if (consultant != null) {
                SpringUtils.getBean(RedisCache.class).setCacheObject(CacheConstants.CONSULTANT_KEY +"id:" + consultantId, consultant);
            }
        }
        return consultant;
    }

    @Override
    public AppConsultant getOrClaimConsultantByUser(Long userId, String mobile)
    {
        AppConsultant bound = appConsultantMapper.selectAppConsultantByUserId(userId);
        if (bound != null)
        {
            if (isConsultantMobileMatched(bound, mobile))
            {
                return bound;
            }
            log.info("顾问绑定手机号校验失败，已解除绑定 consultantId={}, userId={}",
                    bound.getConsultantId(), userId);
            releaseConsultantBinding(bound);
        }
        if (StringUtils.isEmpty(mobile))
        {
            return null;
        }
        return claimConsultantByMobile(userId, mobile, true);
    }

    @Override
    public int applyConsultantAsUser(Long userId, AppConsultant consultant)
    {
        AppConsultant existing = appConsultantMapper.selectAppConsultantByUserId(userId);
        if (existing != null)
        {
            throw new ServiceException("已申请，无法再次提交");
        }
        consultant.setUserId(userId);
        if (StringUtils.isEmpty(consultant.getStatus()))
        {
            consultant.setStatus("00");
        }
        String mobile = consultant.getMobile();
        if (StringUtils.isEmpty(mobile))
        {
            SysUser user = userMapper.selectUserById(userId);
            if (user != null)
            {
                mobile = user.getPhonenumber();
                consultant.setMobile(mobile);
            }
        }
        AppConsultant unclaimed = StringUtils.isNotEmpty(mobile)
                ? appConsultantMapper.selectUnclaimedConsultantByMobile(mobile, false)
                : null;
        if (unclaimed != null)
        {
            consultant.setConsultantId(unclaimed.getConsultantId());
            if (StringUtils.isEmpty(consultant.getConsultantNo()))
            {
                consultant.setConsultantNo(unclaimed.getConsultantNo());
            }
            if ("01".equals(unclaimed.getStatus()) && "00".equals(consultant.getStatus()))
            {
                consultant.setStatus("01");
            }
            return updateAppConsultant(consultant);
        }
        return insertAppConsultant(consultant);
    }

    /**
     * 校验已绑定顾问档案手机号与当前登录用户手机号是否一致
     */
    private boolean isConsultantMobileMatched(AppConsultant consultant, String userMobile)
    {
        if (consultant == null)
        {
            return false;
        }
        String consultantMobile = normalizeMobile(consultant.getMobile());
        String mobile = normalizeMobile(userMobile);
        if (StringUtils.isEmpty(consultantMobile) || StringUtils.isEmpty(mobile))
        {
            return false;
        }
        return consultantMobile.equals(mobile);
    }

    private String normalizeMobile(String mobile)
    {
        return StringUtils.isEmpty(mobile) ? "" : mobile.trim();
    }

    private void releaseConsultantBinding(AppConsultant consultant)
    {
        if (consultant == null || consultant.getConsultantId() == null)
        {
            return;
        }
        appConsultantMapper.clearConsultantUserId(consultant.getConsultantId(), DateUtils.getNowDate());
        clearConsultantCache(consultant, consultant);
    }

    private AppConsultant claimConsultantByMobile(Long userId, String mobile, boolean approvedOnly)
    {
        AppConsultant unclaimed = appConsultantMapper.selectUnclaimedConsultantByMobile(mobile, approvedOnly);
        if (unclaimed == null)
        {
            return null;
        }
        validateUserIdUnique(userId, unclaimed.getConsultantId());
        unclaimed.setUserId(userId);
        unclaimed.setUpdateTime(DateUtils.getNowDate());
        appConsultantMapper.updateAppConsultant(unclaimed);
        clearConsultantCache(unclaimed, null);
        return appConsultantMapper.selectAppConsultantByConsultantId(unclaimed.getConsultantId());
    }

    /**
     * 保存前校验 userId 唯一（PC 录入不设置 userId，由小程序认领）
     */
    private void prepareConsultantForSave(AppConsultant consultant)
    {
        validateUserIdUnique(consultant.getUserId(), consultant.getConsultantId());
    }

    /**
     * 关联用户唯一：一个 sys_user 只能绑定一个顾问
     */
    private void validateUserIdUnique(Long userId, Long excludeConsultantId)
    {
        if (userId == null)
        {
            return;
        }
        AppConsultant existing = appConsultantMapper.selectAppConsultantByUserId(userId);
        if (existing != null && (excludeConsultantId == null || !existing.getConsultantId().equals(excludeConsultantId)))
        {
            throw new ServiceException("关联用户已被顾问「" + existing.getConsultantName() + "」绑定");
        }
    }

    private void clearConsultantCache(AppConsultant consultant, AppConsultant oldConsultant)
    {
        RedisCache cache = SpringUtils.getBean(RedisCache.class);
        if (consultant != null && consultant.getConsultantId() != null)
        {
            cache.deleteObject(CacheConstants.CONSULTANT_KEY + "id:" + consultant.getConsultantId());
        }
        if (consultant != null && consultant.getUserId() != null)
        {
            cache.deleteObject(CacheConstants.CONSULTANT_KEY + "by_user_id:" + consultant.getUserId());
        }
        if (oldConsultant != null && oldConsultant.getUserId() != null
                && (consultant == null || !oldConsultant.getUserId().equals(consultant.getUserId())))
        {
            cache.deleteObject(CacheConstants.CONSULTANT_KEY + "by_user_id:" + oldConsultant.getUserId());
        }
    }
}
