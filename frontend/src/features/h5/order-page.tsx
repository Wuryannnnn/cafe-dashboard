import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useSearch } from '@tanstack/react-router'
import { Minus, Plus, ShoppingCart, X } from 'lucide-react'
import { api } from '@/lib/api'

type Sku = { id: string; name: string; price: number }
type Addon = { id: string; name: string; price: number }
type Product = {
  id: string
  name: string
  price: number
  description?: string
  icon?: string
  skus?: Sku[]
  addons?: Addon[]
}
type Category = { name: string; foods: Product[] }
type ShopConfig = {
  shopName?: string
  shopLogo?: string
  themeColor?: string
  announcement?: string
  banners?: string
}
type CartItem = {
  productId: string
  productName: string
  skuId: string | null
  skuName: string | null
  skuPrice: number
  addons: Addon[]
  addonFee: number
  quantity: number
  remark: string
}

export function H5OrderPage() {
  const search = useSearch({ from: '/order/' }) as { table?: string }
  const tableNumber = search.table ?? ''

  const [diningType, setDiningType] = useState<0 | 1>(tableNumber ? 0 : 0)
  const [activeCat, setActiveCat] = useState(0)
  const [cart, setCart] = useState<CartItem[]>([])
  const [cartOpen, setCartOpen] = useState(false)
  const [specProduct, setSpecProduct] = useState<Product | null>(null)
  const [pickupNumber, setPickupNumber] = useState<string | null>(null)
  const [lastOrderId, setLastOrderId] = useState<string | null>(null)
  const [bannerIdx, setBannerIdx] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const isSnappingRef = useRef(false)

  const { data: cfg } = useQuery({
    queryKey: ['h5-shop-config'],
    queryFn: async () => {
      const r = await api.get<{ data: ShopConfig }>('/buyer/shop/config')
      return r.data.data
    },
  })
  const { data: cats } = useQuery({
    queryKey: ['h5-products'],
    queryFn: async () => {
      const r = await api.get<{ data: Category[] }>('/buyer/product/list')
      return r.data.data
    },
  })

  // 主题色
  useEffect(() => {
    if (cfg?.themeColor) {
      document.documentElement.style.setProperty('--cafe', cfg.themeColor)
    }
  }, [cfg?.themeColor])

  // Banner 自动播放
  const banners = useMemo<string[]>(() => {
    try { return JSON.parse(cfg?.banners ?? '[]') } catch { return [] }
  }, [cfg?.banners])

  useEffect(() => {
    if (banners.length < 2) return
    const t = setInterval(() => setBannerIdx((i) => (i + 1) % banners.length), 3000)
    return () => clearInterval(t)
  }, [banners.length])

  // 滚动监听: 内容滚动时高亮对应的分类 tab
  useEffect(() => {
    const list = cats ?? []
    if (list.length === 0) return
    let raf = 0
    const onScroll = () => {
      if (isSnappingRef.current) return
      if (raf) return
      raf = requestAnimationFrame(() => {
        raf = 0
        const threshold = 170
        let current = 0
        for (let i = 0; i < list.length; i++) {
          const el = document.getElementById('cat-section-' + i)
          if (!el) continue
          if (el.getBoundingClientRect().top - threshold <= 0) current = i
          else break
        }
        setActiveCat((prev) => (prev === current ? prev : current))
      })
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => {
      window.removeEventListener('scroll', onScroll)
      if (raf) cancelAnimationFrame(raf)
    }
  }, [cats])

  const totalCount = cart.reduce((s, c) => s + c.quantity, 0)
  const totalAmount = cart.reduce((s, c) => s + (c.skuPrice + c.addonFee) * c.quantity, 0)
  const shopName = cfg?.shopName ?? 'The Infinite Cafe'
  const shopLogo = cfg?.shopLogo || '/sell/images/logo.jpg'

  const addToCart = (product: Product, sku: Sku | null, addons: Addon[], qty: number, remark = '') => {
    setCart((c) => [...c, {
      productId: product.id,
      productName: product.name,
      skuId: sku?.id ?? null,
      skuName: sku?.name ?? null,
      skuPrice: sku?.price ?? product.price,
      addons,
      addonFee: addons.reduce((s, a) => s + a.price, 0),
      quantity: qty,
      remark,
    }])
  }

  const setItemRemark = (idx: number, text: string) => {
    setCart((c) => {
      const copy = [...c]
      copy[idx] = { ...copy[idx], remark: text }
      return copy
    })
  }

  const setQty = (idx: number, delta: number) => {
    setCart((c) => {
      const copy = [...c]
      copy[idx] = { ...copy[idx], quantity: copy[idx].quantity + delta }
      return copy.filter((it) => it.quantity > 0)
    })
  }

  const submit = async () => {
    // 防重: 提交中再次点击直接忽略, 避免重复下单
    if (submitting || cart.length === 0) return
    setSubmitting(true)
    const items = cart.map((c) => ({
      productId: c.productId,
      productQuantity: c.quantity,
      skuId: c.skuId ?? '',
      addons: c.addons.length ? JSON.stringify(c.addons.map((a) => ({ name: a.name, price: a.price }))) : '',
      addonFee: c.addonFee,
      remark: c.remark.trim(),
    }))
    const orderRemark = cart
      .filter((c) => c.remark.trim())
      .map((c) => `${c.productName}: ${c.remark.trim()}`)
      .join('; ')
    const fd = new URLSearchParams()
    fd.append('name', '顾客')
    fd.append('phone', '00000000000')
    fd.append('openid', 'guest_' + Date.now())
    fd.append('items', JSON.stringify(items))
    fd.append('diningType', String(diningType))
    fd.append('tableNumber', tableNumber)
    fd.append('payType', '0')
    if (orderRemark) fd.append('remark', orderRemark)
    try {
      const r = await api.post<{ code: number; data?: any; msg?: string }>(
        '/buyer/order/create',
        fd.toString(),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      )
      if (r.data.code === 0) {
        setPickupNumber('#' + r.data.data.pickupNumber)
        setLastOrderId(r.data.data.orderId)
        setCart([])
        setCartOpen(false)
      } else {
        alert('下单失败: ' + (r.data.msg || ''))
      }
    } catch (e: any) {
      // 后端业务错误(如库存不足)会带 {code,msg}, 优先展示具体原因
      alert(e?.response?.data?.msg || '网络错误, 请重试')
    } finally {
      setSubmitting(false)
    }
  }

  const goPay = (payType: 0 | 1) => {
    if (!lastOrderId) return
    const returnUrl = location.origin + `/order/status?orderId=${lastOrderId}` + (tableNumber ? `&table=${tableNumber}` : '')
    // payType: 0=微信 / 1=支付宝, 必须告诉后端走哪条分支
    location.href = `/pay?orderId=${lastOrderId}&returnUrl=${encodeURIComponent(returnUrl)}&payType=${payType}`
  }

  // 支付成功页 (下单后)
  if (pickupNumber) {
    return (
      <SuccessPage
        pickupNumber={pickupNumber}
        diningType={diningType}
        tableNumber={tableNumber}
        orderId={lastOrderId!}
        onPay={goPay}
      />
    )
  }

  return (
    <div className='flex min-h-dvh flex-col bg-stone-50 pb-28' style={{ fontFamily: 'Inter, -apple-system, "PingFang SC", sans-serif' }}>
      {/* 顶部 */}
      <header className='sticky top-0 z-30 border-b bg-white px-4 pt-4 pb-3'>
        <div className='flex items-center gap-3'>
          <div
            className='shrink-0 overflow-hidden rounded-full bg-stone-100 ring-2 ring-white'
            style={{ width: 52, height: 52, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}
          >
            <img
              src={shopLogo}
              alt=''
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none' }}
            />
          </div>
          <div className='min-w-0 flex-1'>
            <h1
              className='font-bold tracking-tight leading-tight'
              style={{ color: 'var(--cafe, #6b4226)', fontSize: 20 }}
            >
              {shopName}
            </h1>
            <p className='text-stone-400 text-[11px] leading-tight uppercase tracking-[0.15em] mt-0.5'>Dining &amp; Coffee</p>
          </div>
          {tableNumber && (
            <span
              className='shrink-0 rounded-full px-3 py-1 text-xs font-semibold'
              style={{ background: 'var(--cafe, #6b4226)', color: '#fff' }}
            >
              {tableNumber}
            </span>
          )}
        </div>
        <div className='mt-3 inline-flex rounded-lg bg-stone-100 p-0.5'>
          {[
            { v: 0 as const, label: '堂食' },
            { v: 1 as const, label: '外带' },
          ].map((opt) => (
            <button
              key={opt.v}
              onClick={() => setDiningType(opt.v)}
              className={
                'rounded-md px-5 py-1.5 text-sm font-medium transition ' +
                (diningType === opt.v ? 'bg-white shadow-sm' : 'text-stone-500')
              }
              style={diningType === opt.v ? { color: 'var(--cafe, #6b4226)' } : {}}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </header>

      {cfg?.announcement && (
        <div className='bg-amber-50 px-4 py-2 text-xs text-amber-800'>{cfg.announcement}</div>
      )}

      {/* Banner */}
      {banners.length > 0 && (
        <div className='relative mx-3 mt-3 h-40 overflow-hidden rounded-xl'>
          <div
            className='flex h-full transition-transform duration-500'
            style={{ transform: `translateX(-${bannerIdx * 100}%)` }}
          >
            {banners.map((url, i) => (
              <img key={i} src={url} className='h-full w-full shrink-0 object-cover' alt='' />
            ))}
          </div>
          {banners.length > 1 && (
            <div className='absolute right-0 bottom-2 left-0 flex justify-center gap-1.5'>
              {banners.map((_, i) => (
                <span
                  key={i}
                  className={'h-1 rounded-full transition-all ' + (i === bannerIdx ? 'w-4 bg-white' : 'w-1 bg-white/50')}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* 类目 tabs (点击滚动到对应分块) */}
      <CategoryNav
        cats={cats ?? []}
        active={activeCat}
        onPick={(i) => {
          setActiveCat(i)
          isSnappingRef.current = true
          const el = document.getElementById('cat-section-' + i)
          if (el) {
            const top = el.getBoundingClientRect().top + window.scrollY - 140
            window.scrollTo({ top, behavior: 'smooth' })
          }
          window.setTimeout(() => { isSnappingRef.current = false }, 700)
        }}
      />

      {/* 全部分类按顺序展示, 像星巴克那样滚动浏览 */}
      <div className='p-3 space-y-6'>
        {cats?.map((c, ci) => (
          <section key={ci} id={'cat-section-' + ci}>
            <div className='flex items-baseline justify-between mb-2 px-1'>
              <h2 className='text-lg font-bold tracking-tight' style={{ color: 'var(--cafe, #6b4226)' }}>
                {c.name}
              </h2>
              <span className='text-stone-400 text-xs'>{c.foods.length} 款</span>
            </div>
            <div className='space-y-3'>
              {c.foods.map((p) => (
                <ProductCard key={p.id} product={p} onAdd={() => {
                  if (p.skus?.length || p.addons?.length) {
                    setSpecProduct(p)
                  } else {
                    addToCart(p, null, [], 1)
                  }
                }} />
              ))}
            </div>
          </section>
        ))}
      </div>

      {/* 购物车浮条 */}
      <CartBar
        count={totalCount}
        amount={totalAmount}
        canCheckout={cart.length > 0 && !submitting}
        onCartClick={() => setCartOpen(!cartOpen)}
        onCheckout={submit}
      />

      {/* 购物车展开面板 */}
      {cartOpen && cart.length > 0 && (
        <CartDetail
          cart={cart}
          onClose={() => setCartOpen(false)}
          onClear={() => { setCart([]); setCartOpen(false) }}
          onQty={(i, d) => setQty(i, d)}
          onRemark={setItemRemark}
        />
      )}

      {/* 规格弹窗 */}
      {specProduct && (
        <SpecModal
          product={specProduct}
          onClose={() => setSpecProduct(null)}
          onConfirm={(sku, addons, qty, remark) => {
            addToCart(specProduct, sku, addons, qty, remark)
            setSpecProduct(null)
          }}
        />
      )}
    </div>
  )
}

function ProductCard({ product, onAdd }: { product: Product; onAdd: () => void }) {
  return (
    <div className='flex gap-3 rounded-xl border bg-white p-3.5'>
      <div className='shrink-0 overflow-hidden rounded-lg' style={{ width: 88, height: 88, background: 'linear-gradient(135deg, #F5EDE3 0%, #E8DCC9 100%)' }}>
        {product.icon && (
          <img src={product.icon} alt='' style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }} />
        )}
      </div>
      <div className='flex flex-1 flex-col min-w-0'>
        <div
          className='text-base font-bold leading-tight'
          style={{ color: '#1F1B17', letterSpacing: '-0.01em' }}
        >
          {product.name}
        </div>
        <div className='text-stone-500 mt-1 line-clamp-2 text-xs leading-snug'>{product.description ?? ''}</div>
        <div className='mt-auto flex items-end justify-between pt-1.5'>
          <div className='font-bold tabular-nums' style={{ color: 'var(--cafe, #6b4226)' }}>
            <span className='text-stone-500 me-1 text-xs font-normal'>¥</span>
            <span className='text-lg'>{product.price}</span>
            {product.skus?.length ? <span className='text-stone-500 ms-1 text-xs font-normal'>起</span> : null}
          </div>
          <button
            onClick={onAdd}
            className='flex size-9 items-center justify-center rounded-xl text-white shadow-sm transition active:scale-95'
            style={{ background: 'var(--cafe, #6b4226)' }}
          >
            <Plus className='size-4' />
          </button>
        </div>
      </div>
    </div>
  )
}

function CategoryNav({
  cats,
  active,
  onPick,
}: {
  cats: Category[]
  active: number
  onPick: (i: number) => void
}) {
  const scrollerRef = useRef<HTMLDivElement>(null)
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([])

  useEffect(() => {
    const wrap = scrollerRef.current
    const btn = itemRefs.current[active]
    if (!wrap || !btn) return
    const target = btn.offsetLeft - wrap.clientWidth / 2 + btn.clientWidth / 2
    wrap.scrollTo({ left: Math.max(0, target), behavior: 'smooth' })
  }, [active])

  return (
    <div
      ref={scrollerRef}
      className='sticky top-[110px] z-20 flex gap-1 overflow-x-auto border-b bg-white px-3 py-2 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden'
    >
      {cats.map((c, i) => (
        <button
          key={i}
          ref={(el) => { itemRefs.current[i] = el }}
          onClick={() => onPick(i)}
          className={
            'shrink-0 rounded-lg px-3.5 py-1.5 text-sm font-semibold transition ' +
            (active === i ? 'text-white' : 'text-stone-600')
          }
          style={active === i ? { background: 'var(--cafe, #6b4226)' } : {}}
        >
          {c.name}
        </button>
      ))}
    </div>
  )
}

function SpecModal({
  product,
  onClose,
  onConfirm,
}: {
  product: Product
  onClose: () => void
  onConfirm: (sku: Sku | null, addons: Addon[], qty: number, remark: string) => void
}) {
  const [sku, setSku] = useState<Sku | null>(product.skus?.[0] ?? null)
  const [addons, setAddons] = useState<Addon[]>([])
  const [qty, setQty] = useState(1)
  const [remark, setRemark] = useState('')

  const toggleAddon = (a: Addon) => {
    setAddons((x) => x.some((o) => o.id === a.id) ? x.filter((o) => o.id !== a.id) : [...x, a])
  }

  const price = (sku?.price ?? product.price) + addons.reduce((s, a) => s + a.price, 0)

  return (
    <div className='fixed inset-0 z-50 bg-stone-950/50 backdrop-blur-sm' onClick={onClose}>
      <div className='absolute right-0 bottom-0 left-0 max-h-[82vh] overflow-y-auto rounded-t-2xl bg-white p-5 pb-6' onClick={(e) => e.stopPropagation()}>
        <button onClick={onClose} className='bg-stone-100 absolute top-3.5 right-3.5 flex size-8 items-center justify-center rounded-full text-stone-500'>
          <X className='size-4' />
        </button>
        <h3 className='pe-10 text-lg font-bold'>{product.name}</h3>

        {product.skus && product.skus.length > 0 && (
          <SpecGroup label='规格'>
            {product.skus.map((s) => (
              <SpecOpt key={s.id} active={sku?.id === s.id} onClick={() => setSku(s)}>
                {s.name} ¥{s.price}
              </SpecOpt>
            ))}
          </SpecGroup>
        )}

        {product.addons && product.addons.length > 0 && (
          <SpecGroup label='加料 (可多选)'>
            {product.addons.map((a) => (
              <SpecOpt key={a.id} active={addons.some((o) => o.id === a.id)} onClick={() => toggleAddon(a)} variant='addon'>
                {a.name}{a.price > 0 ? ` +¥${a.price}` : ''}
              </SpecOpt>
            ))}
          </SpecGroup>
        )}

        <SpecGroup label='备注 (可选)'>
          <input
            type='text'
            value={remark}
            onChange={(e) => setRemark(e.target.value)}
            placeholder='少冰 / 去糖 / 多加奶泡…'
            maxLength={60}
            className='w-full rounded-md border border-stone-200 bg-stone-50 px-3 py-2 text-sm placeholder:text-stone-400 focus:border-stone-400 focus:bg-white focus:outline-none'
          />
        </SpecGroup>

        <div className='mt-5 flex items-center justify-between gap-3 border-t pt-4'>
          <div className='inline-flex items-center gap-3 rounded-lg bg-stone-100 p-1'>
            <button onClick={() => setQty(Math.max(1, qty - 1))} className='size-7 rounded-md bg-white text-stone-700'>
              <Minus className='mx-auto size-4' />
            </button>
            <span className='w-5 text-center font-semibold tabular-nums'>{qty}</span>
            <button onClick={() => setQty(qty + 1)} className='size-7 rounded-md bg-white text-stone-700'>
              <Plus className='mx-auto size-4' />
            </button>
          </div>
          <div className='ms-auto flex items-center gap-3'>
            <span className='text-lg font-bold tabular-nums' style={{ color: 'var(--cafe, #6b4226)' }}>¥{(price * qty).toFixed(2)}</span>
            <button
              onClick={() => onConfirm(sku, addons, qty, remark.trim())}
              className='rounded-lg px-5 py-2.5 text-sm font-semibold text-white'
              style={{ background: 'var(--cafe, #6b4226)' }}
            >
              加入购物车
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function SpecGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className='mt-4'>
      <p className='text-stone-500 mb-2 text-xs font-semibold uppercase tracking-wider'>{label}</p>
      <div className='flex flex-wrap gap-2'>{children}</div>
    </div>
  )
}

function SpecOpt({
  active, onClick, children, variant = 'sku',
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
  variant?: 'sku' | 'addon'
}) {
  return (
    <button
      onClick={onClick}
      className={
        'rounded-lg border px-3.5 py-1.5 text-sm font-medium transition ' +
        (active
          ? variant === 'sku'
            ? 'text-white'
            : 'text-amber-800'
          : 'border-stone-300 bg-white text-stone-700')
      }
      style={
        active
          ? variant === 'sku'
            ? { background: 'var(--cafe, #6b4226)', borderColor: 'var(--cafe, #6b4226)' }
            : { background: 'rgba(217, 168, 122, 0.18)', borderColor: '#c9a074' }
          : {}
      }
    >
      {children}
    </button>
  )
}

function CartBar({
  count, amount, canCheckout, onCartClick, onCheckout,
}: {
  count: number
  amount: number
  canCheckout: boolean
  onCartClick: () => void
  onCheckout: () => void
}) {
  return (
    <div className='fixed right-0 bottom-0 left-0 z-40 flex items-center gap-3 border-t bg-white px-4 pt-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]'>
      <button onClick={onCartClick} className='relative flex size-12 items-center justify-center rounded-2xl text-white' style={{ background: 'var(--cafe, #6b4226)' }}>
        <ShoppingCart className='size-5' />
        {count > 0 && (
          <span className='absolute -top-1.5 -right-1.5 flex h-5 min-w-5 items-center justify-center rounded-full border-2 border-white bg-rose-600 px-1 text-[11px] font-bold text-white'>
            {count}
          </span>
        )}
      </button>
      <div className='flex-1'>
        <div className='text-lg font-bold tabular-nums' style={{ color: 'var(--cafe, #6b4226)' }}>¥{amount.toFixed(2)}</div>
        <div className='text-stone-400 text-[11px]'>下一步在线支付</div>
      </div>
      <button
        onClick={onCheckout}
        disabled={!canCheckout}
        className={'rounded-xl px-6 py-2.5 text-sm font-semibold transition ' + (canCheckout ? 'text-white active:scale-[0.97]' : 'cursor-not-allowed bg-stone-200 text-stone-400')}
        style={canCheckout ? { background: 'var(--cafe, #6b4226)' } : {}}
      >
        去下单
      </button>
    </div>
  )
}

function CartDetail({
  cart, onClose, onClear, onQty, onRemark,
}: {
  cart: CartItem[]
  onClose: () => void
  onClear: () => void
  onQty: (i: number, d: number) => void
  onRemark: (i: number, text: string) => void
}) {
  return (
    <div className='fixed inset-x-0 bottom-20 z-30 max-h-[60vh] overflow-y-auto rounded-t-2xl border-t bg-white p-4 shadow-2xl'>
      <div className='mb-3 flex items-center justify-between'>
        <h4 className='font-bold'>已选商品</h4>
        <div className='flex gap-3 text-xs'>
          <button onClick={onClear} className='text-stone-400 hover:text-stone-600'>清空</button>
          <button onClick={onClose} className='text-stone-400 hover:text-stone-600'>收起</button>
        </div>
      </div>
      <div>
        {cart.map((c, i) => (
          <div key={i} className='border-b py-3 last:border-0'>
            <div className='flex items-center gap-3'>
              <div className='min-w-0 flex-1'>
                <div className='font-medium'>{c.productName}</div>
                <div className='text-stone-500 text-xs'>
                  {c.skuName ?? ''}{c.addons.length ? ' / ' + c.addons.map((a) => a.name).join(' + ') : ''}
                </div>
              </div>
              <div className='font-semibold tabular-nums' style={{ color: 'var(--cafe, #6b4226)' }}>
                ¥{((c.skuPrice + c.addonFee) * c.quantity).toFixed(2)}
              </div>
              <div className='inline-flex items-center gap-2 rounded-lg bg-stone-100 p-0.5'>
                <button onClick={() => onQty(i, -1)} className='size-6 rounded bg-white text-stone-700'>
                  <Minus className='mx-auto size-3' />
                </button>
                <span className='w-4 text-center text-sm font-semibold tabular-nums'>{c.quantity}</span>
                <button onClick={() => onQty(i, 1)} className='size-6 rounded bg-white text-stone-700'>
                  <Plus className='mx-auto size-3' />
                </button>
              </div>
            </div>
            <input
              type='text'
              value={c.remark}
              onChange={(e) => onRemark(i, e.target.value)}
              placeholder='单独备注: 少冰 / 去糖 / 多加奶泡…'
              maxLength={60}
              className='mt-2 w-full rounded-md border border-stone-200 bg-stone-50 px-2.5 py-1.5 text-xs placeholder:text-stone-400 focus:border-stone-400 focus:bg-white focus:outline-none'
            />
          </div>
        ))}
      </div>
    </div>
  )
}

function SuccessPage({
  pickupNumber, diningType, tableNumber, orderId, onPay,
}: {
  pickupNumber: string
  diningType: number
  tableNumber: string
  orderId: string
  onPay: (t: 0 | 1) => void
}) {
  return (
    <div className='flex min-h-dvh flex-col items-center justify-start bg-stone-50 px-6 pt-24 text-center'>
      <p className='text-stone-400 text-xs font-semibold uppercase tracking-wider'>您的取餐号</p>
      <div className='mt-3 text-6xl font-bold tabular-nums tracking-tighter' style={{ color: 'var(--cafe, #6b4226)' }}>
        {pickupNumber}
      </div>
      <p className='text-stone-500 mt-1 text-sm'>{diningType === 1 ? '外带' : '堂食'}{tableNumber ? ' · 桌号 ' + tableNumber : ''}</p>

      <div className='mt-12 w-full'>
        <p className='text-stone-500 mb-4 text-sm'>请选择支付方式</p>
        <div className='flex justify-center gap-3'>
          <button onClick={() => onPay(0)} className='rounded-xl border-2 border-emerald-500 bg-white px-6 py-3 text-sm font-semibold text-emerald-600 active:scale-95'>
            微信支付
          </button>
          <button onClick={() => onPay(1)} className='rounded-xl border-2 border-blue-500 bg-white px-6 py-3 text-sm font-semibold text-blue-600 active:scale-95'>
            支付宝
          </button>
        </div>
      </div>

      <Link
        to='/order/status'
        search={{ orderId, table: tableNumber || undefined }}
        className='text-stone-400 mt-12 text-sm hover:text-stone-600'
      >
        先看看订单状态 →
      </Link>
    </div>
  )
}
