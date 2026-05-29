package com.sell.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 角色权限矩阵单元测试 (纯逻辑, 不依赖 Spring/DB).
 * 角色: 0老板 1店长 2收银员 3制作员.
 */
public class AdminPermissionTest {

    // ===== 老板(0): 全部放行 =====
    @Test
    public void boss_allowsEverything() {
        assertTrue(AdminPermission.allowed(0, "GET", "/api/admin/finance-overview"));
        assertTrue(AdminPermission.allowed(0, "POST", "/seller/finance/expense/save"));
        assertTrue(AdminPermission.allowed(0, "GET", "/api/admin/staff"));
        assertTrue(AdminPermission.allowed(0, "GET", "/api/admin/logs"));
        assertTrue(AdminPermission.allowed(0, "POST", "/seller/product/save"));
        assertTrue(AdminPermission.allowed(0, "DELETE", "/api/admin/anything-else"));
    }

    // ===== 店长(1): 除财务/员工/日志外放行 =====
    @Test
    public void manager_blockedFromFinanceStaffLogs() {
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/finance-overview"));
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/expenses"));
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/expense-categories"));
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/damages"));
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/settle-accounts"));
        assertFalse(AdminPermission.allowed(1, "POST", "/seller/finance/expense/save"));
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/staff"));
        assertFalse(AdminPermission.allowed(1, "POST", "/seller/staff/save"));
        assertFalse(AdminPermission.allowed(1, "GET", "/api/admin/logs"));
    }

    @Test
    public void manager_allowedElsewhere() {
        assertTrue(AdminPermission.allowed(1, "GET", "/api/admin/reports/sales"));
        assertTrue(AdminPermission.allowed(1, "POST", "/seller/product/save"));
        assertTrue(AdminPermission.allowed(1, "GET", "/api/admin/members"));
        assertTrue(AdminPermission.allowed(1, "GET", "/api/admin/orders"));
        assertTrue(AdminPermission.allowed(1, "POST", "/seller/coupon/save"));
    }

    // ===== 收银员(2): 订单/收银/会员全权; 桌台/菜单等只读; 财务/报表/员工拒绝 =====
    @Test
    public void cashier_fullOnOrdersCashierMembers() {
        assertTrue(AdminPermission.allowed(2, "POST", "/seller/order/finish"));
        assertTrue(AdminPermission.allowed(2, "GET", "/api/admin/orders/pending"));
        assertTrue(AdminPermission.allowed(2, "POST", "/seller/cashier/offline"));
        assertTrue(AdminPermission.allowed(2, "POST", "/seller/member/recharge"));
        assertTrue(AdminPermission.allowed(2, "POST", "/api/admin/members"));
    }

    @Test
    public void cashier_readonlyReferenceData() {
        assertTrue(AdminPermission.allowed(2, "GET", "/api/admin/tables"));
        assertTrue(AdminPermission.allowed(2, "GET", "/api/admin/products"));
        assertTrue(AdminPermission.allowed(2, "GET", "/api/admin/payment-methods"));
        // 写操作被拒绝(只读)
        assertFalse(AdminPermission.allowed(2, "POST", "/seller/product/save"));
        assertFalse(AdminPermission.allowed(2, "POST", "/seller/table/save"));
    }

    @Test
    public void cashier_blockedFromFinanceReportsStaffMarketing() {
        assertFalse(AdminPermission.allowed(2, "GET", "/api/admin/finance-overview"));
        assertFalse(AdminPermission.allowed(2, "GET", "/api/admin/reports/sales"));
        assertFalse(AdminPermission.allowed(2, "GET", "/api/admin/staff"));
        assertFalse(AdminPermission.allowed(2, "GET", "/api/admin/promotions"));
    }

    // ===== 制作员(3): 订单全权; 菜单只读; 其余拒绝 =====
    @Test
    public void maker_ordersFullAndMenuReadOnly() {
        assertTrue(AdminPermission.allowed(3, "POST", "/seller/order/finish"));
        assertTrue(AdminPermission.allowed(3, "GET", "/api/admin/orders"));
        assertTrue(AdminPermission.allowed(3, "GET", "/api/admin/products"));
        assertFalse(AdminPermission.allowed(3, "GET", "/seller/cashier/list"));
        assertFalse(AdminPermission.allowed(3, "GET", "/api/admin/members"));
        assertFalse(AdminPermission.allowed(3, "GET", "/api/admin/finance-overview"));
    }

    // ===== null / 未知角色: 全部拒绝 =====
    @Test
    public void nullOrUnknownRole_deniedEverywhere() {
        assertFalse(AdminPermission.allowed(null, "GET", "/api/admin/orders"));
        assertFalse(AdminPermission.allowed(9, "GET", "/api/admin/orders"));
    }

    // ===== 前缀边界: /members 不应误匹配 /member-levels =====
    @Test
    public void prefixBoundary_membersVsMemberLevels() {
        // members 对收银员是全权(POST 可); member-levels 只读(POST 拒, GET 可)
        assertTrue(AdminPermission.allowed(2, "POST", "/api/admin/members"));
        assertFalse(AdminPermission.allowed(2, "POST", "/api/admin/member-levels"));
        assertTrue(AdminPermission.allowed(2, "GET", "/api/admin/member-levels"));
    }
}
