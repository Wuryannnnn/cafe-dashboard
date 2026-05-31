import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Minus, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

type Product = {
  productId: string
  productName: string
  productPrice: number
  productStatus?: number
  categoryType?: number
}
type Table = { tableId: number; tableCode: string }

/** 收银台手动建单: 选桌(可选) + 选商品数量 → POST /api/admin/cashier/manual-order. */
export function ManualOrderDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  onCreated?: () => void
}) {
  const [tableId, setTableId] = useState<number | null>(null)
  const [cart, setCart] = useState<Record<string, number>>({})
  const [busy, setBusy] = useState(false)

  const { data: products } = useQuery({
    queryKey: ['products-onsale'],
    enabled: open,
    queryFn: async () => (await api.get<Product[]>('/api/admin/products')).data,
  })
  const { data: tables } = useQuery({
    queryKey: ['tables'],
    enabled: open,
    queryFn: async () => (await api.get<Table[]>('/api/admin/tables')).data,
  })

  const onSale = useMemo(
    () => (products ?? []).filter((p) => p.productStatus == null || p.productStatus === 0),
    [products]
  )
  const total = useMemo(
    () => onSale.reduce((s, p) => s + (cart[p.productId] ?? 0) * (p.productPrice ?? 0), 0),
    [onSale, cart]
  )
  const itemCount = Object.values(cart).reduce((s, n) => s + n, 0)

  const setQty = (id: string, delta: number) =>
    setCart((c) => {
      const next = Math.max(0, (c[id] ?? 0) + delta)
      const copy = { ...c }
      if (next === 0) delete copy[id]
      else copy[id] = next
      return copy
    })

  const reset = () => {
    setCart({})
    setTableId(null)
  }

  const submit = async () => {
    if (itemCount === 0 || busy) return
    setBusy(true)
    try {
      const items = Object.entries(cart).map(([productId, productQuantity]) => ({ productId, productQuantity }))
      const body = new URLSearchParams()
      if (tableId != null) body.append('tableId', String(tableId))
      body.append('items', JSON.stringify(items))
      const res = await api.post<{ code: number; msg: string }>('/api/admin/cashier/manual-order', body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      if (res.data?.code === 0) {
        toast.success(res.data.msg || '已建单')
        reset()
        onCreated?.()
        onOpenChange(false)
      } else {
        toast.error(res.data?.msg || '建单失败')
      }
    } catch {
      toast.error('网络错误, 建单失败')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className='max-w-2xl'>
        <DialogHeader>
          <DialogTitle>手动建单</DialogTitle>
          <DialogDescription>为到店顾客在收银台直接开单(堂食)</DialogDescription>
        </DialogHeader>

        <div className='space-y-4'>
          <div>
            <p className='text-muted-foreground mb-1.5 text-xs'>桌号(可选)</p>
            <div className='flex flex-wrap gap-1'>
              <Chip active={tableId == null} onClick={() => setTableId(null)}>
                无 / 外带
              </Chip>
              {tables?.map((t) => (
                <Chip key={t.tableId} active={tableId === t.tableId} onClick={() => setTableId(t.tableId)}>
                  {t.tableCode}
                </Chip>
              ))}
            </div>
          </div>

          <div className='max-h-72 overflow-y-auto rounded-md border'>
            {onSale.length === 0 ? (
              <div className='text-muted-foreground p-6 text-center text-sm'>没有在售商品</div>
            ) : (
              <div className='divide-y'>
                {onSale.map((p) => (
                  <div key={p.productId} className='flex items-center gap-3 p-2.5'>
                    <div className='min-w-0 flex-1'>
                      <div className='truncate text-sm font-medium'>{p.productName}</div>
                      <div className='text-muted-foreground text-xs tabular-nums'>¥ {p.productPrice}</div>
                    </div>
                    <div className='flex items-center gap-2'>
                      <Button
                        size='icon'
                        variant='outline'
                        className='size-7'
                        disabled={!cart[p.productId]}
                        onClick={() => setQty(p.productId, -1)}
                      >
                        <Minus className='size-3.5' />
                      </Button>
                      <span className='w-6 text-center text-sm tabular-nums'>{cart[p.productId] ?? 0}</span>
                      <Button size='icon' variant='outline' className='size-7' onClick={() => setQty(p.productId, 1)}>
                        <Plus className='size-3.5' />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <DialogFooter className='items-center justify-between sm:justify-between'>
          <div className='text-sm'>
            共 {itemCount} 件 · 合计 <span className='text-primary font-semibold tabular-nums'>¥ {total.toFixed(2)}</span>
          </div>
          <Button disabled={itemCount === 0 || busy} onClick={submit}>
            建单
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={
        'rounded-full border px-3 py-1 text-xs transition-colors ' +
        (active ? 'bg-primary text-primary-foreground border-primary' : 'bg-background text-muted-foreground hover:bg-muted')
      }
    >
      {children}
    </button>
  )
}
