package com.ruoyi.system.domain;

/** Explicit zero class-size maximum means unlimited enrollment, not zero stock. */
public final class EducationCapacityPolicy {
    private EducationCapacityPolicy() { }

    public static boolean isUnlimited(AppGoods goods) {
        return goods != null && "education".equals(goods.getGoodsType())
                && goods.getEducationExt() != null
                && Integer.valueOf(0).equals(goods.getEducationExt().getClassSizeMax());
    }

    /**
     * Older education clients check stock > 0 before enrolling one person.
     * Project one available enrollment, not a fabricated total capacity. The
     * stored stock is untouched and the mapper never decrements unlimited stock.
     */
    public static void prepareLegacyAvailability(AppGoods goods) {
        if (isUnlimited(goods) && "1".equals(goods.getStatus())
                && (goods.getStock() == null || goods.getStock() <= 0)) {
            goods.setStock(1L);
        }
    }
}
