package com.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Updates only the publish status of content managed by service accounts.
 */
public interface ContentStatusMapper
{
    int updateGoodsStatus(@Param("id") Long id, @Param("status") String status);

    int updateActivityStatus(@Param("id") Long id, @Param("status") String status);

    int updateArticleStatus(@Param("id") Long id, @Param("status") String status);

    int updateAdPositionStatus(@Param("id") Long id, @Param("status") String status);

    int updateAdContentStatus(@Param("id") Long id, @Param("status") String status);
}
