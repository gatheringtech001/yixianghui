package com.ruoyi.system.service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

final class CrmLedger
{
    private CrmLedger() { }
    static Map<String, Object> build(List<Map<String, Object>> rows)
    {
        if (rows.size() > 2000) throw new TalentCenterApiException(422, "该客户记录超过单次读取上限，暂不能提供完整汇总");
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> entry = CrmValues.pick(row, "id", "businessLine", "code", "title", "occurredOn", "amountCents", "status", "allocation", "excluded");
            entry.put("excluded", Boolean.TRUE.equals(row.get("excluded")) || "1".equals(String.valueOf(row.get("excluded"))) || !"single".equals(row.get("allocation")));
            entries.add(entry);
        }
        return CrmValues.map("entries", entries, "travel", summary(entries,"travel"), "eldercare", summary(entries,"eldercare"),
                "note", "仅统计已确认关联且当前管家可见的记录，不代表客户全部消费。旅居为订单金额，非实收净额；已取消、退款中、已退款不计入。养老为消费金额，不含充值。多人共同消费与归属未确认的记录不计入个人合计，未填金额不作零处理。");
    }
    private static Map<String, Object> summary(List<Map<String, Object>> entries, String line)
    {
        int count=0, missing=0, excluded=0, shared=0, known=0; long total=0;
        for (Map<String, Object> entry : entries) {
            if (!line.equals(entry.get("businessLine"))) continue;
            count++;
            if ("shared".equals(entry.get("allocation"))) shared++;
            if (Boolean.TRUE.equals(entry.get("excluded"))) { excluded++; continue; }
            if (entry.get("amountCents") == null) { missing++; continue; }
            known++; total = Math.addExact(total, ((Number)entry.get("amountCents")).longValue());
        }
        return CrmValues.map("supported", true, "recordCount", count, "totalCents", known == 0 ? null : total,
                "missingAmountCount", missing, "excludedCount", excluded, "sharedCount", shared);
    }
}
