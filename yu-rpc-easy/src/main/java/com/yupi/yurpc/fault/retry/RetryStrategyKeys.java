package com.yupi.yurpc.fault.retry;

/**
 * 重试策略枚举
 */
public class RetryStrategyKeys {
    /**
     * 不进行重试
     */
    String NO="no";

    /**
     * 固定时间间隔
     */
    String FIXED_INTERVAL="fixed_interval";
}
