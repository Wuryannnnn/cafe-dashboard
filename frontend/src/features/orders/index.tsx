import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ConfigDrawer } from '@/components/config-drawer'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { OrderDetailDialog } from '@/components/order-detail-dialog'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'

type OrderDTO = {
  orderId: string
  buyerName: string
  buyerPhone: string
  orderAmount: number
  pickupNumber?: string
  tableNumber?: string
  orderRemark?: string
  createTime: string
  orderStatus: number
  payStatus: number
  diningType?: number
}

type OrdersPage = {
  content: OrderDTO[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

const STATUS_LABELS: Record<number, string> = {
  0: '新订单',
  1: '制作中',
  2: '待取餐',
  3: '完结',
  4: '已取消',
  5: '已退款',
}

const STATUS_VARIANT: Record<number, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  0: 'secondary',
  1: 'default',
  2: 'outline',
  3: 'secondary',
  4: 'destructive',
  5: 'destructive',
}

export function Orders() {
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState<number | null>(null)
  const [keyword, setKeyword] = useState('')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [detailOrderId, setDetailOrderId] = useState<string | null>(null)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['orders', page, statusFilter, appliedKeyword],
    queryFn: async () =>
      (await api.get<OrdersPage>('/api/admin/orders', {
        params: {
          page,
          size: 20,
          ...(statusFilter != null ? { status: statusFilter } : {}),
          ...(appliedKeyword ? { keyword: appliedKeyword } : {}),
        },
      })).data,
  })

  const pickStatus = (s: number | null) => {
    setStatusFilter(s)
    setPage(1)
  }

  const act = async (path: string) => {
    try {
      await api.get(path)
      qc.invalidateQueries({ queryKey: ['orders'] })
    } catch (e) {
      console.error(e)
      alert('操作失败')
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
            <p className='text-muted-foreground text-xs'>运营</p>
            <h1 className='text-2xl font-bold tracking-tight'>
              订单 <span className='text-muted-foreground ms-2 text-base font-normal'>共 {data?.totalElements ?? 0} 单</span>
            </h1>
          </div>
        </div>

        <div className='mb-4 flex flex-wrap items-center gap-2'>
          <div className='flex flex-wrap gap-1'>
            <Chip active={statusFilter == null} onClick={() => pickStatus(null)}>全部</Chip>
            {[0, 1, 2, 3, 4, 5].map((s) => (
              <Chip key={s} active={statusFilter === s} onClick={() => pickStatus(s)}>
                {STATUS_LABELS[s]}
              </Chip>
            ))}
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              setAppliedKeyword(keyword.trim())
              setPage(1)
            }}
            className='ms-auto flex items-center gap-2'
          >
            <input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder='订单号 / 取餐号 / 桌号 / 手机号'
              className='border-input bg-background w-56 rounded-md border px-3 py-1.5 text-sm'
            />
            <Button type='submit' variant='outline' size='sm'>搜索</Button>
            {appliedKeyword && (
              <Button
                type='button'
                variant='ghost'
                size='sm'
                onClick={() => {
                  setKeyword('')
                  setAppliedKeyword('')
                  setPage(1)
                }}
              >
                清除
              </Button>
            )}
          </form>
        </div>

        <Card>
          <CardContent className='p-0'>
            <div className='overflow-x-auto'>
              <table className='w-full text-sm'>
                <thead>
                  <tr className='text-muted-foreground border-b text-xs uppercase tracking-wide'>
                    <th className='p-3 text-left font-medium'>取餐号</th>
                    <th className='p-3 text-left font-medium'>顾客</th>
                    <th className='p-3 text-left font-medium'>就餐 / 桌号</th>
                    <th className='p-3 text-right font-medium'>金额</th>
                    <th className='p-3 text-left font-medium'>状态</th>
                    <th className='p-3 text-left font-medium'>备注</th>
                    <th className='p-3 text-left font-medium'>创建时间</th>
                    <th className='p-3 text-right font-medium'>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading && (
                    <tr>
                      <td colSpan={8} className='text-muted-foreground p-12 text-center'>
                        加载中…
                      </td>
                    </tr>
                  )}
                  {!isLoading && (!data?.content || data.content.length === 0) && (
                    <tr>
                      <td colSpan={8} className='text-muted-foreground p-12 text-center'>
                        暂无订单
                      </td>
                    </tr>
                  )}
                  {data?.content?.map((o) => (
                    <tr key={o.orderId} className='hover:bg-muted/50 border-b last:border-0'>
                      <td className='p-3 font-semibold'>#{o.pickupNumber ?? '-'}</td>
                      <td className='p-3'>
                        <div>{o.buyerName}</div>
                        <div className='text-muted-foreground text-xs'>{o.buyerPhone}</div>
                      </td>
                      <td className='p-3'>
                        {o.diningType === 1 ? '外带' : '堂食'}
                        {o.tableNumber ? ` · ${o.tableNumber}` : ''}
                      </td>
                      <td className='p-3 text-right font-semibold tabular-nums'>¥ {o.orderAmount}</td>
                      <td className='p-3'>
                        <Badge variant={STATUS_VARIANT[o.orderStatus] ?? 'secondary'}>
                          {STATUS_LABELS[o.orderStatus] ?? o.orderStatus}
                        </Badge>
                      </td>
                      <td className='p-3 text-muted-foreground max-w-[180px] truncate'>
                        {o.orderRemark || '—'}
                      </td>
                      <td className='p-3 text-muted-foreground text-xs'>{o.createTime}</td>
                      <td className='p-3 text-right whitespace-nowrap'>
                        <button
                          onClick={() => setDetailOrderId(o.orderId)}
                          className='text-primary me-3 hover:underline'
                        >
                          详情
                        </button>
                        {o.orderStatus === 0 && (
                          <button
                            onClick={() => act(`/seller/order/making?orderId=${o.orderId}`)}
                            className='text-primary me-3 hover:underline'
                          >
                            开始制作
                          </button>
                        )}
                        {o.orderStatus === 1 && (
                          <button
                            onClick={() => act(`/seller/order/ready?orderId=${o.orderId}`)}
                            className='text-primary me-3 hover:underline'
                          >
                            出餐
                          </button>
                        )}
                        {o.orderStatus === 2 && (
                          <button
                            onClick={() => act(`/seller/order/finish?orderId=${o.orderId}`)}
                            className='text-primary me-3 hover:underline'
                          >
                            完结
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {data && data.totalPages > 1 && (
          <div className='mt-4 flex items-center justify-end gap-2 text-sm'>
            <span className='text-muted-foreground me-2'>
              第 {data.page} / {data.totalPages} 页
            </span>
            <Button
              variant='outline'
              size='sm'
              disabled={page <= 1}
              onClick={() => setPage(page - 1)}
            >
              上一页
            </Button>
            <Button
              variant='outline'
              size='sm'
              disabled={page >= data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              下一页
            </Button>
          </div>
        )}
      </Main>

      <OrderDetailDialog
        orderId={detailOrderId}
        open={!!detailOrderId}
        onOpenChange={(o) => { if (!o) setDetailOrderId(null) }}
      />
    </>
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
