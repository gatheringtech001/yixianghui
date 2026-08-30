package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.talent.TalentCenterAudit;
import com.ruoyi.system.domain.talent.TalentCenterResource;

public interface TalentCenterResourceMapper
{
    SysUser selectEnabledActorByActorId(@Param("actorId") String actorId);
    List<TalentCenterResource> listGoods(@Param("offset") int offset, @Param("limit") int limit);
    List<TalentCenterResource> listActivities(@Param("offset") int offset, @Param("limit") int limit);
    List<TalentCenterResource> listArticles(@Param("offset") int offset, @Param("limit") int limit);
    List<TalentCenterResource> listAds(@Param("offset") int offset, @Param("limit") int limit);
    TalentCenterResource getGoods(Long id);
    TalentCenterResource getActivity(Long id);
    TalentCenterResource getArticle(Long id);
    TalentCenterResource getAd(Long id);
    int updateGoodsStatus(@Param("id") Long id, @Param("expected") String expected, @Param("status") String status);
    int updateActivityStatus(@Param("id") Long id, @Param("expected") String expected, @Param("status") String status);
    int updateArticleStatus(@Param("id") Long id, @Param("expected") String expected, @Param("status") String status);
    int updateAdStatus(@Param("id") Long id, @Param("expected") String expected, @Param("status") String status);
    int insertAudit(TalentCenterAudit audit);
    int updateAuditActor(@Param("auditId") Long auditId, @Param("actorUserId") Long actorUserId);
    int finishAudit(@Param("auditId") Long auditId, @Param("beforeStatus") String beforeStatus,
            @Param("afterStatus") String afterStatus, @Param("result") String result);
}
