package com.ruoyi.web.internal.talent;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.TalentInboxService;
import com.ruoyi.system.service.TalentBindingService;
import com.ruoyi.system.service.TalentWorkflowDetailService;

@RestController
@RequestMapping("/internal/talent-center/v1/workflow")
public class TalentCenterWorkflowController {
    private final TalentInboxService inbox; private final TalentBindingService binding; private final TalentWorkflowDetailService details;
    public TalentCenterWorkflowController(TalentInboxService inbox,TalentBindingService binding,TalentWorkflowDetailService details){this.inbox=inbox;this.binding=binding;this.details=details;}
    @GetMapping("/records/{category}/{id}")
    public AjaxResult detail(@PathVariable String category,@PathVariable String id,HttpServletRequest request){return AjaxResult.success(details.detail(actor(request),scope(request),category+":"+id));}
    @GetMapping("/inbox/{view}")
    public AjaxResult summary(@PathVariable String view,HttpServletRequest request){return AjaxResult.success(inbox.summary(actor(request),scope(request),view));}
    @GetMapping("/inbox/{view}/{category}/{cursor}")
    public AjaxResult page(@PathVariable String view,@PathVariable String category,@PathVariable String cursor,HttpServletRequest request){return AjaxResult.success(inbox.page(actor(request),scope(request),view,category,cursor));}
    @PostMapping("/options")
    public AjaxResult options(@RequestBody Map<String,Object> input,HttpServletRequest request){return AjaxResult.success(binding.options(actor(request),scope(request),input));}
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody Map<String,Object> input,HttpServletRequest request){return AjaxResult.success(binding.preview(actor(request),scope(request),input));}
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody Map<String,Object> input,HttpServletRequest request){return AjaxResult.success(binding.execute(actor(request),scope(request),input));}
    private String actor(HttpServletRequest request){return (String)request.getAttribute(TalentCenterHmacFilter.ACTOR_ID_ATTRIBUTE);}
    private String scope(HttpServletRequest request){return (String)request.getAttribute(TalentCenterHmacFilter.ACTOR_SCOPE_ATTRIBUTE);}
}
