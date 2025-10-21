# yu-rpc-core

yu-rpc-core 是一个轻量级的RPC框架核心模块，提供了配置管理、服务注册发现等核心功能。

## 功能特性

- **多种配置格式支持**：支持 properties、yaml、yml 格式的配置文件
- **配置自动刷新**：支持监听配置文件变更并自动更新配置对象
- **灵活的配置管理**：支持环境标识和配置前缀
- **简洁的API设计**：提供简单易用的配置加载和管理接口

## 配置文件格式

框架支持以下格式的配置文件：

1. application.properties
2. application.yaml
3. application.yml

### 配置文件示例 (application.yml)

```yaml
# RPC框架配置
rpc:
  # 应用名称
  name: "yu-rpc"
  # 应用版本
  version: "1.0"
  # 服务器主机名
  serverHost: "localhost"
  # 服务器端口
  serverPort: 9999
  # 是否启用配置自动刷新
  configReloadEnabled: true
```

## 核心类说明

### RpcApplication
RPC应用入口类，提供RPC框架的初始化、配置管理、配置变更监听等功能。

### ConfigUtils
配置工具类，支持多种格式配置文件加载和配置文件变更监听。

### RpcConfig
RPC框架配置类，包含RPC服务的核心配置项。

### RpcConstant
RPC常量定义接口，包含RPC框架使用的所有常量定义。

## 使用方法

### 初始化RPC应用

```java
// 使用默认配置初始化
RpcApplication.init();

// 使用指定环境初始化
RpcApplication.init("dev");
```

### 获取配置

```java
// 获取当前RPC配置
RpcConfig config = RpcApplication.getInstance().getConfig();
```

### 监听配置变更

```java
// 注册配置变更监听器
RpcApplication.registerConfigChangeListener(new RpcApplication.ConfigChangeListener() {
    @Override
    public void onConfigChanged(RpcConfig newConfig) {
        System.out.println("配置已变更: " + newConfig);
    }
});
```

### 手动刷新配置

```java
// 手动刷新配置
RpcApplication.refreshConfig();
```

## 依赖

- JDK 17+
- Lombok
- Hutool
- SnakeYAML
- SLF4J + Logback

## 许可证

MIT License