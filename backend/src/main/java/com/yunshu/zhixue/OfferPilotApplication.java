package com.yunshu.zhixue;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * OfferPilot 后端启动类。
 *
 * <p>架构形态：模块化单体（D3）。业务代码按领域放在 {@code modules/<领域>}，
 * 模块间单向依赖、只经 service 接口与 DTO 交互。
 *
 * <p>注意：{@code @MapperScan} 只扫描标注了 {@link Mapper} 的接口。
 * 若放开为整包扫描，会把 service 接口误当作 Mapper 注册，运行时报
 * "Invalid bound statement (not found)"。新增 Mapper 时务必加 {@code @Mapper}。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.yunshu.zhixue.modules", annotationClass = Mapper.class)
public class OfferPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferPilotApplication.class, args);
    }
}
