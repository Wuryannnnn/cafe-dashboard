package com.sell.repository;

import com.sell.dataobject.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    Member findByPhone(String phone);

    Member findByOpenid(String openid);

    Page<Member> findByPhoneContainingOrNicknameContaining(String phone, String nickname, Pageable pageable);

    /** 按等级查会员 (定向发券: 指定等级). */
    List<Member> findByLevelId(Integer levelId);

    /** 按标签模糊匹配会员 (定向发券: 指定标签). tags 为逗号分隔串, 用 LIKE 子串匹配. */
    @Query("SELECT m FROM Member m WHERE m.tags LIKE CONCAT('%', :tag, '%')")
    List<Member> findByTagLike(@Param("tag") String tag);

    /**
     * 原子扣减会员余额 (仅当余额充足时才扣), 防止并发双花. 同时累加消费额/次数.
     * 返回受影响行数 (1=成功, 0=余额不足/会员不存在).
     * clearAutomatically: 扣减后清持久化上下文, 使后续 findById 读到最新余额(否则读到旧缓存值).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.balance = m.balance - :amount, "
            + "m.totalSpend = COALESCE(m.totalSpend, 0) + :amount, "
            + "m.spendCount = COALESCE(m.spendCount, 0) + 1, "
            + "m.lastSpendTime = :now, m.updateTime = :now "
            + "WHERE m.memberId = :memberId AND m.balance >= :amount")
    int deductBalance(@Param("memberId") Integer memberId,
                      @Param("amount") BigDecimal amount,
                      @Param("now") Date now);
}
