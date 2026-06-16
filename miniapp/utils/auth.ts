import { request } from './request'

/**
 * 确保已登录: 有 token 直接用; 否则 wx.login 拿 code → POST /mini/login 换 token.
 * token 是后端自建的不透明会话标识(非 session_key), 失效由 401 触发重登.
 */
export function ensureLogin(): Promise<void> {
  const app = getApp()
  if (app.globalData.token) return Promise.resolve()

  return new Promise<void>((resolve, reject) => {
    wx.login({
      success: (r: any) => {
        if (!r.code) {
          reject({ msg: '微信登录失败' })
          return
        }
        request<any>({ url: '/mini/login', method: 'POST', data: { code: r.code } })
          .then((res) => {
            if (res && res.code === 0 && res.data && res.data.token) {
              app.globalData.token = res.data.token
              app.globalData.openid = res.data.openid
              wx.setStorageSync('mini_token', res.data.token)
              resolve()
            } else {
              reject({ msg: (res && res.msg) || '登录失败' })
            }
          })
          .catch(reject)
      },
      fail: () => reject({ msg: '微信登录失败' }),
    })
  })
}
