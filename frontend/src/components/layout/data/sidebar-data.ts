import {
  Coffee,
  LayoutDashboard,
  ScrollText,
  CreditCard,
  Cookie,
  Grid3x3,
  Megaphone,
  BarChart3,
  Tags,
  Sparkles,
  CalendarRange,
  Warehouse,
  ChefHat,
  TrendingUp,
  Package,
  MapPin,
  Users,
  Award,
  PiggyBank,
  Ticket,
  CircleDollarSign,
  Receipt,
  AlertTriangle,
  Banknote,
  Store,
  ToggleRight,
  StickyNote,
  Palette,
  Wallet2,
  Printer,
  UserCog,
  History,
} from 'lucide-react'
import { type SidebarData } from '../types'

export const sidebarData: SidebarData = {
  user: {
    name: '管理员',
    email: 'admin@cafe.local',
    avatar: '/avatars/shadcn.jpg',
  },
  teams: [
    {
      name: 'The Infinite Cafe',
      logo: Coffee,
      logoUrl: '/sell/images/logo.jpg',
      plan: 'Dining & Coffee',
    },
  ],
  navGroups: [
    {
      title: '运营',
      items: [
        { title: '数据看板', url: '/', icon: LayoutDashboard },
        { title: '订单', url: '/orders', icon: ScrollText },
        { title: '收银台', url: '/cashier', icon: CreditCard },
      ],
    },
    {
      title: '菜单',
      items: [
        { title: '商品列表', url: '/products', icon: Cookie },
        { title: '分类', url: '/categories', icon: Tags },
        { title: '菜品属性', url: '/addons', icon: Sparkles },
        { title: '时段菜单', url: '/timeslots', icon: CalendarRange },
      ],
    },
    {
      title: '库存',
      items: [
        { title: 'WMS 入口', url: '/wms', icon: Warehouse },
        { title: '菜品配方', url: '/recipes', icon: ChefHat },
        { title: '物料消耗', url: '/material-cost', icon: TrendingUp },
        { title: '商品库存', url: '/stock', icon: Package },
      ],
    },
    {
      title: '桌台',
      items: [
        { title: '区域', url: '/areas', icon: MapPin },
        { title: '桌台', url: '/tables', icon: Grid3x3 },
      ],
    },
    {
      title: '营销',
      items: [
        { title: '会员', url: '/members', icon: Users },
        { title: '会员等级', url: '/levels', icon: Award },
        { title: '充值方案', url: '/recharge-plans', icon: PiggyBank },
        { title: '优惠券', url: '/coupons', icon: Ticket },
        { title: '营销活动', url: '/promotions', icon: Megaphone },
      ],
    },
    {
      title: '报表',
      items: [
        { title: '营业报表', url: '/reports/sales', icon: BarChart3 },
        { title: '菜品报表', url: '/reports/products', icon: Cookie },
        { title: '收款报表', url: '/reports/payments', icon: CircleDollarSign },
      ],
    },
    {
      title: '财务',
      items: [
        { title: '收支管理', url: '/finance/expense', icon: Receipt },
        { title: '报损管理', url: '/finance/damage', icon: AlertTriangle },
        { title: '结算账户', url: '/finance/account', icon: Banknote },
      ],
    },
    {
      title: '设置',
      items: [
        { title: '门店信息', url: '/store-settings', icon: Store },
        { title: '业务开关', url: '/biz-switch', icon: ToggleRight },
        { title: '常用备注', url: '/remarks', icon: StickyNote },
        { title: '店铺装修', url: '/shop-design', icon: Palette },
        { title: '结账方式', url: '/payment-methods', icon: Wallet2 },
        { title: '打印机配置', url: '/printers', icon: Printer },
        { title: '员工管理', url: '/staff-mgmt', icon: UserCog },
        { title: '操作日志', url: '/op-log', icon: History },
      ],
    },
  ],
}
