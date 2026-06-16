package com.example.nutriuniv.domain.search.repository;

import com.example.nutriuniv.domain.search.entity.PopularKeywordHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PopularKeywordHistoryRepository extends JpaRepository<PopularKeywordHistory, Long> {

    List<PopularKeywordHistory> findBySnapshotDate(LocalDate snapshotDate);

    // 배치 재실행 시 멱등성 보장용
    void deleteBySnapshotDate(LocalDate snapshotDate);
}
