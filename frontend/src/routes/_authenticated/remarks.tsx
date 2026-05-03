import { createFileRoute } from '@tanstack/react-router'
import { RemarksPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/remarks')({ component: RemarksPage })
