import { useLayout } from '@/context/layout-provider'
import { useAuthStore } from '@/stores/auth-store'
import { navAllowed, roleName } from '@/lib/permissions'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from '@/components/ui/sidebar'
// import { AppTitle } from './app-title'
import { sidebarData } from './data/sidebar-data'
import { NavGroup } from './nav-group'
import { NavUser } from './nav-user'
import { TeamSwitcher } from './team-switcher'

export function AppSidebar() {
  const { collapsible, variant } = useLayout()
  const authUser = useAuthStore((s) => s.auth.user)
  const roleCode = authUser?.roleCode

  // 显示真实登录员工(姓名 + 角色), 未登录时回退到默认数据
  const navUser = authUser
    ? {
        name: authUser.name || authUser.email || '员工',
        email: roleName(roleCode) || authUser.email,
        avatar: '',
      }
    : sidebarData.user

  // 按当前登录角色过滤导航; 子项全被过滤掉的分组也隐藏
  const navGroups = sidebarData.navGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) =>
        'url' in item && item.url ? navAllowed(item.url as string, roleCode) : true
      ),
    }))
    .filter((group) => group.items.length > 0)

  return (
    <Sidebar collapsible={collapsible} variant={variant}>
      <SidebarHeader>
        <TeamSwitcher teams={sidebarData.teams} />

        {/* Replace <TeamSwitch /> with the following <AppTitle />
         /* if you want to use the normal app title instead of TeamSwitch dropdown */}
        {/* <AppTitle /> */}
      </SidebarHeader>
      <SidebarContent>
        {navGroups.map((props) => (
          <NavGroup key={props.title} {...props} />
        ))}
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={navUser} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
