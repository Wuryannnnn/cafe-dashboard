import { useState } from 'react'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from '@tanstack/react-router'
import { Loader2, LogIn } from 'lucide-react'
import { toast } from 'sonner'
import { useAuthStore } from '@/stores/auth-store'
import { api } from '@/lib/api'
import { ROLE_NAMES, homeForRole } from '@/lib/permissions'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/password-input'

const formSchema = z.object({
  username: z.string().min(1, '请输入用户名'),
  password: z.string().min(1, '请输入密码'),
})

interface UserAuthFormProps extends React.HTMLAttributes<HTMLFormElement> {
  redirectTo?: string
}

export function UserAuthForm({
  className,
  redirectTo,
  ...props
}: UserAuthFormProps) {
  const [isLoading, setIsLoading] = useState(false)
  const navigate = useNavigate()
  const { auth } = useAuthStore()

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  })

  async function onSubmit(data: z.infer<typeof formSchema>) {
    setIsLoading(true)
    try {
      const res = await api.post<{
        code: number
        msg?: string
        data?: { username: string; name?: string; role: number }
      }>(
        '/api/admin/login',
        new URLSearchParams({ username: data.username, password: data.password })
      )
      if (res.data?.code === 0 && res.data.data) {
        const u = res.data.data
        // 真实凭据是后端下发的 HttpOnly cookie(admin_token); 这里只存展示用的用户信息
        auth.setUser({
          accountNo: u.username,
          email: u.username,
          name: u.name || u.username,
          role: [ROLE_NAMES[u.role] ?? String(u.role)],
          roleCode: u.role,
          exp: Date.now() + 12 * 60 * 60 * 1000,
        })
        auth.setAccessToken('cookie')
        toast.success(`欢迎回来, ${u.name || u.username}!`)
        // 收银员/制作员看不到数据看板, 按角色跳合适首页
        navigate({ to: redirectTo || homeForRole(u.role), replace: true })
      } else {
        toast.error(res.data?.msg || '用户名或密码错误')
      }
    } catch {
      toast.error('登录失败, 请检查网络后重试')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className={cn('grid gap-3', className)}
        {...props}
      >
        <FormField
          control={form.control}
          name='username'
          render={({ field }) => (
            <FormItem>
              <FormLabel>用户名</FormLabel>
              <FormControl>
                <Input placeholder='员工用户名' autoComplete='username' {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name='password'
          render={({ field }) => (
            <FormItem>
              <FormLabel>密码</FormLabel>
              <FormControl>
                <PasswordInput placeholder='********' autoComplete='current-password' {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button className='mt-2' disabled={isLoading}>
          {isLoading ? <Loader2 className='animate-spin' /> : <LogIn />}
          登录
        </Button>
      </form>
    </Form>
  )
}
