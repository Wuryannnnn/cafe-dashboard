import { useRef, useState } from 'react'
import { Upload, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'
import { toast } from 'sonner'

type Props = {
  value?: string
  onChange: (url: string) => void
  size?: number
  /** 提示文字; 不传则使用默认 */
  hint?: string
}

export function ImageUpload({ value, onChange, size = 96, hint }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)

  const onPick = async (file: File) => {
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      toast.error('图片不能超过 5MB')
      return
    }
    setUploading(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const r = await api.post<{ ok: boolean; url?: string; msg?: string }>(
        '/api/upload/image',
        fd,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      if (r.data.ok && r.data.url) {
        onChange(r.data.url)
        toast.success('上传成功')
      } else {
        toast.error('上传失败：' + (r.data.msg ?? ''))
      }
    } catch (e: any) {
      console.error(e)
      toast.error('上传失败')
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div className='flex items-start gap-3'>
      <div
        className='bg-muted flex shrink-0 items-center justify-center overflow-hidden rounded-md border'
        style={{ width: size, height: size }}
      >
        {value ? (
          <img
            src={value}
            alt=''
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none' }}
          />
        ) : (
          <Upload className='text-muted-foreground size-6' />
        )}
      </div>
      <div className='flex flex-col gap-2'>
        <input
          ref={inputRef}
          type='file'
          accept='image/*'
          className='hidden'
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) onPick(f)
          }}
        />
        <div className='flex gap-2'>
          <Button
            type='button'
            variant='outline'
            size='sm'
            disabled={uploading}
            onClick={() => inputRef.current?.click()}
          >
            <Upload className='size-3.5' />
            {uploading ? '上传中…' : value ? '替换图片' : '从电脑上传'}
          </Button>
          {value && (
            <Button
              type='button'
              variant='ghost'
              size='sm'
              onClick={() => onChange('')}
            >
              <X className='size-3.5' /> 移除
            </Button>
          )}
        </div>
        <p className='text-muted-foreground text-xs'>
          {hint ?? '支持 png / jpg / gif / webp，单张 ≤ 5MB'}
        </p>
      </div>
    </div>
  )
}
