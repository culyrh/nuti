package com.example.nutriuniv.domain.logging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FilterLogRequest {
    // category / brand / sort / grade_sort 등
    private String filterType;
    // 선택된 값 (선택)
    private String filterValue;
}
