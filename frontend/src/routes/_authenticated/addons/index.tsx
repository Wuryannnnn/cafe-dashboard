import { createFileRoute } from '@tanstack/react-router'
import { AddonsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/addons/')({ component: AddonsPage })
