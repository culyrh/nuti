package com.example.nutriuniv.domain.admin.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.pns.service.PnsBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - PNS", description = "관리자 영양 점수(PNS) 일괄 계산")
@RestController
@RequiredArgsConstructor
public class AdminPnsController {

    private final PnsBatchService pnsBatchService;

    @Operation(summary = "PNS 점수 일괄 계산",
            description = """
                    전체 활성 상품에 대해 goal(diet/bulk) × EER 4구간(1500/2000/2500/3000)
                    총 8개 조합의 PNS 점수·등급·건강점수·백분위를 계산하여
                    product_pns_by_eer 테이블에 저장합니다.
                    입력 기준: 100g당 영양성분 (*_per_100g 컬럼).
                    기존 데이터는 조합별로 삭제 후 재생성합니다.
                    """)
    @PostMapping("/admin/pns/calculate")
    public ResponseEntity<CommonResponse<PnsBatchService.BatchResult>> calculate() {
        return ResponseEntity.ok(CommonResponse.success(pnsBatchService.calculateAll()));
    }
}