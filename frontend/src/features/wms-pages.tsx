import { useQuery } from '@tanstack/react-query'
import { ExternalLink, CheckCircle2, XCircle } from 'lucide-react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { PageShell } from '@/components/page-shell'
import { SimpleTable } from '@/components/simple-table'
import { Badge } from '@/components/ui/badge'

export function WmsEntry() {
  const { data: alerts } = useQuery({
    queryKey: ['wms-alerts'],
    queryFn: async () => (await api.get<any>('/seller/wms/widget/alerts')).data,
  })
  const { data: overview } = useQuery({
    queryKey: ['wms-overview'],
    queryFn: async () => (await api.get<any>('/seller/wms/widget/overview')).data,
  })
  const { data: shipments } = useQuery({
    queryKey: ['wms-recent-12'],
    queryFn: async () => (await api.get<any>('/seller/wms/widget/recent-shipments', { params: { limit: 12 } })).data,
  })

  const cfg = overview?.configured ?? false

  return (
    <PageShell pretitle='库存' title='WMS 仓库系统'>
      <Card className='mb-4'>
        <CardContent className='flex flex-wrap items-center justify-between gap-4 p-5'>
          <div>
            <p className='text-muted-foreground text-xs font-medium uppercase tracking-wide'>状态</p>
            <p className='mt-1 flex items-center gap-2 text-lg font-semibold'>
              {cfg ? (
                <>
                  <CheckCircle2 className='size-5 text-emerald-600' /> 已连接
                </>
              ) : (
                <>
                  <XCircle className='size-5 text-rose-600' /> 未配置
                </>
              )}
            </p>
            {cfg && (
              <p className='text-muted-foreground mt-1 text-sm'>
                物料 <strong className='text-foreground'>{overview?.itemCount ?? 0}</strong> 项 · 告警{' '}
                <strong className='text-rose-600'>{overview?.alertCount ?? 0}</strong> 项
              </p>
            )}
          </div>
          <div className='flex gap-2'>
            <a href='http://localhost' target='_blank' rel='noreferrer'>
              <Button>
                <ExternalLink className='size-4' /> 打开 WMS
              </Button>
            </a>
            <a href='/sell/seller/wms/settings' target='_blank' rel='noreferrer'>
              <Button variant='outline'>连接设置</Button>
            </a>
          </div>
        </CardContent>
      </Card>

      <div className='grid gap-4 lg:grid-cols-2'>
        <Card>
          <CardContent className='p-5'>
            <h3 className='mb-3 text-sm font-semibold'>库存预警</h3>
            {!cfg ? (
              <p className='text-muted-foreground text-sm'>
                <a href='/sell/seller/wms/settings' target='_blank' rel='noreferrer' className='text-primary'>
                  去配置 WMS →
                </a>
              </p>
            ) : !alerts?.alerts || alerts.alerts.length === 0 ? (
              <p className='text-emerald-600 text-sm'>✓ 全部物料库存充足</p>
            ) : (
              <ul className='space-y-1 text-sm'>
                {alerts.alerts.map((a: any) => (
                  <li key={a.id} className='flex justify-between border-b py-2 last:border-0'>
                    <span>{a.itemName ?? a.skuName ?? `物料 #${a.id}`}</span>
                    <span className='text-rose-600 font-semibold tabular-nums'>剩 {a.quantity}</span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent className='p-5'>
            <h3 className='mb-3 text-sm font-semibold'>最近出库</h3>
            {!cfg ? (
              <p className='text-muted-foreground text-sm'>未连接</p>
            ) : !shipments?.items || shipments.items.length === 0 ? (
              <p className='text-muted-foreground text-sm'>暂无出库记录</p>
            ) : (
              <ul className='space-y-1 text-xs'>
                {shipments.items.map((s: any, i: number) => (
                  <li key={i} className='flex items-center gap-3 border-b py-1.5 last:border-0'>
                    <span className='text-muted-foreground tabular-nums'>{(s.createTime ?? '').toString().substring(5, 16)}</span>
                    <code className='bg-muted text-foreground rounded px-1.5 py-0.5'>{(s.bizOrderNo ?? '-').substring(0, 14)}</code>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}

export function MaterialCost() {
  const { data: shipments, isLoading } = useQuery({
    queryKey: ['wms-recent-50'],
    queryFn: async () => (await api.get<any>('/seller/wms/widget/recent-shipments', { params: { limit: 50 } })).data,
  })

  return (
    <PageShell pretitle='库存 / 报表' title='物料消耗报表'>
      <p className='text-muted-foreground mb-4 text-sm'>
        数据来自 WMS 出库单（按订单触发的销售出库）。含咖啡店订单消耗 + WMS 自建出库。
      </p>
      {!shipments?.configured ? (
        <Card>
          <CardContent className='text-muted-foreground p-12 text-center'>
            未配置 WMS,{' '}
            <a href='/sell/seller/wms/settings' target='_blank' rel='noreferrer' className='text-primary'>
              去配置 →
            </a>
          </CardContent>
        </Card>
      ) : (
        <SimpleTable
          loading={isLoading}
          rows={shipments?.items ?? []}
          rowKey={(r: any, i?: number) => r.shipmentId ?? r.bizOrderNo ?? i ?? Math.random()}
          columns={[
            { header: '出库时间', render: (r: any) => <span className='tabular-nums text-xs'>{(r.createTime ?? '').toString().substring(0, 16)}</span> },
            { header: '关联业务单号', render: (r: any) => <code className='bg-muted text-foreground rounded px-1.5 py-0.5 text-xs'>{r.bizOrderNo ?? '—'}</code> },
            { header: '仓库', render: (r: any) => r.warehouseId ?? '—' },
            { header: '类型', render: (r: any) => <Badge variant='secondary'>{r.optType ?? '—'}</Badge> },
            { header: '金额', align: 'right', render: (r: any) => <span className='tabular-nums'>{r.totalAmount ?? '—'}</span> },
            { header: '操作员', render: (r: any) => r.createBy ?? '—' },
          ]}
        />
      )}
    </PageShell>
  )
}
