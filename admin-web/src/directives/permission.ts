import type { Directive, DirectiveBinding } from 'vue'
import { useAdminStore } from '@/store/user'

/**
 * v-permission 指令：无权限时移除元素（而非仅仅禁用），避免越权入口可见。
 *
 * 用法：v-permission="'question:edit'" 或 v-permission="['question:edit', 'question:add']"
 */
const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding: DirectiveBinding<string | string[]>) {
    const store = useAdminStore()
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const granted = required.some((code) => store.hasPermission(code))
    if (!granted) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default permission
