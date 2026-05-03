import { createFileRoute } from '@tanstack/react-router'
import { H5StatusPage } from '@/features/h5/status-page'
import { z } from 'zod'

export const Route = createFileRoute('/order/status')({
  validateSearch: z.object({
    orderId: z.any().optional(),
    table: z.any().optional(),
  }).passthrough(),
  component: H5StatusPage,
})
