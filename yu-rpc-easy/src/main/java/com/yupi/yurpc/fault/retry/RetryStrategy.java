package com.yupi.yurpc.fault.retry;

import com.yupi.yurpc.model.RpcResponse;

import java.util.concurrent.Callable;

public interface RetryStrategy {

    /**
     * 重试
     *
     * @param callable
     * @return
     */
    RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception;

}
