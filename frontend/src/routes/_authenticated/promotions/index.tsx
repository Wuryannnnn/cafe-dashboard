import { createFileRoute } from '@tanstack/react-router'
import { PromotionsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/promotions/')({ component: PromotionsPage })
