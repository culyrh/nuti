package com.example.nutriuniv.domain.coupang.repository;

import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoupangLinkRepository extends JpaRepository<CoupangLink, Long> {

    Optional<CoupangLink> findByProduct(Product product);

    Page<CoupangLink> findByLinkStatus(String linkStatus, Pageable pageable);

    List<CoupangLink> findAllByLinkStatus(String linkStatus);

    long countByLinkStatus(String linkStatus);

    List<CoupangLink> findByProductIdIn(List<Long> productIds);
}
