package com.ruoyi.web.controller.app;

import java.util.Arrays;
import java.util.Collections;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletResponse;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppActivityOrder;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

class AppOrderRosterExportTest {
    @Test void goodsIncludesProjectFullPhoneAndUnpaidState() {
        AppGoodsOrder order = new AppGoodsOrder();
        order.setGoodsType("education"); order.setGoodsName("合成课程");
        order.setContactName("合成人"); order.setContactPhone("013800000001"); order.setPayStatus("0");
        AppOrderRosterExportController.RosterRow row = AppOrderRosterExportController.goodsRow(order);
        assertEquals("合成课程", row.item); assertEquals("013800000001", row.phone);
        assertEquals("未付款", row.payment); assertEquals("课程", row.business);
    }
    @Test void activityUsesRealTitleAndNeverGuessesUnknownPayment() {
        AppActivityOrder order = new AppActivityOrder();
        order.setActivityId(7L); order.setSignMobile("13800000001"); order.setSignCount(2); order.setPayStatus("9");
        AppOrderRosterExportController.RosterRow row = AppOrderRosterExportController.activityRow(order, "合成活动");
        assertEquals("合成活动", row.item); assertEquals("13800000001", row.phone);
        assertEquals(2, row.quantity); assertEquals("支付状态 9", row.payment);
        assertEquals("已付款", AppOrderRosterExportController.payment("1"));
    }
    @Test void exportUsesExactExistingPermissions() throws Exception {
        for (String type : Arrays.asList("goods", "activity")) {
            Class<?> query = "goods".equals(type) ? AppGoodsOrder.class : AppActivityOrder.class;
            Method m = AppOrderRosterExportController.class.getMethod(type, HttpServletResponse.class, query);
            assertEquals("@ss.hasPermi('system:app_" + type + "_order:export')", m.getAnnotation(PreAuthorize.class).value());
        }
        AppOrderRosterExportController.checkLimit(10000);
        assertThrows(RuntimeException.class, () -> AppOrderRosterExportController.checkLimit(10001));
    }
    @Test void workbookKeepsPhonesAsTextAndNeverCreatesFormulas() throws Exception {
        AppOrderRosterExportController.RosterRow row = new AppOrderRosterExportController.RosterRow();
        row.item = "=HYPERLINK(\"https://example.invalid\")"; row.phone = "013800000001";
        ExcelUtil<AppOrderRosterExportController.RosterRow> util = new ExcelUtil<>(AppOrderRosterExportController.RosterRow.class);
        util.init(Collections.singletonList(row), "名单", "", Excel.Type.EXPORT); util.writeSheet();
        Field f = ExcelUtil.class.getDeclaredField("wb"); f.setAccessible(true);
        try (Workbook wb = (Workbook) f.get(util)) {
            assertEquals(CellType.STRING, wb.getSheetAt(0).getRow(1).getCell(2).getCellType());
            assertNotEquals(CellType.FORMULA, wb.getSheetAt(0).getRow(1).getCell(2).getCellType());
            assertEquals("013800000001", wb.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
        }
    }
}
