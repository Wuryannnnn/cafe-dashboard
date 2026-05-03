import { useRouter } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'

function isCustomerPath() {
  if (typeof window === 'undefined') return false
  const p = window.location.pathname
  return p.startsWith('/order') || p.startsWith('/pay')
}
function homeForCurrentMode() {
  return isCustomerPath() ? '/order' : '/'
}

export function ForbiddenError() {
  const { history } = useRouter()
  const isCustomer = isCustomerPath()
  return (
    <div className='h-svh'>
      <div className='m-auto flex h-full w-full flex-col items-center justify-center gap-2'>
        <h1 className='text-[7rem] leading-tight font-bold'>403</h1>
        <span className='font-medium'>无权访问</span>
        <p className='text-center text-muted-foreground'>
          您没有访问此页面的权限
        </p>
        <div className='mt-6 flex gap-4'>
          <Button variant='outline' onClick={() => history.go(-1)}>
            返回上一页
          </Button>
          <Button onClick={() => { window.location.href = homeForCurrentMode() }}>
            {isCustomer ? '回到点餐' : '回到首页'}
          </Button>
        </div>
      </div>
    </div>
  )
}
