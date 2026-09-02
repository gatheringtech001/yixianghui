package com.ruoyi.web.internal.talent;

import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@RestControllerAdvice(assignableTypes = {
        TalentCenterAdminController.class,
        TalentCenterOperationsController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TalentCenterExceptionHandler
{
    @ExceptionHandler(TalentCenterApiException.class)
    public ResponseEntity<AjaxResult> handle(TalentCenterApiException exception)
    {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(AjaxResult.error(exception.getHttpStatus(), exception.getMessage()));
    }
}
