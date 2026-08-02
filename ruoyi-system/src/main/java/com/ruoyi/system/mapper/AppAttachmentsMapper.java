package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppAttachments;

/**
 * 附件Mapper接口
 * 
 * @author lankong
 * @date 2025-07-20
 */
public interface AppAttachmentsMapper 
{
    /**
     * 查询附件
     * 
     * @param attachmentId 附件主键
     * @return 附件
     */
    public AppAttachments selectAppAttachmentsByAttachmentId(Long attachmentId);

    /**
     * 查询附件列表
     * 
     * @param appAttachments 附件
     * @return 附件集合
     */
    public List<AppAttachments> selectAppAttachmentsList(AppAttachments appAttachments);

    /**
     * 新增附件
     * 
     * @param appAttachments 附件
     * @return 结果
     */
    public int insertAppAttachments(AppAttachments appAttachments);

    /**
     * 修改附件
     * 
     * @param appAttachments 附件
     * @return 结果
     */
    public int updateAppAttachments(AppAttachments appAttachments);

    /**
     * 删除附件
     * 
     * @param attachmentId 附件主键
     * @return 结果
     */
    public int deleteAppAttachmentsByAttachmentId(Long attachmentId);

    /**
     * 批量删除附件
     * 
     * @param attachmentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppAttachmentsByAttachmentIds(Long[] attachmentIds);
}
