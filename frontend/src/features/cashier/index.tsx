import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Coffee, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ConfigDrawer } from '@/components/config-drawer'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { OrderDetailDialog } from '@/components/order-detail-dialog'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'
import { ManualOrderDialog } from './manual-order-dialog'
import { MixedPayDialog } from './mixed-pay-dialog'

type PendingOrder = {
  orderId: string
  pickupNumber?: string
  tableNumber?: string
  orderAmount: number
  createTime: string
}

type Table = { tableId: number; tableCode: string; areaId: number }
type PaymentMethod = { methodId: number; methodName: string; methodCode: string }

export function Cashier() {
  const [tableId, setTableId] = useState<number | null>(null)
  const [detailOrderId, setDetailOrderId] = useState<string | null>(null)
  const [manualOpen, setManualOpen] = useState(false)
  const [mixedOrder, setMixedOrder] = useState<PendingOrder | null>(null)
  const qc = useQueryClient()

  const { data: pending, isLoading } = useQuery({
    queryKey: ['pending', tableId],
    queryFn: async () =>
      (await api.get<PendingOrder[]>('/api/admin/orders/pending', {
        params: tableId ? { tableId } : {},
      })).data,
  })

  const { data: tables } = useQuery({
    queryKey: ['tables'],
    queryFn: async () => (await api.get<Table[]>('/api/admin/tables')).data,
  })

  const { data: methods } = useQuery({
    queryKey: ['payment-methods'],
    queryFn: async () => (await api.get<PaymentMethod[]>('/api/admin/payment-methods')).data,
  })

  const pay = async (orderId: string, methodId: number) => {
    try {
      const body = new URLSearchParams()
      body.append('orderId', orderId)
      body.append('methodId', String(methodId))
      const res = await api.post<{ code: number; msg: string }>('/api/admin/cashier/offline-pay', body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      if (res.data?.code === 0) {
        toast.success(res.data.msg || '收款成功')
        qc.invalidateQueries({ queryKey: ['pending'] })
      } else {
        toast.error(res.data?.msg || '收款失败')
      }
    } catch {
      toast.error('网络错误, 收款失败')
    }
  }

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
        <div className='mb-6 flex items-end justify-between gap-3'>
          <div>
            <p className='text-muted-foreground text-xs'>运营 / 收银</p>
            <h1 className='text-2xl font-bold tracking-tight'>
              收银台 <span className='text-muted-foreground ms-2 text-base font-normal'>· 待收款 {pending?.length ?? 0} 单</span>
            </h1>
          </div>
          <Button onClick={() => setManualOpen(true)}>
            <Plus className='size-4' /> 手动建单
          </Button>
        </div>

        <div className='mb-4 flex flex-wrap gap-1'>
          <FilterChip active={tableId == null} onClick={() => setTableId(null)}>全部</FilterChip>
          {tables?.map((t) => (
            <FilterChip key={t.tableId} active={tableId === t.tableId} onClick={() => setTableId(t.tableId)}>
              {t.tableCode}
            </FilterChip>
          ))}
        </div>

        {isLoading ? (
          <Card>
            <CardContent className='text-muted-foreground p-12 text-center'>加载中…</CardContent>
          </Card>
        ) : !pending || pending.length === 0 ? (
          <Card>
            <CardContent className='text-muted-foreground flex flex-col items-center justify-center gap-3 p-16'>
              <Coffee className='size-10 opacity-30' />
              <p>当前没有待收款的订单</p>
            </CardContent>
          </Card>
        ) : (
          <div className='grid gap-4 md:grid-cols-2'>
            {pending.map((o) => (
              <OrderCard
                key={o.orderId}
                order={o}
                methods={methods ?? []}
                onPay={pay}
                onMixed={() => setMixedOrder(o)}
                onDetail={() => setDetailOrderId(o.orderId)}
              />
            ))}
          </div>
        )}
      </Main>

      <OrderDetailDialog
        orderId={detailOrderId}
        open={!!detailOrderId}
        onOpenChange={(o) => { if (!o) setDetailOrderId(null) }}
        onChanged={() => qc.invalidateQueries({ queryKey: ['pending'] })}
      />

      <ManualOrderDialog
        open={manualOpen}
        onOpenChange={setManualOpen}
        onCreated={() => qc.invalidateQueries({ queryKey: ['pending'] })}
      />

      <MixedPayDialog
        orderId={mixedOrder?.orderId ?? null}
        orderAmount={mixedOrder?.orderAmount ?? 0}
        methods={methods ?? []}
        open={!!mixedOrder}
        onOpenChange={(o) => { if (!o) setMixedOrder(null) }}
        onSettled={() => { setMixedOrder(null); qc.invalidateQueries({ queryKey: ['pending'] }) }}
      />
    </>
  )
}

function OrderCard({
  order,
  methods,
  onPay,
  onMixed,
  onDetail,
}: {
  order: PendingOrder
  methods: PaymentMethod[]
  onPay: (orderId: string, methodId: number) => void
  onMixed: () => void
  onDetail: () => void
}) {
  const [methodId, setMethodId] = useState<number>(methods[0]?.methodId ?? 0)
  return (
    <Card>
      <CardContent className='space-y-3 p-5'>
        <div className='flex items-start justify-between'>
          <div className='flex items-center gap-2'>
            <span className='text-primary text-2xl font-bold tabular-nums'>#{order.pickupNumber ?? '-'}</span>
            {order.tableNumber && (
              <span className='bg-muted text-foreground rounded px-2 py-0.5 text-xs font-semibold'>
                {order.tableNumber}
              </span>
            )}
          </div>
          <div className='text-muted-foreground text-right text-xs'>
            <div>{(order.createTime ?? '').toString().substring(5, 16)}</div>
            <div className='font-mono'>{order.orderId.substring(0, 8)}</div>
          </div>
        </div>

        <div className='flex items-baseline gap-2 py-1'>
          <span className='text-muted-foreground text-sm'>¥</span>
          <span className='text-3xl font-bold tabular-nums'>{order.orderAmount}</span>
        </div>

        <div className='flex items-stretch gap-2'>
          <select
            value={methodId}
            onChange={(e) => setMethodId(Number(e.target.value))}
            className='border-input rounded-md border bg-background px-3 py-2 text-sm'
          >
            {methods.map((m) => (
              <option key={m.methodId} value={m.methodId}>
                {m.methodName}
              </option>
            ))}
          </select>
          <Button className='flex-1' onClick={() => onPay(order.orderId, methodId)}>
            全额收款
          </Button>
          <Button variant='outline' onClick={onMixed} title='分笔 / 组合收款'>
            组合
          </Button>
        </div>

        <div className='text-muted-foreground flex items-center justify-between border-t pt-2 text-xs'>
          <button onClick={onDetail} className='hover:text-primary'>
            查看订单详情 →
          </button>
        </div>
      </CardContent>
    </Card>
  )
}

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      onClick={onClick}
      className={
        'rounded-full border px-3 py-1 text-xs transition-colors ' +
        (active
          ? 'bg-primary text-primary-foreground border-primary'
          : 'bg-background text-muted-foreground hover:bg-muted')
      }
    >
      {children}
    </button>
  )
}
