package com.ruoyi.system.service;

import com.ruoyi.system.mapper.ContentStatusMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContentStatusServiceTest
{
    private ContentStatusMapper mapper;
    private ContentStatusService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(ContentStatusMapper.class);
        service = new ContentStatusService(mapper);
    }

    @Test
    void updatesOnlyRequestedResourceStatus()
    {
        when(mapper.updateGoodsStatus(38L, "1")).thenReturn(1);

        assertEquals(1, service.updateGoods(38L, "1"));

        verify(mapper).updateGoodsStatus(38L, "1");
    }

    @Test
    void rejectsUnsupportedStatus()
    {
        assertThrows(IllegalArgumentException.class, () -> service.updateArticle(12L, "2"));

        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsInvalidResourceId()
    {
        assertThrows(IllegalArgumentException.class, () -> service.updateAdContent(0L, "1"));

        verifyNoInteractions(mapper);
    }
}
