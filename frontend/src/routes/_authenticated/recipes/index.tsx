import { createFileRoute } from '@tanstack/react-router'
import { RecipesPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/recipes/')({ component: RecipesPage })
