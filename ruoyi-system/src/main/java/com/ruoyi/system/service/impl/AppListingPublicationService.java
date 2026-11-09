package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Publication never passes through the aggregate content/SKU update path. */
@Service
public class AppListingPublicationService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RedisCache cache;

    @Transactional(rollbackFor = Exception.class)
    public int changeGoods(Long id, String expectedStatus, String status) {
        validateRequest(id, expectedStatus, status);
        Map<String, Object> row = lock("app_goods", "goods_id", id);
        checkExpected(row, expectedStatus);
        if ("1".equals(status)) {
            requireText(row, "goods_name", "填写商品名称");
            requireText(row, "goods_cover", "上传封面");
            requirePositive(row, "dept_id", "选择所属站点");
            requirePositive(row, "category_id", "选择分类");
            if ("1".equals(text(row.get("is_sku"))) && !"education".equals(text(row.get("goods_type")))) {
                Integer available = jdbc.queryForObject("select count(*) from app_goods_sku where goods_id=? and status='1' and price>0 and stock>0", Integer.class, id);
                if (available == null || available == 0) throw new ServiceException("配置有价格和库存的启用规格后再上架");
            } else {
                requirePositive(row, "price", "填写大于 0 的价格");
                requirePositive(row, "stock", "填写可售库存");
            }
        }
        int changed = update("app_goods", "goods_id", id, status);
        // Evict after commit, never allow the old row to be repopulated before commit.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override public void afterCommit() { cache.deleteObject(CacheConstants.APP_GOODS + "id:" + id); }
        });
        return changed;
    }

    @Transactional(rollbackFor = Exception.class)
    public int changeActivity(Long id, String expectedStatus, String status) {
        validateRequest(id, expectedStatus, status);
        Map<String, Object> row = lock("app_activity", "activity_id", id);
        checkExpected(row, expectedStatus);
        if ("1".equals(status)) {
            requireText(row, "activity_name", "填写活动标题");
            requireText(row, "activity_cover", "上传封面");
            requireText(row, "address", "填写活动地点");
            requireText(row, "activity_time", "填写活动时间");
            requirePositive(row, "dept_id", "选择所属站点");
            if ("0".equals(text(row.get("is_free")))) requirePositive(row, "vip_price", "填写大于 0 的实付价");
            for (String key : new String[]{"sign_end_time", "activity_end_time"}) {
                Object value = row.get(key);
                if (value instanceof Date && ((Date) value).getTime() <= System.currentTimeMillis()) {
                    throw new ServiceException("sign_end_time".equals(key) ? "报名截止时间已过" : "活动已结束");
                }
            }
            if (number(row.get("max_count")).signum() > 0 && number(row.get("sign_count")).compareTo(number(row.get("max_count"))) >= 0) {
                throw new ServiceException("报名名额已满");
            }
        }
        return update("app_activity", "activity_id", id, status);
    }

    private Map<String, Object> lock(String table, String key, Long id) {
        // table/key are internal constants, never supplied by a request.
        List<Map<String, Object>> rows = jdbc.queryForList("select * from " + table + " where " + key + "=? for update", id);
        if (rows.isEmpty()) throw new ServiceException("该内容不存在");
        return rows.get(0);
    }

    private int update(String table, String key, Long id, String status) {
        int changed = jdbc.update("update " + table + " set status=?,update_time=current_timestamp where " + key + "=?", status, id);
        if (changed != 1) throw new ServiceException("状态保存失败，请刷新后重试");
        return changed;
    }

    private static void validateRequest(Long id, String expected, String status) {
        if (id == null || id <= 0 || expected == null || !("".equals(expected) || "0".equals(expected) || "1".equals(expected)) || !("0".equals(status) || "1".equals(status))) {
            throw new ServiceException("上下架参数无效");
        }
    }
    private static void checkExpected(Map<String, Object> row, String expected) {
        if (!expected.equals(text(row.get("status")))) throw new ServiceException("状态已被其他人修改，请刷新列表后重试");
    }
    private static void requireText(Map<String, Object> row, String key, String message) {
        if (text(row.get(key)).trim().isEmpty()) throw new ServiceException(message);
    }
    private static void requirePositive(Map<String, Object> row, String key, String message) {
        if (number(row.get(key)).signum() <= 0) throw new ServiceException(message);
    }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private static BigDecimal number(Object value) {
        try { return new BigDecimal(text(value)); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }
}
