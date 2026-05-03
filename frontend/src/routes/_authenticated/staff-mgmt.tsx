import { createFileRoute } from '@tanstack/react-router'
import { StaffPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/staff-mgmt')({ component: StaffPage })
