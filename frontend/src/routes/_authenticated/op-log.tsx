import { createFileRoute } from '@tanstack/react-router'
import { LogsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/op-log')({ component: LogsPage })
