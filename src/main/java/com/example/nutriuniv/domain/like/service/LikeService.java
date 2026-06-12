package com.example.nutriuniv.domain.like.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.like.dto.LikePageResponse;
import com.example.nutriuniv.domain.like.entity.UserFavorite;
import com.example.nutriuniv.domain.like.repository.UserFavoriteRepository;
import com.example.nutriuniv.domain.pns.service.PnsLookupService;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import com.example.nutriuniv.domain.user.entity.User;
import com.example.nutriuniv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final UserFavoriteRepository userFavoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PnsLookupService pnsLookupService;

    // ── GET /likes ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LikePageResponse getLikes(Long userId, int page, int size) {
        if (page < 1 || size < 1) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM, "page, size는 1 이상이어야 합니다.");
        }

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserFavorite> favPage = userFavoriteRepository
                .findByUserIdAndProductIsActiveTrue(userId, pageable);

        // 등급 IN 쿼리 (N+1 방지)
        int eerBand = pnsLookupService.resolveEerBand(userId);
        List<Long> productIds = favPage.getContent().stream()
                .map(fav -> fav.getProduct().getId()).collect(Collectors.toList());
        Map<Long, String> gradeMap = pnsLookupService.lookupGrades(productIds, eerBand);

        List<LikePageResponse.LikeItem> items = favPage.getContent().stream()
                .map(fav -> {
                    Product p = fav.getProduct();
                    return LikePageResponse.LikeItem.builder()
                            .productId(p.getId())
                            .name(p.getName())
                            .imageUrl(p.getImageUrl())
                            .nutritionScore(p.getNutritionScore())
                            .grade(gradeMap.get(p.getId()))
                            .brand(p.getBrand() == null ? null :
                                    LikePageResponse.BrandInfo.builder()
                                            .id(p.getBrand().getId())
                                            .name(p.getBrand().getName())
                                            .build())
                            .build();
                })
                .toList();

        return LikePageResponse.builder()
                .total(favPage.getTotalElements())
                .items(items)
                .build();
    }

    // ── POST /likes/{productId} ───────────────────────────────────────────────────

    @Transactional
    public void addLike(Long productId, Long userId) {
        // 중복 찜 체크
        if (userFavoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 찜한 상품입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 상품입니다."));

        userFavoriteRepository.save(UserFavorite.create(user, product));
    }

    // ── DELETE /likes/{productId} ─────────────────────────────────────────────────

    @Transactional
    public void removeLike(Long productId, Long userId) {
        UserFavorite favorite = userFavoriteRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "찜 목록에 없는 상품입니다."));

        userFavoriteRepository.delete(favorite);
    }
}