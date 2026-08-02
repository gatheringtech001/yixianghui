package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城配置对象 app_config
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置id */
    private Integer configId;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** 配置标识 */
    @Excel(name = "配置标识")
    private String configKey;

    /** 配置值 */
    @Excel(name = "配置值")
    private String configValue;

    /** 配置方式 */
    @Excel(name = "配置方式")
    private String inputMethod;

    /** 配置状态 */
    @Excel(name = "配置状态")
    private String status;

    public void setConfigId(Integer configId) 
    {
        this.configId = configId;
    }

    public Integer getConfigId() 
    {
        return configId;
    }

    public void setConfigName(String configName) 
    {
        this.configName = configName;
    }

    public String getConfigName() 
    {
        return configName;
    }

    public void setConfigKey(String configKey) 
    {
        this.configKey = configKey;
    }

    public String getConfigKey() 
    {
        return configKey;
    }

    public void setConfigValue(String configValue) 
    {
        this.configValue = configValue;
    }

    public String getConfigValue() 
    {
        return configValue;
    }

    public void setInputMethod(String inputMethod) 
    {
        this.inputMethod = inputMethod;
    }

    public String getInputMethod() 
    {
        return inputMethod;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("configName", getConfigName())
            .append("configKey", getConfigKey())
            .append("configValue", getConfigValue())
            .append("inputMethod", getInputMethod())
            .append("remark", getRemark())
            .append("status", getStatus())
            .toString();
    }
}
