package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class RetailCheckout {
    private RetailCheckout() { }
    @Data public static class Request {
        private List<Long> cartIds;
        private Long goodsId;
        private Integer count;
        private Long addressId;
        private Long couponGotId;
        private String remark;
        private String checkoutKey;
        private String fingerprint;
    }
    @Data public static class Line {
        private Long cartId;
        private Long goodsId;
        private Long categoryId;
        private Long supplierId;
        private Long deptId;
        private String goodsName;
        private String goodsCover;
        private String specifications;
        private String unit;
        private int count;
        private BigDecimal price;
        private BigDecimal shipping;
        private BigDecimal subtotal;
        private BigDecimal discount = BigDecimal.ZERO;
    }
    @Data public static class Coupon {
        private Long gotId;
        private String name;
        private BigDecimal discount;
    }
    @Data public static class Quote {
        private List<Line> items;
        private Long supplierId;
        private String supplierName;
        private BigDecimal moneyTotal;
        private BigDecimal moneyExpress;
        private BigDecimal moneyDiscount = BigDecimal.ZERO;
        private BigDecimal moneyPayable;
        private Long couponGotId;
        private String channelCode;
        private String fingerprint;
        private List<Coupon> coupons = new ArrayList<>();
    }
}
