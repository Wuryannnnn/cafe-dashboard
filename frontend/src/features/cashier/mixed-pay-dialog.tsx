import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

type PaymentMethod = { methodId: number; methodName: string }
type PaymentRecord = { paymentId: number; methodName: string; amount: number }

/** 组合(分笔)收款: 同一订单可分多笔不同方式收款, 累计达到订单金额后自动结清. */
export function MixedPayDialog({
  orderId,
  orderAmount,
  methods,
  open,
  onOpenChange,
  onSettled,
}: {
  orderId: string | null
  orderAmount: number
  methods: PaymentMethod[]
  open: boolean
  onOpenChange: (o: boolean) => void
  onSettled?: () => void
}) {
  const qc = useQueryClient()
  const [methodId, setMethodId] = useState<number>(methods[0]?.methodId ?? 0)
  const [amount, setAmount] = useState('')
  const [busy, setBusy] = useState(false)

  const { data: records } = useQuery({
    queryKey: ['payments', orderId],
    enabled: open && !!orderId,
    queryFn: async () =>
      (await api.get<PaymentRecord[]>('/api/admin/cashier/payments', { params: { orderId } })).data,
  })

  const paid = useMemo(() => (records ?? []).reduce((s, r) => s + (r.amount ?? 0), 0), [records])
  const remaining = Math.max(0, +(orderAmount - paid).toFixed(2))

  // 收满后自动结清, 提示并关闭
  useEffect(() => {
    if (open && records && remaining <= 0) {
      toast.success('已收满, 订单结清')
      qc.invalidateQueries({ queryKey: ['pending'] })
      onSettled?.()
      onOpenChange(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remaining, records, open])

  // 默认把"剩余应收"填进金额框, 方便一键收尾款
  useEffect(() => {
    if (open) setAmount(remaining > 0 ? String(remaining) : '')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, remaining])

  const addRecord = async () => {
    const amt = Number(amount)
    if (!orderId || busy || !amt || amt <= 0) return
    setBusy(true)
    try {
      const body = new URLSearchParams()
      body.append('orderId', orderId)
      body.append('methodId', String(methodId))
      body.append('amount', String(amt))
      const res = await api.post<{ code: number; msg: string }>('/api/admin/cashier/pay-record', body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      if (res.data?.code === 0) {
        toast.success(res.data.msg || '已记一笔')
        qc.invalidateQueries({ queryKey: ['payments', orderId] })
      } else {
        toast.error(res.data?.msg || '收款失败')
      }
    } catch {
      toast.error('网络错误, 收款失败')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className='max-w-md'>
        <DialogHeader>
          <DialogTitle>组合收款</DialogTitle>
          <DialogDescription className='font-mono text-xs'>{orderId}</DialogDescription>
        </DialogHeader>

        <div className='space-y-4'>
          <div className='grid grid-cols-3 gap-2 text-center'>
            <Stat label='应收' value={orderAmount} />
            <Stat label='已收' value={paid} />
            <Stat label='待收' value={remaining} highlight />
          </div>

          {records && records.length > 0 && (
            <div className='divide-y rounded-md border text-sm'>
              {records.map((r) => (
                <div key={r.paymentId} className='flex justify-between px-3 py-1.5'>
                  <span className='text-muted-foreground'>{r.methodName}</span>
                  <span className='tabular-nums'>¥ {r.amount}</span>
                </div>
              ))}
            </div>
          )}

          <div className='flex items-stretch gap-2'>
            <select
              value={methodId}
              onChange={(e) => setMethodId(Number(e.target.value))}
              className='border-input bg-background rounded-md border px-3 py-2 text-sm'
            >
              {methods.map((m) => (
                <option key={m.methodId} value={m.methodId}>
                  {m.methodName}
                </option>
              ))}
            </select>
            <Input
              type='number'
              min='0'
              step='0.01'
              placeholder='金额'
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className='flex-1'
            />
            <Button disabled={busy || !amount} onClick={addRecord}>
              记一笔
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

function Stat({ label, value, highlight }: { label: string; value: number; highlight?: boolean }) {
  return (
    <div className='bg-muted/40 rounded-md p-2'>
      <div className='text-muted-foreground text-xs'>{label}</div>
      <div className={'font-semibold tabular-nums ' + (highlight ? 'text-primary' : '')}>¥ {value}</div>
    </div>
  )
}
