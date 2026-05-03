import { useEffect, useState, type ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { ImageUpload } from '@/components/image-upload'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { api } from '@/lib/api'
import { toast } from 'sonner'

export type FieldDef = {
  key: string
  label: string
  type?: 'text' | 'number' | 'textarea' | 'select' | 'switch' | 'color' | 'image'
  options?: { label: string; value: any }[]
  placeholder?: string
  required?: boolean
  /** When type=number step */
  step?: string
  hidden?: boolean
}

type Props = {
  /** dialog 标题 */
  title: string
  /** 后端 POST 提交地址 */
  postUrl: string
  /** 字段定义 */
  fields: FieldDef[]
  /** 初始数据（编辑时提供） */
  initial?: Record<string, any>
  /** trigger 元素，控制弹窗打开 */
  trigger: ReactNode
  /** 保存成功后回调 */
  onSaved?: () => void
  /** content type；默认 form-data，老接口大多用此格式 */
  contentType?: 'form' | 'json'
}

export function CrudDialog({
  title,
  postUrl,
  fields,
  initial,
  trigger,
  onSaved,
  contentType = 'form',
}: Props) {
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<Record<string, any>>({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) setForm(initial ?? {})
  }, [open, initial])

  const submit = async () => {
    for (const f of fields) {
      if (f.required && (form[f.key] === undefined || form[f.key] === '' || form[f.key] === null)) {
        toast.error(`「${f.label}」不能为空`)
        return
      }
    }
    setSaving(true)
    try {
      let r
      if (contentType === 'json') {
        r = await api.post<{ code?: number; msg?: string }>(postUrl, form, {
          headers: { 'Content-Type': 'application/json' },
        })
      } else {
        const fd = new FormData()
        for (const [k, v] of Object.entries(form)) {
          if (v === undefined || v === null) continue
          fd.append(k, typeof v === 'boolean' ? String(v) : String(v))
        }
        r = await api.post<{ code?: number; msg?: string }>(postUrl, fd)
      }
      if (r.data && r.data.code === 1) {
        toast.error(r.data.msg || '保存失败')
        return
      }
      toast.success('已保存')
      setOpen(false)
      onSaved?.()
    } catch (e: any) {
      console.error(e)
      const msg = e?.response?.status === 404
        ? '接口不存在 (后端可能未重启)'
        : e?.response?.data?.msg ?? e?.message ?? ''
      toast.error('保存失败：' + msg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent className='max-w-md'>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className='space-y-3'>
          {fields.filter((f) => !f.hidden).map((f) => (
            <FieldInput key={f.key} field={f} value={form[f.key]} onChange={(v) => setForm({ ...form, [f.key]: v })} />
          ))}
        </div>
        <DialogFooter>
          <Button variant='outline' onClick={() => setOpen(false)} disabled={saving}>
            取消
          </Button>
          <Button onClick={submit} disabled={saving}>
            {saving ? '保存中…' : '保存'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function FieldInput({
  field: f,
  value,
  onChange,
}: {
  field: FieldDef
  value: any
  onChange: (v: any) => void
}) {
  return (
    <div>
      <Label className='mb-1.5 block text-sm font-medium'>
        {f.label}
        {f.required && <span className='text-rose-600 ms-1'>*</span>}
      </Label>
      {f.type === 'textarea' ? (
        <textarea
          className='border-input bg-background w-full rounded-md border px-3 py-2 text-sm'
          value={value ?? ''}
          rows={3}
          onChange={(e) => onChange(e.target.value)}
          placeholder={f.placeholder}
        />
      ) : f.type === 'select' ? (
        <select
          className='border-input bg-background w-full rounded-md border px-3 py-2 text-sm'
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value=''>请选择</option>
          {f.options?.map((o, i) => (
            <option key={i} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      ) : f.type === 'switch' ? (
        <div className='flex items-center gap-2'>
          <Switch
            checked={value === true || value === 'true'}
            onCheckedChange={(v) => onChange(v)}
          />
          <span className='text-muted-foreground text-xs'>{value === true || value === 'true' ? '启用' : '关闭'}</span>
        </div>
      ) : f.type === 'color' ? (
        <div className='flex items-center gap-2'>
          <input
            type='color'
            value={value ?? '#6b4226'}
            onChange={(e) => onChange(e.target.value)}
            className='h-9 w-12 cursor-pointer rounded border'
          />
          <Input value={value ?? ''} onChange={(e) => onChange(e.target.value)} className='font-mono w-32' />
        </div>
      ) : f.type === 'image' ? (
        <ImageUpload value={value} onChange={onChange} />
      ) : (
        <Input
          type={f.type === 'number' ? 'number' : 'text'}
          step={f.step}
          value={value ?? ''}
          onChange={(e) => onChange(f.type === 'number' && e.target.value !== '' ? Number(e.target.value) : e.target.value)}
          placeholder={f.placeholder}
        />
      )}
    </div>
  )
}

// 删除按钮请使用 <ConfirmDeleteButton /> (自带 AlertDialog), 不要再手写 confirm()
