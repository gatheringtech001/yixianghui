package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 飞书一次性迁移数据查询。
 */
public interface FeishuMigrationMapper
{
    List<Map<String, Object>> selectTables();

    List<Map<String, Object>> selectFields(@Param("sourceTableId") String sourceTableId);

    List<String> selectSensitiveFieldNames(@Param("sourceTableId") String sourceTableId);

    List<Map<String, Object>> selectRecords(@Param("sourceTableId") String sourceTableId,
                                            @Param("mergeStatus") String mergeStatus);

    List<Map<String, Object>> selectRelations(@Param("sourceTableId") String sourceTableId,
                                              @Param("sourceRecordId") String sourceRecordId);
}
