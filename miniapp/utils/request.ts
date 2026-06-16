import { BASE_URL } from './config'

interface ReqOpts {
  url: string
  method?: 'GET' | 'POST'
  data?: Record<string, any>
  auth?: boolean // 是否带 token (受保护接口)
}

/**
 * wx.request 封装: 拼 BASE_URL, 表单编码(后端用 @RequestParam), 受保护接口注入 Authorization.
 * 401 时清掉本地 token 并 reject({code:401}), 由调用方触发重新登录.
 */
export function request<T = any>(opts: ReqOpts): Promise<T> {
  const app = getApp()
  const header: Record<string, string> = {
    'content-type': 'application/x-www-form-urlencoded',
  }
  if (opts.auth && app.globalData.token) {
    header['Authorization'] = app.globalData.token
  }
  return new Promise<T>((resolve, reject) => {
    wx.request({
      url: BASE_URL + opts.url,
      method: opts.method || 'GET',
      data: opts.data || {},
      header,
      success: (res: any) => {
        if (res.statusCode === 401) {
          app.globalData.token = ''
          wx.removeStorageSync('mini_token')
          reject({ code: 401, msg: '登录已过期' })
          return
        }
        resolve(res.data as T)
      },
      fail: (e: any) => reject({ code: -1, msg: (e && e.errMsg) || '网络异常' }),
    })
  })
}
