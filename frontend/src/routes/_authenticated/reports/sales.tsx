import { createFileRoute } from '@tanstack/react-router'
import { ReportsSales } from '@/features/reports'

export const Route = createFileRoute('/_authenticated/reports/sales')({ component: ReportsSales })
