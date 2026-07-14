package com.example.nutriuniv.domain.logging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 노출 로그 요청 — 한 화면에 보인 상품들을 배열로 배치 전송한다.
 */
@Getter
@NoArgsConstructor
public class ImpressionLogRequest {

    // 노출 위치: LIST / RECOMMENDATION / SEARCH
    private String surface;

    // surface = SEARCH 일 때만 (선택)
    private String keyword;

    private List<Item> items;

    @Getter
    @NoArgsConstructor
    public static class Item {
        private Long productId;
        private Integer position;
    }
}
