package com.example.nutriuniv.domain.search.repository;

import com.example.nutriuniv.domain.search.entity.PopularKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PopularKeywordRepository extends JpaRepository<PopularKeyword, Long> {

    List<PopularKeyword> findTop10ByOrderByRankAsc();

    // 배치가 현재 순위를 통째로 교체할 때 사용
    @Modifying
    @Query("DELETE FROM PopularKeyword")
    void deleteAllInBulk();
}
