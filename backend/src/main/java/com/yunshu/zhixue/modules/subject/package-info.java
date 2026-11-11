/**
 * 学科与阶段。
 *
 * <p>职责：学科 CRUD、阶段（挂于学科下，一对多）CRUD 与启用停用。
 * 学科同时是面试题的"方向"维度（A2/A3），删除学科前需校验其下无阶段/题目。
 */
package com.yunshu.zhixue.modules.subject;
