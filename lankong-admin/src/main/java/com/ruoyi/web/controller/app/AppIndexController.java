package com.ruoyi.web.controller.app;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;
import com.ruoyi.generator.service.IGenTableColumnService;
import com.ruoyi.generator.service.IGenTableService;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.service.*;
import io.swagger.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ruoyi.common.utils.PageUtils.startPage;

/**
 * @ClassName AppIndexController
 * @Description 应用功能控制器
 * @Author Lbc
 * @Date 2025/4/6 16:15
 * @Version 1.0
 */
@Api("公共接口控制器")
@RestController
@RequestMapping("/mnp/index")
public class AppIndexController extends BaseController {

    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;

    @Autowired
    private ISysDeptService deptService;
    @Autowired
    private IAppGoodsService goodsService;
    @Autowired
    private IAppGoodsCategoryService goodsCategoryService;
    @Autowired
    private IAppAdPositionService adPositionService;
    @Autowired
    private IAppAdContentService adContentService;
    @Autowired
    private IAppArticleService articleService;
    @Autowired
    private IAppArticleCategoryService articleCategoryService;
    @Autowired
    private IAppSinglePageService singlePageService;
    @Autowired
    private IAppCardService cardService;
    @Autowired
    private IAppActivityCategoryService activityCategoryService;
    @Autowired
    private IAppActivityService activityService;
    @Autowired
    private IAppCustomerService appCustomerService;

    @Autowired
    private IAppGoodsSkuService skuService;
    @Autowired
    private IAppGoodsSkuOptionService skuOptionService;
    @Autowired
    private IAppGoodsSkuDataService skuDataService;
    @Autowired
    private ISysNoticeService noticeService;

    @Value("${tencent.map.key:}")
    private String tencentMapKey;

    /**
     * 查询数据模型
     */
    @ApiOperation("查询数据模型")
    @Anonymous
    @GetMapping("/temp/api_model_list")
    public TableDataInfo list(@RequestParam(name = "tableName", required = false) String tableComment)
    {
        GenTable genTable = new GenTable();
        genTable.setTableComment(tableComment);
        startPage();
        List<GenTable> list = genTableService.selectGenTableList(genTable);
        return getDataTable(list);
    }

    /**
     * 查询数据表字段列表
     */
    @ApiOperation("查询数据表字段列表")
    @Anonymous
    @GetMapping(value = "/temp/api_field_list/{tableId}")
    public TableDataInfo columnList(@PathVariable(name = "tableId") Long tableId)
    {
        TableDataInfo dataInfo = new TableDataInfo();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        dataInfo.setRows(list);
        dataInfo.setTotal(list.size());
        return dataInfo;
    }

    /************************* 商城公共接口 **************************/
    /**
     * 查询所在分站
     */
    @ApiOperation("查询所在分站")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "lng", value = "经度", required = true),
            @ApiImplicitParam(name = "lat", value = "纬度", required = true)
    })
    @Anonymous
    @GetMapping(value = "/get_site")
    public AjaxResult getInfo(AppArea area)
    {
        // todo 根据经纬度信息查询所在分站
        SysDept dataInfo = deptService.selectDeptById(108L);
        return success(dataInfo);
    }

    @Anonymous
    @GetMapping(value = "/get_site_bydepId/{deptId}")
    public AjaxResult getInfoById(@PathVariable Long deptId)
    {
        SysDept dataInfo = deptService.selectDeptById(deptId);
        if (dataInfo == null) {
            return error("站点不存在(deptId=" + deptId + ")");
        }
        return success(dataInfo);
    }

    /**
     * 查询分站列表
     */
    @ApiOperation("查询分站列表")
    @Anonymous
    @GetMapping(value = "/get_site_list")
    public TableDataInfo siteList(SysDept dept)
    {
        startPage();
        List<SysDept> list = deptService.selectSiteList(dept);
        return getDataTable(list);
    }

    /**
     * 查询省份/区域列表（其下存在分站城市）
     */
    @ApiOperation("查询省份列表")
    @Anonymous
    @GetMapping(value = "/get_provinces")
    public AjaxResult getProvinces()
    {
        List<SysDept> list = deptService.selectProvinceList();
        return success(list);
    }

    /**
     * 查询省份下的分站城市列表
     */
    @ApiOperation("查询城市列表")
    @Anonymous
    @GetMapping(value = "/get_cities")
    public AjaxResult getCities(@RequestParam Long provinceId)
    {
        SysDept query = new SysDept();
        query.setParentId(provinceId);
        List<SysDept> list = deptService.selectSiteList(query);
        return success(list);
    }

    /**
     * 查询商品分类
     */
    @ApiOperation("查询商品分类")
    @Anonymous
    @GetMapping(value = "/get_goods_category")
    public AjaxResult goodsCategoryList(AppGoodsCategory appGoodsCategory)
    {
        List<AppGoodsCategory> list = goodsCategoryService.selectAppGoodsCategoryList(appGoodsCategory);
        return success(list);
    }

    /**
     * 查询商品列表
     */
    @ApiOperation("查询商品列表")
    @Anonymous
    @GetMapping(value = "/get_goods_list")
    public AjaxResult goodsList(AppGoods appGoods)
    {
        startPage();
        if (null!=appGoods && appGoods.getDeptId() == null) {
            appGoods.setDeptId(100L);
        }else{
            appGoods = new AppGoods();
            appGoods.setDeptId(100L);
        }
        if(null!=appGoods && null!=appGoods.getCategoryId()) {
            appGoods.setCategoryIds(goodsCategoryService.selectAppGoodsCategoryAllIdsById(appGoods.getCategoryId()));
            appGoods.setCategoryId(null);
        }
        List<AppGoods> list = goodsService.selectAppGoodsList(appGoods);
        return success(list);
    }

    @ApiOperation("查询商品列表")
    @Anonymous
    @PostMapping(value = "/queryGoodsList")
    public AjaxResult queryGoodsList(@RequestBody AppGoods appGoods)
    {
        startPage();
        if (null!=appGoods && appGoods.getDeptId() == null) {
            appGoods.setDeptId(100L);
        }else{
            if(appGoods==null) {
                appGoods = new AppGoods();
                appGoods.setDeptId(100L);
            }
        }
        if(null!=appGoods && null!=appGoods.getCategoryId()) {
            appGoods.setCategoryIds(goodsCategoryService.selectAppGoodsCategoryAllIdsById(appGoods.getCategoryId()));
            appGoods.setCategoryId(null);
        }
        List<AppGoods> list = goodsService.selectAppGoodsList(appGoods);
        return success(list);
    }

    /**
     * 查询商品详情
     */
    @ApiOperation("查询商品详情")
    @Anonymous
    @GetMapping(value = "/get_goods_info/{goodsId}")
    public AjaxResult goodsInfo(@PathVariable(name = "goodsId") Long goodsId)
    {
        AppGoods info = goodsService.selectAppGoodsByGoodsId(goodsId);
        // C 端不返回停用规格，避免详情可见、下单才提示「已停用」
        if (info != null && info.getOptionList() != null && !info.getOptionList().isEmpty()) {
            List<AppGoodsSku> enabledSkus = new ArrayList<>();
            for (AppGoodsSku sku : info.getOptionList()) {
                if (isSkuEnabledForApp(sku)) {
                    enabledSkus.add(sku);
                }
            }
            info.setOptionList(enabledSkus);
        }
        return success(info);
    }

    /** 未设置状态视为可用；显式 0/停用 不返回给小程序 */
    private boolean isSkuEnabledForApp(AppGoodsSku sku) {
        if (sku == null) {
            return false;
        }
        String status = StringUtils.trimToEmpty(sku.getStatus());
        if (StringUtils.isEmpty(status)) {
            return true;
        }
        return !("0".equals(status) || "停用".equals(status));
    }

    /**
     * 查询广告位列表
     */
    @ApiOperation("查询广告位列表")
    @Anonymous
    @GetMapping(value = "/get_ad_position_list")
    public AjaxResult adPositionList(AppAdPosition appAdPosition)
    {
        startPage();
        List<AppAdPosition> list = adPositionService.selectAppAdPositionList(appAdPosition);
        return success(list);
    }

    /**
     * 查询广告内容列表
     */
    @ApiOperation("查询广告内容列表")
    @Anonymous
    @GetMapping(value = "/get_ad_content_list")
    public AjaxResult adContentList(AppAdContent appAdContent)
    {
        List<AppAdContent> list = adContentService.selectAppAdContentList(appAdContent);
        return success(list);
    }

    @ApiOperation("查询广告内容列表")
    @Anonymous
    @GetMapping(value = "/get_ad_content_list_by_position_id/{positionId}")
    public AjaxResult getAdContentListByPositionId(@PathVariable("positionId") Long positionId)
    {
        AppAdContent appAdContent = new AppAdContent();
        appAdContent.setPositionId(positionId);
        List<AppAdContent> list = adContentService.selectAppAdContentListByPositionId(positionId);
        return success(list);
    }


    /**
     * 查询图文分类列表
     */
    @ApiOperation("查询图文分类列表")
    @Anonymous
    @GetMapping(value = "/get_article_category_list")
    public AjaxResult articleCategoryList(AppArticleCategory appArticleCategory)
    {
        List<AppArticleCategory> list = articleCategoryService.selectAppArticleCategoryList(appArticleCategory);
        return success(list);
    }

    /**
     * 查询图文列表
     */
    @ApiOperation("查询图文列表")
    @Anonymous
    @GetMapping(value = "/get_article_list")
    public AjaxResult articleList(AppArticle appArticle)
    {
        startPage();
        List<AppArticle> list = articleService.selectAppArticleList(appArticle);
        return success(list);
    }

    /**
     * 查询单页文章详情
     */
    @ApiOperation("查询单页文章详情")
    @ApiImplicitParam(name = "pageId", value = "文章ID", required = true, dataType = "int", paramType = "path", dataTypeClass = Integer.class)
    @Anonymous
    @GetMapping(value = "/get_single_page/{pageId}")
    public AjaxResult singlePage(@PathVariable("pageId") Long pageId)
    {
        AppSinglePage singlePage = singlePageService.selectAppSinglePageByPageId(pageId);
        return success(singlePage);
    }

    /**
     * 查询通知公告详情（小程序公开）
     */
    @ApiOperation("查询通知公告详情")
    @ApiImplicitParam(name = "noticeId", value = "公告ID", required = true, dataType = "int", paramType = "path", dataTypeClass = Integer.class)
    @Anonymous
    @GetMapping(value = "/get_notice/{noticeId}")
    public AjaxResult getNotice(@PathVariable("noticeId") Long noticeId)
    {
        SysNotice notice = noticeService.selectNoticeById(noticeId);
        if (notice == null || !"0".equals(notice.getStatus()))
        {
            return error("公告不存在或已关闭");
        }
        return success(notice);
    }

    /**
     * 查询会员卡列表
     */
    @ApiOperation("查询会员卡列表")
    @Anonymous
    @GetMapping("/card_list")
    public TableDataInfo card_list(AppCard appCard)
    {
        startPage();
        List<AppCard> list = cardService.selectAppCardList(appCard);
        return getDataTable(list);
    }

    /**
     * 获取会员卡详细信息
     */
    @ApiOperation("获取会员卡详细信息")
    @Anonymous
    @GetMapping(value = "/card_info/{cardId}")
    public AjaxResult card_getInfo(@PathVariable("cardId") Long cardId)
    {
        return success(cardService.selectAppCardByCardId(cardId));
    }

    /**
     * 查询活动分类列表
     */
    @ApiOperation("查询活动分类列表")
    @Anonymous
    @GetMapping("/activity_category_list")
    public AjaxResult activity_category_list(AppActivityCategory appActivityCategory)
    {
        List<AppActivityCategory> list = activityCategoryService.selectAppActivityCategoryList(appActivityCategory);
        return success(list);
    }

    /**
     * 获取活动分类详细信息
     */
    @ApiOperation("获取活动分类详细信息")
    @Anonymous
    @GetMapping(value = "/activity_category_info/{categoryId}")
    public AjaxResult activity_category_getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(activityCategoryService.selectAppActivityCategoryByCategoryId(categoryId));
    }


    /**
     * 查询活动列表
     */
    @ApiOperation("查询活动列表")
    @Anonymous
    @GetMapping("/activity_list")
    public TableDataInfo activity_list(AppActivity appActivity)
    {
        startPage();
        List<AppActivity> list = activityService.selectAppActivityList(appActivity);
        return getDataTable(list);
    }

    /**
     * 获取活动详细信息
     */
    @ApiOperation("获取活动详细信息")
    @Anonymous
    @GetMapping(value = "/activity_info/{activityId}")
    public AjaxResult activity_getInfo(@PathVariable("activityId") Long activityId)
    {
        return success(activityService.selectAppActivityByActivityId(activityId));
    }

    /**
     * 地址解析（用于小程序地图导航）
     */
    @ApiOperation("地址解析")
    @Anonymous
    @GetMapping(value = "/geocode_address")
    public AjaxResult geocodeAddress(@RequestParam("address") String address)
    {
        if (StringUtils.isEmpty(address))
        {
            return error("地址不能为空");
        }
        if (StringUtils.isEmpty(tencentMapKey))
        {
            return error("地图服务未配置");
        }
        try
        {
            String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8.name());
            String param = "address=" + encodedAddress + "&key=" + tencentMapKey;
            String response = HttpUtils.sendGet("https://apis.map.qq.com/ws/geocoder/v1/", param);
            if (StringUtils.isEmpty(response))
            {
                return error("地址解析失败");
            }
            JSONObject json = JSONObject.parseObject(response);
            if (json.getIntValue("status") != 0)
            {
                return error(StringUtils.defaultIfEmpty(json.getString("message"), "地址解析失败"));
            }
            JSONObject location = json.getJSONObject("result").getJSONObject("location");
            Map<String, Object> data = new HashMap<>();
            data.put("latitude", location.getDouble("lat"));
            data.put("longitude", location.getDouble("lng"));
            return success(data);
        }
        catch (Exception e)
        {
            return error("地址解析失败");
        }
    }

    /************** SKU 部分  *************************/

    /**
     * 获取商品sku
     */
    @ApiOperation("获取商品sku")
    @Anonymous
    @RequestMapping("/goods_sku_list")
    public AjaxResult goods_sku_list(@RequestParam("goodsId") Long goodsId) {
        List<AppGoodsSku> list = skuService.selectAppGoodsSkuListByGoodsId(goodsId);
        return AjaxResult.success(list);
    }

    /**
     * 获取商品sku
     */
    @ApiOperation("获取商品sku选项")
    @Anonymous
    @RequestMapping("/goods_sku_option")
    public AjaxResult goods_sku_option(@RequestParam("skuId") Long skuId) {
        List<AppGoodsSkuOption> list = skuOptionService.selectAppGoodsSkuOptionListBySkuId(skuId);
        return AjaxResult.success(list);
    }

    /**
     * 获取商品sku组合数据
     */
    @ApiOperation("获取商品sku组合数据")
    @Anonymous
    @RequestMapping("/goods_sku_data")
    public AjaxResult goods_sku_data(@RequestParam("goodsId") Long goodsId) {
        List<AppGoodsSkuData> list = skuDataService.selectAppGoodsSkuListByGoodsId(goodsId);
        return AjaxResult.success(list);
    }

    /**
     * 获取商品sku组合数据详情
     */
    @ApiOperation("获取商品sku组合数据详情")
    @Anonymous
    @RequestMapping("/goods_sku_data_info")
    public AjaxResult goods_sku_data_info(@RequestParam("skuDataId") Long skuDataId) {
        AppGoodsSkuData info = skuDataService.selectAppGoodsSkuDataByDataId(skuDataId);
        return AjaxResult.success(info);
    }

    /**
    *
    *
  20250503
  ALTER TABLE `lk-shzxj`.`app_goods_category`
  ADD COLUMN `is_hot` SMALLINT DEFAULT 0  NOT NULL   COMMENT '是否热门' AFTER `category_icon`,
  ADD COLUMN `link_type` VARCHAR(50) DEFAULT 'goods'  NULL   COMMENT '链接类型' AFTER `is_hot`,
  ADD COLUMN `link_id` BIGINT DEFAULT 0  NULL   COMMENT '链接目标ID' AFTER `link_type`;
  ALTER TABLE `lk-shzxj`.`app_goods`
  ADD COLUMN `vip_price` DECIMAL(10,2) NULL   COMMENT '会员价' AFTER `price`;
    *
    20250511
  ALTER TABLE `lk-shzxj`.`app_goods`
  ADD COLUMN `award_golden` INT UNSIGNED NULL   COMMENT '奖励金币' AFTER `award_grand_parent_ratio`;
    *
    20250518
    ALTER TABLE `lk-shzxj`.`app_user_inviter`
  ADD COLUMN `inviter_type` VARCHAR(50) NULL   COMMENT '邀请人类型' AFTER `total_award`,
  ADD COLUMN `inviter_card_level` VARCHAR(50) NULL   COMMENT '邀请人会员等级' AFTER `inviter_type`,
  ADD COLUMN `consultant_name` VARCHAR(50) NULL   COMMENT '顾问姓名' AFTER `inviter_card_level`;
  字典：顾问状态consultant_status：00待审核；01审核通过；02审核拒绝
  ALTER TABLE `lk-shzxj`.`app_consultant`
  ADD COLUMN `remark` VARCHAR(255) NULL   COMMENT '备注' AFTER `status`;
  *
  CREATE TABLE `lk-shzxj`.`app_supplier`(
  `supplier_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `supplier_name` VARCHAR(100) COMMENT '供应商名称',
  `supplier_code` VARCHAR(100) COMMENT '供应商编码',
  `status` VARCHAR(10) COMMENT '供应商状态',
  PRIMARY KEY (`supplier_id`)
) ENGINE=MYISAM CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='供应商';

* ALTER TABLE `lk-shzxj`.`app_customer_income`
  ADD COLUMN `customer_id` BIGINT NULL   COMMENT '客户ID' AFTER `remark`;
  *
  字典：字符串日期common_date_str：
  *
  20250521
  字典：产品类别 product_type
  ALTER TABLE `lk-shzxj`.`app_customer`
  ADD COLUMN `medication_remark` VARCHAR(255) NULL   COMMENT '服用药品名称及使用方式及剂量' AFTER `medication_long`;
ALTER TABLE `lk-shzxj`.`app_customer`
  ADD COLUMN `child_pension_month` VARCHAR(50) NULL   COMMENT '子女或其他补贴' AFTER `pension_month`;
  ALTER TABLE `lk-shzxj`.`app_customer_income`
  CHANGE `product_type` `product_type` VARCHAR(255) CHARSET utf8mb4 COLLATE utf8mb4_general_ci NULL   COMMENT '提成类别';

*
20250523
-- ----------------------------
-- 第三方授权表
-- ----------------------------
drop table if exists sys_auth_user;
create table sys_auth_user (
  auth_id           bigint(20)      not null auto_increment    comment '授权ID',
  uuid              varchar(500)    not null                   comment '第三方平台用户唯一ID',
  user_id           bigint(20)      not null                   comment '系统用户ID',
  user_name         VARCHAR(50)     NOT NULL                   COMMENT '登录账号',
  nick_name         VARCHAR(30)     DEFAULT ''                 COMMENT '用户昵称',
  avatar            varchar(500)    default ''                 comment '头像地址',
  email             varchar(255)    default ''                 comment '用户邮箱',
  source            varchar(255)    default ''                 comment '用户来源',
  create_time       datetime                                   comment '创建时间',
  primary key (auth_id)
) engine=innodb auto_increment=100 comment = '第三方授权表';
    *
    *
    * medication_remark

 20250525
 ALTER TABLE `lk-shzxj`.`app_activity`
  ADD COLUMN `sign_count` INT NULL   COMMENT '报名人数' AFTER `is_hot`,
  ADD COLUMN `max_count` INT NULL   COMMENT '允许报名人数' AFTER `sign_count`,
  ADD COLUMN `sign_end_time` DATETIME NULL   COMMENT '报名截止时间' AFTER `max_count`,
  ADD COLUMN `activity_time` VARCHAR(255) NULL   COMMENT '活动时间' AFTER `sign_end_time`;
    *
  20250529
  ALTER TABLE `lk-shzxj`.`app_customer`
  ADD COLUMN `disease_body` VARCHAR(100) NULL   COMMENT '疾病诊断->躯体疾病' AFTER `disease_mental`;
    *
  ALTER TABLE `lk-shzxj`.`sys_user`
  CHANGE `user_name` `user_name` VARCHAR(50) CHARSET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL   COMMENT '用户账号';

*
  20250603
  RENAME TABLE `lk-shzxj`.`app_user` TO `lk-shzxj`.`app_user_info`;
  代码生成：用户信息app_user_info
  *
  ALTER TABLE `lk-shzxj`.`app_user_info`
  CHANGE `golden` `golden` BIGINT(20) UNSIGNED DEFAULT 0  NULL   COMMENT '金币数量',
  CHANGE `score` `score` BIGINT(20) UNSIGNED DEFAULT 0  NULL   COMMENT '积分数量',
  CHANGE `money` `money` DECIMAL(10,2) UNSIGNED DEFAULT 0  NOT NULL   COMMENT '钱包',
  CHANGE `true_name` `true_name` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '真实姓名',
  CHANGE `idcard` `idcard` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '身份证号',
  CHANGE `bank_title` `bank_title` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '提现银行',
  CHANGE `bank_username` `bank_username` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '银行户名',
  CHANGE `bank_account` `bank_account` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '银行卡号',
  CHANGE `city` `city` VARCHAR(255) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '用户所在城市',
  CHANGE `wexin_account` `wexin_account` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '微信号',
  CHANGE `weixin_openid` `weixin_openid` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '微信openid',
  CHANGE `alipay_account` `alipay_account` VARCHAR(100) CHARSET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT ''  NULL   COMMENT '支付宝账号';



     20251019
     ALTER TABLE `lk-shzxj`.`app_goods_sku_option`
     ADD COLUMN `option_param` VARCHAR(255) NULL   COMMENT '选项参数' AFTER `option_name`;
     20251021
     ALTER TABLE `lk-shzxj`.`app_goods_sku_data`
     ADD COLUMN `remark` VARCHAR(255) NULL   COMMENT '选项参数' AFTER `data_stock`;


     * */
}
