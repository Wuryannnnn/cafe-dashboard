import { createFileRoute } from '@tanstack/react-router'
import { Cashier } from '@/features/cashier'

export const Route = createFileRoute('/_authenticated/cashier/')({
  component: Cashier,
})
