package com.votzz.backend.repository;

import com.votzz.backend.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCode(String code);
    Optional<Coupon> findByCodeAndActiveTrue(String code);
}