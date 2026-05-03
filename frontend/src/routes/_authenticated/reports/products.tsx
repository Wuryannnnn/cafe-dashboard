import { createFileRoute } from '@tanstack/react-router'
import { ReportsProducts } from '@/features/reports'

export const Route = createFileRoute('/_authenticated/reports/products')({ component: ReportsProducts })
