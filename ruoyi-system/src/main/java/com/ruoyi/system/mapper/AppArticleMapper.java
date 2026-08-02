package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppArticle;

/**
 * 图文内容Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppArticleMapper 
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
     * 删除图文内容
     * 
     * @param articleId 图文内容主键
     * @return 结果
     */
    public int deleteAppArticleByArticleId(Long articleId);

    /**
     * 批量删除图文内容
     * 
     * @param articleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppArticleByArticleIds(Long[] articleIds);
}
