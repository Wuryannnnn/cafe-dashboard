import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'
import { H5PayPage } from '@/features/h5/pay-page'

export const Route = createFileRoute('/pay')({
  // 用 z.any() 兜底, 因为 TanStack Router 会把数字字符串转成 number, 跟 z.string() 冲突
  validateSearch: z.object({
    orderId: z.any().optional(),
    returnUrl: z.any().optional(),
    payType: z.any().optional(),
  }).passthrough(),
  component: H5PayPage,
})
