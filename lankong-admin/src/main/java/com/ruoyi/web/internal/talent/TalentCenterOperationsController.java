package com.ruoyi.web.internal.talent;

import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.talent.TalentCenterOperationUpdateRequest;
import com.ruoyi.system.service.TalentCenterOperationsService;
import com.ruoyi.system.service.TalentCenterTestViewService;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@RestController
@RequestMapping("/internal/talent-center/v1/operations")
public class TalentCenterOperationsController
{
    private final TalentCenterOperationsService service;
    private final TalentCenterTestViewService testView;

    public TalentCenterOperationsController(TalentCenterOperationsService service, TalentCenterTestViewService testView)
    {
        this.service = service;
        this.testView = testView;
    }

    @GetMapping("/test-view")
    public AjaxResult testView(HttpServletRequest request)
    {
        return AjaxResult.success(testView.status(actorId(request)));
    }

    @PutMapping("/test-view/{state}")
    public AjaxResult switchTestView(@PathVariable String state, HttpServletRequest request)
    {
        if (!"on".equals(state) && !"off".equals(state))
            throw new TalentCenterApiException(400, "测试视角参数不正确");
        return AjaxResult.success(testView.setEnabled(actorId(request), "on".equals(state)));
    }

    @GetMapping
    public AjaxResult snapshot(HttpServletRequest request)
    {
        return AjaxResult.success(service.snapshot(actorId(request), actorScope(request)));
    }

    @GetMapping("/actor-status")
    public AjaxResult actorStatus(HttpServletRequest request)
    {
        return AjaxResult.success(service.actorStatus(actorId(request), actorScope(request)));
    }
    @GetMapping("/orders/{recordId}")
    public AjaxResult order(@PathVariable String recordId, HttpServletRequest request)
    {
        return AjaxResult.success(service.order(actorId(request), actorScope(request), recordId));
    }

    @GetMapping({"/commissions/{recipient}/{before}", "/commissions/{recipient}/{before}/{month}"})
    public AjaxResult commissions(@PathVariable String recipient, @PathVariable String before,
            @PathVariable(required = false) String month, HttpServletRequest request)
    {
        return AjaxResult.success(service.commissions(actorId(request), actorScope(request), recipient, before, month));
    }

    @PutMapping("/{businessLine}/{resource}/{recordId}")
    public ResponseEntity<AjaxResult> update(@PathVariable String businessLine, @PathVariable String resource,
            @PathVariable String recordId, @RequestBody TalentCenterOperationUpdateRequest body,
            HttpServletRequest request)
    {
        return ResponseEntity.ok(AjaxResult.success(service.update(actorId(request), actorScope(request), businessLine, resource,
                recordId, body, request.getHeader("Idempotency-Key"))));
    }

    private String actorId(HttpServletRequest request)
    {
        return (String) request.getAttribute(TalentCenterHmacFilter.ACTOR_ID_ATTRIBUTE);
    }

    private String actorScope(HttpServletRequest request)
    {
        return (String) request.getAttribute(TalentCenterHmacFilter.ACTOR_SCOPE_ATTRIBUTE);
    }
}
