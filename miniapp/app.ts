App({
  globalData: {
    token: '' as string,
    openid: '' as string,
    tableId: '' as string,
  },
  onLaunch() {
    const t = wx.getStorageSync('mini_token')
    if (t) this.globalData.token = t
  },
})
