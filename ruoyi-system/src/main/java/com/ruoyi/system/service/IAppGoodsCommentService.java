package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsComment;

/**
 * 商品评价Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsCommentService 
{
    /**
     * 查询商品评价
     * 
     * @param commentId 商品评价主键
     * @return 商品评价
     */
    public AppGoodsComment selectAppGoodsCommentByCommentId(Long commentId);

    /**
     * 查询商品评价列表
     * 
     * @param appGoodsComment 商品评价
     * @return 商品评价集合
     */
    public List<AppGoodsComment> selectAppGoodsCommentList(AppGoodsComment appGoodsComment);

    /**
     * 新增商品评价
     * 
     * @param appGoodsComment 商品评价
     * @return 结果
     */
    public int insertAppGoodsComment(AppGoodsComment appGoodsComment);

    /**
     * 修改商品评价
     * 
     * @param appGoodsComment 商品评价
     * @return 结果
     */
    public int updateAppGoodsComment(AppGoodsComment appGoodsComment);

    /**
     * 批量删除商品评价
     * 
     * @param commentIds 需要删除的商品评价主键集合
     * @return 结果
     */
    public int deleteAppGoodsCommentByCommentIds(Long[] commentIds);

    /**
     * 删除商品评价信息
     * 
     * @param commentId 商品评价主键
     * @return 结果
     */
    public int deleteAppGoodsCommentByCommentId(Long commentId);
}
