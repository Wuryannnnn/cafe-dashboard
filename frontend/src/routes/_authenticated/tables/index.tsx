import { createFileRoute } from '@tanstack/react-router'
import { TablesPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/tables/')({ component: TablesPage })
