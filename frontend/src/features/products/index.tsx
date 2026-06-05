import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Copy, Pencil, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ConfigDrawer } from '@/components/config-drawer'
import { ConfirmDeleteButton } from '@/components/confirm-delete-button'
import { CrudDialog, type FieldDef } from '@/components/crud-dialog'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'

type Product = {
  productId: string
  productName: string
  productPrice: number
  productStock: number
  productDescription?: string
  productIcon?: string
  productStatus: number
  categoryType: number
  h5Display?: number
}

type Category = {
  categoryId: number
  categoryName: string
  categoryType: number
}

export function Products() {
  const [filter, setFilter] = useState<number | null>(null)
  const qc = useQueryClient()
  const refetch = () => qc.invalidateQueries({ queryKey: ['products'] })

  // 切换商品在顾客端(H5)是否展示
  const toggleH5 = async (p: Product) => {
    const show = (p.h5Display ?? 1) === 0 // 当前隐藏 → 改为展示
    try {
      const body = new URLSearchParams()
      body.append('show', String(show))
      const res = await api.post<{ code: number; msg: string }>(
        `/api/admin/products/${p.productId}/h5-display`,
        body.toString(),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      )
      if (res.data?.code === 0) {
        toast.success(res.data.msg)
        refetch()
      } else {
        toast.error(res.data?.msg || '操作失败')
      }
    } catch {
      toast.error('网络错误, 操作失败')
    }
  }

  // 复制商品(连规格), 生成默认停售的副本供改名调价后上架
  const copyProduct = async (p: Product) => {
    try {
      const res = await api.post<{ code: number; msg: string }>(
        `/api/admin/products/${p.productId}/copy`,
        ''
      )
      if (res.data?.code === 0) {
        toast.success(res.data.msg)
        refetch()
      } else {
        toast.error(res.data?.msg || '复制失败')
      }
    } catch {
      toast.error('网络错误, 复制失败')
    }
  }

  const { data: products, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: async () => (await api.get<Product[]>('/api/admin/products')).data,
  })
  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get<Category[]>('/api/admin/categories')).data,
  })

  const list = filter == null
    ? products
    : products?.filter((p) => p.categoryType === filter)

  const productFields: FieldDef[] = [
    { key: 'productId', label: 'ID', hidden: true },
    { key: 'productName', label: '商品名', required: true },
    { key: 'productPrice', label: '价格', type: 'number', required: true, step: '0.01' },
    { key: 'productStock', label: '库存', type: 'number', required: true },
    { key: 'productDescription', label: '描述', type: 'textarea' },
    { key: 'productIcon', label: '商品图片', type: 'image' },
    { key: 'categoryType', label: '分类', type: 'select', required: true,
      options: (categories ?? []).map((c) => ({ label: c.categoryName, value: c.categoryType })) },
    { key: 'productStatus', label: '状态', type: 'select',
      options: [{ label: '在售', value: 0 }, { label: '停售', value: 1 }] },
  ]

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
            <p className='text-muted-foreground text-xs'>菜单</p>
            <h1 className='text-2xl font-bold tracking-tight'>商品 <span className='text-muted-foreground ms-2 text-base font-normal'>共 {products?.length ?? 0} 件</span></h1>
          </div>
          <CrudDialog
            title='新增商品'
            postUrl='/seller/product/save'
            fields={productFields}
            onSaved={refetch}
            trigger={
              <Button>
                <Plus className='size-4' /> 新增商品
              </Button>
            }
          />
        </div>

        <div className='mb-4 flex flex-wrap gap-1'>
          <FilterChip active={filter == null} onClick={() => setFilter(null)}>
            全部
          </FilterChip>
          {categories?.map((c) => (
            <FilterChip
              key={c.categoryId}
              active={filter === c.categoryType}
              onClick={() => setFilter(c.categoryType)}
            >
              {c.categoryName}
            </FilterChip>
          ))}
        </div>

        <Card>
          <CardContent className='p-0'>
            <div className='overflow-x-auto'>
              <table className='w-full text-sm'>
                <thead>
                  <tr className='text-muted-foreground border-b text-xs uppercase tracking-wide'>
                    <th className='p-3 text-left font-medium'>商品</th>
                    <th className='p-3 text-left font-medium'>分类</th>
                    <th className='p-3 text-right font-medium'>价格</th>
                    <th className='p-3 text-right font-medium'>库存</th>
                    <th className='p-3 text-left font-medium'>状态</th>
                    <th className='p-3 text-right font-medium'>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading && (
                    <tr><td colSpan={6} className='text-muted-foreground p-12 text-center'>加载中…</td></tr>
                  )}
                  {!isLoading && (!list || list.length === 0) && (
                    <tr><td colSpan={6} className='text-muted-foreground p-12 text-center'>暂无商品</td></tr>
                  )}
                  {list?.map((p) => {
                    const cat = categories?.find((c) => c.categoryType === p.categoryType)
                    return (
                      <tr key={p.productId} className='hover:bg-muted/50 border-b last:border-0'>
                        <td className='p-3'>
                          <div className='flex items-center gap-3'>
                            <div
                              className='shrink-0 overflow-hidden rounded-lg bg-amber-100/40'
                              style={{ width: 44, height: 44 }}
                            >
                              {p.productIcon && (
                                <img
                                  src={p.productIcon}
                                  alt=''
                                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                                />
                              )}
                            </div>
                            <div className='min-w-0'>
                              <div className='font-medium'>{p.productName}</div>
                              <div className='text-muted-foreground truncate text-xs max-w-[280px]'>
                                {p.productDescription ?? ''}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className='p-3 text-muted-foreground'>{cat?.categoryName ?? '—'}</td>
                        <td className='p-3 text-right font-semibold tabular-nums'>¥ {p.productPrice}</td>
                        <td className='p-3 text-right tabular-nums'>{p.productStock}</td>
                        <td className='p-3'>
                          <span className='inline-flex items-center gap-1.5'>
                            {p.productStatus === 0 ? (
                              <Badge variant='secondary' className='bg-emerald-100 text-emerald-700 hover:bg-emerald-100'>
                                在售
                              </Badge>
                            ) : (
                              <Badge variant='destructive'>停售</Badge>
                            )}
                            <button onClick={() => toggleH5(p)} title='点击切换顾客端(H5)是否展示'>
                              <Badge
                                variant={(p.h5Display ?? 1) === 0 ? 'outline' : 'secondary'}
                                className='cursor-pointer text-xs'
                              >
                                {(p.h5Display ?? 1) === 0 ? 'H5隐藏' : 'H5展示'}
                              </Badge>
                            </button>
                          </span>
                        </td>
                        <td className='p-3 text-right whitespace-nowrap'>
                          <span className='inline-flex items-center gap-3'>
                            <CrudDialog
                              title={`编辑 ${p.productName}`}
                              postUrl='/seller/product/save'
                              fields={productFields}
                              initial={p}
                              onSaved={refetch}
                              trigger={
                                <button className='text-primary text-xs hover:underline'>
                                  <Pencil className='inline size-3.5' /> 编辑
                                </button>
                              }
                            />
                            <button
                              onClick={() => copyProduct(p)}
                              className='text-muted-foreground text-xs hover:underline'
                              title='复制为新商品(默认停售)'
                            >
                              <Copy className='inline size-3.5' /> 复制
                            </button>
                            <ConfirmDeleteButton
                              url={
                                p.productStatus === 0
                                  ? `/seller/product/off_sale?productId=${p.productId}`
                                  : `/seller/product/on_sale?productId=${p.productId}`
                              }
                              onDone={refetch}
                              title={p.productStatus === 0 ? '停售商品' : '上架商品'}
                              description={
                                p.productStatus === 0
                                  ? `确认将「${p.productName}」停售?`
                                  : `确认将「${p.productName}」重新上架?`
                              }
                              className='text-muted-foreground text-xs hover:underline'
                              trigger={p.productStatus === 0 ? '停售' : '上架'}
                            />
                            <ConfirmDeleteButton
                              url={`/seller/product/delete?productId=${p.productId}`}
                              onDone={refetch}
                              title={`删除商品「${p.productName}」?`}
                              description='此操作不可恢复, 已被订单引用的商品会自动降级为停售'
                            />
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      </Main>
    </>
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
