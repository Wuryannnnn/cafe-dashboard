import { createFileRoute } from '@tanstack/react-router'
import { PaymentMethodsPage } from '@/features/list-pages'

export const Route = createFileRoute('/_authenticated/payment-methods')({ component: PaymentMethodsPage })
