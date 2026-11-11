package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.SystemSettingVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统设置（04 §5.12）。
 */
public interface SystemSettingService {

    SystemSettingVO get();

    void save(SystemSettingUpdateRequest request, String clientIp);

    void reset(String clientIp);

    /**
     * 上传 Logo / 默认头像（A-F01）。
     *
     * @param scene logo（256×256）或 avatar（200×200）
     */
    String uploadImage(MultipartFile file, String scene, String clientIp);
}
