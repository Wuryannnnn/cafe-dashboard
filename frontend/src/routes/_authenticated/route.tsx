import { createFileRoute, redirect } from '@tanstack/react-router'
import { api } from '@/lib/api'
import { ROLE_NAMES, navAllowed, homeForRole } from '@/lib/permissions'
import { useAuthStore } from '@/stores/auth-store'
import { AuthenticatedLayout } from '@/components/layout/authenticated-layout'

export const Route = createFileRoute('/_authenticated')({
  // 进入后台前校验登录态: 调用 /api/admin/me, 未登录跳转登录页.
  // 真实凭据是 HttpOnly 的 admin_token cookie, 由 withCredentials 自动携带.
  beforeLoad: async ({ location }) => {
    let role: number | null = null
    try {
      const res = await api.get<{
        code: number
        data?: { username: string; name?: string; role: number }
      }>('/api/admin/me')
      if (res.data?.code === 0 && res.data.data) {
        const u = res.data.data
        role = u.role
        // 回填登录态(含角色码), 使刷新/直达页面后导航与权限仍可用
        useAuthStore.getState().auth.setUser({
          accountNo: u.username,
          email: u.username,
          name: u.name || u.username,
          role: [ROLE_NAMES[u.role] ?? String(u.role)],
          roleCode: u.role,
          exp: Date.now() + 12 * 60 * 60 * 1000,
        })
      }
    } catch {
      role = null
    }
    if (role == null) {
      throw redirect({ to: '/sign-in', search: { redirect: location.href } })
    }
    // 已登录但当前角色无权访问该路由 → 跳到该角色首页, 而不是渲染空壳页再弹 403
    if (!navAllowed(location.pathname, role)) {
      throw redirect({ to: homeForRole(role) })
    }
  },
  component: AuthenticatedLayout,
})
