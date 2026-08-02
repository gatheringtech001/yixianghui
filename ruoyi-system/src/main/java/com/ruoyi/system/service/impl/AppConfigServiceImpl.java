package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppConfigMapper;
import com.ruoyi.system.domain.AppConfig;
import com.ruoyi.system.service.IAppConfigService;

/**
 * 商城配置Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppConfigServiceImpl implements IAppConfigService 
{
    @Autowired
    private AppConfigMapper appConfigMapper;

    /**
     * 查询商城配置
     * 
     * @param configId 商城配置主键
     * @return 商城配置
     */
    @Override
    public AppConfig selectAppConfigByConfigId(Integer configId)
    {
        return appConfigMapper.selectAppConfigByConfigId(configId);
    }

    /**
     * 查询商城配置列表
     * 
     * @param appConfig 商城配置
     * @return 商城配置
     */
    @Override
    public List<AppConfig> selectAppConfigList(AppConfig appConfig)
    {
        return appConfigMapper.selectAppConfigList(appConfig);
    }

    /**
     * 新增商城配置
     * 
     * @param appConfig 商城配置
     * @return 结果
     */
    @Override
    public int insertAppConfig(AppConfig appConfig)
    {
        return appConfigMapper.insertAppConfig(appConfig);
    }

    /**
     * 修改商城配置
     * 
     * @param appConfig 商城配置
     * @return 结果
     */
    @Override
    public int updateAppConfig(AppConfig appConfig)
    {
        return appConfigMapper.updateAppConfig(appConfig);
    }

    /**
     * 批量删除商城配置
     * 
     * @param configIds 需要删除的商城配置主键
     * @return 结果
     */
    @Override
    public int deleteAppConfigByConfigIds(Integer[] configIds)
    {
        return appConfigMapper.deleteAppConfigByConfigIds(configIds);
    }

    /**
     * 删除商城配置信息
     * 
     * @param configId 商城配置主键
     * @return 结果
     */
    @Override
    public int deleteAppConfigByConfigId(Integer configId)
    {
        return appConfigMapper.deleteAppConfigByConfigId(configId);
    }
}
