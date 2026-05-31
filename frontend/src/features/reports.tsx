import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis, Tooltip, Cell, PieChart, Pie } from 'recharts'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { PageShell } from '@/components/page-shell'
import { SimpleTable } from '@/components/simple-table'
import { Card, CardContent } from '@/components/ui/card'

const PIE_COLORS = ['#0a0a0a', '#737373', '#10b981', '#f59e0b']

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

/** 导出 Excel: 走后端 /seller/report/export, 同源带 HttpOnly cookie, 浏览器据 Content-Disposition 下载. */
function ExportButton({ type, days }: { type: 'sales' | 'products' | 'payments'; days: number }) {
  const onExport = () => {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - (days - 1))
    const fmt = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    const url = `/sell/seller/report/export?type=${type}&start=${fmt(start)}&end=${fmt(end)}`
    const a = document.createElement('a')
    a.href = url
    a.rel = 'noopener'
    document.body.appendChild(a)
    a.click()
    a.remove()
  }
  return (
    <button
      onClick={onExport}
      className='border-input text-muted-foreground hover:text-foreground bg-background inline-flex items-center gap-1 rounded-md border px-3 py-1 text-xs font-medium transition-colors'
    >
      <Download className='size-3.5' /> 导出 Excel
    </button>
  )
}

function ReportActions({ type, days, onChange }: { type: 'sales' | 'products' | 'payments'; days: number; onChange: (n: number) => void }) {
  return (
    <div className='flex items-center gap-2'>
      <ExportButton type={type} days={days} />
      <PeriodSwitch value={days} onChange={onChange} />
    </div>
  )
}

export function ReportsSales() {
  const [days, setDays] = useState(7)
  const { data, isLoading } = useQuery({
    queryKey: ['report-sales', days],
    queryFn: async () => (await api.get<any>('/api/admin/reports/sales', { params: { days } })).data,
  })

  return (
    <PageShell pretitle='报表 / 营业' title='营业报表' actions={<ReportActions type='sales' days={days} onChange={setDays} />}>
      <div className='mb-4 grid gap-3 sm:grid-cols-3'>
        <Stat label='总营业额' value={`¥ ${data?.totals?.revenue ?? 0}`} />
        <Stat label='订单数' value={data?.totals?.orderCount ?? 0} />
        <Stat label='客单价' value={`¥ ${data?.totals?.avgPrice ?? 0}`} />
      </div>

      <Card className='mb-4'>
        <CardContent className='p-5'>
          <h3 className='mb-3 text-sm font-semibold'>时段分析 (24 小时)</h3>
          <ResponsiveContainer width='100%' height={220}>
            <BarChart data={data?.hourly ?? []} margin={{ top: 5, right: 5, bottom: 5, left: 0 }}>
              <XAxis dataKey='hour' fontSize={11} stroke='#888' tickLine={false} axisLine={false} />
              <YAxis fontSize={11} stroke='#888' tickLine={false} axisLine={false} tickFormatter={(v) => `¥${v}`} />
              <Tooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} contentStyle={{ fontSize: 12, borderRadius: 8 }} />
              <Bar dataKey='revenue' radius={[4, 4, 0, 0]} className='fill-primary' />
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      <h3 className='mb-3 text-sm font-semibold'>每日明细</h3>
      <SimpleTable
        loading={isLoading}
        rows={data?.daily}
        rowKey={(r: any) => r.date}
        columns={[
          { header: '日期', render: (r: any) => <span className='tabular-nums'>{r.date}</span> },
          { header: '营业额', align: 'right', render: (r: any) => <span className='tabular-nums font-semibold'>¥ {r.revenue}</span> },
          { header: '订单数', align: 'right', render: (r: any) => <span className='tabular-nums'>{r.orderCount}</span> },
          { header: '客单价', align: 'right', render: (r: any) => <span className='tabular-nums'>¥ {r.avgPrice}</span> },
        ]}
      />
    </PageShell>
  )
}

export function ReportsProducts() {
  const [days, setDays] = useState(30)
  const { data, isLoading } = useQuery({
    queryKey: ['report-products', days],
    queryFn: async () => (await api.get<any>('/api/admin/reports/products', { params: { days } })).data,
  })

  return (
    <PageShell pretitle='报表 / 菜品' title='菜品报表' actions={<ReportActions type='products' days={days} onChange={setDays} />}>
      <SimpleTable
        loading={isLoading}
        rows={data?.topProducts}
        rowKey={(r: any) => r.rank}
        columns={[
          {
            header: '排名',
            render: (r: any) => (
              <span
                className={
                  'inline-flex h-6 w-6 items-center justify-center rounded text-xs font-semibold ' +
                  (r.rank === 1
                    ? 'bg-amber-100 text-amber-700'
                    : r.rank === 2
                      ? 'bg-blue-100 text-blue-700'
                      : r.rank === 3
                        ? 'bg-emerald-100 text-emerald-700'
                        : 'bg-muted text-muted-foreground')
                }
              >
                {r.rank}
              </span>
            ),
          },
          { header: '菜品', render: (r: any) => <span className='font-medium'>{r.name}</span> },
          { header: '销量', align: 'right', render: (r: any) => <span className='tabular-nums font-semibold'>{r.qty}</span> },
        ]}
      />
    </PageShell>
  )
}

export function ReportsPayments() {
  const [days, setDays] = useState(30)
  const { data, isLoading } = useQuery({
    queryKey: ['report-payments', days],
    queryFn: async () => (await api.get<any>('/api/admin/reports/payments', { params: { days } })).data,
  })

  const total = data?.rows?.reduce((s: number, r: any) => s + Number(r.amount ?? 0), 0) ?? 0

  return (
    <PageShell pretitle='报表 / 收款' title='收款报表' actions={<ReportActions type='payments' days={days} onChange={setDays} />}>
      <div className='grid gap-4 lg:grid-cols-2'>
        <Card>
          <CardContent className='p-5'>
            <h3 className='mb-3 text-sm font-semibold'>支付方式占比</h3>
            {(!data?.rows || data.rows.length === 0) && !isLoading ? (
              <p className='text-muted-foreground py-12 text-center text-sm'>暂无数据</p>
            ) : (
              <ResponsiveContainer width='100%' height={260}>
                <PieChart>
                  <Pie
                    data={data?.rows ?? []}
                    dataKey='amount'
                    nameKey='name'
                    cx='50%'
                    cy='50%'
                    innerRadius={50}
                    outerRadius={90}
                    label={(e) => e.name}
                  >
                    {(data?.rows ?? []).map((_: any, i: number) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: any) => `¥ ${v}`} contentStyle={{ fontSize: 12, borderRadius: 8 }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent className='p-5'>
            <h3 className='mb-3 text-sm font-semibold'>明细</h3>
            <table className='w-full text-sm'>
              <thead>
                <tr className='text-muted-foreground border-b text-xs uppercase'>
                  <th className='p-2 text-left font-medium'>方式</th>
                  <th className='p-2 text-right font-medium'>笔数</th>
                  <th className='p-2 text-right font-medium'>金额</th>
                  <th className='p-2 text-right font-medium'>占比</th>
                </tr>
              </thead>
              <tbody>
                {data?.rows?.map((r: any, i: number) => (
                  <tr key={i} className='border-b last:border-0'>
                    <td className='p-2'>
                      <Badge variant='secondary'>{r.name}</Badge>
                    </td>
                    <td className='p-2 text-right tabular-nums'>{r.count}</td>
                    <td className='p-2 text-right tabular-nums font-semibold'>¥ {r.amount}</td>
                    <td className='p-2 text-right tabular-nums text-muted-foreground'>
                      {total > 0 ? ((Number(r.amount ?? 0) / total) * 100).toFixed(1) : 0}%
                    </td>
                  </tr>
                ))}
                {(!data?.rows || data.rows.length === 0) && (
                  <tr><td colSpan={4} className='text-muted-foreground p-8 text-center text-sm'>暂无数据</td></tr>
                )}
              </tbody>
            </table>
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}

function Stat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Card>
      <CardContent className='p-4'>
        <p className='text-muted-foreground text-xs font-medium uppercase tracking-wide'>{label}</p>
        <p className='mt-1 text-2xl font-bold tabular-nums'>{value}</p>
      </CardContent>
    </Card>
  )
}
