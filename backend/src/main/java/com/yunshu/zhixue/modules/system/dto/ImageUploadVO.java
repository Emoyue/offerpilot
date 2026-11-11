package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图片上传结果（04 A-F01 / §2.10）。
 *
 * @param url 对象存储直链，可直接用于 img src
 */
@Schema(description = "图片上传结果")
public record ImageUploadVO(@Schema(description = "图片直链") String url) {
}
