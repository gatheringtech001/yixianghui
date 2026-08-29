package com.ruoyi.system.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 旅居订单同步到飞书所需的只读投影。
 */
@Data
public class TravelOrderSyncRecord
{
    private Long orderId;
    private String orderNo;
    private String contactName;
    private String contactPhone;
    private String skuName;
    private Date checkInDate;
    private Date checkOutDate;
    private Long roomCount;
    private Integer peopleCount;
    private BigDecimal payMoney;
    private String status;
    private String remark;
}
