import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useSearch } from '@tanstack/react-router'
import { api } from '@/lib/api'

const STATUS_NAMES: Record<number, string> = {
  0: '已下单',
  1: '制作中',
  2: '待取餐',
  3: '已完成',
  4: '已取消',
}

const STEPS = [
  { code: 0, label: '已下单' },
  { code: 1, label: '制作中' },
  { code: 2, label: '待取餐' },
  { code: 3, label: '已完成' },
]

export function H5StatusPage() {
  const search = useSearch({ from: '/order/status' }) as { orderId?: string; out_trade_no?: string; table?: string }
  // 支付宝同步跳回带的是 out_trade_no, 不是 orderId, 兼容一下
  const orderId = search.orderId ?? search.out_trade_no
  const tableNumber = search.table ?? ''

  const { data, isLoading, isFetched } = useQuery({
    queryKey: ['h5-order-status', orderId],
    queryFn: async () => {
      const r = await api.get<{ code: number; data: any }>(`/buyer/order/status?orderId=${orderId}`)
      return r.data.code === 0 ? r.data.data : null
    },
    // 订单完成 / 取消后停止轮询
    refetchInterval: (q: any) => {
      const d = q.state?.data
      if (!d) return false
      return d.orderStatus >= 3 ? false : 5000
    },
    enabled: !!orderId,
    retry: false,
  })

  // 主题色
  const { data: cfg } = useQuery({
    queryKey: ['h5-shop-config'],
    queryFn: async () => (await api.get<{ data: any }>('/buyer/shop/config')).data.data,
  })
  useEffect(() => {
    if (cfg?.themeColor) document.documentElement.style.setProperty('--cafe', cfg.themeColor)
  }, [cfg?.themeColor])

  if (!orderId) {
    return <NotFound msg='订单号缺失' />
  }
  if (isLoading) {
    return <div className='text-stone-400 p-12 text-center text-sm'>加载中…</div>
  }
  // 已查询完, 但 data 是 null —— 订单不存在 / 已被清空
  if (isFetched && !data) {
    return <NotFound msg='订单不存在或已过期' />
  }
  if (!data) {
    return <div className='text-stone-400 p-12 text-center text-sm'>加载中…</div>
  }

  const status: number = data.orderStatus
  const isCancelled = status === 4
  const progress = isCancelled ? 0 : Math.min(status, 3)
  const statusText = STATUS_NAMES[status] ?? '未知'
  const dining = data.diningType === 1 ? '外带' : '堂食'
  const payType = data.payType === 1 ? '支付宝' : '微信支付'

  return (
    <div className='min-h-dvh bg-stone-50 pb-12' style={{ fontFamily: 'Inter, -apple-system, "PingFang SC", sans-serif' }}>
      {/* 头部取餐号 */}
      <header
        className='relative overflow-hidden px-4 pt-8 pb-10 text-center text-white'
        style={isCancelled
          ? { background: 'linear-gradient(180deg, #6b7280 0%, #4b5563 100%)' }
          : { background: 'linear-gradient(180deg, var(--cafe, #6b4226) 0%, #4A2D18 100%)' }
        }
      >
        <p className='text-xs font-semibold uppercase tracking-widest text-white/70'>取餐号</p>
        <div className='mt-2 text-6xl font-bold tabular-nums tracking-tighter'>#{data.pickupNumber ?? '-'}</div>
        <p className='mt-1.5 text-sm'>{statusText}</p>
      </header>

      {/* 状态进度 */}
      {!isCancelled && (
        <Card className='-mt-4 relative z-10'>
          <div className='relative mb-2'>
            <div className='absolute top-3.5 right-3.5 left-3.5 h-0.5 bg-stone-200' />
            <div
              className='absolute top-3.5 left-3.5 h-0.5 transition-all duration-500'
              style={{
                background: 'var(--cafe, #6b4226)',
                width: `calc(${(progress / (STEPS.length - 1)) * 100}% - ${(progress / (STEPS.length - 1)) * 7}px)`,
              }}
            />
            <div className='relative flex justify-between'>
              {STEPS.map((s, i) => {
                const done = status >= s.code
                const active = status === s.code
                return (
                  <div key={s.code} className='flex flex-col items-center gap-1.5' style={{ minWidth: 60 }}>
                    <div
                      className={
                        'flex size-7 items-center justify-center rounded-full border-2 text-xs font-bold transition ' +
                        (done ? 'text-white' : 'bg-white text-stone-400')
                      }
                      style={
                        done
                          ? { background: 'var(--cafe, #6b4226)', borderColor: 'var(--cafe, #6b4226)' }
                          : active
                            ? { borderColor: 'var(--cafe, #6b4226)', color: 'var(--cafe, #6b4226)' }
                            : { borderColor: '#e7e5e4' }
                      }
                    >
                      {done ? '✓' : i + 1}
                    </div>
                    <span
                      className={'text-xs font-medium ' + (done || active ? 'text-stone-900' : 'text-stone-400')}
                      style={done || active ? { color: 'var(--cafe, #6b4226)' } : {}}
                    >
                      {s.label}
                    </span>
                  </div>
                )
              })}
            </div>
          </div>
          <div className='border-t pt-3 text-center text-sm font-medium' style={{ color: 'var(--cafe, #6b4226)' }}>
            {status === 0 && '订单已收到, 即将开始制作'}
            {status === 1 && '正在制作中, 请稍候…'}
            {status === 2 && '已出餐, 请到取餐台领取'}
            {status === 3 && '感谢光临 ☕'}
          </div>
        </Card>
      )}

      {/* 订单信息 */}
      <Card>
        <SectionTitle>订单信息</SectionTitle>
        <InfoRow k='订单号' v={data.orderId} />
        <InfoRow k='就餐方式' v={`${dining}${data.tableNumber ? ' · 桌号 ' + data.tableNumber : ''}`} />
        {data.orderRemark && <InfoRow k='备注' v={data.orderRemark} />}
        <InfoRow k='支付方式' v={payType} />
        <InfoRow k='下单时间' v={fmtTime(data.createTime)} />
      </Card>

      {/* 商品明细 */}
      <Card>
        <SectionTitle>商品明细</SectionTitle>
        {(data.orderDetailList ?? []).map((item: any, i: number) => {
          let extras: string[] = []
          if (item.skuName) extras.push(item.skuName)
          if (item.addons && item.addons !== '[]') {
            try {
              const a = JSON.parse(item.addons)
              if (Array.isArray(a)) extras.push('+ ' + a.map((x: any) => x.name).join(' / '))
            } catch {}
          }
          return (
            <div key={i} className='flex items-start gap-3 border-b py-2.5 last:border-0 text-sm'>
              <div className='min-w-0 flex-1 font-medium'>
                {item.productName}
                {extras.length > 0 && (
                  <div className='text-stone-500 text-xs font-normal'>{extras.join(' / ')}</div>
                )}
              </div>
              <span className='text-stone-500 text-xs tabular-nums'>×{item.productQuantity}</span>
              <span className='font-semibold tabular-nums'>¥{(item.productPrice * item.productQuantity).toFixed(2)}</span>
            </div>
          )
        })}
        <div className='mt-3 flex items-baseline justify-between border-t-2 pt-3'>
          <span className='text-stone-500 text-sm'>合计</span>
          <span className='text-2xl font-bold tabular-nums' style={{ color: 'var(--cafe, #6b4226)' }}>¥{data.orderAmount ?? 0}</span>
        </div>
      </Card>

      <div className='mx-3 mt-5'>
        <Link
          to='/order'
          search={{ table: tableNumber || undefined }}
          className='block rounded-xl py-3 text-center text-sm font-semibold text-white active:scale-[0.97]'
          style={{ background: 'var(--cafe, #6b4226)' }}
        >
          继续点餐
        </Link>
      </div>
    </div>
  )
}

function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <div className={'mx-3 mt-3 rounded-2xl border bg-white p-4 ' + className}>{children}</div>
}
function SectionTitle({ children }: { children: React.ReactNode }) {
  return <p className='text-stone-500 mb-3 text-xs font-semibold uppercase tracking-widest'>{children}</p>
}
function InfoRow({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div className='flex justify-between gap-3 border-b py-2 text-sm last:border-0'>
      <span className='text-stone-500 shrink-0'>{k}</span>
      <span className='text-end font-medium tabular-nums'>{v}</span>
    </div>
  )
}

function NotFound({ msg }: { msg: string }) {
  return (
    <div className='flex min-h-dvh flex-col items-center justify-center gap-4 bg-stone-50 px-6 text-center'>
      <p className='text-lg font-semibold text-stone-700'>{msg}</p>
      <p className='text-stone-400 text-sm'>这一单可能因为系统重启被清空</p>
      <Link
        to='/order'
        className='mt-4 rounded-xl px-6 py-2.5 text-sm font-semibold text-white active:scale-95'
        style={{ background: 'var(--cafe, #6b4226)' }}
      >
        重新点单
      </Link>
    </div>
  )
}

function fmtTime(ts: any) {
  if (!ts) return '—'
  const d = new Date(typeof ts === 'number' ? ts * 1000 : ts)
  const p = (n: number) => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
