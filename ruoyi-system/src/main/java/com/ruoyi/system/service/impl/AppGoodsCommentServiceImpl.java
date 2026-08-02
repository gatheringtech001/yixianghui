package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsCommentMapper;
import com.ruoyi.system.domain.AppGoodsComment;
import com.ruoyi.system.service.IAppGoodsCommentService;

/**
 * 商品评价Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsCommentServiceImpl implements IAppGoodsCommentService 
{
    @Autowired
    private AppGoodsCommentMapper appGoodsCommentMapper;

    /**
     * 查询商品评价
     * 
     * @param commentId 商品评价主键
     * @return 商品评价
     */
    @Override
    public AppGoodsComment selectAppGoodsCommentByCommentId(Long commentId)
    {
        return appGoodsCommentMapper.selectAppGoodsCommentByCommentId(commentId);
    }

    /**
     * 查询商品评价列表
     * 
     * @param appGoodsComment 商品评价
     * @return 商品评价
     */
    @Override
    public List<AppGoodsComment> selectAppGoodsCommentList(AppGoodsComment appGoodsComment)
    {
        return appGoodsCommentMapper.selectAppGoodsCommentList(appGoodsComment);
    }

    /**
     * 新增商品评价
     * 
     * @param appGoodsComment 商品评价
     * @return 结果
     */
    @Override
    public int insertAppGoodsComment(AppGoodsComment appGoodsComment)
    {
        appGoodsComment.setCreateTime(DateUtils.getNowDate());
        return appGoodsCommentMapper.insertAppGoodsComment(appGoodsComment);
    }

    /**
     * 修改商品评价
     * 
     * @param appGoodsComment 商品评价
     * @return 结果
     */
    @Override
    public int updateAppGoodsComment(AppGoodsComment appGoodsComment)
    {
        return appGoodsCommentMapper.updateAppGoodsComment(appGoodsComment);
    }

    /**
     * 批量删除商品评价
     * 
     * @param commentIds 需要删除的商品评价主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCommentByCommentIds(Long[] commentIds)
    {
        return appGoodsCommentMapper.deleteAppGoodsCommentByCommentIds(commentIds);
    }

    /**
     * 删除商品评价信息
     * 
     * @param commentId 商品评价主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCommentByCommentId(Long commentId)
    {
        return appGoodsCommentMapper.deleteAppGoodsCommentByCommentId(commentId);
    }
}
