// 后台角色 (与后端一致): 0=老板 1=店长 2=收银员 3=制作员
export type Role = number

export const ROLE_NAMES = ['老板', '店长', '收银员', '制作员'] as const

export function roleName(role: Role | null | undefined): string {
  return role == null ? '' : (ROLE_NAMES[role] ?? String(role))
}

// 导航项可见角色; 未列出的 url 默认 [老板, 店长].
// 注意: 这是"导航可见性", 真正的接口强制在后端 AdminPermission.
const NAV_ROLES: { prefix: string; roles: Role[] }[] = [
  { prefix: '/orders', roles: [0, 1, 2, 3] },
  { prefix: '/cashier', roles: [0, 1, 2] },
  { prefix: '/areas', roles: [0, 1, 2] },
  { prefix: '/tables', roles: [0, 1, 2] },
  { prefix: '/finance', roles: [0] },
  { prefix: '/staff-mgmt', roles: [0] },
  { prefix: '/op-log', roles: [0] },
]
const DEFAULT_NAV_ROLES: Role[] = [0, 1]

export function navAllowed(url: string, role: Role | null | undefined): boolean {
  if (role == null) return false
  if (url === '/') return [0, 1].includes(role) // 数据看板
  const hit = NAV_ROLES.find(
    (r) => url === r.prefix || url.startsWith(r.prefix + '/')
  )
  const roles = hit ? hit.roles : DEFAULT_NAV_ROLES
  return roles.includes(role)
}

// 登录后按角色跳转到合适的首页 (收银员/制作员看不到数据看板)
export function homeForRole(role: Role | null | undefined): string {
  switch (role) {
    case 2: // 收银员
      return '/cashier'
    case 3: // 制作员
      return '/orders'
    default:
      return '/'
  }
}
