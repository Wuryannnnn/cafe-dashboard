import { request } from '../../utils/request'
import { ensureLogin } from '../../utils/auth'

Page({
  data: {
    orderId: '',
    status: '正在唤起支付…',
    error: '',
  },

  onLoad(query: any) {
    this.setData({ orderId: query.orderId })
    this.pay()
  },

  pay() {
    this.setData({ error: '', status: '正在唤起支付…' })
    ensureLogin()
      .then(() =>
        request<any>({
          url: '/pay/mini/create',
          method: 'POST',
          auth: true,
          data: { orderId: this.data.orderId },
        })
      )
      .then((res) => {
        // /pay/mini/create 直接返回 5 字段; 失败返回 {code:-1, msg}
        if (!res || res.code === -1 || !res.paySign) {
          this.setData({ error: (res && res.msg) || '支付发起失败', status: '' })
          return
        }
        wx.requestPayment({
          timeStamp: res.timeStamp,
          nonceStr: res.nonceStr,
          package: res.package,
          signType: res.signType || 'RSA',
          paySign: res.paySign,
          success: () => {
            wx.redirectTo({ url: `/pages/status/index?orderId=${this.data.orderId}` })
          },
          fail: (e: any) => {
            const cancelled = e && e.errMsg && e.errMsg.indexOf('cancel') >= 0
            this.setData({ error: cancelled ? '已取消支付' : '支付未完成', status: '' })
          },
        })
      })
      .catch((e: any) => this.setData({ error: (e && e.msg) || '支付发起失败', status: '' }))
  },

  retry() {
    this.pay()
  },

  toStatus() {
    wx.redirectTo({ url: `/pages/status/index?orderId=${this.data.orderId}` })
  },
})
