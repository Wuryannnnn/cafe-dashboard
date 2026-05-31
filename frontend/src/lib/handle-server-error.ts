import { AxiosError } from 'axios'
import { toast } from 'sonner'

export function handleServerError(error: unknown) {
  if (import.meta.env.DEV) {
    // eslint-disable-next-line no-console
    console.log(error)
  }

  let errMsg = 'Something went wrong!'

  if (
    error &&
    typeof error === 'object' &&
    'status' in error &&
    Number(error.status) === 204
  ) {
    errMsg = 'No content.'
  }

  if (error instanceof AxiosError) {
    // 后端统一返回 ResultVO { code, msg, data }; 兼容 message/title 字段
    const data = error.response?.data as
      | { msg?: string; message?: string; title?: string }
      | undefined
    const serverMsg = data?.msg ?? data?.message ?? data?.title
    if (typeof serverMsg === 'string' && serverMsg.length > 0) {
      errMsg = serverMsg
    }
  }

  toast.error(errMsg)
}
