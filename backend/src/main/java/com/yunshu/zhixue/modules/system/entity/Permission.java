package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 权限点（permission 表，04 附录 A）。
 *
 * <p>两层结构：{@code type=1} 为组节点（仅用于权限树勾选），{@code type=2} 为可校验的操作点。
 * 登录时按角色取全部 code 写入令牌；组节点也一并携带，便于前端还原权限树的勾选状态。
 */
@TableName("permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String name;
    private String code;
    private Integer type;
    private Integer sortOrder;

    @TableLogic
    private Integer deleteFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
}
