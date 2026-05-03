import { createFileRoute } from '@tanstack/react-router'
import { StockPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/stock/')({ component: StockPage })
