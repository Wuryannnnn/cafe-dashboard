import { createFileRoute } from '@tanstack/react-router'
import { CouponsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/coupons/')({ component: CouponsPage })
