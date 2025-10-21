package com.yupi.example.provider;

/**
 * 服务提供者配置工具类
 * 用于解析和获取运行时配置参数（如端口号）
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public class ProviderConfigUtil {

    /**
     * 获取服务器端口号
     * 优先级：VM参数 (-Dserver.port) > 程序参数 (args[0]) > 默认端口
     *
     * @param args        程序启动参数
     * @param defaultPort 默认端口号
     * @return 最终使用的端口号
     */
    public static int getServerPort(String[] args, int defaultPort) {
        int serverPort = defaultPort;
        String source = "默认配置";

        // 1. 优先检查 VM options: -Dserver.port=8081
        String vmPort = System.getProperty("server.port");
        if (vmPort != null && !vmPort.isEmpty()) {
            try {
                serverPort = Integer.parseInt(vmPort);
                source = "VM 参数 (-Dserver.port=" + serverPort + ")";
            } catch (NumberFormatException e) {
                System.err.println("⚠️  VM 参数端口格式错误: " + vmPort + "，将使用默认端口");
            }
        }
        // 2. 如果没有 VM 参数，检查 Program arguments
        else if (args.length > 0) {
            try {
                serverPort = Integer.parseInt(args[0]);
                source = "程序参数 (args[0]=" + serverPort + ")";
            } catch (NumberFormatException e) {
                System.err.println("⚠️  程序参数端口格式错误: " + args[0] + "，将使用默认端口");
            }
        }

        System.out.println("📌 使用端口: " + serverPort + " (来源: " + source + ")");
        return serverPort;
    }

   
}
