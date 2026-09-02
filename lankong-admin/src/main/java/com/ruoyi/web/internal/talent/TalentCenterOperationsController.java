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

@RestController
@RequestMapping("/internal/talent-center/v1/operations")
public class TalentCenterOperationsController
{
    private final TalentCenterOperationsService service;

    public TalentCenterOperationsController(TalentCenterOperationsService service)
    {
        this.service = service;
    }

    @GetMapping
    public AjaxResult snapshot(HttpServletRequest request)
    {
        return AjaxResult.success(service.snapshot(actorId(request), actorScope(request)));
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
