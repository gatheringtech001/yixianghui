package com.ruoyi.web.controller.system;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IAppGoodsOrderService;

/**
 * 首页
 *
 * @author ruoyi
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    @Autowired
    private IAppGoodsOrderService goodsOrderService;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", ruoyiConfig.getName(), ruoyiConfig.getVersion());
    }

    /**
     * 兼容旧回调地址：转发到真实支付通知处理，避免只回 SUCCESS 却不落库
     */
    @PostMapping("/wxpay/notify")
    public String wxpayNotify(HttpServletRequest request)
    {
        return goodsOrderService.wxpayNotify(request);
    }
}
