import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDown, ArrowUp, Plus, Save, Trash2 } from 'lucide-react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ImageUpload } from '@/components/image-upload'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { PageShell } from '@/components/page-shell'
import { toast } from 'sonner'

export function StoreSettings() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['shop-config'],
    queryFn: async () => (await api.get<Record<string, string>>('/api/admin/shop-config')).data,
  })

  const [form, setForm] = useState({
    name: '',
    address: '',
    phone: '',
    openHours: '',
    logo: '',
  })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (data) {
      setForm({
        name: data['store.name'] ?? '',
        address: data['store.address'] ?? '',
        phone: data['store.phone'] ?? '',
        openHours: data['store.openHours'] ?? '',
        logo: data['store.logo'] ?? '',
      })
    }
  }, [data])

  const save = async () => {
    setSaving(true)
    try {
      const fd = new FormData()
      fd.append('name', form.name)
      fd.append('address', form.address)
      fd.append('phone', form.phone)
      fd.append('openHours', form.openHours)
      fd.append('logo', form.logo)
      await api.post('/seller/setting/store/save', fd)
      qc.invalidateQueries({ queryKey: ['shop-config'] })
      toast.success('保存成功')
    } catch (e) {
      console.error(e)
      toast.error('保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <PageShell pretitle='设置 / 店铺' title='门店信息'>
      <Card>
        <CardContent className='space-y-5 p-6 max-w-2xl'>
          {isLoading ? (
            <p className='text-muted-foreground text-sm'>加载中…</p>
          ) : (
            <>
              <Field label='门店名称'>
                <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder='咖啡厅' />
              </Field>
              <Field label='门店地址'>
                <Input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </Field>
              <Field label='联系电话'>
                <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </Field>
              <Field label='营业时间'>
                <Input
                  value={form.openHours}
                  onChange={(e) => setForm({ ...form, openHours: e.target.value })}
                  placeholder='周一至周日 08:00 - 22:00'
                />
              </Field>
              <Field label='门店 Logo'>
                <ImageUpload value={form.logo} onChange={(url) => setForm({ ...form, logo: url })} />
              </Field>
              <div className='pt-2'>
                <Button onClick={save} disabled={saving}>
                  <Save className='size-4' />
                  {saving ? '保存中…' : '保存'}
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </PageShell>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <Label className='mb-1.5 block text-sm font-medium'>{label}</Label>
      {children}
    </div>
  )
}

export function ShopDesign() {
  const qc = useQueryClient()
  const { data: cfg, isLoading } = useQuery({
    queryKey: ['shop-config'],
    queryFn: async () => (await api.get<Record<string, string>>('/api/admin/shop-config')).data,
  })
  const { data: cats } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get<any[]>('/api/admin/categories')).data,
  })

  // 基本信息
  const [basic, setBasic] = useState({
    shopName: '',
    shopLogo: '',
    announcement: '',
    themeColor: '#6b4226',
  })
  const [savingBasic, setSavingBasic] = useState(false)

  useEffect(() => {
    if (cfg) {
      setBasic({
        shopName: cfg.shopName ?? '',
        shopLogo: cfg.shopLogo ?? '',
        announcement: cfg.announcement ?? '',
        themeColor: cfg.themeColor ?? '#6b4226',
      })
      try {
        const arr = JSON.parse(cfg.banners ?? '[]')
        if (Array.isArray(arr)) setBanners(arr)
      } catch {}
    }
  }, [cfg])

  const saveBasic = async () => {
    setSavingBasic(true)
    try {
      const fd = new FormData()
      fd.append('shopName', basic.shopName)
      fd.append('shopLogo', basic.shopLogo)
      fd.append('announcement', basic.announcement)
      fd.append('themeColor', basic.themeColor)
      await api.post('/seller/shop/saveBasic', fd)
      qc.invalidateQueries({ queryKey: ['shop-config'] })
      toast.success('基本信息已保存')
    } catch (e) {
      console.error(e)
      toast.error('保存失败')
    } finally {
      setSavingBasic(false)
    }
  }

  // Banner
  const [banners, setBanners] = useState<string[]>([])
  const [savingBanners, setSavingBanners] = useState(false)

  const saveBanners = async () => {
    setSavingBanners(true)
    try {
      const fd = new FormData()
      fd.append('banners', JSON.stringify(banners.filter(Boolean)))
      await api.post('/seller/shop/saveBanners', fd)
      qc.invalidateQueries({ queryKey: ['shop-config'] })
      toast.success('Banner 已保存')
    } catch (e) {
      console.error(e)
      toast.error('保存失败')
    } finally {
      setSavingBanners(false)
    }
  }

  // 类目排序
  const [sorted, setSorted] = useState<any[]>([])
  const [savingSort, setSavingSort] = useState(false)
  useEffect(() => {
    if (cats) setSorted([...cats])
  }, [cats])

  const move = (i: number, dir: -1 | 1) => {
    const j = i + dir
    if (j < 0 || j >= sorted.length) return
    const copy = [...sorted]
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
    setSorted(copy)
  }

  const saveSort = async () => {
    setSavingSort(true)
    try {
      const payload = sorted.map((c, i) => ({ id: c.categoryId, sort: i }))
      await api.post('/seller/shop/saveCategorySort', payload, {
        headers: { 'Content-Type': 'application/json' },
      })
      qc.invalidateQueries({ queryKey: ['categories'] })
      toast.success('类目排序已保存')
    } catch (e) {
      console.error(e)
      toast.error('保存失败')
    } finally {
      setSavingSort(false)
    }
  }

  if (isLoading) {
    return (
      <PageShell pretitle='设置 / 店铺' title='店铺装修'>
        <p className='text-muted-foreground text-sm'>加载中…</p>
      </PageShell>
    )
  }

  return (
    <PageShell pretitle='设置 / 店铺' title='店铺装修'>
      <div className='space-y-4 max-w-3xl'>
        {/* 基本信息 */}
        <Card>
          <CardContent className='space-y-4 p-6'>
            <h2 className='text-base font-semibold'>基本信息</h2>
            <Field label='店铺名称'>
              <Input value={basic.shopName} onChange={(e) => setBasic({ ...basic, shopName: e.target.value })} />
            </Field>
            <Field label='店铺 Logo'>
              <ImageUpload value={basic.shopLogo} onChange={(url) => setBasic({ ...basic, shopLogo: url })} />
            </Field>
            <Field label='公告'>
              <Input value={basic.announcement} onChange={(e) => setBasic({ ...basic, announcement: e.target.value })} placeholder='如: 新品上市, 全场第二杯半价' />
            </Field>
            <Field label='主题色'>
              <div className='flex items-center gap-3'>
                <input
                  type='color'
                  value={basic.themeColor}
                  onChange={(e) => setBasic({ ...basic, themeColor: e.target.value })}
                  className='h-9 w-16 cursor-pointer rounded border'
                />
                <Input
                  className='font-mono w-32'
                  value={basic.themeColor}
                  onChange={(e) => setBasic({ ...basic, themeColor: e.target.value })}
                />
              </div>
            </Field>
            <div className='pt-2'>
              <Button onClick={saveBasic} disabled={savingBasic}>
                <Save className='size-4' /> {savingBasic ? '保存中…' : '保存基本信息'}
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Banner */}
        <Card>
          <CardContent className='space-y-3 p-6'>
            <h2 className='text-base font-semibold'>轮播 Banner</h2>
            {banners.length === 0 && (
              <p className='text-muted-foreground text-sm'>未添加 Banner</p>
            )}
            <div className='space-y-3'>
              {banners.map((url, i) => (
                <div key={i} className='flex items-start gap-3 rounded-md border p-3'>
                  <ImageUpload
                    value={url}
                    onChange={(v) => {
                      const copy = [...banners]
                      copy[i] = v
                      setBanners(copy)
                    }}
                    size={72}
                    hint='宽幅 banner，建议 750×300'
                  />
                  <Button
                    variant='ghost'
                    size='icon'
                    className='ms-auto'
                    onClick={() => setBanners(banners.filter((_, j) => j !== i))}
                  >
                    <Trash2 className='size-4' />
                  </Button>
                </div>
              ))}
            </div>
            <div className='flex items-center gap-2 pt-2'>
              <Button variant='outline' onClick={() => setBanners([...banners, ''])}>
                <Plus className='size-4' /> 添加 Banner
              </Button>
              <Button onClick={saveBanners} disabled={savingBanners}>
                <Save className='size-4' /> {savingBanners ? '保存中…' : '保存 Banner'}
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* 类目排序 */}
        <Card>
          <CardContent className='space-y-3 p-6'>
            <div className='flex items-center justify-between'>
              <h2 className='text-base font-semibold'>类目排序</h2>
              <p className='text-muted-foreground text-xs'>用上下箭头调整顺序</p>
            </div>
            <div className='space-y-1'>
              {sorted.map((c, i) => (
                <div key={c.categoryId} className='hover:bg-muted/50 flex items-center gap-2 rounded border p-2'>
                  <span className='text-muted-foreground w-8 text-center text-xs tabular-nums'>{i + 1}</span>
                  <span className='flex-1 font-medium'>{c.categoryName}</span>
                  <Button variant='ghost' size='icon' disabled={i === 0} onClick={() => move(i, -1)}>
                    <ArrowUp className='size-4' />
                  </Button>
                  <Button variant='ghost' size='icon' disabled={i === sorted.length - 1} onClick={() => move(i, 1)}>
                    <ArrowDown className='size-4' />
                  </Button>
                </div>
              ))}
            </div>
            <div className='pt-2'>
              <Button onClick={saveSort} disabled={savingSort}>
                <Save className='size-4' /> {savingSort ? '保存中…' : '保存类目排序'}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}
