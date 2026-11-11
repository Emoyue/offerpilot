package com.yunshu.zhixue.modules.subject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学科 Mapper。简单查询走 MyBatis-Plus，复杂统计在 resources/mapper 下写 XML。
 */
@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {
}
