package com.ssafy.rescuemungz.caseboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CasePostRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 300, message = "제목은 300자 이내로 입력해 주세요.")
        String title,

        @NotBlank(message = "카테고리를 입력해 주세요.")
        @Size(max = 50, message = "카테고리는 50자 이내로 입력해 주세요.")
        String category,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 5000, message = "내용은 5000자 이내로 입력해 주세요.")
        String content,

        @Size(max = 5, message = "사진은 최대 5개까지 등록할 수 있습니다.")
        List<@Size(max = 3_000_000, message = "이미지 한 장의 용량이 너무 큽니다. 더 작은 이미지를 사용해 주세요.") String> imageUrls
) {
}
