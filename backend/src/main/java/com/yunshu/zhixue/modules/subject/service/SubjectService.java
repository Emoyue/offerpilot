package com.yunshu.zhixue.modules.subject.service;

import com.yunshu.zhixue.modules.subject.dto.SubjectVO;
import java.util.List;

/**
 * 学科查询服务。写操作（管理端 CRUD）在 subject 模块落地时补充。
 */
public interface SubjectService {

    /** 全部学科，按 sort_order 升序。 */
    List<SubjectVO> listAll();
}
