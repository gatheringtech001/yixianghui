package com.ruoyi.system.service;

import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderBackendSourcePolicyTest
{
    @Test
    void ordersDoNotHaveAFeishuSynchronizationPath()
    {
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "com.ruoyi.system.service.feishu.TravelOrderFeishuSyncService"));
        assertFalse(Arrays.stream(AppGoodsOrderMapper.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().contains("TravelOrder")));
    }
}
