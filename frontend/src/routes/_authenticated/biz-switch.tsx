import { createFileRoute } from '@tanstack/react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { PageShell } from '@/components/page-shell'
import { Card, CardContent } from '@/components/ui/card'
import { Switch } from '@/components/ui/switch'

const SWITCHES = [
  { key: 'switch.qrOrder', label: '扫码点餐', desc: '关闭后顾客无法通过扫码下单' },
  { key: 'switch.autoAcceptOrder', label: '自动接单', desc: '新订单自动转为制作中' },
  { key: 'switch.memberRegister', label: '会员注册', desc: '允许顾客注册会员' },
  { key: 'switch.payBeforeServe', label: '先付后食', desc: '关闭则先食后付' },
]

function BizSwitch() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['shop-config'],
    queryFn: async () => (await api.get<Record<string, string>>('/api/admin/shop-config')).data,
  })

  const setSwitch = async (key: string, enabled: boolean) => {
    // 后端会按 SWITCH_KEYS 依次读取, 缺失的字段默认 false
    // 所以必须把全部开关的当前值一起提交, 只覆盖被点击的那一个
    const body = new URLSearchParams()
    SWITCHES.forEach((s) => {
      const v = s.key === key ? enabled : data?.[s.key] === 'true'
      body.append(s.key, String(v))
    })
    try {
      await api.post('/seller/setting/switch/save', body.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      qc.invalidateQueries({ queryKey: ['shop-config'] })
    } catch (e) {
      console.error(e)
      alert('保存失败')
    }
  }

  return (
    <PageShell pretitle='设置 / 店铺' title='业务开关'>
      <Card>
        <CardContent className='divide-y p-0'>
          {isLoading && <div className='text-muted-foreground p-12 text-center'>加载中…</div>}
          {!isLoading && SWITCHES.map((s) => {
            const enabled = data?.[s.key] === 'true'
            return (
              <div key={s.key} className='flex items-center justify-between p-4'>
                <div>
                  <div className='font-medium'>{s.label}</div>
                  <div className='text-muted-foreground text-xs'>{s.desc}</div>
                </div>
                <Switch checked={enabled} onCheckedChange={(v) => setSwitch(s.key, v)} />
              </div>
            )
          })}
        </CardContent>
      </Card>
    </PageShell>
  )
}

export const Route = createFileRoute('/_authenticated/biz-switch')({ component: BizSwitch })
