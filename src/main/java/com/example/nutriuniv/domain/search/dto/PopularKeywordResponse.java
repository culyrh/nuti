package com.example.nutriuniv.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopularKeywordResponse {
    private int rank;
    private String keyword;

    // 전일 순위 (전일 순위권 밖이면 null = 신규 진입)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer previousRank;

    // UP | DOWN | SAME | NEW — 표시 방법은 프론트가 결정
    private String change;

    // previousRank - rank (양수=상승, 음수=하락). 신규(NEW)면 null
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer rankDelta;
}
