package com.ruoyi.web.internal.talent;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.CrmReadService;
import com.ruoyi.system.service.CrmWriteService;

@RestController
@RequestMapping("/internal/talent-center/v1/crm")
public class CrmController
{
    private final CrmReadService reads;
    private final CrmWriteService writes;
    public CrmController(CrmReadService reads,CrmWriteService writes) { this.reads=reads; this.writes=writes; }
    @GetMapping public AjaxResult workspace(HttpServletRequest request) { return AjaxResult.success(reads.workspace(actor(request),scope(request))); }
    @GetMapping("/customers/{customer}") public AjaxResult detail(@PathVariable String customer,HttpServletRequest request) {
        return AjaxResult.success(reads.detail(actor(request),scope(request),customer));
    }
    @GetMapping("/customers/{customer}/events/{before}") public AjaxResult events(@PathVariable String customer,@PathVariable String before,HttpServletRequest request) {
        return AjaxResult.success(reads.events(actor(request),scope(request),customer,before));
    }
    @PostMapping("/customers/{customer}/{action}") public AjaxResult create(@PathVariable String customer,@PathVariable String action,
            @RequestBody Map<String,Object> input,HttpServletRequest request) {
        return AjaxResult.success(writes.save(actor(request),scope(request),customer,action,null,input));
    }
    @PutMapping("/customers/{customer}/{action}/{entityId}") public AjaxResult update(@PathVariable String customer,@PathVariable String action,
            @PathVariable String entityId,@RequestBody Map<String,Object> input,HttpServletRequest request) {
        return AjaxResult.success(writes.save(actor(request),scope(request),customer,action,entityId,input));
    }
    private String actor(HttpServletRequest request) { return (String)request.getAttribute(TalentCenterHmacFilter.ACTOR_ID_ATTRIBUTE); }
    private String scope(HttpServletRequest request) { return (String)request.getAttribute(TalentCenterHmacFilter.ACTOR_SCOPE_ATTRIBUTE); }
}
