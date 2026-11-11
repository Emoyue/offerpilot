package com.yunshu.zhixue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置（S3 兼容协议，D8）。换云 OSS/COS 只改配置，不改代码。
 *
 * @param endpoint  服务端点，如 http://127.0.0.1:9000
 * @param accessKey 访问密钥
 * @param secretKey 访问密钥
 * @param bucket    业务桶名
 * @param publicUrl 对外可访问的基地址（用于拼接图片直链）
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicUrl
) {
}
