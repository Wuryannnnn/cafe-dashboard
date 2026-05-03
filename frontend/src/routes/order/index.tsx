import { createFileRoute } from '@tanstack/react-router'
import { H5OrderPage } from '@/features/h5/order-page'
import { z } from 'zod'

export const Route = createFileRoute('/order/')({
  // 用 z.any() 兜底, 因为 TanStack Router 会把 ?customer=1 自动转成 number 1, 跟 z.string() 冲突
  validateSearch: z.object({
    table: z.any().optional(),
    customer: z.any().optional(),
  }).passthrough(),
  component: H5OrderPage,
})
