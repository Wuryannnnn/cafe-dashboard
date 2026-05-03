import { createFileRoute } from '@tanstack/react-router'
import { MembersPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/members/')({ component: MembersPage })
