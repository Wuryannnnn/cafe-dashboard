import { createFileRoute } from '@tanstack/react-router'
import { ShopDesign } from '@/features/settings-pages'

export const Route = createFileRoute('/_authenticated/shop-design')({ component: ShopDesign })
