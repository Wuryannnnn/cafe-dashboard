import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

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
  onChanged,
}: {
  orderId: string | null
  open: boolean
  onOpenChange: (o: boolean) => void
  /** 订单状态被操作改变后回调, 供父列表刷新. */
  onChanged?: () => void
}) {
  const qc = useQueryClient()
  const [busy, setBusy] = useState(false)
  const [edit, setEdit] = useState<null | 'amount' | 'discount'>(null)
  const [editVal, setEditVal] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['order-detail', orderId],
    enabled: open && !!orderId,
    queryFn: async () => (await api.get<OrderDTO>(`/api/admin/orders/${orderId}`)).data,
  })

  // 调用订单操作接口; 后端统一返回 {code,msg}, code!=0 视为业务失败.
  const run = async (path: string, okMsg: string, params?: Record<string, string | number>) => {
    if (!orderId || busy) return
    setBusy(true)
    try {
      const body = new URLSearchParams()
      if (params) for (const k in params) body.append(k, String(params[k]))
      const res = await api.post<{ code: number; msg: string }>(
        `/api/admin/orders/${orderId}/${path}`,
        body.toString(),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      )
      if (res.data?.code === 0) {
        toast.success(res.data.msg || okMsg)
        setEdit(null)
        setEditVal('')
        qc.invalidateQueries({ queryKey: ['order-detail', orderId] })
        qc.invalidateQueries({ queryKey: ['pending'] })
        qc.invalidateQueries({ queryKey: ['orders'] })
        onChanged?.()
      } else {
        toast.error(res.data?.msg || '操作失败')
      }
    } catch {
      toast.error('网络错误, 操作失败')
    } finally {
      setBusy(false)
    }
  }

  const confirmRun = (msg: string, path: string, okMsg: string) => {
    if (window.confirm(msg)) run(path, okMsg)
  }

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

            {/* 订单操作 (PRD 6.3 / 7.3): 按状态显示制作流转 / 取消 / 退款 / 改价 / 打折 / 免单 */}
            <div className='space-y-2 border-t pt-3'>
              <div className='flex flex-wrap gap-2'>
                {data.orderStatus === 0 && (
                  <Button size='sm' disabled={busy} onClick={() => run('making', '已接单')}>
                    开始制作
                  </Button>
                )}
                {data.orderStatus === 1 && (
                  <Button size='sm' disabled={busy} onClick={() => run('ready', '待取餐')}>
                    制作完成
                  </Button>
                )}
                {data.orderStatus <= 2 && (
                  <Button size='sm' variant='outline' disabled={busy} onClick={() => run('finish', '已完结')}>
                    完结
                  </Button>
                )}
                {data.orderStatus <= 2 && (
                  <Button
                    size='sm'
                    variant='outline'
                    disabled={busy}
                    onClick={() => confirmRun('确认取消该订单? 已支付将原路退款。', 'cancel', '已取消')}
                  >
                    取消订单
                  </Button>
                )}
                {data.payStatus === 1 && data.orderStatus !== 5 && (
                  <Button
                    size='sm'
                    variant='destructive'
                    disabled={busy}
                    onClick={() => confirmRun('确认对该已支付订单退款?', 'refund', '退款成功')}
                  >
                    退款
                  </Button>
                )}
                {data.payStatus !== 1 && data.orderStatus <= 2 && (
                  <>
                    <Button
                      size='sm'
                      variant='outline'
                      disabled={busy}
                      onClick={() => {
                        setEdit(edit === 'amount' ? null : 'amount')
                        setEditVal('')
                      }}
                    >
                      改价
                    </Button>
                    <Button
                      size='sm'
                      variant='outline'
                      disabled={busy}
                      onClick={() => {
                        setEdit(edit === 'discount' ? null : 'discount')
                        setEditVal('')
                      }}
                    >
                      打折
                    </Button>
                    <Button
                      size='sm'
                      variant='outline'
                      disabled={busy}
                      onClick={() => confirmRun('确认整单免单(金额改为 0)?', 'free', '已免单')}
                    >
                      免单
                    </Button>
                  </>
                )}
              </div>

              {edit === 'amount' && (
                <div className='flex items-center gap-2'>
                  <Input
                    type='number'
                    min='0'
                    step='0.01'
                    placeholder='新金额'
                    value={editVal}
                    onChange={(e) => setEditVal(e.target.value)}
                    className='h-8 w-32'
                  />
                  <Button size='sm' disabled={busy || !editVal} onClick={() => run('amount', '改价成功', { newAmount: editVal })}>
                    确认改价
                  </Button>
                </div>
              )}
              {edit === 'discount' && (
                <div className='flex items-center gap-2'>
                  <Input
                    type='number'
                    min='1'
                    max='100'
                    step='1'
                    placeholder='折扣 1-100, 如 85 = 85折'
                    value={editVal}
                    onChange={(e) => setEditVal(e.target.value)}
                    className='h-8 w-52'
                  />
                  <Button size='sm' disabled={busy || !editVal} onClick={() => run('discount', '打折成功', { discountRate: editVal })}>
                    确认打折
                  </Button>
                </div>
              )}
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
