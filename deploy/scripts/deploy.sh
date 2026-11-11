#!/usr/bin/env bash
# =============================================================================
# OfferPilot - 部署脚本
# 把 deploy/dist/ 下的产物推到目标机：前端静态目录 + 后端 jar，然后重启服务
#
# 用法：
#   ./scripts/deploy.sh                    # 推送到远程（读取下面的变量或环境变量）
#   ./scripts/deploy.sh --local            # 本机部署（Nginx 与后端都在本机时）
#   ./scripts/deploy.sh --yes              # 跳过二次确认
#   ./scripts/deploy.sh --dry-run          # 只打印将执行的命令
#
# 远程部署需先配置免密登录：ssh-copy-id user@host
# 目标机目录约定：
#   /usr/share/nginx/portal   学员端静态产物
#   /usr/share/nginx/admin    管理端静态产物
#   /opt/yszx/app.jar         后端 jar
#   systemd 单元 yszx-backend 管理后端进程（未安装则回退 nohup 启动）
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DIST_DIR="$DEPLOY_DIR/dist"

DEPLOY_HOST="${DEPLOY_HOST:-}"
DEPLOY_USER="${DEPLOY_USER:-root}"
APP_DIR="${APP_DIR:-/opt/yszx}"
NGINX_ROOT="${NGINX_ROOT:-/usr/share/nginx}"
SERVICE_NAME="${SERVICE_NAME:-yszx-backend}"

MODE="remote"; ASSUME_YES=0; DRY_RUN=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --local) MODE="local"; shift ;;
    --yes|-y) ASSUME_YES=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) sed -n '2,20p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "未知参数: $1" >&2; exit 2 ;;
  esac
done

log()  { printf '\033[36m[deploy]\033[0m %s\n' "$*"; }
warn() { printf '\033[33m[deploy]\033[0m %s\n' "$*"; }
die()  { printf '\033[31m[deploy]\033[0m %s\n' "$*" >&2; exit 1; }
run()  { if [[ $DRY_RUN -eq 1 ]]; then echo "+ $*"; else eval "$@"; fi; }

# ------------------------------------------------------------------ 产物检查
[[ -d "$DIST_DIR" ]] || die "未找到 $DIST_DIR，请先执行 ./scripts/build.sh"
[[ -d "$DIST_DIR/portal" ]] || warn "缺少 dist/portal（学员端产物），将跳过"
[[ -d "$DIST_DIR/admin"  ]] || warn "缺少 dist/admin（管理端产物），将跳过"
[[ -f "$DIST_DIR/backend.jar" ]] || warn "缺少 dist/backend.jar，将跳过后端更新"
if [[ ! -d "$DIST_DIR/portal" && ! -d "$DIST_DIR/admin" && ! -f "$DIST_DIR/backend.jar" ]]; then
  die "dist/ 内没有任何可部署产物"
fi

# ------------------------------------------------------------------ 目标定义
if [[ "$MODE" == "remote" ]]; then
  [[ -n "$DEPLOY_HOST" ]] || die "请先设置 DEPLOY_HOST，例如：DEPLOY_HOST=1.2.3.4 DEPLOY_USER=root ./scripts/deploy.sh"
  TARGET="${DEPLOY_USER}@${DEPLOY_HOST}"
fi

confirm() {
  [[ $ASSUME_YES -eq 1 || $DRY_RUN -eq 1 ]] && return 0
  if [[ "$MODE" == "local" ]]; then
    printf '即将部署到【本机】：%s/portal、%s/admin、%s/app.jar，并重启 %s。继续？[y/N] ' \
      "$NGINX_ROOT" "$NGINX_ROOT" "$APP_DIR" "$SERVICE_NAME"
  else
    printf '即将部署到【%s】：%s/portal、%s/admin、%s/app.jar，并重启 %s。继续？[y/N] ' \
      "$TARGET" "$NGINX_ROOT" "$NGINX_ROOT" "$APP_DIR" "$SERVICE_NAME"
  fi
  read -r ans
  [[ "$ans" == "y" || "$ans" == "Y" ]] || { log "已取消"; exit 0; }
}

confirm

# ------------------------------------------------------------------ 远端准备
prepare_remote() {
  run "ssh \"$TARGET\" 'mkdir -p $NGINX_ROOT/portal $NGINX_ROOT/admin $APP_DIR'"
}

push_remote() {
  local rsync_opts=(-az --delete --human-readable)
  if [[ $DRY_RUN -eq 1 ]]; then rsync_opts+=(--dry-run); fi
  [[ -d "$DIST_DIR/portal" ]] && run "rsync ${rsync_opts[*]} \"$DIST_DIR/portal/\" \"$TARGET:$NGINX_ROOT/portal/\""
  [[ -d "$DIST_DIR/admin"  ]] && run "rsync ${rsync_opts[*]} \"$DIST_DIR/admin/\"  \"$TARGET:$NGINX_ROOT/admin/\""
  if [[ -f "$DIST_DIR/backend.jar" ]]; then
    run "rsync -az --human-readable \"$DIST_DIR/backend.jar\" \"$TARGET:$APP_DIR/app.jar\""
  fi
}

restart_remote() {
  # Nginx 先做语法检查再 reload，避免配置写坏导致整站 502
  run "ssh \"$TARGET\" 'nginx -t && systemctl reload nginx'"
  if [[ -f "$DIST_DIR/backend.jar" ]]; then
    run "ssh \"$TARGET\" 'if systemctl list-unit-files | grep -q \\\"^$SERVICE_NAME\\\\.service\\\"; then systemctl restart $SERVICE_NAME; else cd $APP_DIR && (pkill -f app.jar || true) && nohup java -jar app.jar > app.log 2>&1 & fi'"
  fi
}

push_local() {
  local rsync_opts=(-a --delete --human-readable)
  if [[ $DRY_RUN -eq 1 ]]; then rsync_opts+=(--dry-run); fi
  run "mkdir -p \"$NGINX_ROOT/portal\" \"$NGINX_ROOT/admin\" \"$APP_DIR\""
  [[ -d "$DIST_DIR/portal" ]] && run "rsync ${rsync_opts[*]} \"$DIST_DIR/portal/\" \"$NGINX_ROOT/portal/\""
  [[ -d "$DIST_DIR/admin"  ]] && run "rsync ${rsync_opts[*]} \"$DIST_DIR/admin/\"  \"$NGINX_ROOT/admin/\""
  [[ -f "$DIST_DIR/backend.jar" ]] && run "cp -f \"$DIST_DIR/backend.jar\" \"$APP_DIR/app.jar\""
}

restart_local() {
  run "sudo nginx -t && sudo systemctl reload nginx"
  if [[ -f "$DIST_DIR/backend.jar" ]]; then
    run "sudo systemctl restart \"$SERVICE_NAME\" 2>/dev/null || (cd \"$APP_DIR\" && pkill -f app.jar || true; nohup java -jar app.jar > app.log 2>&1 &)"
  fi
}

if [[ "$MODE" == "local" ]]; then
  log "本机部署"
  push_local
  restart_local
else
  log "远程部署 → $TARGET"
  prepare_remote
  push_remote
  restart_remote
fi

log "完成。自检：curl -I http://<host>/  与  curl -I http://<host>/admin/"
