/**
 * AI 面试。
 *
 * <p>职责：面试配置与配额校验、会话逐题作答与评分、报告生成与落库、面试记录。
 * 关键约定（D4-4）：发起不落库 → 逐题评分暂存 Redis → finish 单事务一次性落库。
 */
package com.yunshu.zhixue.modules.interview;
