package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.ruoyi.system.domain.RetailCheckout.Line;
import com.ruoyi.system.domain.RetailCheckout.Quote;

final class RetailPricing {
    private RetailPricing() { }
    static Quote total(List<Line> lines) {
        Quote quote = new Quote();
        quote.setItems(lines);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal shipping = BigDecimal.ZERO;
        for (Line line : lines) {
            line.setSubtotal(line.getPrice().multiply(BigDecimal.valueOf(line.getCount())).setScale(2));
            total = total.add(line.getSubtotal());
            // 沿用商品配置的运费：每个商品行收一次，不擅自改成整单包邮。
            shipping = shipping.add(line.getShipping());
        }
        quote.setMoneyTotal(total.setScale(2));
        quote.setMoneyExpress(shipping.setScale(2));
        quote.setMoneyPayable(total.add(shipping).setScale(2));
        return quote;
    }
    static void allocate(Quote quote, BigDecimal requested, List<Line> eligible) {
        BigDecimal basis = eligible.stream().map(Line::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = requested.min(basis).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = discount;
        for (int i = 0; i < eligible.size(); i++) {
            Line line = eligible.get(i);
            BigDecimal amount = i == eligible.size() - 1 ? remaining :
                    discount.multiply(line.getSubtotal()).divide(basis, 2, RoundingMode.DOWN);
            line.setDiscount(amount);
            remaining = remaining.subtract(amount);
        }
        quote.setMoneyDiscount(discount);
        quote.setMoneyPayable(quote.getMoneyTotal().add(quote.getMoneyExpress()).subtract(discount));
    }
}
