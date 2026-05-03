import { createFileRoute } from '@tanstack/react-router'
import { TimeslotsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/timeslots/')({ component: TimeslotsPage })
