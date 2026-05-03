import { createFileRoute } from '@tanstack/react-router'
import { AreasPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/areas/')({ component: AreasPage })
