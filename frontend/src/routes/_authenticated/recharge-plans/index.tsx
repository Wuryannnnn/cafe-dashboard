import { createFileRoute } from '@tanstack/react-router'
import { RechargePlansPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/recharge-plans/')({ component: RechargePlansPage })
