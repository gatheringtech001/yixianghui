package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import com.ruoyi.system.domain.RetailCheckout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetailPricingTest {
    @Test void totalsUseEveryLineAndAllocateDiscountWithoutLosingCents() {
        RetailCheckout.Line corn = line("29.90", 2, "5");
        RetailCheckout.Line tea = line("60", 1, "0");
        RetailCheckout.Quote quote = RetailPricing.total(Arrays.asList(corn, tea));
        assertEquals(new BigDecimal("119.80"), quote.getMoneyTotal());
        assertEquals(new BigDecimal("5.00"), quote.getMoneyExpress());
        RetailPricing.allocate(quote, new BigDecimal("10"), Arrays.asList(corn, tea));
        assertEquals(new BigDecimal("114.80"), quote.getMoneyPayable());
        assertEquals(new BigDecimal("10.00"), corn.getDiscount().add(tea.getDiscount()));
    }
    @Test void itemCouponNeverDiscountsOtherItemsOrShipping() {
        RetailCheckout.Line corn = line("10", 1, "5");
        RetailCheckout.Line tea = line("60", 1, "0");
        RetailCheckout.Quote quote = RetailPricing.total(Arrays.asList(corn, tea));
        RetailPricing.allocate(quote, new BigDecimal("20"), Arrays.asList(corn));
        assertEquals(new BigDecimal("65.00"), quote.getMoneyPayable());
        assertEquals(BigDecimal.ZERO, tea.getDiscount());
        assertEquals(new BigDecimal("10.00"), corn.getDiscount());
    }
    private RetailCheckout.Line line(String price, int count, String shipping) {
        RetailCheckout.Line line = new RetailCheckout.Line();
        line.setPrice(new BigDecimal(price)); line.setCount(count);
        line.setShipping(new BigDecimal(shipping));
        return line;
    }
}
