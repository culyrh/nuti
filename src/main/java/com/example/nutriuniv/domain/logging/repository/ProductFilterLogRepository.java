package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.ProductFilterLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFilterLogRepository extends JpaRepository<ProductFilterLog, Long> {
}
