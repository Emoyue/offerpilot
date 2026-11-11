package com.yunshu.zhixue.support.storage;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * MinIO 实现（S3 兼容协议）。桶由部署时的 minio-init 任务创建，这里只做兜底校验。
 */
@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public MinioStorageService(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String upload(String objectName, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .stream(stream, size, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return publicUrl(objectName);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败：" + ex.getMessage());
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件删除失败：" + ex.getMessage());
        }
    }

    @Override
    public String publicUrl(String objectName) {
        String base = properties.publicUrl().endsWith("/")
                ? properties.publicUrl().substring(0, properties.publicUrl().length() - 1)
                : properties.publicUrl();
        return base + "/" + properties.bucket() + "/" + objectName;
    }

    @Override
    public boolean bucketExists() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
        } catch (Exception ex) {
            return false;
        }
    }
}
