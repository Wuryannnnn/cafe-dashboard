import { request } from '../../utils/request'
import { ensureLogin } from '../../utils/auth'

Page({
  data: {
    categories: [] as any[],
    cart: {} as Record<string, number>,
    cartCount: 0,
    total: '0.00',
    tableId: '',
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
        if (res && res.code === 0) this.setData({ categories: res.data || [] })
        else wx.showToast({ title: (res && res.msg) || '菜单加载失败', icon: 'none' })
      })
      .catch(() => wx.showToast({ title: '网络异常', icon: 'none' }))
  },

  add(e: any) {
    const id = e.currentTarget.dataset.id
    const cart = { ...this.data.cart }
    cart[id] = (cart[id] || 0) + 1
    this.recalc(cart)
  },

  minus(e: any) {
    const id = e.currentTarget.dataset.id
    const cart = { ...this.data.cart }
    if (cart[id]) {
      cart[id] -= 1
      if (cart[id] <= 0) delete cart[id]
    }
    this.recalc(cart)
  },

  recalc(cart: Record<string, number>) {
    const priceMap = this.priceMap()
    let count = 0
    let total = 0
    Object.keys(cart).forEach((id) => {
      count += cart[id]
      total += (priceMap[id] || 0) * cart[id]
    })
    this.setData({ cart, cartCount: count, total: total.toFixed(2) })
  },

  priceMap(): Record<string, number> {
    const m: Record<string, number> = {}
    this.data.categories.forEach((c: any) =>
      (c.foods || []).forEach((f: any) => {
        m[f.productId] = Number(f.productPrice)
      })
    )
    return m
  },

  submit() {
    if (this.data.cartCount === 0) {
      wx.showToast({ title: '请先选购', icon: 'none' })
      return
    }
    if (this.data.submitting) return
    this.setData({ submitting: true })

    const items = Object.keys(this.data.cart).map((id) => ({
      productId: id,
      productQuantity: this.data.cart[id],
    }))

    ensureLogin()
      .then(() =>
        request<any>({
          url: '/mini/order/create',
          method: 'POST',
          auth: true,
          data: { tableId: this.data.tableId || '', items: JSON.stringify(items) },
        })
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
