import { createFileRoute } from '@tanstack/react-router'
import { ExpensesPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/finance/expense')({ component: ExpensesPage })
