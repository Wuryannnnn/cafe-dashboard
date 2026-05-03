import { useEffect, useState } from 'react'
import { useSearch } from '@tanstack/react-router'
import { api } from '@/lib/api'

declare global {
  interface Window {
    WeixinJSBridge: any
  }
}

type PaySign = {
  payType: 'wechat' | 'alipay'
  appId?: string
  timeStamp?: string
  nonceStr?: string
  packAge?: string
  paySign?: string
  returnUrl: string
  mwebUrl?: string
  codeUrl?: string
  payUri?: string
  body?: string
}

export function H5PayPage() {
  const search = useSearch({ from: '/pay' }) as { orderId?: string; returnUrl?: string; payType?: string }
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<'loading' | 'invoking' | 'redirecting'>('loading')

  useEffect(() => {
    if (!search.orderId || !search.returnUrl) {
      setError('参数缺失 (orderId / returnUrl)')
      return
    }
    ;(async () => {
      try {
        const r = await api.get<PaySign & { code?: number; msg?: string }>('/pay/create', {
          params: {
            orderId: search.orderId,
            returnUrl: search.returnUrl,
            ...(search.payType !== undefined ? { payType: search.payType } : {}),
          },
        })
        const sign = r.data
        // 后端业务错误 (订单不存在 / 已支付 etc.) 返回 {code, msg} 而无 payType
        if (!sign.payType) {
          const msg = sign.msg || '支付发起失败 (后端无返回)'
          setError(`${msg} (orderId=${search.orderId})`)
          return
        }
        if (sign.payType === 'alipay') {
          // 支付宝 WAP: 跳转地址优先级 mwebUrl → payUri; body 是要 auto-submit 的表单 HTML
          const url = sign.mwebUrl || sign.payUri
          if (url) {
            setStatus('redirecting')
            location.href = url
            return
          }
          if (sign.body) {
            setStatus('redirecting')
            // 写入 form HTML 让浏览器自动提交到支付宝
            document.open()
            document.write(sign.body)
            document.close()
            return
          }
          setError('支付宝未返回跳转地址')
          return
        }
        // 微信 JSAPI
        invokeWxPay(sign, () => {
          setStatus('redirecting')
          location.href = sign.returnUrl
        }, (msg) => setError(msg))
        setStatus('invoking')
      } catch (e: any) {
        setError('支付发起失败：' + (e?.response?.data?.message ?? e?.message ?? ''))
      }
    })()
  }, [search.orderId, search.returnUrl])

  return (
    <div className='flex min-h-dvh items-center justify-center bg-stone-50 p-6 text-center'>
      <div>
        {error ? (
          <>
            <p className='text-rose-600 font-medium'>支付失败</p>
            <p className='text-stone-500 mt-2 text-sm'>{error}</p>
            {search.returnUrl && (
              <a href={search.returnUrl} className='text-primary mt-6 inline-block text-sm hover:underline'>
                返回订单
              </a>
            )}
          </>
        ) : (
          <>
            <p className='text-stone-700 font-medium'>
              {status === 'loading' && '正在唤起支付…'}
              {status === 'invoking' && '请在微信中完成支付…'}
              {status === 'redirecting' && '跳转中…'}
            </p>
            <p className='text-stone-400 mt-2 text-xs'>请保持网络畅通</p>
          </>
        )}
      </div>
    </div>
  )
}

function invokeWxPay(sign: PaySign, onSuccess: () => void, onError: (msg: string) => void) {
  const args = {
    appId: sign.appId,
    timeStamp: sign.timeStamp,
    nonceStr: sign.nonceStr,
    package: sign.packAge,
    signType: 'MD5',
    paySign: sign.paySign,
  }
  const ready = () => {
    window.WeixinJSBridge.invoke('getBrandWCPayRequest', args, (res: any) => {
      if (res?.err_msg === 'get_brand_wcpay_request:ok') {
        onSuccess()
      } else if (res?.err_msg === 'get_brand_wcpay_request:cancel') {
        onError('已取消支付')
      } else {
        onError('支付未成功 (' + (res?.err_msg ?? 'unknown') + ')')
      }
    })
  }
  if (typeof window.WeixinJSBridge === 'undefined') {
    document.addEventListener('WeixinJSBridgeReady', ready, false)
  } else {
    ready()
  }
}
