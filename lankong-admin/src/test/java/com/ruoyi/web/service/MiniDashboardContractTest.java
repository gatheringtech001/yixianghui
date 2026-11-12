package com.ruoyi.web.service;
import java.time.*;
import java.util.*;
import java.io.IOException;
import java.lang.reflect.Field;
import com.alibaba.fastjson2.JSONObject;
/** Standalone runtime contract tests, runnable against the deployed Java 8 dependency set. */
public class MiniDashboardContractTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    static class FakeTraffic extends WeChatTrafficClient {
        String body; int calls; int error; boolean broken;
        FakeTraffic(String response) throws Exception {
            body=response;
            for (String name:new String[]{"appId","appSecret"}) { Field f=WeChatTrafficClient.class.getDeclaredField(name); f.setAccessible(true); f.set(this,"synthetic-test"); }
        }
        @Override protected JSONObject post(String url,JSONObject request) throws IOException {
            calls++;
            if (broken) throw new IOException("secret-url-must-not-leak");
            if (url.contains("stable_token")) {
                check(Boolean.FALSE.equals(request.getBoolean("force_refresh")));
                return JSONObject.parseObject(error==0?"{\"access_token\":\"synthetic\",\"expires_in\":7200}":"{\"errcode\":"+error+"}");
            }
            check(request.getString("begin_date").equals(request.getString("end_date")));
            return JSONObject.parseObject(body);
        }
    }
    public static void main(String[] args) throws Exception {
        org.springframework.web.bind.annotation.RequestParam binding = com.ruoyi.web.controller.monitor.SysUserOnlineController.class.getMethod("getUserStaticCount",String.class).getParameters()[0].getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);
        check("period".equals(binding.name()) && "30d".equals(binding.defaultValue()));
        LocalDate day=LocalDate.of(2026,11,12);
        check(MiniDashboardService.periodStart("today",day).equals(day));
        check(MiniDashboardService.periodStart("yesterday",day).equals(day.minusDays(1)));
        check(MiniDashboardService.periodStart("7d",day).equals(day.minusDays(6)));
        check(MiniDashboardService.periodStart("30d",day).equals(day.minusDays(29)));
        check(MiniDashboardService.periodStart("all",day)==null);
        try { MiniDashboardService.periodStart("injection",day); throw new AssertionError(); } catch(com.ruoyi.common.exception.ServiceException expected) { }
        String date=LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1).toString().replace("-","");
        FakeTraffic ok=new FakeTraffic("{\"list\":[{\"ref_date\":\""+date+"\",\"visit_uv\":12,\"visit_pv\":28,\"visit_uv_new\":4,\"session_cnt\":20,\"stay_time_uv\":15.5}]}");
        Map<String,Object> traffic=ok.yesterday(); check("available".equals(traffic.get("status"))); check(Long.valueOf(12).equals(traffic.get("visitors"))); check(Long.valueOf(28).equals(traffic.get("pageViews"))); ok.yesterday(); check(ok.calls==2);
        FakeTraffic zero=new FakeTraffic("{\"list\":[{\"ref_date\":\""+date+"\",\"visit_uv\":0,\"visit_pv\":0}]}"); check("available".equals(zero.yesterday().get("status")));
        FakeTraffic missing=new FakeTraffic("{\"list\":[]}"); check("pending".equals(missing.yesterday().get("status"))); check(!missing.yesterday().containsKey("visitors"));
        FakeTraffic wrongDate=new FakeTraffic("{\"list\":[{\"ref_date\":\"20000101\",\"visit_uv\":999,\"visit_pv\":999}]}"); check("pending".equals(wrongDate.yesterday().get("status")));
        FakeTraffic denied=new FakeTraffic("{\"errcode\":48001}"); check("unavailable".equals(denied.yesterday().get("status"))); check(!denied.yesterday().containsKey("visitors"));
        FakeTraffic auth=new FakeTraffic("{}"); auth.error=40064; check(Integer.valueOf(40064).equals(auth.yesterday().get("errorCode")));
        FakeTraffic network=new FakeTraffic("{}"); network.broken=true; check(!network.yesterday().toString().contains("secret-url"));
        check(!new WeChatTrafficClient().yesterday().containsKey("visitors"));
        System.out.println("MiniDashboard Java contracts passed (date ranges, traffic, cache, real zero, missing data, permission and safe errors)");
    }
}
