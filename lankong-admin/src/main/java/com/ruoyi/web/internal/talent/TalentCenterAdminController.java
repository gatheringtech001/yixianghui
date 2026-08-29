package com.ruoyi.web.internal.talent;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.talent.TalentCenterResource;
import com.ruoyi.system.domain.talent.TalentCenterStatusRequest;
import com.ruoyi.system.service.TalentCenterAdminService;

@RestController
@RequestMapping("/internal/talent-center/v1/admin/resources")
public class TalentCenterAdminController
{
    private final TalentCenterAdminService service;

    public TalentCenterAdminController(TalentCenterAdminService service)
    {
        this.service = service;
    }

    @GetMapping("/{type}")
    public AjaxResult list(@PathVariable String type,
            @RequestHeader(value = "X-Actor-Unionid", required = false) String actorUnionid,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize)
    {
        List<TalentCenterResource> resources = service.list(type, actorUnionid, pageNum, pageSize);
        return AjaxResult.success(resources).put("pageNum", pageNum).put("pageSize", pageSize);
    }

    @GetMapping("/{type}/{id}")
    public AjaxResult get(@PathVariable String type, @PathVariable Long id,
            @RequestHeader(value = "X-Actor-Unionid", required = false) String actorUnionid)
    {
        return AjaxResult.success(service.get(type, id, actorUnionid));
    }

    @PutMapping("/{type}/{id}/status")
    public ResponseEntity<AjaxResult> updateStatus(@PathVariable String type, @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody TalentCenterStatusRequest body, HttpServletRequest request)
    {
        String serviceId = (String) request.getAttribute(TalentCenterHmacFilter.SERVICE_ID_ATTRIBUTE);
        TalentCenterResource resource = service.updateStatus(type, id, body, idempotencyKey, serviceId,
                IpUtils.getIpAddr(request));
        return ResponseEntity.ok(AjaxResult.success(resource));
    }
}
