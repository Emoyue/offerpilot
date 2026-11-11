import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/api/http'

/**
 * 学员登录态。令牌存 localStorage，刷新页面不掉登录；
 * 失效由 http 拦截器统一处理（401 → 清理并跳登录）。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const nickname = ref<string>('')

  const isLoggedIn = computed(() => Boolean(token.value))

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  function setNickname(value: string) {
    nickname.value = value
  }

  function clear() {
    token.value = null
    nickname.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, nickname, isLoggedIn, setToken, setNickname, clear }
})
