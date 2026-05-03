import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import {
  ArrowDown,
  ArrowUp,
  Banknote,
  Coffee,
  Cookie,
  Plus,
  ShoppingBag,
  TrendingUp,
} from 'lucide-react'
import { api, type DashboardData, type RecentShipments, type WmsAlert, type WmsOverview } from '@/lib/api'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { ConfigDrawer } from '@/components/config-drawer'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'
import { TrendBar } from './components/trend-bar'

export function Dashboard() {
  const [days, setDays] = useState(7)

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard', days],
    queryFn: async () => (await api.get<DashboardData>('/seller/dashboard/data', { params: { days } })).data,
  })

  const { data: alerts } = useQuery({
    queryKey: ['wms-alerts'],
    queryFn: async () => (await api.get<WmsAlert>('/seller/wms/widget/alerts')).data,
  })
  const { data: overview } = useQuery({
    queryKey: ['wms-overview'],
    queryFn: async () => (await api.get<WmsOverview>('/seller/wms/widget/overview')).data,
  })
  const { data: shipments } = useQuery({
    queryKey: ['wms-recent', 6],
    queryFn: async () => (await api.get<RecentShipments>('/seller/wms/widget/recent-shipments', { params: { limit: 6 } })).data,
  })

  const todayLabel = formatToday()

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
            <p className='text-muted-foreground text-xs'>{todayLabel}</p>
            <h1 className='text-2xl font-bold tracking-tight'>数据看板</h1>
          </div>
          <div className='flex items-center gap-2'>
            <PeriodSwitch value={days} onChange={setDays} />
            <Link to='/cashier'>
              <Button>
                <Plus className='size-4' /> 去收银
              </Button>
            </Link>
          </div>
        </div>

        <div className='grid gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-4'>
          <StatCard
            icon={<Banknote className='size-4' />}
            label='今日营业额'
            value={`¥${data?.todayRevenue ?? 0}`}
            sub={<TrendDelta today={data?.todayRevenue} yest={data?.yestRevenue} />}
            loading={isLoading}
          />
          <StatCard
            icon={<ShoppingBag className='size-4' />}
            label='今日订单数'
            value={data?.todayOrders ?? 0}
            sub={<TrendDelta today={data?.todayOrders} yest={data?.yestOrders} />}
            loading={isLoading}
          />
          <StatCard
            icon={<Coffee className='size-4' />}
            label='客单价'
            value={`¥${data?.avgPrice ?? 0}`}
            sub='今日均值'
            loading={isLoading}
          />
          <StatCard
            icon={<Cookie className='size-4' />}
            label='活跃菜品'
            value={data?.topProducts?.length ?? 0}
            sub={<Link to='/products' className='hover:text-primary'>查看清单 →</Link>}
            loading={isLoading}
          />
        </div>

        <div className='grid grid-cols-1 gap-4 lg:grid-cols-7'>
          <Card className='col-span-1 lg:col-span-4'>
            <CardHeader className='flex flex-row items-center justify-between'>
              <div>
                <CardTitle>营业额趋势</CardTitle>
                <CardDescription>近 {days} 天</CardDescription>
              </div>
            </CardHeader>
            <CardContent className='ps-2'>
              <TrendBar
                labels={data?.trendLabels ?? []}
                values={data?.trendRevenue ?? []}
              />
            </CardContent>
          </Card>

          <Card className='col-span-1 lg:col-span-3'>
            <CardHeader>
              <CardTitle>今日订单状态</CardTitle>
              <CardDescription>实时</CardDescription>
            </CardHeader>
            <CardContent>
              <StatusList dist={data?.statusDist ?? {}} />
            </CardContent>
          </Card>
        </div>

        <div className='grid grid-cols-1 gap-4 lg:grid-cols-7 mt-4'>
          <Card className='col-span-1 lg:col-span-4'>
            <CardHeader>
              <CardTitle>热销 TOP 10</CardTitle>
              <CardDescription>近 {days} 天销量</CardDescription>
            </CardHeader>
            <CardContent>
              <TopProducts items={data?.topProducts ?? []} />
            </CardContent>
          </Card>

          <Card className='col-span-1 lg:col-span-3'>
            <CardHeader className='flex flex-row items-center justify-between'>
              <div>
                <CardTitle className='flex items-center gap-2'>
                  库存预警
                  {overview?.configured && (
                    <span className='text-muted-foreground text-xs font-normal'>
                      物料 {overview.itemCount} · 告警{' '}
                      <span className='text-destructive font-medium'>
                        {overview.alertCount ?? 0}
                      </span>
                    </span>
                  )}
                </CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <WmsAlerts alerts={alerts} />
              {shipments?.configured && shipments.items && shipments.items.length > 0 && (
                <div className='mt-6'>
                  <p className='text-muted-foreground mb-2 text-xs font-medium'>最近出库</p>
                  <RecentShipmentsList items={shipments.items} />
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </Main>
    </>
  )
}

function StatCard({
  icon,
  label,
  value,
  sub,
  loading,
}: {
  icon: React.ReactNode
  label: string
  value: React.ReactNode
  sub: React.ReactNode
  loading?: boolean
}) {
  return (
    <Card>
      <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
        <CardTitle className='text-sm font-medium'>{label}</CardTitle>
        <span className='text-muted-foreground'>{icon}</span>
      </CardHeader>
      <CardContent>
        <div className='text-2xl font-bold tabular-nums'>
          {loading ? '—' : value}
        </div>
        <div className='text-muted-foreground mt-1 text-xs'>{sub}</div>
      </CardContent>
    </Card>
  )
}

function TrendDelta({ today, yest }: { today?: number; yest?: number }) {
  if (!today || !yest) return <span>较昨日 —</span>
  const diff = today - yest
  if (diff === 0) return <span>较昨日 持平</span>
  const pct = ((diff / yest) * 100).toFixed(1)
  const up = diff > 0
  return (
    <span className={up ? 'text-destructive' : 'text-emerald-600'}>
      较昨日 {up ? <ArrowUp className='inline size-3' /> : <ArrowDown className='inline size-3' />}
      {Math.abs(Number(pct))}%
    </span>
  )
}

function PeriodSwitch({ value, onChange }: { value: number; onChange: (n: number) => void }) {
  const opts = [7, 14, 30]
  return (
    <div className='inline-flex rounded-md border bg-background p-0.5'>
      {opts.map((n) => (
        <button
          key={n}
          onClick={() => onChange(n)}
          className={
            'px-3 py-1 text-xs font-medium rounded-sm transition-colors ' +
            (value === n
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground')
          }
        >
          近 {n} 天
        </button>
      ))}
    </div>
  )
}

function StatusList({ dist }: { dist: Record<string, number> }) {
  const colors: Record<string, string> = {
    新订单: 'bg-amber-500',
    制作中: 'bg-orange-600',
    待取餐: 'bg-blue-500',
    完结: 'bg-emerald-500',
  }
  const entries = Object.entries(dist)
  if (entries.length === 0)
    return <p className='text-muted-foreground py-6 text-center text-sm'>暂无数据</p>
  return (
    <div className='space-y-1'>
      {entries.map(([k, v]) => (
        <div key={k} className='flex items-center border-b py-2 last:border-0 text-sm'>
          <span className={'me-2 size-2 rounded-full ' + (colors[k] ?? 'bg-muted-foreground')} />
          <span className='flex-1'>{k}</span>
          <span className='font-semibold tabular-nums'>{v}</span>
        </div>
      ))}
    </div>
  )
}

function TopProducts({ items }: { items: { rank: number; name: string; qty: number }[] }) {
  if (items.length === 0)
    return <p className='text-muted-foreground py-8 text-center text-sm'>暂无数据</p>
  return (
    <div className='space-y-1'>
      {items.map((p) => (
        <div key={p.rank} className='flex items-center border-b py-2 last:border-0 text-sm'>
          <span
            className={
              'me-3 inline-flex h-6 w-6 items-center justify-center rounded text-xs font-semibold ' +
              (p.rank === 1
                ? 'bg-amber-100 text-amber-700'
                : p.rank === 2
                  ? 'bg-blue-100 text-blue-700'
                  : p.rank === 3
                    ? 'bg-emerald-100 text-emerald-700'
                    : 'bg-muted text-muted-foreground')
            }
          >
            {p.rank}
          </span>
          <span className='flex-1'>{p.name}</span>
          <span className='font-semibold tabular-nums'>{p.qty}</span>
        </div>
      ))}
    </div>
  )
}

function WmsAlerts({ alerts }: { alerts?: WmsAlert }) {
  if (!alerts) return <p className='text-muted-foreground py-4 text-sm'>加载中…</p>
  if (!alerts.configured)
    return (
      <p className='text-muted-foreground py-4 text-sm'>
        未配置 WMS, <Link to='/wms' className='text-primary'>去配置 →</Link>
      </p>
    )
  if (!alerts.alerts || alerts.alerts.length === 0)
    return (
      <p className='text-emerald-600 py-2 text-sm'>
        <TrendingUp className='inline size-4 me-1' /> 全部物料库存充足
      </p>
    )
  return (
    <div className='space-y-1'>
      {alerts.alerts.map((a) => (
        <div key={a.id} className='flex items-center justify-between border-b py-2 last:border-0 text-sm'>
          <span>{a.itemName ?? a.skuName ?? `物料 #${a.id}`}</span>
          <span className='text-destructive font-semibold tabular-nums'>剩 {a.quantity}</span>
        </div>
      ))}
    </div>
  )
}

function RecentShipmentsList({
  items,
}: {
  items: { createTime: string; bizOrderNo: string }[]
}) {
  return (
    <div className='space-y-1'>
      {items.map((s, i) => (
        <div key={i} className='flex items-center gap-3 text-xs border-b py-1.5 last:border-0'>
          <span className='text-muted-foreground tabular-nums'>{(s.createTime ?? '').toString().substring(5, 16)}</span>
          <code className='bg-muted text-foreground rounded px-1.5 py-0.5 text-[11px]'>
            {(s.bizOrderNo ?? '-').substring(0, 14)}
          </code>
        </div>
      ))}
    </div>
  )
}

function formatToday() {
  const d = new Date()
  const w = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 · ${w[d.getDay()]}`
}
