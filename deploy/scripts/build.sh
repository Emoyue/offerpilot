#!/usr/bin/env bash
# =============================================================================
# OfferPilot - 构建脚本
# 后端打 jar + 两端前端构建，产物统一汇总到 deploy/dist/
#
# 用法：
#   ./scripts/build.sh                  # 全量构建
#   ./scripts/build.sh --only backend   # 只构建后端
#   ./scripts/build.sh --only frontend  # 只构建前端
#   ./scripts/build.sh --skip-tests     # 默认已跳过测试，保留参数兼容
#
# 环境要求：JDK 17+、Maven 3.9+、Node 20 LTS、pnpm 9
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$DEPLOY_DIR/.." && pwd)"
DIST_DIR="$DEPLOY_DIR/dist"

ONLY="all"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --only) ONLY="${2:-all}"; shift 2 ;;
    --skip-tests) shift ;;
    -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "未知参数: $1" >&2; exit 2 ;;
  esac
done

log()  { printf '\033[36m[build]\033[0m %s\n' "$*"; }
warn() { printf '\033[33m[build]\033[0m %s\n' "$*"; }
die()  { printf '\033[31m[build]\033[0m %s\n' "$*" >&2; exit 1; }

need() { command -v "$1" >/dev/null 2>&1 || die "缺少命令 $1，请先安装"; }

build_backend() {
  local pom="$ROOT_DIR/backend/pom.xml"
  if [[ ! -f "$pom" ]]; then
    warn "跳过后端：$pom 不存在（后端工程尚未创建）"
    return 0
  fi
  need mvn
  log "后端打包：mvn clean package -DskipTests"
  ( cd "$ROOT_DIR/backend" && mvn -q -B clean package -DskipTests )
  local jar
  jar="$(find "$ROOT_DIR/backend/target" -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)"
  [[ -n "$jar" ]] || die "未找到打包产物（backend/target/*.jar）"
  cp "$jar" "$DIST_DIR/backend.jar"
  log "后端产物：dist/backend.jar ($(du -h "$DIST_DIR/backend.jar" | cut -f1))"
}

build_frontend() {
  local app="$1"   # portal-web | admin-web
  local out="$2"   # portal | admin
  local dir="$ROOT_DIR/$app"
  if [[ ! -f "$dir/package.json" ]]; then
    warn "跳过 $app：$dir/package.json 不存在（前端工程尚未创建）"
    return 0
  fi
  need node
  if ! command -v pnpm >/dev/null 2>&1; then
    need corepack
    log "未检测到 pnpm，尝试 corepack enable"
    corepack enable
  fi
  log "$app 构建：pnpm install --frozen-lockfile && pnpm build"
  ( cd "$dir" && pnpm install --frozen-lockfile && pnpm build )
  [[ -d "$dir/dist" ]] || die "$app 构建失败：未生成 dist/"
  rm -rf "${DIST_DIR:?}/$out"
  mkdir -p "$DIST_DIR/$out"
  cp -r "$dir/dist/." "$DIST_DIR/$out/"
  log "前端产物：dist/$out/（$(find "$DIST_DIR/$out" -type f | wc -l | tr -d ' ') 个文件）"
}

mkdir -p "$DIST_DIR"

case "$ONLY" in
  all)      build_backend; build_frontend portal-web portal; build_frontend admin-web admin ;;
  backend)  build_backend ;;
  frontend) build_frontend portal-web portal; build_frontend admin-web admin ;;
  *) die "--only 仅支持 all|backend|frontend" ;;
esac

log "完成。产物目录：$DIST_DIR"
ls -1 "$DIST_DIR" 2>/dev/null || true
