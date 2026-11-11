package com.yunshu.zhixue.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页插件 + 时间字段自动填充。
 *
 * <p>时间策略（03 §2）：存储统一 UTC。{@code created_at} 有数据库默认值
 * {@code (UTC_TIMESTAMP())}，这里再在应用层兜一次；{@code updated_at} 因 MySQL
 * 不支持 {@code ON UPDATE (UTC_TIMESTAMP())}，必须由本处理器填充。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    @Bean
    public MetaObjectHandler utcMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, nowUtc);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, nowUtc);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now(ZoneOffset.UTC));
            }
        };
    }
}
