package com.ruoyi.system.service;

import java.math.BigDecimal;
import com.ruoyi.system.domain.AppPayLog;

/**
 * Unified gold coin service: grant on pay / reverse on refund (idempotent).
 * Card biz types are reserved; controlled by gold.scope.card (default false).
 */
public interface IAppGoldService
{
    /**
     * Grant gold by paid fen. Default: 1 yuan = 1 gold (configurable rate).
     *
     * @param userId user id
     * @param businessType see AppGoldBizType
     * @param businessId order / record id
     * @param payFen paid amount in fen
     * @param refNo pay no for idempotency; may be null
     * @return granted amount, 0 if skipped/duplicate
     */
    long grantOnPay(Long userId, String businessType, Long businessId, BigDecimal payFen, String refNo);

    /**
     * Reverse gold by refund fen; deduct to 0 if balance insufficient.
     */
    long reverseOnRefund(Long userId, String businessType, Long businessId, BigDecimal refundFen, String refNo);

    /**
     * Compensate grant from pay log (notify/sync).
     */
    long grantByPayLog(AppPayLog payLog);

    /**
     * Convert fen to gold amount (with rate).
     */
    long calcGoldByFen(BigDecimal fen);

    /**
     * Grant invite-register reward to inviter (idempotent by newUserId).
     *
     * @param inviterUserId inviter user id
     * @param newUserId invited new user id
     * @param gold reward amount
     * @return granted amount, 0 if skipped/duplicate
     */
    long grantInviteRegister(Long inviterUserId, Long newUserId, long gold);
}