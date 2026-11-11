package com.yunshu.zhixue.support.web;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.config.StorageProperties;
import com.yunshu.zhixue.support.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 环境自检：一次请求同时验证 MySQL / Redis / MinIO 三项连通性。
 *
 * <p>用途：新环境拉起、排障时先打这个接口，能立刻区分"代码问题"还是"中间件没起"。
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "健康检查", description = "基础环境连通性自检，无需登录")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public HealthController(JdbcTemplate jdbcTemplate,
                            StringRedisTemplate stringRedisTemplate,
                            StorageService storageService,
                            StorageProperties storageProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    @GetMapping
    @Operation(summary = "环境连通性自检")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean allUp = true;

        Map<String, Object> mysql = new LinkedHashMap<>();
        try {
            Integer alive = jdbcTemplate.queryForObject("select 1", Integer.class);
            Integer tables = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database()", Integer.class);
            Integer subjects = jdbcTemplate.queryForObject(
                    "select count(*) from subject where delete_flag = 0", Integer.class);
            mysql.put("status", alive != null && alive == 1 ? "UP" : "DOWN");
            mysql.put("tables", tables);
            mysql.put("subjects", subjects);
        } catch (Exception ex) {
            mysql.put("status", "DOWN");
            mysql.put("error", rootMessage(ex));
            allUp = false;
        }

        Map<String, Object> redis = new LinkedHashMap<>();
        try {
            String pong = stringRedisTemplate.execute(connection -> connection.ping(), true);
            Long dbSize = stringRedisTemplate.execute(connection -> connection.dbSize(), true);
            redis.put("status", "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN");
            redis.put("keys", dbSize);
        } catch (Exception ex) {
            redis.put("status", "DOWN");
            redis.put("error", rootMessage(ex));
            allUp = false;
        }

        Map<String, Object> minio = new LinkedHashMap<>();
        try {
            boolean exists = storageService.bucketExists();
            minio.put("status", exists ? "UP" : "DOWN");
            minio.put("bucket", storageProperties.bucket());
            minio.put("endpoint", storageProperties.endpoint());
            if (!exists) {
                allUp = false;
            }
        } catch (Exception ex) {
            minio.put("status", "DOWN");
            minio.put("error", rootMessage(ex));
            allUp = false;
        }

        result.put("status", allUp ? "UP" : "DOWN");
        result.put("mysql", mysql);
        result.put("redis", redis);
        result.put("minio", minio);
        result.put("serverTimeUtc", LocalDateTime.now(ZoneOffset.UTC).toString());
        return ApiResponse.ok(result);
    }

    private String rootMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
