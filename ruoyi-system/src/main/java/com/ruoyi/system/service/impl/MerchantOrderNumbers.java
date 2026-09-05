package com.ruoyi.system.service.impl;

import java.util.Locale;
import com.ruoyi.common.exception.ServiceException;

final class MerchantOrderNumbers {
    private MerchantOrderNumbers() { }

    static String create(String prefix, Long id) {
        if (id == null || id <= 0 || !prefix.matches("[0-9]{2}")) {
            throw new ServiceException("订单编号无效");
        }
        return String.format(Locale.ROOT, "%s%020d", prefix, id);
    }

    static boolean valid(String number) {
        return number != null && number.matches("[A-Za-z0-9_\\-|*]{6,32}");
    }

    static String forPayment(String current, String prefix, Long id) {
        if (valid(current)) return current;
        // 仅修正此次回归生成的短编号；未知格式不能擅自换单号。
        if (!(prefix + id).equals(current)) throw new ServiceException("支付单号异常，请联系客服");
        return create(prefix, id);
    }
}
