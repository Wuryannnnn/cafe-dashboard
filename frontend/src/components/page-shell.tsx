import { ConfigDrawer } from '@/components/config-drawer'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'

type Props = {
  pretitle?: string
  title: React.ReactNode
  actions?: React.ReactNode
  children: React.ReactNode
}

/** 通用页面外壳: header + 标题 + 操作 + 内容 */
export function PageShell({ pretitle, title, actions, children }: Props) {
  return (
    <>
      <Header>
        <Search />
        <div className='ms-auto flex items-center gap-2'>
          <ThemeSwitch />
          <ConfigDrawer />
          <ProfileDropdown />
        </div>
      </Header>
      <Main>
        <div className='mb-6 flex flex-wrap items-end justify-between gap-3'>
          <div>
            {pretitle && <p className='text-muted-foreground text-xs'>{pretitle}</p>}
            <h1 className='text-2xl font-bold tracking-tight'>{title}</h1>
          </div>
          {actions && <div className='flex flex-wrap items-center gap-2'>{actions}</div>}
        </div>
        {children}
      </Main>
    </>
  )
}
