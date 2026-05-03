import { createFileRoute } from '@tanstack/react-router'
import { PrintersPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/printers')({ component: PrintersPage })
