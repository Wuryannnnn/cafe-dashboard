import { createFileRoute } from '@tanstack/react-router'
import { ReportsPayments } from '@/features/reports'

export const Route = createFileRoute('/_authenticated/reports/payments')({ component: ReportsPayments })
