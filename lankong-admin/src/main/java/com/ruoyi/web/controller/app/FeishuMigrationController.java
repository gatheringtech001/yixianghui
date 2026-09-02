package com.ruoyi.web.controller.app;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.mapper.FeishuMigrationMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 飞书一次性迁移数据。原始值只读，避免绕过正式业务状态机。
 */
@RestController
@RequestMapping("/system/feishu_migration")
@Api(tags = "飞书迁移数据")
public class FeishuMigrationController extends BaseController
{
    private static final String MASKED_VALUE = "******";

    @Autowired
    private FeishuMigrationMapper migrationMapper;

    @PreAuthorize("@ss.hasPermi('system:feishu_migration:list')")
    @GetMapping("/tables")
    @ApiOperation("查询飞书迁移表目录")
    public AjaxResult tables()
    {
        return success(migrationMapper.selectTables());
    }

    @PreAuthorize("@ss.hasPermi('system:feishu_migration:list')")
    @GetMapping("/fields/{sourceTableId}")
    @ApiOperation("查询飞书迁移字段目录")
    public AjaxResult fields(@PathVariable String sourceTableId)
    {
        return success(migrationMapper.selectFields(sourceTableId));
    }

    @PreAuthorize("@ss.hasPermi('system:feishu_migration:list')")
    @GetMapping("/records")
    @ApiOperation("查询飞书迁移记录")
    public TableDataInfo records(@RequestParam String sourceTableId,
                                 @RequestParam(required = false) String mergeStatus)
    {
        if (StringUtils.isEmpty(sourceTableId))
        {
            return getDataTable(java.util.Collections.emptyList());
        }
        Set<String> sensitiveFields = migrationMapper.selectSensitiveFieldNames(sourceTableId)
                .stream().collect(Collectors.toSet());
        startPage();
        List<Map<String, Object>> records = migrationMapper.selectRecords(sourceTableId, mergeStatus);
        records.forEach(record -> maskSensitiveFields(record, sensitiveFields));
        return getDataTable(records);
    }

    @PreAuthorize("@ss.hasPermi('system:feishu_migration:list')")
    @GetMapping("/relations")
    @ApiOperation("查询飞书结构化业务关系")
    public AjaxResult relations(@RequestParam String sourceTableId,
                                @RequestParam String sourceRecordId)
    {
        if (StringUtils.isEmpty(sourceTableId) || StringUtils.isEmpty(sourceRecordId))
        {
            return error("源表和源记录不能为空");
        }
        return success(migrationMapper.selectRelations(sourceTableId, sourceRecordId));
    }

    static void maskSensitiveFields(Map<String, Object> record, Set<String> sensitiveFields)
    {
        Object raw = record.remove("fieldsJson");
        JSONObject fields = raw == null ? new JSONObject() : JSONObject.parseObject(String.valueOf(raw));
        sensitiveFields.forEach(name -> {
            if (fields.containsKey(name) && fields.get(name) != null)
            {
                fields.put(name, MASKED_VALUE);
            }
        });
        record.put("fields", fields);
    }
}
