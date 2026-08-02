package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppAttachmentsMapper;
import com.ruoyi.system.domain.AppAttachments;
import com.ruoyi.system.service.IAppAttachmentsService;

/**
 * 附件Service业务层处理
 * 
 * @author lankong
 * @date 2025-07-20
 */
@Service
public class AppAttachmentsServiceImpl implements IAppAttachmentsService 
{
    @Autowired
    private AppAttachmentsMapper appAttachmentsMapper;

    /**
     * 查询附件
     * 
     * @param attachmentId 附件主键
     * @return 附件
     */
    @Override
    public AppAttachments selectAppAttachmentsByAttachmentId(Long attachmentId)
    {
        return appAttachmentsMapper.selectAppAttachmentsByAttachmentId(attachmentId);
    }

    /**
     * 查询附件列表
     * 
     * @param appAttachments 附件
     * @return 附件
     */
    @Override
    public List<AppAttachments> selectAppAttachmentsList(AppAttachments appAttachments)
    {
        return appAttachmentsMapper.selectAppAttachmentsList(appAttachments);
    }

    /**
     * 新增附件
     * 
     * @param appAttachments 附件
     * @return 结果
     */
    @Override
    public int insertAppAttachments(AppAttachments appAttachments)
    {
        appAttachments.setCreateTime(DateUtils.getNowDate());
        return appAttachmentsMapper.insertAppAttachments(appAttachments);
    }

    /**
     * 修改附件
     * 
     * @param appAttachments 附件
     * @return 结果
     */
    @Override
    public int updateAppAttachments(AppAttachments appAttachments)
    {
        appAttachments.setUpdateTime(DateUtils.getNowDate());
        return appAttachmentsMapper.updateAppAttachments(appAttachments);
    }

    /**
     * 批量删除附件
     * 
     * @param attachmentIds 需要删除的附件主键
     * @return 结果
     */
    @Override
    public int deleteAppAttachmentsByAttachmentIds(Long[] attachmentIds)
    {
        return appAttachmentsMapper.deleteAppAttachmentsByAttachmentIds(attachmentIds);
    }

    /**
     * 删除附件信息
     * 
     * @param attachmentId 附件主键
     * @return 结果
     */
    @Override
    public int deleteAppAttachmentsByAttachmentId(Long attachmentId)
    {
        return appAttachmentsMapper.deleteAppAttachmentsByAttachmentId(attachmentId);
    }
}
