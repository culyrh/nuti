package com.example.nutriuniv.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PopularKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private int searchCount = 0;

    @Column(name = "rank", nullable = false)
    private int rank;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PopularKeyword create(String keyword, int searchCount, int rank) {
        PopularKeyword pk = new PopularKeyword();
        pk.keyword = keyword;
        pk.searchCount = searchCount;
        pk.rank = rank;
        return pk;
    }
}
