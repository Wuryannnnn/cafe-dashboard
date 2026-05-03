import { useRouter } from '@tanstack/react-router'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

type GeneralErrorProps = React.HTMLAttributes<HTMLDivElement> & {
  minimal?: boolean
}

/** 路径以 /order 或 /pay 开头视作顾客 H5, 跳点餐页; 否则跳后台首页 */
function isCustomerPath() {
  if (typeof window === 'undefined') return false
  const p = window.location.pathname
  return p.startsWith('/order') || p.startsWith('/pay')
}
function homeForCurrentMode() {
  return isCustomerPath() ? '/order' : '/'
}

export function GeneralError({
  className,
  minimal = false,
}: GeneralErrorProps) {
  const { history } = useRouter()
  const isCustomer = isCustomerPath()
  return (
    <div className={cn('h-svh w-full', className)}>
      <div className='m-auto flex h-full w-full flex-col items-center justify-center gap-2'>
        {!minimal && (
          <h1 className='text-[7rem] leading-tight font-bold'>出错了</h1>
        )}
        <span className='font-medium'>系统繁忙，请稍后重试</span>
        <p className='text-center text-muted-foreground'>
          抱歉给您带来不便 <br /> 如多次出现请联系店员
        </p>
        {!minimal && (
          <div className='mt-6 flex gap-4'>
            <Button variant='outline' onClick={() => history.go(-1)}>
              返回上一页
            </Button>
            <Button onClick={() => { window.location.href = homeForCurrentMode() }}>
              {isCustomer ? '回到点餐' : '回到首页'}
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
