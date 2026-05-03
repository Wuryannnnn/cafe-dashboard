import { createFileRoute } from '@tanstack/react-router'
import { SettleAccountsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/finance/account')({ component: SettleAccountsPage })
