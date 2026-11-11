# 自动生成的契约类型

本目录内容由 `pnpm gen:types` 从后端 `openapi.json` 生成（D21），**不要手工修改**。

```bash
# 后端已启动（默认 8080）时执行
pnpm gen:types
```

约定：
- 生成文件命名为 `openapi.d.ts`，随接口契约变更重新生成并提交。
- 业务侧类型别名写在 `src/types/index.ts`，从生成物里取 DTO 组合，不复制字段。
