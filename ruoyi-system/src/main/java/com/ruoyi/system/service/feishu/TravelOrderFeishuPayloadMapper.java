package com.ruoyi.system.service.feishu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.TravelOrderSyncRecord;

/**
 * 小程序旅居订单与飞书预订订单表的字段映射。
 */
public final class TravelOrderFeishuPayloadMapper
{
    private TravelOrderFeishuPayloadMapper()
    {
    }

    public static JSONObject toFields(TravelOrderSyncRecord order, String ownerOpenId)
    {
        if (order == null || StringUtils.isEmpty(order.getOrderNo()))
        {
            throw new IllegalArgumentException("旅居订单编号不能为空");
        }
        if (StringUtils.isEmpty(ownerOpenId))
        {
            throw new IllegalArgumentException("飞书客服负责人不能为空");
        }

        JSONObject fields = new JSONObject();
        fields.put("小程序订单号", order.getOrderNo());
        fields.put("渠道", "小程序 ");
        fields.put("订单状态", orderStatus(order.getStatus()));
        putText(fields, "客户名称", order.getContactName());
        putText(fields, "联系方式", order.getContactPhone());
        putMultiSelect(fields, "房型", roomType(order.getSkuName()));
        putText(fields, "备注", order.getRemark());
        putDate(fields, "入住日期", order.getCheckInDate());
        putDate(fields, "离店日期", order.getCheckOutDate());
        putNumber(fields, "房间数", order.getRoomCount());
        putNumber(fields, "同行人数", order.getPeopleCount());
        putNumber(fields, "消费金额", order.getPayMoney());

        JSONArray owners = new JSONArray();
        JSONObject owner = new JSONObject();
        owner.put("id", ownerOpenId);
        owners.add(owner);
        fields.put("客服负责人", owners);
        return fields;
    }

    static String orderStatus(String status)
    {
        if ("0".equals(status)) return "待确认";
        if ("1".equals(status)) return "已确认";
        if ("2".equals(status)) return "已取消";
        if ("3".equals(status)) return "退款中";
        if ("4".equals(status)) return "已退款";
        throw new IllegalArgumentException("未知旅居订单状态: " + status);
    }

    static String roomType(String skuName)
    {
        String name = StringUtils.trimToEmpty(skuName);
        if (name.contains("豪华") && (name.contains("双人") || name.contains("双床") || name.contains("标间")))
            return "豪华双人间";
        if (name.contains("大床")) return "标准大床房";
        if (name.contains("三人")) return "三人间";
        if (name.contains("家庭")) return "标准家庭房";
        if (name.contains("套房")) return "标准套房";
        if (name.contains("双人") || name.contains("双床") || name.contains("标间")) return "标准双人间";
        return null;
    }

    private static void putText(JSONObject fields, String name, String value)
    {
        if (StringUtils.isNotEmpty(value)) fields.put(name, value);
    }

    private static void putDate(JSONObject fields, String name, java.util.Date value)
    {
        if (value != null) fields.put(name, value.getTime());
    }

    private static void putNumber(JSONObject fields, String name, Number value)
    {
        if (value != null) fields.put(name, value);
    }

    private static void putMultiSelect(JSONObject fields, String name, String value)
    {
        if (StringUtils.isNotEmpty(value)) fields.put(name, new String[]{value});
    }
}
