import { request } from '../../utils/request'
import { ensureLogin } from '../../utils/auth'

// ---- 金额一律用「分」做整数运算, 避免浮点误差(0.1+0.2≠0.3) ----
function toFen(x: any): number {
  return Math.round((Number(x) || 0) * 100)
}
function fenToYuan(fen: number): string {
  return (fen / 100).toFixed(2)
}

Page({
  data: {
    categories: [] as any[],   // [{name,type,foods:[{id,name,price,priceLabel,icon,description,skus,addons,hasSpec}]}]
    activeCat: 0,              // 左侧分类高亮
    scrollIntoId: '',          // 右侧菜单滚动锚点
    cartLines: [] as any[],    // 购物车行(每个 商品+规格+加料 组合一行)
    qtyByProduct: {} as Record<string, number>, // productId -> 总件数(跨组合), 菜单卡片角标用
    cartCount: 0,
    totalLabel: '0.00',
    tableId: '',
    diningType: 0,             // 0 堂食 / 1 外带
    cartOpen: false,           // 购物车明细 sheet
    // 规格/加料弹层
    specOpen: false,
    specFood: null as any,
    specSkuId: '',
    specAddonMap: {} as Record<string, boolean>,
    specPriceLabel: '0.00',
    submitting: false,
  },

  onLoad(query: any) {
    const app = getApp()
    // 桌台: 小程序码 scene=tableId; 普通参数 tableId 兜底
    let tableId = (query && (query.tableId || query.scene)) || ''
    if (tableId) {
      tableId = decodeURIComponent(tableId)
      app.globalData.tableId = tableId
      this.setData({ tableId })
      wx.setNavigationBarTitle({ title: `点单 · ${tableId}号桌` })
    }
    this.loadMenu()
  },

  loadMenu() {
    request<any>({ url: '/buyer/product/list' })
      .then((res) => {
        if (res && res.code === 0) {
          this.setData({ categories: this.normalize(res.data || []) }, () => {
            // 等渲染完再量各分类区块的位置, 供手动滚动时高亮联动
            setTimeout(() => this.computeCatTops(), 60)
          })
        } else {
          wx.showToast({ title: (res && res.msg) || '菜单加载失败', icon: 'none' })
        }
      })
      .catch(() => wx.showToast({ title: '网络异常', icon: 'none' }))
  },

  // 后端 @JsonProperty 后的真实字段: 商品 id/name/price/description/icon/skus/addons;
  // 规格 id/name/price/stock; 加料 id/name/price. 这里顺手把价格预格式化(WXML 不能算).
  normalize(cats: any[]): any[] {
    return (cats || []).map((c: any) => ({
      name: c.name,
      type: c.type,
      foods: (c.foods || []).map((f: any) => {
        const skus = (f.skus || []).map((s: any) => ({
          id: s.id, name: s.name, price: s.price,
          priceLabel: (Number(s.price) || 0).toFixed(2), stock: s.stock,
        }))
        const addons = (f.addons || []).map((a: any) => ({
          id: a.id, name: a.name, price: a.price,
          priceLabel: (Number(a.price) || 0).toFixed(2),
        }))
        return {
          id: f.id, name: f.name, price: f.price,
          priceLabel: (Number(f.price) || 0).toFixed(2),
          description: f.description, icon: f.icon,
          skus, addons,
          hasSpec: skus.length > 0 || addons.length > 0,
        }
      }),
    }))
  },

  findFood(id: string): any {
    let found: any = null
    this.data.categories.forEach((c: any) =>
      (c.foods || []).forEach((f: any) => { if (f.id === id) found = f })
    )
    return found
  },

  // ---------- 左侧分类 ↔ 右侧菜单 联动 ----------
  computeCatTops() {
    const q = wx.createSelectorQuery().in(this)
    q.selectAll('.cat-section').boundingClientRect()
    q.select('.menu').scrollOffset()
    q.select('.menu').boundingClientRect()
    q.exec((res: any) => {
      const rects = res && res[0]
      const scroll = res && res[1]
      const menuRect = res && res[2]
      if (!rects || !rects.length || !menuRect || !scroll) return
      // 各分类区块相对菜单内容顶部的偏移
      this.catTops = rects.map((r: any) => r.top - menuRect.top + scroll.scrollTop)
    })
  },

  onMenuScroll(e: any) {
    if (this.lockSync) return // tap 跳转时短暂屏蔽, 防止抖动
    const tops = this.catTops
    if (!tops || !tops.length) return
    const y = (e && e.detail && e.detail.scrollTop) || 0
    let idx = 0
    for (let i = 0; i < tops.length; i++) {
      if (y >= tops[i] - 4) idx = i
      else break
    }
    if (idx !== this.data.activeCat) this.setData({ activeCat: idx })
  },

  tapCat(e: any) {
    const idx = e.currentTarget.dataset.idx
    this.lockSync = true
    this.setData({ activeCat: idx, scrollIntoId: 'cat-' + idx })
    setTimeout(() => { this.lockSync = false }, 400)
  },

  // ---------- 购物车行操作 ----------
  lineKey(productId: string, skuId: string, addonIds: string[]): string {
    return productId + '#' + (skuId || '') + '#' + (addonIds || []).slice().sort().join(',')
  },

  addLine(food: any, sku: any, addons: any[], delta: number) {
    const skuId = sku ? sku.id : ''
    const addonIds = (addons || []).map((a: any) => a.id)
    const key = this.lineKey(food.id, skuId, addonIds)
    const lines = this.data.cartLines.slice()
    const idx = lines.findIndex((l: any) => l.key === key)
    if (idx >= 0) {
      lines[idx] = { ...lines[idx], qty: lines[idx].qty + delta }
      if (lines[idx].qty <= 0) lines.splice(idx, 1)
    } else if (delta > 0) {
      const addonFen = (addons || []).reduce((s: number, a: any) => s + toFen(a.price), 0)
      const unitFen = (sku ? toFen(sku.price) : toFen(food.price)) + addonFen
      const parts: string[] = []
      if (sku) parts.push(sku.name)
      if (addons && addons.length) parts.push(addons.map((a: any) => a.name).join('+'))
      lines.push({
        key,
        productId: food.id,
        name: food.name,
        skuId,
        skuName: sku ? sku.name : '',
        addons: (addons || []).map((a: any) => ({ name: a.name, price: Number(a.price) })),
        addonFen,
        summary: parts.join(' · '),
        unitFen,
        qty: delta,
      })
    }
    this.recalc(lines)
  },

  changeLine(key: string, delta: number) {
    const lines = this.data.cartLines.slice()
    const idx = lines.findIndex((l: any) => l.key === key)
    if (idx < 0) return
    lines[idx] = { ...lines[idx], qty: lines[idx].qty + delta }
    if (lines[idx].qty <= 0) lines.splice(idx, 1)
    this.recalc(lines)
  },

  recalc(lines: any[]) {
    let count = 0
    let totalFen = 0
    const qtyByProduct: Record<string, number> = {}
    const out = lines.map((l: any) => {
      count += l.qty
      totalFen += l.unitFen * l.qty
      qtyByProduct[l.productId] = (qtyByProduct[l.productId] || 0) + l.qty
      return { ...l, subLabel: fenToYuan(l.unitFen * l.qty) }
    })
    const patch: any = {
      cartLines: out,
      cartCount: count,
      totalLabel: fenToYuan(totalFen),
      qtyByProduct,
    }
    if (count === 0) patch.cartOpen = false
    this.setData(patch)
  },

  // 无规格商品: 直接加减
  addSimple(e: any) {
    const food = this.findFood(e.currentTarget.dataset.id)
    if (food) this.addLine(food, null, [], 1)
  },
  minusSimple(e: any) {
    const id = e.currentTarget.dataset.id
    this.changeLine(this.lineKey(id, '', []), -1)
  },

  // 购物车 sheet 里的加减(按行 key)
  linePlus(e: any) { this.changeLine(e.currentTarget.dataset.key, 1) },
  lineMinus(e: any) { this.changeLine(e.currentTarget.dataset.key, -1) },

  // ---------- 购物车明细 sheet ----------
  toggleCart() {
    if (this.data.cartCount === 0) {
      wx.showToast({ title: '购物车是空的', icon: 'none' })
      return
    }
    this.setData({ cartOpen: !this.data.cartOpen })
  },
  closeCart() { this.setData({ cartOpen: false }) },
  clearCart() { this.recalc([]) },

  // ---------- 规格/加料弹层 ----------
  openSpec(e: any) {
    const food = this.findFood(e.currentTarget.dataset.id)
    if (!food) return
    const specSkuId = food.skus.length ? food.skus[0].id : ''
    this.setData(
      { specOpen: true, specFood: food, specSkuId, specAddonMap: {} },
      () => this.recalcSpecPrice()
    )
  },
  closeSpec() { this.setData({ specOpen: false, specFood: null, specSkuId: '', specAddonMap: {} }) },

  pickSku(e: any) {
    this.setData({ specSkuId: e.currentTarget.dataset.id }, () => this.recalcSpecPrice())
  },
  toggleAddon(e: any) {
    const id = e.currentTarget.dataset.id
    const map = { ...this.data.specAddonMap }
    if (map[id]) delete map[id]
    else map[id] = true
    this.setData({ specAddonMap: map }, () => this.recalcSpecPrice())
  },
  recalcSpecPrice() {
    const food = this.data.specFood
    if (!food) return
    let fen = 0
    if (food.skus.length) {
      const sku = food.skus.find((s: any) => s.id === this.data.specSkuId) || food.skus[0]
      fen += toFen(sku.price)
    } else {
      fen += toFen(food.price)
    }
    food.addons.forEach((a: any) => { if (this.data.specAddonMap[a.id]) fen += toFen(a.price) })
    this.setData({ specPriceLabel: fenToYuan(fen) })
  },
  confirmSpec() {
    const food = this.data.specFood
    if (!food) return
    let sku: any = null
    if (food.skus.length) {
      sku = food.skus.find((s: any) => s.id === this.data.specSkuId) || food.skus[0]
    }
    const addons = food.addons.filter((a: any) => this.data.specAddonMap[a.id])
    this.addLine(food, sku, addons, 1)
    this.closeSpec()
  },

  // ---------- 堂食/外带 ----------
  setDining(e: any) {
    const v = Number(e.currentTarget.dataset.v)
    this.setData({ diningType: v })
    // 标题同步: 外带显示"外带", 堂食有桌台显示桌号
    let title = '点单'
    if (v === 1) title = '点单 · 外带'
    else if (this.data.tableId) title = `点单 · ${this.data.tableId}号桌`
    wx.setNavigationBarTitle({ title })
  },

  noop() { /* 吸收 sheet/弹层内部点击, 防止穿透到遮罩关闭 */ },

  // ---------- 下单 ----------
  submit() {
    if (this.data.cartCount === 0) {
      wx.showToast({ title: '请先选购', icon: 'none' })
      return
    }
    if (this.data.submitting) return
    this.setData({ submitting: true })

    const items = this.data.cartLines.map((l: any) => {
      const it: any = { productId: l.productId, productQuantity: l.qty }
      if (l.skuId) it.skuId = l.skuId
      if (l.addonFen > 0) {
        it.addonFee = fenToYuan(l.addonFen)         // 后端 BigDecimal, 传字符串避免浮点
        it.addons = JSON.stringify(l.addons)        // 加料明细, 入库做小票记录
      }
      return it
    })

    const data: any = { items: JSON.stringify(items), diningType: this.data.diningType }
    // 堂食且有桌台才带 tableId(后端 Integer, 空串会 400); 外带不绑桌台
    if (this.data.diningType === 0 && this.data.tableId) data.tableId = this.data.tableId

    ensureLogin()
      .then(() =>
        request<any>({ url: '/mini/order/create', method: 'POST', auth: true, data })
      )
      .then((res) => {
        this.setData({ submitting: false })
        if (res && res.code === 0 && res.data) {
          wx.redirectTo({ url: `/pages/pay/index?orderId=${res.data.orderId}` })
        } else {
          wx.showToast({ title: (res && res.msg) || '下单失败', icon: 'none' })
        }
      })
      .catch((e: any) => {
        this.setData({ submitting: false })
        wx.showToast({ title: (e && e.msg) || '下单失败', icon: 'none' })
      })
  },
})
