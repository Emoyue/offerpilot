package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.SystemSettingVO;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import com.yunshu.zhixue.modules.system.mapper.SystemSettingMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import com.yunshu.zhixue.security.CurrentAdmin;
import com.yunshu.zhixue.support.storage.StorageService;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统设置实现。
 *
 * <p>4 个配置项按 config_key upsert；图片上传后只返回直链，用户点「保存设置」才落库，
 * 避免上传即生效造成未保存的脏数据。
 */
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final String KEY_SITE_NAME = "site_name";
    private static final String KEY_SITE_DESCRIPTION = "site_description";
    private static final String KEY_LOGO_URL = "logo_url";
    private static final String KEY_DEFAULT_AVATAR = "default_avatar";

    private static final String DEFAULT_SITE_NAME = "OfferPilot";
    private static final String DEFAULT_SITE_DESCRIPTION = "";

    private static final String TARGET_SETTING = "系统设置";
    private static final String ACTION_UPDATE = "修改";
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;

    private final SystemSettingMapper systemSettingMapper;
    private final StorageService storageService;
    private final OperationLogService operationLogService;
    private final CurrentAdmin currentAdmin;

    public SystemSettingServiceImpl(SystemSettingMapper systemSettingMapper,
                                    StorageService storageService,
                                    OperationLogService operationLogService,
                                    CurrentAdmin currentAdmin) {
        this.systemSettingMapper = systemSettingMapper;
        this.storageService = storageService;
        this.operationLogService = operationLogService;
        this.currentAdmin = currentAdmin;
    }

    @Override
    public SystemSettingVO get() {
        Map<String, String> values = loadValues();
        return new SystemSettingVO(
                values.getOrDefault(KEY_SITE_NAME, DEFAULT_SITE_NAME),
                values.getOrDefault(KEY_SITE_DESCRIPTION, DEFAULT_SITE_DESCRIPTION),
                values.getOrDefault(KEY_LOGO_URL, ""),
                values.getOrDefault(KEY_DEFAULT_AVATAR, ""));
    }

    @Override
    @Transactional
    public void save(SystemSettingUpdateRequest request, String clientIp) {
        upsert(KEY_SITE_NAME, request.siteName().trim(), "站点名称（首页与管理端标题）");
        upsert(KEY_SITE_DESCRIPTION, nullToEmpty(request.siteDescription()), "站点描述（SEO 与首页副标题）");
        upsert(KEY_LOGO_URL, nullToEmpty(request.logoUrl()), "平台 Logo（MinIO 对象 URL，留空用默认图标）");
        upsert(KEY_DEFAULT_AVATAR, nullToEmpty(request.defaultAvatar()), "学员默认头像（MinIO 对象 URL，留空用默认图）");

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_UPDATE,
                TARGET_SETTING, "基本信息", clientIp);
    }

    @Override
    @Transactional
    public void reset(String clientIp) {
        upsert(KEY_SITE_NAME, DEFAULT_SITE_NAME, "站点名称（首页与管理端标题）");
        upsert(KEY_SITE_DESCRIPTION, DEFAULT_SITE_DESCRIPTION, "站点描述（SEO 与首页副标题）");
        upsert(KEY_LOGO_URL, "", "平台 Logo（MinIO 对象 URL，留空用默认图标）");
        upsert(KEY_DEFAULT_AVATAR, "", "学员默认头像（MinIO 对象 URL，留空用默认图）");

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_UPDATE,
                TARGET_SETTING, "恢复默认设置", clientIp);
    }

    @Override
    public String uploadImage(MultipartFile file, String scene, String clientIp) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择要上传的图片");
        }
        String contentType = file.getContentType();
        boolean allowed = "image/png".equalsIgnoreCase(contentType) || "image/jpeg".equalsIgnoreCase(contentType);
        if (!allowed) {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 JPG / PNG 图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片不能超过 2MB");
        }

        BufferedImage source;
        try {
            source = ImageIO.read(file.getInputStream());
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片解析失败，请更换文件");
        }
        if (source == null) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片解析失败，请更换文件");
        }

        int targetSize = "avatar".equalsIgnoreCase(scene) ? 200 : 256;
        byte[] content = renderSquarePng(source, targetSize);
        String objectName = "admin/" + ("avatar".equalsIgnoreCase(scene) ? "avatar" : "logo")
                + "/" + UUID.randomUUID().toString().replace("-", "") + ".png";
        String url = storageService.upload(objectName, new ByteArrayInputStream(content),
                content.length, "image/png");

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), "新增",
                "图片", ("avatar".equalsIgnoreCase(scene) ? "默认头像" : "平台 Logo") + " " + objectName, clientIp);
        return url;
    }

    /** 居中裁成正方形再等比缩放到目标尺寸，保证 Logo 不变形（04 §2.10）。 */
    private byte[] renderSquarePng(BufferedImage source, int targetSize) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage square = source.getSubimage(x, y, side, side);

        BufferedImage target = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(square, 0, 0, targetSize, targetSize, null);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(target, "png", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片处理失败，请更换文件");
        }
    }

    private Map<String, String> loadValues() {
        List<SystemSetting> settings = systemSettingMapper.selectList(Wrappers.<SystemSetting>lambdaQuery());
        Map<String, String> values = new HashMap<>();
        settings.forEach(item -> values.put(item.getConfigKey(), item.getConfigValue()));
        return values;
    }

    private void upsert(String key, String value, String remark) {
        SystemSetting existing = systemSettingMapper.selectOne(Wrappers.<SystemSetting>lambdaQuery()
                .eq(SystemSetting::getConfigKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            SystemSetting setting = new SystemSetting();
            setting.setConfigKey(key);
            setting.setConfigValue(value);
            setting.setRemark(remark);
            systemSettingMapper.insert(setting);
            return;
        }
        SystemSetting update = new SystemSetting();
        update.setId(existing.getId());
        update.setConfigValue(value);
        systemSettingMapper.updateById(update);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
