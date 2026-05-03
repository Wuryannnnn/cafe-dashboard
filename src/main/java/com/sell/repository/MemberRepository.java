package com.sell.repository;

import com.sell.dataobject.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    Member findByPhone(String phone);

    Member findByOpenid(String openid);

    Page<Member> findByPhoneContainingOrNicknameContaining(String phone, String nickname, Pageable pageable);
}
