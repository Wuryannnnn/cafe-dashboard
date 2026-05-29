import axios from 'axios'

export const api = axios.create({
  baseURL: '/sell',
  withCredentials: true,
  timeout: 15000,
})

export type DashboardData = {
  todayRevenue: number
  todayOrders: number
  avgPrice: number
  yestRevenue: number
  yestOrders: number
  trendLabels: string[]
  trendRevenue: number[]
  trendOrders: number[]
  payTypes: { name: string; count: number; amount: number }[]
  topProducts: { rank: number; name: string; qty: number }[]
  statusDist: Record<string, number>
}

export type WmsAlert = {
  configured: boolean
  alerts?: { id: number; itemName?: string; skuName?: string; quantity: number }[]
}

export type WmsOverview = {
  configured: boolean
  itemCount?: number
  alertCount?: number
  /** 待重试的出库数 (本地补偿队列). */
  pendingCount?: number
  /** 重试彻底失败、需人工介入的出库数 (本地与 WMS 已劈叉). */
  failedCount?: number
}

export type RecentShipments = {
  configured: boolean
  items?: { createTime: string; bizOrderNo: string }[]
}
