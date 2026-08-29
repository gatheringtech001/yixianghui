package com.ruoyi.system.service;

import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.mapper.ContentStatusMapper;

/**
 * Minimal mutation surface for content service accounts.
 */
@Service
public class ContentStatusService
{
    private final ContentStatusMapper mapper;

    public ContentStatusService(ContentStatusMapper mapper)
    {
        this.mapper = mapper;
    }

    public int updateGoods(Long id, String status)
    {
        validate(id, status);
        return mapper.updateGoodsStatus(id, status);
    }

    public int updateActivity(Long id, String status)
    {
        validate(id, status);
        return mapper.updateActivityStatus(id, status);
    }

    public int updateArticle(Long id, String status)
    {
        validate(id, status);
        return mapper.updateArticleStatus(id, status);
    }

    public int updateAdPosition(Long id, String status)
    {
        validate(id, status);
        return mapper.updateAdPositionStatus(id, status);
    }

    public int updateAdContent(Long id, String status)
    {
        validate(id, status);
        return mapper.updateAdContentStatus(id, status);
    }

    private void validate(Long id, String status)
    {
        if (id == null || id <= 0)
        {
            throw new IllegalArgumentException("资源ID必须为正整数");
        }
        if (!StringUtils.equalsAny(status, "0", "1"))
        {
            throw new IllegalArgumentException("状态只允许为0或1");
        }
    }
}
