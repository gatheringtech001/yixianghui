package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppConfig;

/**
 * 商城配置Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppConfigService 
{
    /**
     * 查询商城配置
     * 
     * @param configId 商城配置主键
     * @return 商城配置
     */
    public AppConfig selectAppConfigByConfigId(Integer configId);

    /**
     * 查询商城配置列表
     * 
     * @param appConfig 商城配置
     * @return 商城配置集合
     */
    public List<AppConfig> selectAppConfigList(AppConfig appConfig);

    /**
     * 新增商城配置
     * 
     * @param appConfig 商城配置
     * @return 结果
     */
    public int insertAppConfig(AppConfig appConfig);

    /**
     * 修改商城配置
     * 
     * @param appConfig 商城配置
     * @return 结果
     */
    public int updateAppConfig(AppConfig appConfig);

    /**
     * 批量删除商城配置
     * 
     * @param configIds 需要删除的商城配置主键集合
     * @return 结果
     */
    public int deleteAppConfigByConfigIds(Integer[] configIds);

    /**
     * 删除商城配置信息
     * 
     * @param configId 商城配置主键
     * @return 结果
     */
    public int deleteAppConfigByConfigId(Integer configId);
}
