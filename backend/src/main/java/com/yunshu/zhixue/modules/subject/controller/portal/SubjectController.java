package com.yunshu.zhixue.modules.subject.controller.portal;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.subject.dto.SubjectVO;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端 · 学科列表（题库筛选与 AI 面试方向复用同一套学科体系）。
 *
 * <p>接口编号对应 04 文档 P-B01 的筛选基础数据，正式契约在题库模块落地时补齐。
 */
@RestController
@RequestMapping("/api/portal/v1/subjects")
@Tag(name = "学员端-学科", description = "学科列表，供题库筛选与面试方向选择")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    @Operation(summary = "学科列表")
    public ApiResponse<List<SubjectVO>> list() {
        return ApiResponse.ok(subjectService.listAll());
    }
}
