package com.yupi.yurpc.utils;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Properties;

/**
 * 配置工具类
 */
public class ConfigUtils {

    /**
     * 加载配置对象
     *
     * @param tClass
     * @param prefix
     * @param <T>
     * @return
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix) {
        return loadConfig(tClass, prefix, "");
    }

    /**
     * 加载配置对象，支持区分环境
     *
     * @param tClass
     * @param prefix
     * @param environment
     * @param <T>
     * @return
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix, String environment) {
        // 尝试按顺序加载不同格式的配置文件
        String[] configFiles = getConfigFileNames(environment);
        
        for (String configFile : configFiles) {
            try {
                if (configFile.endsWith(".properties")) {
                    // 检查配置文件是否存在
                    InputStream inputStream = ConfigUtils.class.getClassLoader().getResourceAsStream(configFile);
                    if (inputStream == null) {
                        continue; // 文件不存在，尝试下一个
                    }
                    Props props = new Props(configFile);
                    return props.toBean(tClass, prefix);
                } else if (configFile.endsWith(".yml") || configFile.endsWith(".yaml")) {
                    // 检查配置文件是否存在
                    InputStream inputStream = ConfigUtils.class.getClassLoader().getResourceAsStream(configFile);
                    if (inputStream == null) {
                        continue; // 文件不存在，尝试下一个
                    }
                    return loadYamlConfig(tClass, configFile, prefix);
                }
            } catch (Exception e) {
                // 如果当前文件不存在或加载失败，继续尝试下一个
                continue;
            }
        }
        
        // 如果所有格式都加载失败，抛出异常
        throw new RuntimeException("Failed to load configuration file for environment: " + environment);
    }
    
    /**
     * 获取配置文件名数组，按优先级排序
     *
     * @param environment 环境标识
     * @return 配置文件名数组
     */
    private static String[] getConfigFileNames(String environment) {
        StringBuilder configFileBase = new StringBuilder("application");
        if (StrUtil.isNotBlank(environment)) {
            configFileBase.append("-").append(environment);
        }
        
        return new String[] {
            configFileBase + ".properties",
            configFileBase + ".yml",
            configFileBase + ".yaml"
        };
    }
    
    /**
     * 加载YAML格式的配置文件
     *
     * @param tClass 配置类类型
     * @param configFile 配置文件名
     * @param prefix 配置前缀
     * @param <T> 配置类类型
     * @return 配置对象
     */
    @SuppressWarnings("unchecked")
    private static <T> T loadYamlConfig(Class<T> tClass, String configFile, String prefix) {
        Yaml yaml = new Yaml();
        InputStream inputStream = ConfigUtils.class.getClassLoader().getResourceAsStream(configFile);
        
        if (inputStream == null) {
            throw new RuntimeException("Configuration file not found: " + configFile);
        }
        
        // 加载YAML数据
        Object yamlData = yaml.load(inputStream);
        
        // 转换为Properties对象以便使用toBean方法
        Properties properties = new Properties();
        // 对于YAML文件，我们直接使用顶层键，不需要添加额外的前缀
        convertToProperties(properties, yamlData, "");
        
        // 使用Props来创建配置对象
        Props props = new Props(properties);
        return props.toBean(tClass, prefix);
    }
    
    /**
     * 将YAML数据转换为Properties对象
     *
     * @param properties Properties对象
     * @param data YAML数据
     * @param prefix 前缀
     */
    @SuppressWarnings("unchecked")
    private static void convertToProperties(Properties properties, Object data, String prefix) {
        if (data instanceof java.util.Map) {
            ((java.util.Map<String, Object>) data).forEach((key, value) -> {
                String newKey = StrUtil.isBlank(prefix) ? key : prefix + "." + key;
                if (value instanceof java.util.Map) {
                    convertToProperties(properties, value, newKey);
                } else {
                    properties.setProperty(newKey, value != null ? value.toString() : "");
                }
            });
        }
    }
}
