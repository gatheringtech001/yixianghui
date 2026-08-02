package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppArticle;

/**
 * 图文内容Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppArticleService 
{
    /**
     * 查询图文内容
     * 
     * @param articleId 图文内容主键
     * @return 图文内容
     */
    public AppArticle selectAppArticleByArticleId(Long articleId);

    /**
     * 查询图文内容列表
     * 
     * @param appArticle 图文内容
     * @return 图文内容集合
     */
    public List<AppArticle> selectAppArticleList(AppArticle appArticle);

    /**
     * 新增图文内容
     * 
     * @param appArticle 图文内容
     * @return 结果
     */
    public int insertAppArticle(AppArticle appArticle);

    /**
     * 修改图文内容
     * 
     * @param appArticle 图文内容
     * @return 结果
     */
    public int updateAppArticle(AppArticle appArticle);

    /**
     * 批量删除图文内容
     * 
     * @param articleIds 需要删除的图文内容主键集合
     * @return 结果
     */
    public int deleteAppArticleByArticleIds(Long[] articleIds);

    /**
     * 删除图文内容信息
     * 
     * @param articleId 图文内容主键
     * @return 结果
     */
    public int deleteAppArticleByArticleId(Long articleId);
}
