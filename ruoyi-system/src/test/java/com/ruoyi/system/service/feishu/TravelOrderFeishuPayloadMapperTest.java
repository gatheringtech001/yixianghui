package com.ruoyi.system.service.feishu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.domain.TravelOrderSyncRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TravelOrderFeishuPayloadMapperTest
{
    @Test
    void mapsConfirmedTravelOrderWithoutUnsupportedFields()
    {
        TravelOrderSyncRecord order = sampleOrder("1");

        JSONObject fields = TravelOrderFeishuPayloadMapper.toFields(order, "ou_owner");

        assertEquals("20001", fields.getString("小程序订单号"));
        assertEquals("小程序 ", fields.getString("渠道"));
        assertEquals("已确认", fields.getString("订单状态"));
        assertEquals(new BigDecimal("688.00"), fields.getBigDecimal("消费金额"));
        assertEquals("标准双人间", fields.getJSONArray("房型").getString(0));
        JSONArray owners = fields.getJSONArray("客服负责人");
        assertEquals("ou_owner", owners.getJSONObject(0).getString("id"));
        assertFalse(fields.containsKey("订单编号"));
        assertFalse(fields.containsKey("关联基地"));
        assertFalse(fields.containsKey("入住晚数"));
        assertFalse(fields.containsKey("间夜数"));
        assertFalse(fields.containsKey("关联客户"));
        assertFalse(fields.containsKey("服务备注"));
    }

    @Test
    void mapsRefundStatuses()
    {
        assertEquals("退款中", TravelOrderFeishuPayloadMapper.orderStatus("3"));
        assertEquals("已退款", TravelOrderFeishuPayloadMapper.orderStatus("4"));
    }

    @Test
    void normalizesPackageRoomNameToExistingFeishuOption()
    {
        TravelOrderSyncRecord order = sampleOrder("1");
        order.setSkuName("测试标准双床房");

        JSONObject fields = TravelOrderFeishuPayloadMapper.toFields(order, "ou_owner");

        assertEquals("标准双人间", fields.getJSONArray("房型").getString(0));
    }

    @Test
    void omitsUnknownRoomTypeInsteadOfRejectingWholeRecord()
    {
        TravelOrderSyncRecord order = sampleOrder("1");
        order.setSkuName("2天1晚");

        JSONObject fields = TravelOrderFeishuPayloadMapper.toFields(order, "ou_owner");

        assertFalse(fields.containsKey("房型"));
    }

    private TravelOrderSyncRecord sampleOrder(String status)
    {
        TravelOrderSyncRecord order = new TravelOrderSyncRecord();
        order.setOrderId(1L);
        order.setOrderNo("20001");
        order.setContactName("测试联系人");
        order.setContactPhone("13000000000");
        order.setSkuName("标准双人间");
        order.setCheckInDate(new Date(1788105600000L));
        order.setCheckOutDate(new Date(1788364800000L));
        order.setRoomCount(2L);
        order.setPeopleCount(4);
        order.setPayMoney(new BigDecimal("688.00"));
        order.setStatus(status);
        order.setRemark("靠近电梯");
        return order;
    }
}
