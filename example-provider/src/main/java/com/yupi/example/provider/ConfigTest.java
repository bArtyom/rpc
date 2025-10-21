package com.yupi.example.provider;

import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.utils.ConfigUtils;

public class ConfigTest {
    public static void main(String[] args) {
        try {
            System.out.println("Starting config loading test...");
            
            // 测试YAML配置加载
            System.out.println("Loading config with prefix 'rpc'...");
            RpcConfig config = ConfigUtils.loadConfig(RpcConfig.class, "rpc");
            System.out.println("Loaded config successfully!");
            System.out.println("Config details:");
            System.out.println("  Name: " + config.getName());
            System.out.println("  Version: " + config.getVersion());
            System.out.println("  Server Host: " + config.getServerHost());
            System.out.println("  Server Port: " + config.getServerPort());
            
            // 验证配置是否正确加载
            if ("yu-rpc-yaml".equals(config.getName()) && 
                "1.0-yaml".equals(config.getVersion()) && 
                "localhost".equals(config.getServerHost()) && 
                Integer.valueOf(9093).equals(config.getServerPort())) {
                System.out.println("YAML configuration loaded correctly!");
            } else {
                System.out.println("YAML configuration did not load as expected!");
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}