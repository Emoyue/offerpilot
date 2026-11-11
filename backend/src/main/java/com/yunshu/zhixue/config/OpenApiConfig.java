package com.yunshu.zhixue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 契约配置（D21）。
 *
 * <p>{@code openapi.json} 是两端 TypeScript 类型的唯一来源，
 * 由 {@code openapi-typescript} 生成到各自 {@code src/types/generated/}，不手工维护第二份。
 */
@Configuration
public class OpenApiConfig {

    private static final String PORTAL_SCHEME = "portalToken";
    private static final String ADMIN_SCHEME = "adminToken";

    @Bean
    public OpenAPI yunshuZhixueOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OfferPilot API")
                        .version("v1")
                        .description("""
                                学员端前缀 /api/portal/v1，管理端前缀 /api/admin/v1。
                                两端 JWT 独立、互不通用；管理端另需 RBAC 权限码校验。
                                """))
                .components(new Components()
                        .addSecuritySchemes(PORTAL_SCHEME, bearer("学员端 accessToken"))
                        .addSecuritySchemes(ADMIN_SCHEME, bearer("管理端 accessToken")))
                .addSecurityItem(new SecurityRequirement().addList(PORTAL_SCHEME))
                .addSecurityItem(new SecurityRequirement().addList(ADMIN_SCHEME));
    }

    private SecurityScheme bearer(String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(description);
    }
}
