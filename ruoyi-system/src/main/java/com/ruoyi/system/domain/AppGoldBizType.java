package com.ruoyi.system.domain;

/**
 * Gold business types stored in app_user_gold_log.business_type.
 * Keep Chinese values identical to historical records for idempotency/UI.
 * Card types are reserved; enabled by gold.scope.card.
 */
public final class AppGoldBizType
{
    private AppGoldBizType()
    {
    }

    /** goods/hotel/education pay */
    public static final String GOODS_PAY = "\u8ba2\u5355\u652f\u4ed8";

    /** goods/hotel/education refund */
    public static final String GOODS_REFUND = "\u8ba2\u5355\u9000\u6b3e";

    /** activity pay */
    public static final String ACTIVITY_PAY = "\u6d3b\u52a8\u62a5\u540d\u652f\u4ed8";

    /** activity refund */
    public static final String ACTIVITY_REFUND = "\u6d3b\u52a8\u9000\u6b3e";

    /** member card pay (reserved) */
    public static final String CARD_PAY = "\u5f00\u901a\u4f1a\u5458\u5361\u652f\u4ed8";

    /** member card refund (reserved) */
    public static final String CARD_REFUND = "\u4f1a\u5458\u5361\u9000\u6b3e";

    /** sign-in */
    public static final String SIGN = "\u7b7e\u5230";

    /** invite register reward */
    public static final String INVITE_REGISTER = "\u9080\u8bf7\u6ce8\u518c";

    public static final String TITLE_PAY = "\u652f\u4ed8\u83b7\u53d6\u91d1\u5e01";

    public static final String TITLE_REFUND = "\u9000\u6b3e\u6263\u56de\u91d1\u5e01";

    public static final String TITLE_INVITE = "\u9080\u8bf7\u6ce8\u518c\u5956\u52b1";
}