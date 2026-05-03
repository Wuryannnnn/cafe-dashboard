import { createFileRoute } from '@tanstack/react-router'
import { MaterialCost } from '@/features/wms-pages'

export const Route = createFileRoute('/_authenticated/material-cost/')({ component: MaterialCost })
