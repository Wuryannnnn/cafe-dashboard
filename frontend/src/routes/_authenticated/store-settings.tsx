import { createFileRoute } from '@tanstack/react-router'
import { StoreSettings } from '@/features/settings-pages'

export const Route = createFileRoute('/_authenticated/store-settings')({ component: StoreSettings })
