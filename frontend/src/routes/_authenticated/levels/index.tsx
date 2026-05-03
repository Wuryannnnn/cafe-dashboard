import { createFileRoute } from '@tanstack/react-router'
import { MemberLevelsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/levels/')({ component: MemberLevelsPage })
