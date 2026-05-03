import { createFileRoute } from '@tanstack/react-router'
import { CategoriesPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/categories/')({ component: CategoriesPage })
