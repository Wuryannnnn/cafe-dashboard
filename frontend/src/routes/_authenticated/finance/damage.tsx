import { createFileRoute } from '@tanstack/react-router'
import { DamagesPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/finance/damage')({ component: DamagesPage })
