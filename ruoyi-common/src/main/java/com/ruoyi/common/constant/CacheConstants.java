package com.ruoyi.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 顾问缓存Key
     */
    public static final String CONSULTANT_KEY = "app_consultant:";

    /**
     * 客户缓存Key
     */
    public static final String CUSTOMER_KEY = "app_customer:";

    /**
     * 部门缓存Key
     */
    public static final String DEPT_KEY = "sys_dept:";

    /**
     * 商品分类缓存Key
     */
    public static final String APP_GOODS_CATEGROY = "app_goods_category:";
    /**
     * 商品缓存Key
     */
    public static final String APP_GOODS = "app_goods:";
    public static final String SYS_USER = "sys_user:";
    public static final String SUPPLIER_KEY = "app_supplier:";

    public static final String APP_GOODS_ORDER = "app_goods_order:";
}
