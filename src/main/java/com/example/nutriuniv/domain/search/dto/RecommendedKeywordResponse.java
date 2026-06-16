package com.example.nutriuniv.domain.search.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 추천 검색어 — 추천 엔진이 내려주는 제품을 검색어처럼 노출.
 * 클릭 시 productId로 상세 딥링크.
 */
@Getter
@Builder
public class RecommendedKeywordResponse {
    private Long productId;
    private String name;
}
