package com.yunshu.zhixue.modules.subject.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.subject.dto.SubjectVO;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 学科查询实现。逻辑删除由 MyBatis-Plus 自动追加 delete_flag = 0 条件。
 */
@Service
public class SubjectServiceImpl implements SubjectService {

    private final SubjectMapper subjectMapper;

    public SubjectServiceImpl(SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    @Override
    public List<SubjectVO> listAll() {
        List<Subject> subjects = subjectMapper.selectList(
                Wrappers.<Subject>lambdaQuery().orderByAsc(Subject::getSortOrder));
        return subjects.stream().map(SubjectVO::from).toList();
    }
}
