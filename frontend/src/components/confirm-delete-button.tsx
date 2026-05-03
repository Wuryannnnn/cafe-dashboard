import { useState, type ReactNode } from 'react'
import { Trash2 } from 'lucide-react'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { api } from '@/lib/api'
import { toast } from 'sonner'

type Props = {
  /** 删除接口 URL (GET) */
  url: string
  /** 删除完成后的回调 (一般是 invalidateQueries) */
  onDone: () => void
  /** 弹窗标题, 默认「确认删除?」 */
  title?: string
  /** 弹窗描述, 一般写明「此操作不可恢复」之类 */
  description?: ReactNode
  /** 自定义触发器, 不传则用默认红色「删除」文本按钮 */
  trigger?: ReactNode
  /** 触发器外层 className, 仅在使用默认触发器时生效 */
  className?: string
}

export function ConfirmDeleteButton({
  url,
  onDone,
  title = '确认删除?',
  description = '此操作不可恢复',
  trigger,
  className,
}: Props) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const handleConfirm = async (e: React.MouseEvent) => {
    e.preventDefault()
    setBusy(true)
    try {
      const r = await api.get<{ code?: number; msg?: string }>(url)
      if (r.data && r.data.code === 1) {
        toast.error(r.data.msg || '删除失败')
        return
      }
      toast.success('已删除')
      setOpen(false)
      onDone()
    } catch (e: any) {
      console.error(e)
      const msg = e?.response?.status === 404
        ? '接口不存在 (后端可能未重启)'
        : e?.response?.data?.msg || e?.message || '请重试'
      toast.error('删除失败：' + msg)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <button
        type='button'
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setOpen(true)
        }}
        className={className ?? 'text-rose-600 text-xs hover:underline'}
      >
        {trigger ?? (
          <>
            <Trash2 className='inline size-3.5' /> 删除
          </>
        )}
      </button>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={busy}>取消</AlertDialogCancel>
          <AlertDialogAction
            disabled={busy}
            onClick={handleConfirm}
            className='bg-rose-600 hover:bg-rose-700 focus-visible:ring-rose-600'
          >
            {busy ? '删除中…' : '确认删除'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
