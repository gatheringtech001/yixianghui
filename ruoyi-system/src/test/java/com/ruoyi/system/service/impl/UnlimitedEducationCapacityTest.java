package com.ruoyi.system.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.AppGoodsEducationExt;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.EducationCapacityPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnlimitedEducationCapacityTest {
    private AppGoods course(Integer maximum, Long stock) {
        AppGoods g = new AppGoods();
        g.setGoodsType("education"); g.setStatus("1"); g.setStock(stock);
        AppGoodsEducationExt ext = new AppGoodsEducationExt();
        ext.setClassSizeMax(maximum); g.setEducationExt(ext);
        return g;
    }

    private void validate(AppGoods g) throws Exception {
        AppGoodsOrder o = new AppGoodsOrder();
        o.setGoodsCount(1L); o.setContactName("测试联系人"); o.setContactPhone("13800138000");
        Method m = AppGoodsOrderServiceImpl.class.getDeclaredMethod("validateEducationOrder", AppGoodsOrder.class, AppGoods.class);
        m.setAccessible(true); m.invoke(new AppGoodsOrderServiceImpl(), o, g);
    }

    @Test void unlimitedCourseAcceptsZeroStock() throws Exception {
        validate(course(0, 0L));
    }

    @Test void unlimitedCourseAcceptsNullStock() throws Exception {
        validate(course(0, null));
    }

    @Test void unspecifiedMaximumIsNotUnlimited() throws Exception {
        AppGoods g = course(null, 0L);
        assertFalse(EducationCapacityPolicy.isUnlimited(g));
        assertTrue(assertThrows(InvocationTargetException.class, () -> validate(g)).getCause() instanceof ServiceException);
    }

    @Test void finiteCourseStillRejectsZeroStock() throws Exception {
        assertTrue(assertThrows(InvocationTargetException.class, () -> validate(course(6, 0L))).getCause() instanceof ServiceException);
    }

    @Test void unlimitedCourseStillRejectsClosedSignup() throws Exception {
        AppGoods g = course(0, 0L); g.getEducationExt().setSignupEnd("2000-01-01");
        assertTrue(assertThrows(InvocationTargetException.class, () -> validate(g)).getCause() instanceof ServiceException);
    }

    @Test void legacyProjectionOnlyChangesExplicitUnlimitedPublishedCourses() {
        AppGoods g = course(0, 0L);
        EducationCapacityPolicy.prepareLegacyAvailability(g);
        assertEquals(1L, g.getStock()); assertEquals(0, g.getEducationExt().getClassSizeMax());
        AppGoods finite = course(6, 0L); EducationCapacityPolicy.prepareLegacyAvailability(finite); assertEquals(0L, finite.getStock());
        AppGoods hidden = course(0, 0L); hidden.setStatus("0"); EducationCapacityPolicy.prepareLegacyAvailability(hidden); assertEquals(0L, hidden.getStock());
        AppGoods retail = course(0, 0L); retail.setGoodsType("online"); EducationCapacityPolicy.prepareLegacyAvailability(retail); assertEquals(0L, retail.getStock());
    }
}
