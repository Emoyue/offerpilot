package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置 Mapper（key-value，无逻辑删除）。
 */
@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {
}
