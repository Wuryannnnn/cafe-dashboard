/**
 * 简单 list 页面集合（会员/等级/优惠券/营销/常用备注/员工/日志/打印机/结算账户 等）
 * 全部读自 /api/admin/*; 增删改通过 CrudDialog + 老 controller 的 /save /delete (返回 200 即成功).
 */
import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ConfirmDeleteButton } from '@/components/confirm-delete-button'
import { CrudDialog, type FieldDef } from '@/components/crud-dialog'
import { PageShell } from '@/components/page-shell'
import { SimpleTable } from '@/components/simple-table'

const fmt = (ts: any) =>
  !ts ? '—' : typeof ts === 'string' ? ts.substring(0, 16) : new Date(ts).toLocaleString('zh-CN')

function CreateBtn(props: { title: string; postUrl: string; fields: FieldDef[]; onSaved: () => void }) {
  return (
    <CrudDialog
      title={`新建${props.title}`}
      postUrl={props.postUrl}
      fields={props.fields}
      onSaved={props.onSaved}
      trigger={
        <Button>
          <Plus className='size-4' /> 新建
        </Button>
      }
    />
  )
}

function EditDel({
  title,
  postUrl,
  deleteUrl,
  fields,
  initial,
  onSaved,
}: {
  title: string
  postUrl: string
  deleteUrl?: string
  fields: FieldDef[]
  initial: any
  onSaved: () => void
}) {
  return (
    <span className='inline-flex items-center gap-1'>
      <CrudDialog
        title={`编辑${title}`}
        postUrl={postUrl}
        fields={fields}
        initial={initial}
        onSaved={onSaved}
        trigger={
          <button className='text-primary text-xs hover:underline'>
            <Pencil className='inline size-3.5' /> 编辑
          </button>
        }
      />
      {deleteUrl && (
        <span className='ms-3'>
          <ConfirmDeleteButton
            url={deleteUrl}
            onDone={onSaved}
            description={`确定要删除该${title}吗? 此操作不可恢复`}
          />
        </span>
      )}
    </span>
  )
}

export function MembersPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['members'] })
  const { data, isLoading } = useQuery({
    queryKey: ['members'],
    queryFn: async () => (await api.get<any[]>('/api/admin/members')).data,
  })
  const { data: levels } = useQuery({
    queryKey: ['member-levels'],
    queryFn: async () => (await api.get<any[]>('/api/admin/member-levels')).data,
  })
  const lvName = (id: number) => levels?.find((l) => l.levelId === id)?.levelName ?? '-'

  const memberFields: FieldDef[] = [
    { key: 'memberId', label: 'ID', hidden: true },
    { key: 'phone', label: '手机号', required: true },
    { key: 'nickname', label: '昵称' },
    { key: 'levelId', label: '会员等级', type: 'select',
      options: (levels ?? []).map((l) => ({ label: l.levelName, value: l.levelId })) },
    { key: 'channel', label: '渠道', placeholder: '门店 / 线上 / 推荐' },
    { key: 'tags', label: '标签 (逗号分隔)' },
    { key: 'remark', label: '备注', type: 'textarea' },
  ]

  return (
    <PageShell
      pretitle='营销 / 会员'
      title={<>会员 <span className='text-muted-foreground ms-2 text-base font-normal'>共 {data?.length ?? 0} 人</span></>}
      actions={<CreateBtn title='会员' postUrl='/seller/member/save' fields={memberFields} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.memberId}
        columns={[
          { header: '手机号', render: (r) => r.phone },
          { header: '昵称', render: (r) => r.nickname || '—' },
          { header: '等级', render: (r) => <Badge variant='secondary'>{lvName(r.levelId)}</Badge> },
          { header: '余额', align: 'right', render: (r) => <span className='tabular-nums'>¥ {r.balance ?? 0}</span> },
          { header: '积分', align: 'right', render: (r) => <span className='tabular-nums'>{r.points ?? 0}</span> },
          { header: '累计消费', align: 'right', render: (r) => <span className='tabular-nums'>¥ {r.totalSpend ?? 0}</span> },
          { header: '消费次数', align: 'right', render: (r) => <span className='tabular-nums'>{r.spendCount ?? 0}</span> },
          { header: '注册', render: (r) => <span className='text-muted-foreground text-xs'>{fmt(r.registerTime)}</span> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='会员' postUrl='/seller/member/save' deleteUrl={`/seller/member/delete?memberId=${r.memberId}`} fields={memberFields} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const LEVEL_FIELDS: FieldDef[] = [
  { key: 'levelId', label: 'ID', hidden: true },
  { key: 'levelName', label: '等级名', required: true, placeholder: '银卡 / 金卡 …' },
  { key: 'upgradeAmount', label: '升级累计消费额', type: 'number', required: true, step: '0.01' },
  { key: 'upgradeCount', label: '升级累计次数（可空）', type: 'number' },
  { key: 'discountRate', label: '折扣（100=无折扣）', type: 'number', required: true },
  { key: 'sortOrder', label: '排序', type: 'number' },
]

export function MemberLevelsPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['member-levels'] })
  const { data, isLoading } = useQuery({
    queryKey: ['member-levels'],
    queryFn: async () => (await api.get<any[]>('/api/admin/member-levels')).data,
  })
  return (
    <PageShell
      pretitle='营销 / 会员等级'
      title='会员等级'
      actions={<CreateBtn title='等级' postUrl='/seller/level/save' fields={LEVEL_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.levelId}
        columns={[
          { header: '等级', render: (r) => <span className='font-medium'>{r.levelName}</span> },
          { header: '升级金额', align: 'right', render: (r) => <span className='tabular-nums'>¥ {r.upgradeAmount}</span> },
          { header: '升级次数', align: 'right', render: (r) => <span className='tabular-nums'>{r.upgradeCount ?? '—'}</span> },
          { header: '折扣', align: 'right', render: (r) => <span className='tabular-nums'>{r.discountRate}%</span> },
          { header: '排序', align: 'right', render: (r) => r.sortOrder },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='等级' postUrl='/seller/level/save' deleteUrl={`/seller/level/delete?levelId=${r.levelId}`} fields={LEVEL_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const PLAN_FIELDS: FieldDef[] = [
  { key: 'planId', label: 'ID', hidden: true },
  { key: 'planName', label: '方案名', required: true },
  { key: 'payAmount', label: '充值金额', type: 'number', required: true, step: '0.01' },
  { key: 'giveAmount', label: '赠送金额', type: 'number', required: true, step: '0.01' },
  { key: 'sortOrder', label: '排序', type: 'number' },
  { key: 'enabled', label: '启用', type: 'switch' },
]

export function RechargePlansPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['recharge-plans'] })
  const { data, isLoading } = useQuery({
    queryKey: ['recharge-plans'],
    queryFn: async () => (await api.get<any[]>('/api/admin/recharge-plans')).data,
  })
  return (
    <PageShell
      pretitle='营销 / 充值方案'
      title='充值方案'
      actions={<CreateBtn title='方案' postUrl='/seller/rechargePlan/save' fields={PLAN_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.planId}
        columns={[
          { header: '方案', render: (r) => <span className='font-medium'>{r.planName}</span> },
          { header: '充值', align: 'right', render: (r) => <span className='tabular-nums'>¥ {r.payAmount}</span> },
          { header: '赠送', align: 'right', render: (r) => <span className='tabular-nums text-emerald-600'>+¥ {r.giveAmount}</span> },
          { header: '启用', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>是</Badge> : <Badge variant='destructive'>否</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='方案' postUrl='/seller/rechargePlan/save' deleteUrl={`/seller/rechargePlan/delete?planId=${r.planId}`} fields={PLAN_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const COUPON_FIELDS: FieldDef[] = [
  { key: 'couponId', label: 'ID', hidden: true },
  { key: 'couponName', label: '名称', required: true },
  { key: 'couponType', label: '类型', type: 'select', required: true, options: [{ label: '满减', value: 0 }, { label: '折扣 (%)', value: 1 }] },
  { key: 'faceValue', label: '面额 (满减¥/折扣%)', type: 'number', required: true, step: '0.01' },
  { key: 'minSpend', label: '使用门槛 (满 ¥)', type: 'number', step: '0.01' },
  { key: 'totalQuantity', label: '发放总量', type: 'number' },
  { key: 'enabled', label: '启用', type: 'switch' },
]

export function CouponsPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['coupons'] })
  const { data, isLoading } = useQuery({
    queryKey: ['coupons'],
    queryFn: async () => (await api.get<any[]>('/api/admin/coupons')).data,
  })
  return (
    <PageShell
      pretitle='营销 / 优惠券'
      title='优惠券'
      actions={<CreateBtn title='优惠券' postUrl='/seller/coupon/save' fields={COUPON_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.couponId}
        columns={[
          { header: '名称', render: (r) => <span className='font-medium'>{r.couponName}</span> },
          { header: '类型', render: (r) => r.couponType === 1 ? '折扣' : '满减' },
          { header: '面额', align: 'right', render: (r) => r.couponType === 1 ? `${r.faceValue}%` : `¥${r.faceValue}` },
          { header: '门槛', align: 'right', render: (r) => r.minSpend > 0 ? `满 ¥${r.minSpend}` : '无' },
          { header: '已发放', align: 'right', render: (r) => <span className='tabular-nums'>{r.issuedCount ?? 0}</span> },
          { header: '已核销', align: 'right', render: (r) => <span className='tabular-nums'>{r.usedCount ?? 0}</span> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='优惠券' postUrl='/seller/coupon/save' deleteUrl={`/seller/coupon/delete?couponId=${r.couponId}`} fields={COUPON_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const PROMO_FIELDS: FieldDef[] = [
  { key: 'promotionId', label: 'ID', hidden: true },
  { key: 'promotionName', label: '活动名', required: true },
  { key: 'promotionType', label: '类型 (1=满减,2=买赠,3=折扣)', type: 'number', required: true },
  { key: 'configJson', label: '配置 JSON', type: 'textarea', placeholder: '{"threshold":100,"discount":10}' },
  { key: 'sortOrder', label: '排序', type: 'number' },
  { key: 'enabled', label: '启用', type: 'switch' },
]

export function PromotionsPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['promotions'] })
  const { data, isLoading } = useQuery({
    queryKey: ['promotions'],
    queryFn: async () => (await api.get<any[]>('/api/admin/promotions')).data,
  })
  return (
    <PageShell
      pretitle='营销 / 活动'
      title='营销活动'
      actions={<CreateBtn title='活动' postUrl='/seller/promotion/save' fields={PROMO_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.promotionId}
        columns={[
          { header: '活动', render: (r) => <span className='font-medium'>{r.promotionName}</span> },
          { header: '类型', render: (r) => r.promotionType },
          { header: '启用', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>是</Badge> : <Badge variant='destructive'>否</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='活动' postUrl='/seller/promotion/save' deleteUrl={`/seller/promotion/delete?promotionId=${r.promotionId}`} fields={PROMO_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const REMARK_FIELDS: FieldDef[] = [
  { key: 'remarkId', label: 'ID', hidden: true },
  { key: 'text', label: '备注内容', required: true, placeholder: '少冰 / 多冰 / 去糖 …' },
  { key: 'sortOrder', label: '排序', type: 'number' },
]

export function RemarksPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['remarks'] })
  const { data, isLoading } = useQuery({
    queryKey: ['remarks'],
    queryFn: async () => (await api.get<any[]>('/api/admin/remarks')).data,
  })
  return (
    <PageShell
      pretitle='设置 / 常用备注'
      title='常用备注'
      actions={<CreateBtn title='备注' postUrl='/seller/setting/remark/save' fields={REMARK_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.remarkId}
        columns={[
          { header: '备注内容', render: (r) => r.text },
          { header: '排序', align: 'right', render: (r) => r.sortOrder },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='备注' postUrl='/seller/setting/remark/save' deleteUrl={`/seller/setting/remark/delete?remarkId=${r.remarkId}`} fields={REMARK_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const STAFF_FIELDS: FieldDef[] = [
  { key: 'staffId', label: 'ID', hidden: true },
  { key: 'username', label: '用户名', required: true },
  { key: 'password', label: '密码 (新增必填, 编辑留空不改)' },
  { key: 'name', label: '姓名', required: true },
  { key: 'phone', label: '手机' },
  { key: 'role', label: '角色', type: 'select', required: true, options: [
      { label: '老板', value: 0 }, { label: '店长', value: 1 }, { label: '收银员', value: 2 }, { label: '制作员', value: 3 }
  ]},
  { key: 'enabled', label: '启用', type: 'switch' },
]

export function StaffPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['staff'] })
  const { data, isLoading } = useQuery({
    queryKey: ['staff'],
    queryFn: async () => (await api.get<any[]>('/api/admin/staff')).data,
  })
  const role = (r: number) => ['老板', '店长', '收银员', '制作员'][r] ?? '员工'
  return (
    <PageShell
      pretitle='设置 / 员工'
      title='员工管理'
      actions={<CreateBtn title='员工' postUrl='/seller/staff/save' fields={STAFF_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.staffId}
        columns={[
          { header: '用户名', render: (r) => <span className='font-mono'>{r.username}</span> },
          { header: '姓名', render: (r) => <span className='font-medium'>{r.name}</span> },
          { header: '手机', render: (r) => r.phone },
          { header: '角色', render: (r) => <Badge variant='secondary'>{role(r.role)}</Badge> },
          { header: '状态', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>启用</Badge> : <Badge variant='destructive'>停用</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='员工' postUrl='/seller/staff/save' deleteUrl={`/seller/staff/delete?staffId=${r.staffId}`} fields={STAFF_FIELDS} initial={{ ...r, password: '' }} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

export function LogsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['logs'],
    queryFn: async () => (await api.get<any[]>('/api/admin/logs')).data,
  })
  return (
    <PageShell pretitle='设置 / 操作日志' title='操作日志'>
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.logId}
        columns={[
          { header: '时间', render: (r) => <span className='text-muted-foreground text-xs tabular-nums'>{fmt(r.createTime)}</span> },
          { header: '操作员', render: (r) => r.operatorName ?? r.operator ?? '—' },
          { header: '类型', render: (r) => <Badge variant='secondary'>{r.operationType}</Badge> },
          { header: '说明', render: (r) => <span className='text-sm'>{r.description ?? r.detail ?? ''}</span> },
        ]}
      />
    </PageShell>
  )
}

const PRINTER_FIELDS: FieldDef[] = [
  { key: 'printerId', label: 'ID', hidden: true },
  { key: 'printerName', label: '名称', required: true, placeholder: '吧台打印机' },
  { key: 'station', label: '工位', type: 'select', required: true, options: [
      { label: '吧台 (咖啡/饮品)', value: 0 }, { label: '后厨 (轻食/小吃)', value: 1 }
  ]},
  { key: 'ipAddress', label: 'IP 地址', required: true, placeholder: '192.168.1.100' },
  { key: 'port', label: '端口', type: 'number' },
]

export function PrintersPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['printers'] })
  const { data, isLoading } = useQuery({
    queryKey: ['printers'],
    queryFn: async () => (await api.get<any[]>('/api/admin/printers')).data,
  })
  return (
    <PageShell
      pretitle='设置 / 打印机'
      title='打印机配置'
      actions={<CreateBtn title='打印机' postUrl='/seller/printer/save' fields={PRINTER_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.printerId}
        columns={[
          { header: '名称', render: (r) => r.printerName },
          { header: '工位', render: (r) => r.station === 1 ? '后厨' : '吧台' },
          { header: 'IP', render: (r) => <span className='font-mono text-xs'>{r.ipAddress}</span> },
          { header: '端口', align: 'right', render: (r) => r.port },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='打印机' postUrl='/seller/printer/save' deleteUrl={`/seller/printer/delete?printerId=${r.printerId}`} fields={PRINTER_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const ACCOUNT_FIELDS: FieldDef[] = [
  { key: 'accountId', label: 'ID', hidden: true },
  { key: 'accountName', label: '账户名', required: true, placeholder: '主结算账户' },
  { key: 'accountType', label: '类型', type: 'select', required: true, options: [
      { label: '微信商户号', value: 'wechat' }, { label: '支付宝商户号', value: 'alipay' }, { label: '银行账户', value: 'bank' }
  ]},
  { key: 'accountNo', label: '账号' },
  { key: 'holderName', label: '持有人' },
  { key: 'remark', label: '备注', type: 'textarea' },
  { key: 'enabled', label: '启用', type: 'switch' },
]

export function SettleAccountsPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['settle-accounts'] })
  const { data, isLoading } = useQuery({
    queryKey: ['settle-accounts'],
    queryFn: async () => (await api.get<any[]>('/api/admin/settle-accounts')).data,
  })
  return (
    <PageShell
      pretitle='财务 / 结算'
      title='结算账户'
      actions={<CreateBtn title='账户' postUrl='/seller/finance/account/save' fields={ACCOUNT_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.accountId}
        columns={[
          { header: '账户名', render: (r) => <span className='font-medium'>{r.accountName}</span> },
          { header: '类型', render: (r) => r.accountType },
          { header: '账号', render: (r) => <span className='font-mono text-xs'>{r.accountNo ?? '—'}</span> },
          { header: '持有人', render: (r) => r.holderName ?? '—' },
          { header: '启用', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>是</Badge> : <Badge variant='destructive'>否</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='账户' postUrl='/seller/finance/account/save' deleteUrl={`/seller/finance/account/delete?accountId=${r.accountId}`} fields={ACCOUNT_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

export function ExpensesPage() {
  const qc = useQueryClient()
  const refetch = () => { qc.invalidateQueries({ queryKey: ['expenses'] }); qc.invalidateQueries({ queryKey: ['finance-overview'] }) }
  const { data: ov } = useQuery({
    queryKey: ['finance-overview'],
    queryFn: async () =>
      (await api.get<{
        monthOrderRevenue: number
        monthIncome: number
        monthExpense: number
        monthBalance: number
        monthNet: number
      }>('/api/admin/finance-overview')).data,
  })
  const { data: cats } = useQuery({
    queryKey: ['expense-categories'],
    queryFn: async () => (await api.get<any[]>('/api/admin/expense-categories')).data,
  })
  const { data, isLoading } = useQuery({
    queryKey: ['expenses'],
    queryFn: async () => (await api.get<any[]>('/api/admin/expenses')).data,
  })

  const recordFields: FieldDef[] = [
    { key: 'categoryId', label: '分类', type: 'select', required: true,
      options: (cats ?? []).map((c) => ({ label: `${c.categoryName} (${c.categoryType === 1 ? '收入' : '支出'})`, value: c.categoryId })) },
    { key: 'amount', label: '金额', type: 'number', required: true, step: '0.01' },
    { key: 'occurDate', label: '日期', placeholder: 'YYYY-MM-DD', required: true },
    { key: 'remark', label: '备注', type: 'textarea' },
  ]

  return (
    <PageShell
      pretitle='财务 / 收支'
      title='收支管理'
      actions={<CreateBtn title='收支记录' postUrl='/seller/finance/expense/recordSave' fields={recordFields} onSaved={refetch} />}
    >
      <div className='mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4'>
        <StatBox label='本月营业收入' value={`¥ ${ov?.monthOrderRevenue ?? 0}`} variant='income' />
        <StatBox label='本月其他收入' value={`¥ ${ov?.monthIncome ?? 0}`} variant='income' />
        <StatBox label='本月日常支出' value={`¥ ${ov?.monthExpense ?? 0}`} variant='expense' />
        <StatBox label='本月净利' value={`¥ ${ov?.monthNet ?? 0}`} variant='balance' />
      </div>
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.recordId}
        columns={[
          { header: '日期', render: (r) => <span className='tabular-nums text-xs'>{fmt(r.occurDate)}</span> },
          { header: '分类', render: (r) => <Badge variant='secondary'>{r.categoryName ?? '—'}</Badge> },
          { header: '类型', render: (r) => r.recordType === 1 ? <span className='text-emerald-600'>收入</span> : <span className='text-rose-600'>支出</span> },
          { header: '金额', align: 'right', render: (r) => <span className='tabular-nums font-semibold'>¥ {r.amount}</span> },
          { header: '备注', render: (r) => <span className='text-muted-foreground text-xs'>{r.remark ?? ''}</span> },
          { header: '操作员', render: (r) => r.operator ?? '—' },
          {
            header: '操作', align: 'right',
            render: (r) => (
              <ConfirmDeleteButton
                url={`/seller/finance/expense/recordDelete?recordId=${r.recordId}`}
                onDone={refetch}
                description='确定要删除此条收支记录吗?'
              />
            ),
          },
        ]}
      />
    </PageShell>
  )
}

const DAMAGE_FIELDS: FieldDef[] = [
  { key: 'itemName', label: '物品名', required: true, placeholder: '如: 美式咖啡 / 杯子' },
  { key: 'quantity', label: '数量', type: 'number', required: true },
  { key: 'amount', label: '估损金额', type: 'number', required: true, step: '0.01' },
  { key: 'occurDate', label: '日期', placeholder: 'YYYY-MM-DD', required: true },
  { key: 'reason', label: '原因', required: true, placeholder: '过期/打翻/操作失误 …' },
  { key: 'remark', label: '备注', type: 'textarea' },
]

export function DamagesPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['damages'] })
  const { data, isLoading } = useQuery({
    queryKey: ['damages'],
    queryFn: async () => (await api.get<any[]>('/api/admin/damages')).data,
  })
  return (
    <PageShell
      pretitle='财务 / 报损'
      title='报损管理'
      actions={<CreateBtn title='报损' postUrl='/seller/finance/damage/save' fields={DAMAGE_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.recordId}
        columns={[
          { header: '日期', render: (r) => <span className='tabular-nums text-xs'>{fmt(r.occurDate)}</span> },
          { header: '物品', render: (r) => <span className='font-medium'>{r.itemName}</span> },
          { header: '数量', align: 'right', render: (r) => <span className='tabular-nums'>{r.quantity}</span> },
          { header: '估损金额', align: 'right', render: (r) => <span className='tabular-nums text-rose-600'>¥ {r.amount ?? 0}</span> },
          { header: '原因', render: (r) => r.reason ?? '—' },
          { header: '操作员', render: (r) => r.operator ?? '—' },
          {
            header: '操作', align: 'right',
            render: (r) => (
              <ConfirmDeleteButton
                url={`/seller/finance/damage/delete?recordId=${r.recordId}`}
                onDone={refetch}
                description='确定要删除此条报损记录吗?'
              />
            ),
          },
        ]}
      />
    </PageShell>
  )
}

const AREA_FIELDS: FieldDef[] = [
  { key: 'areaId', label: 'ID', hidden: true },
  { key: 'areaName', label: '区域名', required: true, placeholder: '吧台区 / 大厅 / 户外 / VIP' },
  { key: 'sortOrder', label: '排序', type: 'number' },
]

export function AreasPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['areas'] })
  const { data, isLoading } = useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await api.get<any[]>('/api/admin/areas')).data,
  })
  return (
    <PageShell
      pretitle='桌台 / 区域'
      title='区域管理'
      actions={<CreateBtn title='区域' postUrl='/seller/area/save' fields={AREA_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.areaId}
        columns={[
          { header: '区域名', render: (r) => <span className='font-medium'>{r.areaName}</span> },
          { header: '排序', align: 'right', render: (r) => r.sortOrder },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='区域' postUrl='/seller/area/save' deleteUrl={`/seller/area/delete?areaId=${r.areaId}`} fields={AREA_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

export function TablesPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['tables'] })
  const { data: tables, isLoading } = useQuery({
    queryKey: ['tables'],
    queryFn: async () => (await api.get<any[]>('/api/admin/tables')).data,
  })
  const { data: areas } = useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await api.get<any[]>('/api/admin/areas')).data,
  })
  const areaName = (id: number) => areas?.find((a) => a.areaId === id)?.areaName ?? '—'

  const tableFields: FieldDef[] = [
    { key: 'tableId', label: 'ID', hidden: true },
    { key: 'areaId', label: '所属区域', type: 'select', required: true,
      options: (areas ?? []).map((a) => ({ label: a.areaName, value: a.areaId })) },
    { key: 'tableCode', label: '桌号编码', required: true, placeholder: 'B1 / A2 / O1' },
    { key: 'seatCount', label: '座位数', type: 'number' },
    { key: 'sortOrder', label: '排序', type: 'number' },
    { key: 'enabled', label: '启用', type: 'switch' },
  ]

  return (
    <PageShell
      pretitle='桌台 / 桌台'
      title={<>桌台 <span className='text-muted-foreground ms-2 text-base font-normal'>共 {tables?.length ?? 0} 桌</span></>}
      actions={<CreateBtn title='桌台' postUrl='/seller/table/save' fields={tableFields} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={tables}
        rowKey={(r) => r.tableId}
        columns={[
          { header: '编号', render: (r) => <span className='font-semibold'>{r.tableCode}</span> },
          { header: '所属区域', render: (r) => areaName(r.areaId) },
          { header: '座位数', align: 'right', render: (r) => r.seatCount ?? 0 },
          { header: '排序', align: 'right', render: (r) => r.sortOrder ?? 0 },
          { header: '启用', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>是</Badge> : <Badge variant='destructive'>否</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='桌台' postUrl='/seller/table/save' deleteUrl={`/seller/table/delete?tableId=${r.tableId}`} fields={tableFields} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const PAYMENT_FIELDS: FieldDef[] = [
  { key: 'methodId', label: 'ID', hidden: true },
  { key: 'methodName', label: '名称', required: true, placeholder: '微信 / 支付宝 / 现金 …' },
  { key: 'methodCode', label: '标识码', required: true, placeholder: 'wechat / alipay / cash' },
  { key: 'sortOrder', label: '排序', type: 'number' },
  { key: 'enabled', label: '启用', type: 'switch' },
  { key: 'isDefault', label: '设为默认', type: 'switch' },
]

export function PaymentMethodsPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['payment-methods'] })
  const { data, isLoading } = useQuery({
    queryKey: ['payment-methods'],
    queryFn: async () => (await api.get<any[]>('/api/admin/payment-methods')).data,
  })
  return (
    <PageShell
      pretitle='设置 / 收银'
      title='结账方式'
      actions={<CreateBtn title='结账方式' postUrl='/seller/payment/save' fields={PAYMENT_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.methodId}
        columns={[
          { header: '名称', render: (r) => <span className='font-medium'>{r.methodName}</span> },
          { header: '标识码', render: (r) => <code className='bg-muted text-foreground rounded px-1.5 py-0.5 text-xs'>{r.methodCode}</code> },
          { header: '排序', align: 'right', render: (r) => r.sortOrder },
          { header: '启用', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>是</Badge> : <Badge variant='destructive'>否</Badge> },
          { header: '默认', render: (r) => r.isDefault ? <Badge variant='default'>默认</Badge> : '—' },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='结账方式' postUrl='/seller/payment/save' deleteUrl={`/seller/payment/delete?methodId=${r.methodId}`} fields={PAYMENT_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const CATEGORY_FIELDS: FieldDef[] = [
  { key: 'categoryId', label: 'ID', hidden: true },
  { key: 'categoryName', label: '分类名', required: true },
  { key: 'categoryType', label: '类型码 (整数, 唯一)', type: 'number', required: true },
  { key: 'printStation', label: '出餐工位', type: 'select', required: true, options: [
      { label: '吧台 (咖啡/饮品)', value: 0 }, { label: '后厨 (轻食/小吃)', value: 1 }
  ]},
]

export function CategoriesPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['categories'] })
  const { data, isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get<any[]>('/api/admin/categories')).data,
  })
  const station = (s: number) => ['吧台 (咖啡/饮品)', '后厨 (轻食/小吃)'][s] ?? '其他'
  return (
    <PageShell
      pretitle='菜单 / 分类'
      title='分类管理'
      actions={<CreateBtn title='分类' postUrl='/seller/category/save' fields={CATEGORY_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.categoryId}
        columns={[
          { header: '分类名', render: (r) => <span className='font-medium'>{r.categoryName}</span> },
          { header: '类型码', align: 'right', render: (r) => <code className='bg-muted text-foreground rounded px-1.5 py-0.5 text-xs'>{r.categoryType}</code> },
          { header: '出餐工位', render: (r) => <Badge variant='secondary'>{station(r.printStation ?? 0)}</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='分类' postUrl='/seller/category/save' deleteUrl={`/seller/category/delete?categoryId=${r.categoryId}`} fields={CATEGORY_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

const ADDON_GROUP_FIELDS: FieldDef[] = [
  { key: 'groupId', label: 'ID', hidden: true },
  { key: 'groupName', label: '组名', required: true, placeholder: '糖度 / 加料 / 温度' },
  { key: 'required', label: '必选', type: 'switch' },
  { key: 'multipleChoice', label: '多选', type: 'switch' },
  { key: 'sortOrder', label: '排序', type: 'number' },
]

export function AddonsPage() {
  const qc = useQueryClient()
  const refetch = () => { qc.invalidateQueries({ queryKey: ['addon-groups'] }); qc.invalidateQueries({ queryKey: ['addons'] }) }
  const { data: groups, isLoading } = useQuery({
    queryKey: ['addon-groups'],
    queryFn: async () => (await api.get<any[]>('/api/admin/addon-groups')).data,
  })
  const { data: addons } = useQuery({
    queryKey: ['addons'],
    queryFn: async () => (await api.get<any[]>('/api/admin/addons')).data,
  })

  const addonFieldsFor = (_groupId: number): FieldDef[] => [
    { key: 'addonId', label: 'ID', hidden: true },
    { key: 'groupId', label: 'GroupId', hidden: true },
    { key: 'addonName', label: '属性值', required: true, placeholder: '加浓 / 换燕麦奶 …' },
    { key: 'addonPrice', label: '加价 (0 = 免费)', type: 'number', required: true, step: '0.01' },
  ]
  void groups
  void addons

  return (
    <PageShell
      pretitle='菜单 / 属性'
      title='菜品属性 / 加料'
      actions={<CreateBtn title='属性组' postUrl='/seller/addonGroup/save' fields={ADDON_GROUP_FIELDS} onSaved={refetch} />}
    >
      {isLoading && <p className='text-muted-foreground text-sm'>加载中…</p>}
      <div className='space-y-4'>
        {groups?.map((g) => {
          const items = addons?.filter((a) => a.groupId === g.groupId) ?? []
          return (
            <div key={g.groupId} className='rounded-lg border bg-card'>
              <div className='flex items-center justify-between border-b p-4'>
                <div>
                  <span className='font-semibold'>{g.groupName}</span>
                  <span className='text-muted-foreground ms-3 text-xs'>
                    {g.required ? '必选' : '可选'} · {g.multipleChoice ? '多选' : '单选'} · {items.length} 项
                  </span>
                </div>
                <div className='flex items-center gap-2 text-xs'>
                  <CrudDialog
                    title='编辑属性组'
                    postUrl='/seller/addonGroup/save'
                    fields={ADDON_GROUP_FIELDS}
                    initial={g}
                    onSaved={refetch}
                    trigger={<button className='text-primary hover:underline'><Pencil className='inline size-3.5' /> 编辑组</button>}
                  />
                  <ConfirmDeleteButton
                    url={`/seller/addonGroup/delete?groupId=${g.groupId}`}
                    onDone={refetch}
                    title={`删除属性组「${g.groupName}」?`}
                    description='其下的属性值也会一并删除, 此操作不可恢复'
                    className='text-rose-600 hover:underline'
                    trigger={<><Trash2 className='inline size-3.5' /> 删除组</>}
                  />
                </div>
              </div>
              <div className='flex flex-wrap items-center gap-2 p-4'>
                {items.length === 0 && (
                  <span className='text-muted-foreground text-sm'>暂无属性值</span>
                )}
                {items.map((a) => (
                  <span key={a.addonId} className='bg-muted text-foreground inline-flex items-center gap-1.5 rounded px-2 py-1 text-xs'>
                    {a.addonName}
                    {a.addonPrice > 0 && <span className='text-primary font-semibold'>+¥{a.addonPrice}</span>}
                    <ConfirmDeleteButton
                      url={`/seller/addonGroup/addonDelete?addonId=${a.addonId}`}
                      onDone={refetch}
                      title={`删除属性值「${a.addonName}」?`}
                      description='此操作不可恢复'
                      className='text-rose-600 ms-1 leading-none hover:underline'
                      trigger={<span className='text-base'>×</span>}
                    />
                  </span>
                ))}
                <CrudDialog
                  title={`新增属性值 - ${g.groupName}`}
                  postUrl='/seller/addonGroup/addonSave'
                  fields={addonFieldsFor(g.groupId)}
                  initial={{ groupId: g.groupId }}
                  onSaved={refetch}
                  trigger={<Button size='sm' variant='outline'>+ 添加</Button>}
                />
              </div>
            </div>
          )
        })}
        {!isLoading && groups && groups.length === 0 && (
          <p className='text-muted-foreground py-8 text-center text-sm'>暂无属性组，点右上角新建</p>
        )}
      </div>
    </PageShell>
  )
}

const TIMESLOT_FIELDS: FieldDef[] = [
  { key: 'slotId', label: 'ID', hidden: true },
  { key: 'slotName', label: '时段名', required: true, placeholder: '早餐 / 午市 / 晚市' },
  { key: 'startTime', label: '开始时间', required: true, placeholder: '08:00' },
  { key: 'endTime', label: '结束时间', required: true, placeholder: '11:00' },
  { key: 'productIds', label: '关联菜品 ID (逗号分隔)', placeholder: 'prod001,prod002' },
  { key: 'sortOrder', label: '排序', type: 'number' },
  { key: 'enabled', label: '启用', type: 'switch' },
]

export function TimeslotsPage() {
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['timeslots'] })
  const { data, isLoading } = useQuery({
    queryKey: ['timeslots'],
    queryFn: async () => (await api.get<any[]>('/api/admin/timeslots')).data,
  })
  return (
    <PageShell
      pretitle='菜单 / 时段'
      title='时段菜单'
      actions={<CreateBtn title='时段' postUrl='/seller/timeslot/save' fields={TIMESLOT_FIELDS} onSaved={refetch} />}
    >
      <SimpleTable
        loading={isLoading}
        rows={data}
        rowKey={(r) => r.slotId}
        columns={[
          { header: '时段名', render: (r) => <span className='font-medium'>{r.slotName}</span> },
          { header: '起止', render: (r) => <span className='font-mono text-xs'>{r.startTime} – {r.endTime}</span> },
          { header: '关联菜品数', align: 'right', render: (r) => (r.productIds ? r.productIds.split(',').filter(Boolean).length : 0) },
          { header: '启用', render: (r) => r.enabled ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>是</Badge> : <Badge variant='destructive'>否</Badge> },
          {
            header: '操作', align: 'right',
            render: (r) => <EditDel title='时段' postUrl='/seller/timeslot/save' deleteUrl={`/seller/timeslot/delete?slotId=${r.slotId}`} fields={TIMESLOT_FIELDS} initial={r} onSaved={refetch} />,
          },
        ]}
      />
    </PageShell>
  )
}

export function StockPage() {
  const { data: products } = useQuery({
    queryKey: ['products'],
    queryFn: async () => (await api.get<any[]>('/api/admin/products')).data,
  })
  const { data: records, isLoading } = useQuery({
    queryKey: ['stock-records'],
    queryFn: async () => (await api.get<any[]>('/api/admin/stock-records', { params: { limit: 50 } })).data,
  })
  const pname = (id: string) => products?.find((p) => p.productId === id)?.productName ?? id

  return (
    <PageShell pretitle='库存 / 商品' title='商品库存'>
      <div className='mb-4 grid gap-3 sm:grid-cols-3'>
        <StatBox label='SKU 总数' value={String(products?.length ?? 0)} variant='balance' />
        <StatBox label='总库存件数' value={String(products?.reduce((s, p) => s + (p.productStock ?? 0), 0) ?? 0)} variant='income' />
        <StatBox label='下架商品' value={String(products?.filter((p) => p.productStatus !== 0).length ?? 0)} variant='expense' />
      </div>
      <div className='space-y-4'>
        <div>
          <h2 className='mb-3 text-sm font-semibold'>当前库存</h2>
          <SimpleTable
            rows={products}
            rowKey={(r: any) => r.productId}
            columns={[
              { header: '商品', render: (r: any) => <span className='font-medium'>{r.productName}</span> },
              { header: '价格', align: 'right', render: (r: any) => `¥ ${r.productPrice}` },
              { header: '库存', align: 'right', render: (r: any) => <span className='tabular-nums font-semibold'>{r.productStock}</span> },
              { header: '状态', render: (r: any) => r.productStatus === 0 ? <Badge variant='secondary' className='bg-emerald-100 text-emerald-700'>在售</Badge> : <Badge variant='destructive'>停售</Badge> },
            ]}
          />
        </div>

        <div>
          <h2 className='mb-3 text-sm font-semibold'>近期变动 <span className='text-muted-foreground font-normal'>(最多 50 条)</span></h2>
          <SimpleTable
            loading={isLoading}
            rows={records}
            rowKey={(r: any) => r.recordId}
            columns={[
              { header: '时间', render: (r: any) => <span className='text-muted-foreground text-xs tabular-nums'>{fmt(r.createTime)}</span> },
              { header: '商品', render: (r: any) => pname(r.productId) },
              { header: '变化', align: 'right', render: (r: any) => (
                <span className={(r.changeQty ?? 0) >= 0 ? 'text-emerald-600 tabular-nums font-semibold' : 'text-rose-600 tabular-nums font-semibold'}>
                  {(r.changeQty ?? 0) >= 0 ? '+' : ''}{r.changeQty}
                </span>
              )},
              { header: '类型', render: (r: any) => <Badge variant='secondary'>{r.changeType ?? '—'}</Badge> },
              { header: '备注', render: (r: any) => <span className='text-muted-foreground text-xs'>{r.remark ?? ''}</span> },
            ]}
          />
        </div>
      </div>
    </PageShell>
  )
}

export function RecipesPage() {
  const qc = useQueryClient()
  const { data: products } = useQuery({
    queryKey: ['products'],
    queryFn: async () => (await api.get<any[]>('/api/admin/products')).data,
  })
  const { data: recipes, isLoading } = useQuery({
    queryKey: ['recipes'],
    queryFn: async () => (await api.get<any[]>('/api/admin/recipes')).data,
  })
  const { data: skus } = useQuery({
    queryKey: ['skus'],
    queryFn: async () => (await api.get<any[]>('/api/admin/skus')).data,
  })
  const { data: wmsItems } = useQuery({
    queryKey: ['wms-items'],
    queryFn: async () => (await api.get<any[]>('/api/admin/wms-items')).data,
  })

  const recipeRefetch = () => qc.invalidateQueries({ queryKey: ['recipes'] })

  return (
    <PageShell pretitle='库存 / 配方' title='菜品配方 BOM'>
      <p className='text-muted-foreground mb-4 text-sm'>
        每个菜品消耗的 WMS 物料。订单完成时自动按配方扣减原料。
        {(!wmsItems || wmsItems.length === 0) && (
          <span className='text-amber-600 ms-2'>· WMS 未连接，物料下拉为空</span>
        )}
      </p>
      <div className='space-y-3'>
        {products?.map((p) => {
          const items = recipes?.filter((r) => r.productId === p.productId) ?? []
          const productSkus = skus?.filter((s) => s.productId === p.productId) ?? []
          return (
            <div key={p.productId} className='rounded-lg border bg-card'>
              <div className='flex items-center justify-between border-b p-4'>
                <div>
                  <span className='font-semibold'>{p.productName}</span>
                  <span className='text-muted-foreground ms-2 font-mono text-xs'>{p.productId}</span>
                  <span className='text-muted-foreground ms-3 text-xs'>{items.length} 项配方 · {productSkus.length} 个规格</span>
                </div>
              </div>
              {items.length > 0 && (
                <table className='w-full text-sm'>
                  <thead>
                    <tr className='text-muted-foreground border-b text-xs uppercase'>
                      <th className='p-3 text-left font-medium'>WMS 物料</th>
                      <th className='p-3 text-left font-medium'>规格 (SKU)</th>
                      <th className='p-3 text-right font-medium'>消耗量</th>
                      <th className='p-3 text-left font-medium'>单位</th>
                      <th className='p-3 text-right font-medium'>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((r) => (
                      <tr key={r.recipeId} className='border-b last:border-0'>
                        <td className='p-3'>{r.wmsItemName ?? `物料 #${r.wmsItemId}`}</td>
                        <td className='p-3 text-muted-foreground'>{r.skuId ? (productSkus.find((s) => s.skuId === r.skuId)?.skuName ?? r.skuId) : '所有规格通用'}</td>
                        <td className='p-3 text-right tabular-nums font-semibold'>{r.quantity}</td>
                        <td className='p-3 text-muted-foreground'>{r.unit ?? '—'}</td>
                        <td className='p-3 text-right'>
                          <ConfirmDeleteButton
                            url={`/seller/wms/recipe/delete?recipeId=${r.recipeId}`}
                            onDone={recipeRefetch}
                            description='确定要从配方中移除该物料吗?'
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <RecipeAddForm
                productId={p.productId}
                skus={productSkus}
                wmsItems={wmsItems ?? []}
                onSaved={() => qc.invalidateQueries({ queryKey: ['recipes'] })}
              />
            </div>
          )
        })}
        {!isLoading && (!products || products.length === 0) && (
          <p className='text-muted-foreground py-8 text-center text-sm'>暂无商品</p>
        )}
      </div>
    </PageShell>
  )
}

function RecipeAddForm({
  productId,
  skus,
  wmsItems,
  onSaved,
}: {
  productId: string
  skus: any[]
  wmsItems: any[]
  onSaved: () => void
}) {
  const [wmsItemId, setWmsItemId] = useState<string>('')
  const [skuId, setSkuId] = useState<string>('')
  const [qty, setQty] = useState<string>('')
  const [unit, setUnit] = useState<string>('')
  const [saving, setSaving] = useState(false)

  const save = async () => {
    if (!wmsItemId || !qty) {
      alert('请选择物料并填写消耗量')
      return
    }
    setSaving(true)
    try {
      const item = wmsItems.find((i) => String(i.id) === wmsItemId)
      const fd = new FormData()
      fd.append('productId', productId)
      if (skuId) fd.append('skuId', skuId)
      fd.append('wmsItemId', wmsItemId)
      if (item) fd.append('wmsItemName', String(item.itemName ?? item.name ?? ''))
      fd.append('quantity', qty)
      fd.append('unit', unit || (item?.unit ?? ''))
      await api.post('/seller/wms/recipe/save', fd)
      setWmsItemId('')
      setSkuId('')
      setQty('')
      setUnit('')
      onSaved()
    } catch (e) {
      alert('保存失败')
    } finally {
      setSaving(false)
    }
  }

  const onPickItem = (id: string) => {
    setWmsItemId(id)
    const it = wmsItems.find((i) => String(i.id) === id)
    // 切物料时, 用物料自带单位覆盖
    setUnit(it ? String(it.unit ?? '') : '')
  }

  return (
    <div className='bg-muted/30 flex flex-wrap items-center gap-2 border-t p-3 text-sm'>
      <select
        value={wmsItemId}
        onChange={(e) => onPickItem(e.target.value)}
        className='border-input bg-background rounded-md border px-2.5 py-1.5 text-xs'
      >
        <option value=''>-- 选物料 --</option>
        {wmsItems.map((it) => (
          <option key={it.id} value={it.id}>
            {(it.itemName ?? it.name)} {it.unit ? `(${it.unit})` : ''}
          </option>
        ))}
      </select>
      <select
        value={skuId}
        onChange={(e) => setSkuId(e.target.value)}
        className='border-input bg-background rounded-md border px-2.5 py-1.5 text-xs'
        title='这条配方适用的菜品规格'
      >
        <option value=''>适用所有规格</option>
        {skus.map((s) => (
          <option key={s.skuId} value={s.skuId}>
            仅 {s.skuName}
          </option>
        ))}
      </select>
      <input
        type='number'
        step='0.01'
        placeholder='用量'
        value={qty}
        onChange={(e) => setQty(e.target.value)}
        className='border-input bg-background rounded-md border px-2.5 py-1.5 text-xs w-20'
      />
      <span className='text-muted-foreground text-xs min-w-[2rem]'>{unit || '—'}</span>
      <Button size='sm' onClick={save} disabled={saving}>
        {saving ? '保存中…' : '+ 添加配方'}
      </Button>
    </div>
  )
}

function StatBox({ label, value, variant }: { label: string; value: string; variant: 'income' | 'expense' | 'balance' }) {
  const accent =
    variant === 'income'
      ? 'border-l-emerald-500'
      : variant === 'expense'
        ? 'border-l-rose-500'
        : 'border-l-sky-500'
  return (
    <div className={'rounded-lg border bg-card border-l-4 p-4 ' + accent}>
      <p className='text-muted-foreground text-xs font-medium uppercase tracking-wide'>{label}</p>
      <p className='mt-1 text-2xl font-bold tabular-nums'>{value}</p>
    </div>
  )
}
