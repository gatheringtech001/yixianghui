package com.ruoyi.system.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.CrmReadMapper;

@Service
public class CrmReadService
{
    private final CrmAccess access;
    private final CrmReadMapper mapper;
    public CrmReadService(CrmAccess access, CrmReadMapper mapper) { this.access = access; this.mapper = mapper; }

    @Transactional(readOnly = true)
    public Map<String, Object> workspace(String actor, String scope)
    {
        Map<String, Object> filters = access.resolve(actor, scope, false);
        Map<String, List<Map<String, Object>>> origins = new LinkedHashMap<>();
        for (Map<String, Object> origin : mapper.origins(filters))
            origins.computeIfAbsent(String.valueOf(origin.get("customerId")), key -> new ArrayList<>()).add(origin);
        List<Map<String, Object>> customers = new ArrayList<>();
        for (Map<String, Object> row : mapper.customers(filters)) {
            List<Map<String, Object>> sources = origins.get(String.valueOf(row.get("id")));
            customers.add(summary(row, sources == null ? new ArrayList<>() : sources));
        }
        return CrmValues.map("readOnly", filters.get("readOnly"), "customers", customers,
                "tasks", mapper.tasks(filters), "needs", mapper.needs(filters), "readAt", Instant.now().toString());
    }
    private Map<String, Object> summary(Map<String, Object> row, List<Map<String, Object>> origins)
    {
        Map<String, Object> result = CrmValues.pick(row, "id", "name", "status", "phoneMasked", "needs", "lastFollowupAt");
        Set<String> lines = new LinkedHashSet<>(), channels = new LinkedHashSet<>(), needs = new LinkedHashSet<>();
        boolean notes = CrmValues.text(row.get("note")) != null;
        addText(channels, row.get("source")); addText(needs, row.get("needs"));
        for (Map<String, Object> origin : origins) {
            lines.add(String.valueOf(origin.get("businessLine"))); addText(channels, origin.get("source")); addText(needs, origin.get("needs"));
            notes = notes || CrmValues.text(origin.get("note")) != null;
        }
        result.put("needs", needs.isEmpty() ? null : String.join(" · ", needs));
        result.put("businessLines", lines); result.put("sources", channels); result.put("historicalNotes", notes);
        return result;
    }
    private void addText(Set<String> values, Object raw) { String text = CrmValues.text(raw); if (text != null) values.add(text); }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String actor, String scope, String customerId)
    {
        Map<String, Object> filters = access.resolve(actor, scope, false);
        filters.put("customerId", CrmValues.customer(customerId));
        Map<String, Object> row = requireCustomer(filters);
        Map<String, Object> profile = CrmValues.pick(row, "id", "name", "status", "phone", "sex", "birthday", "age", "address", "registeredAt", "needs");
        profile.put("age", CrmValues.age(row));
        List<Map<String, Object>> contacts = new ArrayList<>(), notes = new ArrayList<>(), dates = new ArrayList<>(), origins = new ArrayList<>();
        historical(row, "客户档案", contacts, notes, dates);
        for (Map<String, Object> origin : mapper.origins(filters)) {
            Map<String, Object> item = CrmValues.pick(origin, "businessLine", "source", "status", "needs");
            for (String key : new String[]{"source", "status", "needs"}) item.put(key, CrmValues.text(item.get(key)));
            origins.add(item);
            historical(origin, "travel".equals(origin.get("businessLine")) ? "飞书旅居客户" : "飞书养老客户", contacts, notes, dates);
        }
        Map<String, Object> events = eventPage(filters);
        List<Map<String, Object>> ledger = new ArrayList<>(mapper.travel(filters)); ledger.addAll(mapper.eldercare(filters));
        return CrmValues.map("readOnly", filters.get("readOnly"), "profile", profile, "contacts", contacts, "origins", origins,
                "historicalNotes", notes, "historicalDates", dates, "events", events.get("events"), "nextEventCursor", events.get("nextCursor"),
                "tasks", mapper.tasks(filters), "needs", mapper.needs(filters), "consumption", CrmLedger.build(ledger), "readAt", Instant.now().toString());
    }
    private void historical(Map<String, Object> row, String source, List<Map<String, Object>> contacts,
            List<Map<String, Object>> notes, List<Map<String, Object>> dates)
    {
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> contact = CrmValues.map("name", CrmValues.text(row.get("name"+i)), "relation", CrmValues.text(row.get("relation"+i)),
                    "phone", CrmValues.text(row.get("phone"+i)), "source", source);
            if (contact.get("name") != null || contact.get("phone") != null) contacts.add(contact);
        }
        String note = CrmValues.text(row.get("note"));
        if (note != null) notes.add(CrmValues.map("source", source, "content", note, "updatedAt", row.get("updatedAt")));
        String[] fields = {"firstAt", "secondAt", "lastAt"}, labels = {"首次回访", "第二次回访", "最近回访"};
        for (int i=0; i<fields.length; i++) if (row.get(fields[i]) != null)
            dates.add(CrmValues.map("label", source + " · " + labels[i], "occurredAt", row.get(fields[i])));
    }
    @Transactional(readOnly = true)
    public Map<String, Object> events(String actor, String scope, String customerId, String before)
    {
        Map<String, Object> filters = access.resolve(actor, scope, false);
        filters.put("customerId", CrmValues.customer(customerId)); requireCustomer(filters);
        if (!"first".equals(before)) {
            if (before == null || !before.matches("[1-9][0-9]{0,17}")) throw new TalentCenterApiException(400, "记录游标不正确");
            filters.put("before", Long.valueOf(before));
        }
        return eventPage(filters);
    }
    private Map<String, Object> eventPage(Map<String, Object> filters)
    {
        List<Map<String, Object>> rows = mapper.events(filters), events = new ArrayList<>();
        for (Map<String, Object> row : rows.subList(0, Math.min(50, rows.size())))
            events.add(CrmValues.pick(row, "id", "kind", "content", "method", "actorName", "occurredAt", "createdAt"));
        return CrmValues.map("events", events, "nextCursor", rows.size()>50 ? events.get(49).get("id") : null);
    }
    private Map<String, Object> requireCustomer(Map<String, Object> filters)
    {
        Map<String, Object> row = mapper.profile(filters);
        if (row == null) throw new TalentCenterApiException(404, "客户不存在或无权查看");
        return row;
    }
}
