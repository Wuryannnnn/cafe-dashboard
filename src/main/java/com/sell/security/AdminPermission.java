package com.sell.security;

/**
 * 后台角色权限矩阵 (PRD 10).
 * 角色: 0=老板, 1=店长, 2=收银员, 3=制作员.
 *
 * - 老板:   全部功能.
 * - 店长:   除"财务敏感数据"(收支/报损/结算/财务总览) 与 员工管理/操作日志 外的全部功能.
 * - 收银员: 收银结账 + 订单 + 会员交易(全权); 桌台/菜单/会员等级/结账方式/备注/门店配置(只读).
 * - 制作员: 订单(查看/改状态全权); 菜单(只读, 用于制作页展示).
 *
 * 注意: 这是按 URL 前缀的模块级控制, 收银/制作员对参考数据是"只读(仅 GET)".
 * 与前端导航的可见性配套 (frontend/src/lib/permissions.ts), 但后端才是真正的强制点.
 */
public final class AdminPermission {

    private AdminPermission() {}

    /** 财务敏感数据: 仅老板. */
    private static final String[] FINANCE = {
            "/api/admin/finance-overview", "/api/admin/expenses", "/api/admin/expense-categories",
            "/api/admin/damages", "/api/admin/settle-accounts", "/seller/finance"
    };

    /** 员工管理 / 操作日志: 仅老板. */
    private static final String[] BOSS_ONLY = {
            "/api/admin/staff", "/seller/staff", "/api/admin/logs"
    };

    /** 收银员全权 (任意方法). */
    private static final String[] CASHIER_FULL = {
            "/api/admin/orders", "/seller/order", "/seller/cashier", "/api/admin/cashier",
            "/api/admin/members", "/seller/member",
            "/seller/printer", "/seller/printSettings", "/seller/qrcode"
    };

    /** 收银员只读 (仅 GET). */
    private static final String[] CASHIER_READ = {
            "/api/admin/tables", "/api/admin/areas",
            "/api/admin/products", "/api/admin/categories", "/api/admin/skus",
            "/api/admin/addons", "/api/admin/addon-groups",
            "/api/admin/member-levels", "/api/admin/payment-methods",
            "/api/admin/remarks", "/api/admin/shop-config"
    };

    /** 制作员全权 (订单及状态变更). */
    private static final String[] MAKER_FULL = {
            "/api/admin/orders", "/seller/order"
    };

    /** 制作员只读 (制作页展示菜品信息). */
    private static final String[] MAKER_READ = {
            "/api/admin/products", "/api/admin/categories", "/api/admin/skus",
            "/api/admin/addons", "/api/admin/addon-groups"
    };

    /** 判断指定角色能否访问 (method, path). path 已去除 context-path, 形如 /api/admin/orders. */
    public static boolean allowed(Integer role, String method, String path) {
        int r = role == null ? -1 : role;
        boolean get = "GET".equalsIgnoreCase(method);
        switch (r) {
            case 0: // 老板
                return true;
            case 1: // 店长
                return !matches(path, FINANCE) && !matches(path, BOSS_ONLY);
            case 2: // 收银员
                return matches(path, CASHIER_FULL) || (get && matches(path, CASHIER_READ));
            case 3: // 制作员
                return matches(path, MAKER_FULL) || (get && matches(path, MAKER_READ));
            default:
                return false;
        }
    }

    private static boolean matches(String path, String[] prefixes) {
        for (String p : prefixes) {
            if (path.equals(p) || path.startsWith(p + "/")) return true;
        }
        return false;
    }
}
