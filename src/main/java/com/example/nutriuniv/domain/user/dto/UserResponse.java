package com.example.nutriuniv.domain.user.dto;

import com.example.nutriuniv.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private String gender;
    private LocalDate birthDate;
    private String role;   // USER / ADMIN — 프론트 관리자 페이지 분기용

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .role(user.getRole())
                .build();
    }
}