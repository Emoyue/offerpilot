package com.yunshu.zhixue.support.storage;

import java.io.InputStream;

/**
 * 对象存储能力（S3 兼容，D8）。业务只依赖本接口，便于将来换云 OSS/COS。
 */
public interface StorageService {

    /**
     * 上传对象。
     *
     * @param objectName  对象键，如 avatar/2026/uuid.png
     * @param stream      内容流（由调用方负责关闭）
     * @param size        字节数，未知传 -1
     * @param contentType MIME 类型，如 image/png
     * @return 可直接访问的公网地址
     */
    String upload(String objectName, InputStream stream, long size, String contentType);

    /** 删除对象；不存在不报错。 */
    void delete(String objectName);

    /** 拼接匿名直链（桶已设 download 策略时可直接用于 img src）。 */
    String publicUrl(String objectName);

    /** 桶是否存在，用于启动自检与健康检查。 */
    boolean bucketExists();
}
