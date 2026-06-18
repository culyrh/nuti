package com.example.nutriuniv.domain.user.repository;

import com.example.nutriuniv.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 활성 유저만 조회 (탈퇴 유저 제외)
    Optional<User> findByOauthProviderAndOauthIdAndIsActiveTrue(String oauthProvider, String oauthId);

    // 관리자 유저 검색 (이름 또는 이메일)
    @Query("SELECT u FROM User u WHERE :keyword IS NULL OR u.name LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}