package com.sell.repository;

import com.sell.dataobject.OrderMaster;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface OrderMasterRepository extends JpaRepository<OrderMaster, String> {

    Page<OrderMaster> findByBuyerOpenid(String buyerOpenid, Pageable pageable);

    /**
     * 悲观行锁读取订单, 用于 paid/cancel/refund 串行化:
     * 防止微信/支付宝并发回调重复置为已支付, 以及"取消+退款"并发各自返还一次库存(库存翻倍虚高).
     * 拿到锁后必须以返回实体的最新状态为准做判断, 不能信任调用方传入的旧 DTO 状态.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderMaster o WHERE o.orderId = :orderId")
    Optional<OrderMaster> findByOrderIdForUpdate(@Param("orderId") String orderId);

    /**
     * 订单列表筛选: 按状态 + 关键字(订单号/取餐号/桌号/手机号 模糊匹配).
     * status / kw 为 null 时该条件不生效, 排序与分页由 Pageable 提供.
     */
    @Query("SELECT o FROM OrderMaster o WHERE "
            + "(:status IS NULL OR o.orderStatus = :status) AND "
            + "(:kw IS NULL OR o.orderId LIKE CONCAT('%', :kw, '%') "
            + "OR o.pickupNumber LIKE CONCAT('%', :kw, '%') "
            + "OR o.tableNumber LIKE CONCAT('%', :kw, '%') "
            + "OR o.buyerPhone LIKE CONCAT('%', :kw, '%'))")
    Page<OrderMaster> search(@Param("status") Integer status, @Param("kw") String kw, Pageable pageable);

    // 营收口径统一为"已支付"(payStatus=1): 自动排除未支付(0)与已退款(2, 退款后置为该状态),
    // 与 sumByPayType 一致, 使总额卡片与每日/品类明细可对账.

    /** 指定时间范围内的已支付订单 */
    @Query("SELECT o FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.payStatus = 1")
    List<OrderMaster> findByDateRange(@Param("start") Date start, @Param("end") Date end);

    /** 统计指定时间范围内的已支付订单数 */
    @Query("SELECT COUNT(o) FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.payStatus = 1")
    Long countByDateRange(@Param("start") Date start, @Param("end") Date end);

    /** 统计指定时间范围内的已支付总金额 */
    @Query("SELECT COALESCE(SUM(o.orderAmount), 0) FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.payStatus = 1")
    java.math.BigDecimal sumAmountByDateRange(@Param("start") Date start, @Param("end") Date end);

    /** 按支付方式统计 */
    @Query("SELECT o.payType, COUNT(o), COALESCE(SUM(o.orderAmount), 0) FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end AND o.orderStatus <> 4 AND o.payStatus = 1 GROUP BY o.payType")
    List<Object[]> sumByPayType(@Param("start") Date start, @Param("end") Date end);

    /** 按状态查询占用中的桌台订单 (排除 tableId 为空的). */
    List<OrderMaster> findByOrderStatusInAndTableIdIsNotNull(List<Integer> orderStatuses);

    /** 某日所有订单的取餐号 (用于重启后恢复当日取餐号计数器, 避免重号). */
    @Query("SELECT o.pickupNumber FROM OrderMaster o WHERE o.createTime >= :start AND o.createTime < :end")
    List<String> findPickupNumbersByDateRange(@Param("start") Date start, @Param("end") Date end);
}
