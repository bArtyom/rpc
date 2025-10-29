package com.yupi.yurpc.fault.tolerant;

import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.server.tcp.VertxTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
public class FailOverTolerantStrategy implements TolerantStrategy {
    
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // 1. 记录故障转移日志
        log.warn("服务调用失败，启动 Fail-Over 容错策略，尝试切换到其他节点", e);
        
        // 2. 从 context 中获取必要信息
        List<ServiceMetaInfo> serviceMetaInfoList = (List<ServiceMetaInfo>) context.get("serviceMetaInfoList");
        ServiceMetaInfo failedServiceMetaInfo = (ServiceMetaInfo) context.get("serviceMetaInfo");
        RpcRequest rpcRequest = (RpcRequest) context.get("rpcRequest");
        
        // 3. 参数校验
        if (serviceMetaInfoList == null || serviceMetaInfoList.isEmpty()) {
            log.error("服务节点列表为空，无法进行 Fail-Over");
            return createErrorResponse("服务节点列表为空，无法进行故障转移");
        }
        
        if (serviceMetaInfoList.size() == 1) {
            log.warn("只有一个服务节点，无法进行 Fail-Over");
            return createErrorResponse("只有一个服务节点，无法进行故障转移");
        }
        
        if (rpcRequest == null) {
            log.error("RpcRequest 为空，无法重试");
            return createErrorResponse("请求信息为空，无法重试");
        }
        
        // 4. 记录已经尝试过的节点，避免重复尝试
        Set<String> triedNodes = new HashSet<>();
        triedNodes.add(failedServiceMetaInfo.getServiceNodeKey());
        
        log.info("开始 Fail-Over，已失败节点：{}，可用节点总数：{}", 
                failedServiceMetaInfo.getServiceNodeKey(), serviceMetaInfoList.size());
        
        // 5. 遍历所有服务节点，尝试调用其他节点
        for (ServiceMetaInfo serviceMetaInfo : serviceMetaInfoList) {
            String nodeKey = serviceMetaInfo.getServiceNodeKey();
            
            // 跳过已经尝试失败的节点
            if (triedNodes.contains(nodeKey)) {
                log.debug("跳过已尝试的节点：{}", nodeKey);
                continue;
            }
            
            // 标记为已尝试
            triedNodes.add(nodeKey);
            
            try {
                log.info("Fail-Over 尝试调用节点 [{}/{}]：{}", 
                        triedNodes.size(), serviceMetaInfoList.size(), nodeKey);
                
                // 6. 调用新节点
                RpcResponse rpcResponse = VertxTcpClient.doRequest(rpcRequest, serviceMetaInfo);
                
                // 7. 调用成功，记录日志并返回结果
                log.info("✅ Fail-Over 成功！节点 {} 调用成功", nodeKey);
                rpcResponse.setMessage("Fail-Over 成功 - 切换到节点：" + nodeKey);
                return rpcResponse;
                
            } catch (Exception retryException) {
                // 8. 当前节点也失败，记录日志，继续尝试下一个
                log.warn("❌ Fail-Over 尝试节点 {} 失败：{}", nodeKey, retryException.getMessage());
                
                // 如果是最后一个节点，记录详细错误
                if (triedNodes.size() >= serviceMetaInfoList.size()) {
                    log.error("所有服务节点都已尝试失败，Fail-Over 最终失败", retryException);
                }
            }
        }
        
        // 9. 所有节点都失败，返回错误响应
        String errorMessage = String.format(
                "Fail-Over 失败：所有 %d 个服务节点都不可用", 
                serviceMetaInfoList.size()
        );
        log.error(errorMessage);
        return createErrorResponse(errorMessage);
    }
    
    /**
     * 创建错误响应
     */
    private RpcResponse createErrorResponse(String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.setMessage(errorMessage);
        response.setException(new RuntimeException(errorMessage));
        return response;
    }
}
