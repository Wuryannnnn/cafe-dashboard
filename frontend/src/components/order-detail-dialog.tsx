import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

type Detail = {
  detailId: string
  productId: string
  productName: string
  productPrice: number
  productQuantity: number
  productIcon?: string
  skuName?: string
  addons?: string
  addonFee?: number
}

type OrderDTO = {
  orderId: string
  buyerName?: string
  buyerPhone?: string
  orderAmount: number
  orderStatus: number
  payStatus: number
  payType?: number
  diningType?: number
  tableNumber?: string
  pickupNumber?: string
  orderRemark?: string
  createTime?: number
  updateTime?: number
  orderDetailList?: Detail[]
}

const ORDER_STATUS: Record<number, string> = {
  0: '新订单',
  1: '制作中',
  2: '待取餐',
  3: '完结',
  4: '已取消',
  5: '已退款',
}
const PAY_STATUS: Record<number, string> = { 0: '未支付', 1: '已支付', 2: '已退款' }
const PAY_TYPE: Record<number, string> = { 0: '微信', 1: '现金', 2: '支付宝', 3: '会员卡', 4: '其它' }
const DINING: Record<number, string> = { 0: '堂食', 1: '外带', 2: '外卖' }

function fmt(t?: number) {
  if (!t) return '-'
  const d = new Date(t)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function parseAddons(s?: string): { name: string; price: number }[] {
  if (!s) return []
  try {
    const v = JSON.parse(s)
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

export function OrderDetailDialog({
  orderId,
  open,
  onOpenChange,
}: {
  orderId: string | null
  open: boolean
  onOpenChange: (o: boolean) => void
}) {
  const { data, isLoading } = useQuery({
    queryKey: ['order-detail', orderId],
    enabled: open && !!orderId,
    queryFn: async () => (await api.get<OrderDTO>(`/api/admin/orders/${orderId}`)).data,
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className='max-w-2xl'>
        <DialogHeader>
          <DialogTitle>
            订单详情 {data?.pickupNumber && <span className='text-primary ms-2'>#{data.pickupNumber}</span>}
          </DialogTitle>
          <DialogDescription className='font-mono text-xs'>{orderId}</DialogDescription>
        </DialogHeader>

        {isLoading || !data ? (
          <div className='text-muted-foreground py-12 text-center text-sm'>加载中…</div>
        ) : (
          <div className='space-y-4'>
            <div className='grid grid-cols-2 gap-3 text-sm sm:grid-cols-3'>
              <Field label='订单状态'>
                <Badge>{ORDER_STATUS[data.orderStatus] ?? data.orderStatus}</Badge>
              </Field>
              <Field label='支付状态'>
                <Badge variant={data.payStatus === 1 ? 'default' : 'secondary'}>
                  {PAY_STATUS[data.payStatus] ?? data.payStatus}
                </Badge>
              </Field>
              <Field label='支付方式'>{PAY_TYPE[data.payType ?? -1] ?? '—'}</Field>
              <Field label='就餐方式'>{DINING[data.diningType ?? 0] ?? '-'}</Field>
              <Field label='桌号'>{data.tableNumber || '—'}</Field>
              <Field label='下单时间'>{fmt(data.createTime)}</Field>
              <Field label='顾客'>{data.buyerName || '—'}</Field>
              <Field label='联系方式'>{data.buyerPhone || '—'}</Field>
              <Field label='总金额'>
                <span className='text-primary font-semibold tabular-nums'>¥ {data.orderAmount}</span>
              </Field>
            </div>

            {data.orderRemark && (
              <div className='bg-muted/50 rounded-md p-3 text-sm'>
                <span className='text-muted-foreground me-2 text-xs font-medium'>备注</span>
                {data.orderRemark}
              </div>
            )}

            <div>
              <h4 className='mb-2 text-sm font-medium'>商品明细</h4>
              <div className='border-input divide-y rounded-md border'>
                {(data.orderDetailList ?? []).map((d) => {
                  const ad = parseAddons(d.addons)
                  return (
                    <div key={d.detailId} className='flex items-start gap-3 p-3'>
                      <div className='bg-muted size-12 shrink-0 overflow-hidden rounded'>
                        {d.productIcon && (
                          <img
                            src={d.productIcon}
                            alt=''
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            onError={(e) => {
                              ;(e.currentTarget as HTMLImageElement).style.display = 'none'
                            }}
                          />
                        )}
                      </div>
                      <div className='min-w-0 flex-1'>
                        <div className='flex items-baseline justify-between gap-3'>
                          <div className='font-medium'>{d.productName}</div>
                          <div className='text-muted-foreground text-xs tabular-nums'>×{d.productQuantity}</div>
                        </div>
                        {d.skuName && (
                          <div className='text-muted-foreground text-xs'>{d.skuName}</div>
                        )}
                        {ad.length > 0 && (
                          <div className='text-muted-foreground text-xs'>
                            加料: {ad.map((a) => `${a.name}+¥${a.price}`).join(' / ')}
                          </div>
                        )}
                      </div>
                      <div className='text-right text-sm tabular-nums'>
                        <div>¥ {d.productPrice}</div>
                        {d.addonFee && d.addonFee > 0 ? (
                          <div className='text-muted-foreground text-xs'>+¥ {d.addonFee}</div>
                        ) : null}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className='flex flex-col gap-1'>
      <span className='text-muted-foreground text-xs'>{label}</span>
      <span>{children}</span>
    </div>
  )
}
