import { describe, it, expect } from 'vitest'
import { navAllowed, homeForRole, roleName } from './permissions'

describe('roleName', () => {
  it('maps role codes to names', () => {
    expect(roleName(0)).toBe('老板')
    expect(roleName(1)).toBe('店长')
    expect(roleName(2)).toBe('收银员')
    expect(roleName(3)).toBe('制作员')
  })
  it('handles null / undefined / unknown', () => {
    expect(roleName(null)).toBe('')
    expect(roleName(undefined)).toBe('')
    expect(roleName(9)).toBe('9')
  })
})

describe('homeForRole', () => {
  it('boss/manager -> dashboard', () => {
    expect(homeForRole(0)).toBe('/')
    expect(homeForRole(1)).toBe('/')
  })
  it('cashier -> /cashier', () => expect(homeForRole(2)).toBe('/cashier'))
  it('maker -> /orders', () => expect(homeForRole(3)).toBe('/orders'))
  it('null -> dashboard (fallback)', () => expect(homeForRole(null)).toBe('/'))
})

describe('navAllowed', () => {
  it('denies when role is null/undefined', () => {
    expect(navAllowed('/orders', null)).toBe(false)
    expect(navAllowed('/', undefined)).toBe(false)
  })

  it('boss(0) is allowed on listed + default routes', () => {
    expect(navAllowed('/', 0)).toBe(true)
    expect(navAllowed('/finance/expense', 0)).toBe(true)
    expect(navAllowed('/staff-mgmt', 0)).toBe(true)
    expect(navAllowed('/op-log', 0)).toBe(true)
    expect(navAllowed('/products', 0)).toBe(true) // 默认 [0,1]
  })

  it('manager(1): finance/staff/log blocked, rest allowed', () => {
    expect(navAllowed('/finance/expense', 1)).toBe(false)
    expect(navAllowed('/staff-mgmt', 1)).toBe(false)
    expect(navAllowed('/op-log', 1)).toBe(false)
    expect(navAllowed('/reports/sales', 1)).toBe(true)
    expect(navAllowed('/products', 1)).toBe(true)
    expect(navAllowed('/', 1)).toBe(true)
  })

  it('cashier(2): only orders/cashier/tables/areas', () => {
    expect(navAllowed('/orders', 2)).toBe(true)
    expect(navAllowed('/cashier', 2)).toBe(true)
    expect(navAllowed('/tables', 2)).toBe(true)
    expect(navAllowed('/areas', 2)).toBe(true)
    expect(navAllowed('/', 2)).toBe(false) // 看板不对收银员开放
    expect(navAllowed('/products', 2)).toBe(false)
    expect(navAllowed('/finance/expense', 2)).toBe(false)
    expect(navAllowed('/reports/sales', 2)).toBe(false)
  })

  it('maker(3): only orders', () => {
    expect(navAllowed('/orders', 3)).toBe(true)
    expect(navAllowed('/cashier', 3)).toBe(false)
    expect(navAllowed('/tables', 3)).toBe(false)
    expect(navAllowed('/', 3)).toBe(false)
    expect(navAllowed('/products', 3)).toBe(false)
  })

  it('matches sub-paths by prefix', () => {
    expect(navAllowed('/orders/123', 2)).toBe(true) // /orders 前缀
    expect(navAllowed('/finance/account', 1)).toBe(false) // /finance 前缀
  })
})
