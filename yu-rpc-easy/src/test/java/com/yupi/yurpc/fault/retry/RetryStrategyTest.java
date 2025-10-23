package com.yupi.yurpc.fault.retry;

import com.yupi.yurpc.model.RpcResponse;
import org.junit.Test;

import java.util.concurrent.Callable;

/***
 * 重试策略测试类
 */
public class RetryStrategyTest {

    RetryStrategy retryStrategy=new NoRetryStrategy();

    @Test
    public void doRetry()  {
        try {
            RpcResponse rpcResponse=retryStrategy.doRetry(() ->{
                    System.out.println("调用方法");
                    throw new Exception("调用方法异常");
            });
            System.out.println("返回结果："+rpcResponse);
        } catch (Exception e) {
            System.out.println("重试多次失败");
            throw new RuntimeException(e);
        }
    }

    RetryStrategy retryStrategy2=new FixedIntervalRetryStrategy();
    @Test
    public void doRetry2()  {
        try {
            RpcResponse rpcResponse=retryStrategy2.doRetry(() ->{
                System.out.println("调用方法");
                throw new Exception("调用方法异常");
            });
        } catch (Exception e) {
            System.out.println("重试多次失败");
            throw new RuntimeException(e);
        }

    }

}
