package com.yupi.yurpc.fault.tolerant;

import com.yupi.yurpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 降级到其他服务 - 容错策略（Fail-Back）
 * 
 * <p>当远程服务调用失败时，不直接抛出异常，而是降级到备用方案：</p>
 * <ul>
 *   <li>1. 如果 context 中提供了降级服务实例（fallbackService），则调用降级服务的同名方法</li>
 *   <li>2. 否则返回 Mock 默认值（基本类型返回默认值，对象类型返回 null）</li>
 * </ul>
 * 
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>- 远程服务不可用时，返回缓存数据或默认数据</li>
 *   <li>- 非核心服务调用失败时，返回降级响应，保证主流程可用</li>
 *   <li>- 防止服务雪崩，提供有损但可用的服务</li>
 * </ul>
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@Slf4j
public class FailBackTolerantStrategy implements TolerantStrategy {

    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // 1. 记录降级日志（重要：让开发者知道服务降级了）
        log.warn("服务调用失败，启动 Fail-Back 降级策略", e);
        
        // 2. 创建降级响应对象
        RpcResponse rpcResponse = new RpcResponse();
        
        // 3. 尝试从 context 中获取降级所需的信息
        if (context != null && !context.isEmpty()) {
            // 3.1 获取方法信息
            Method method = (Method) context.get("method");
            Object[] args = (Object[]) context.get("args");
            Object fallbackService = context.get("fallbackService");
            
            // 3.2 如果提供了降级服务实例，则调用降级服务
            if (fallbackService != null && method != null) {
                try {
                    log.info("调用降级服务: {}.{}", 
                            fallbackService.getClass().getSimpleName(), 
                            method.getName());
                    
                    // 通过反射调用降级服务的同名方法
                    Object fallbackResult = method.invoke(fallbackService, args);
                    rpcResponse.setData(fallbackResult);
                    rpcResponse.setDataType(method.getReturnType());
                    rpcResponse.setMessage("服务降级成功 - 使用备用服务");
                    
                    log.info("降级服务调用成功");
                    return rpcResponse;
                } catch (Exception fallbackException) {
                    // 降级服务也失败了，记录日志，继续执行 Mock 逻辑
                    log.error("降级服务调用失败，将返回 Mock 默认值", fallbackException);
                }
            }
            
            // 3.3 如果有方法信息，返回该方法返回类型的 Mock 默认值
            if (method != null) {
                Class<?> returnType = method.getReturnType();
                Object mockData = getMockDefaultValue(returnType);
                rpcResponse.setData(mockData);
                rpcResponse.setDataType(returnType);
                rpcResponse.setMessage("服务降级 - 返回 Mock 默认值");
                
                log.info("返回 Mock 默认值: type={}, value={}", 
                        returnType.getSimpleName(), mockData);
                return rpcResponse;
            }
        }
        
        // 4. 如果没有 context 信息，返回空响应
        rpcResponse.setData(null);
        rpcResponse.setMessage("服务降级 - 无降级方案，返回空响应");
        log.warn("未提供 context 信息，无法生成 Mock 数据，返回空响应");
        
        return rpcResponse;
    }
    
    /**
     * 生成指定类型的 Mock 默认值
     * <p>参考 Dubbo Mock 机制的实现</p>
     *
     * @param type 返回值类型
     * @return Mock 默认值
     */
    private Object getMockDefaultValue(Class<?> type) {
        // 基本类型的默认值
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return false;
            } else if (type == byte.class) {
                return (byte) 0;
            } else if (type == short.class) {
                return (short) 0;
            } else if (type == int.class) {
                return 0;
            } else if (type == long.class) {
                return 0L;
            } else if (type == float.class) {
                return 0.0f;
            } else if (type == double.class) {
                return 0.0d;
            } else if (type == char.class) {
                return '\u0000';
            }
        }
        
        // 对象类型返回 null（也可以扩展返回空集合、空字符串等）
        return null;
    }
}
