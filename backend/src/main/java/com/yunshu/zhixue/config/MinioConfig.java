package com.yunshu.zhixue.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 *
 * <p>连接 127.0.0.1 这类地址时 S3 SDK 需要 path-style 访问，MinIO SDK 默认即为 path-style，
 * 无需额外开关；若将来换成 AWS SDK，需显式开启 {@code forcePathStyle}。
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }
}
