package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppArticleMapper;
import com.ruoyi.system.domain.AppArticle;
import com.ruoyi.system.service.IAppArticleService;

/**
 * 图文内容Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppArticleServiceImpl implements IAppArticleService 
{
    @Autowired
    private AppArticleMapper appArticleMapper;

    /**
     * 查询图文内容
     * 
     * @param articleId 图文内容主键
     * @return 图文内容
     */
    @Override
    public AppArticle selectAppArticleByArticleId(Long articleId)
    {
        return appArticleMapper.selectAppArticleByArticleId(articleId);
    }

    /**
     * 查询图文内容列表
     * 
     * @param appArticle 图文内容
     * @return 图文内容
     */
    @Override
    public List<AppArticle> selectAppArticleList(AppArticle appArticle)
    {
        return appArticleMapper.selectAppArticleList(appArticle);
    }

    /**
     * 新增图文内容
     * 
     * @param appArticle 图文内容
     * @return 结果
     */
    @Override
    public int insertAppArticle(AppArticle appArticle)
    {
        appArticle.setCreateTime(DateUtils.getNowDate());
        return appArticleMapper.insertAppArticle(appArticle);
    }

    /**
     * 修改图文内容
     * 
     * @param appArticle 图文内容
     * @return 结果
     */
    @Override
    public int updateAppArticle(AppArticle appArticle)
    {
        appArticle.setUpdateTime(DateUtils.getNowDate());
        return appArticleMapper.updateAppArticle(appArticle);
    }

    /**
     * 批量删除图文内容
     * 
     * @param articleIds 需要删除的图文内容主键
     * @return 结果
     */
    @Override
    public int deleteAppArticleByArticleIds(Long[] articleIds)
    {
        return appArticleMapper.deleteAppArticleByArticleIds(articleIds);
    }

    /**
     * 删除图文内容信息
     * 
     * @param articleId 图文内容主键
     * @return 结果
     */
    @Override
    public int deleteAppArticleByArticleId(Long articleId)
    {
        return appArticleMapper.deleteAppArticleByArticleId(articleId);
    }
}
