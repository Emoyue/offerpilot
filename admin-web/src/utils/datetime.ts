/**
 * 时间展示：服务端统一存 UTC 并在接口中返回 ISO-8601（带 Z），
 * 管理端按 Asia/Shanghai 展示（04 §2.11 / D17）。
 */

const DISPLAY_FORMATTER = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
})

/** UTC ISO → 北京时间 `YYYY-MM-DD HH:mm:ss`；空值显示占位符。 */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  const parts = DISPLAY_FORMATTER.formatToParts(date)
  const pick = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? ''
  return `${pick('year')}-${pick('month')}-${pick('day')} ${pick('hour')}:${pick('minute')}:${pick('second')}`
}

/** 仅日期部分，用于表格紧凑展示。 */
export function formatDate(value?: string | null): string {
  const full = formatDateTime(value)
  return full === '—' ? full : full.slice(0, 10)
}
