import { request } from '../../utils/request'

const STATUS_TEXT: Record<number, string> = {
  0: '已下单',
  1: '制作中',
  2: '待取餐',
  3: '已完成',
  4: '已取消',
  5: '已退款',
}

Page({
  data: {
    orderId: '',
    pickupNumber: '',
    amount: '',
    statusCode: -1,
    statusText: '查询中…',
    timer: 0,
  },

  onLoad(query: any) {
    this.setData({ orderId: query.orderId })
    this.fetch()
    const timer = setInterval(() => this.fetch(), 5000)
    this.setData({ timer })
  },

  onUnload() {
    if (this.data.timer) clearInterval(this.data.timer)
  },

  fetch() {
    request<any>({ url: `/buyer/order/status?orderId=${this.data.orderId}` })
      .then((res) => {
        if (res && res.code === 0 && res.data) {
          const o = res.data
          this.setData({
            pickupNumber: o.pickupNumber || '',
            amount: o.orderAmount,
            statusCode: o.orderStatus,
            statusText: STATUS_TEXT[o.orderStatus] || '处理中',
          })
          // 终态停止轮询
          if (o.orderStatus === 3 || o.orderStatus === 4 || o.orderStatus === 5) {
            if (this.data.timer) clearInterval(this.data.timer)
          }
        }
      })
      .catch(() => {})
  },

  backToOrder() {
    wx.reLaunch({ url: '/pages/order/index' })
  },
})
