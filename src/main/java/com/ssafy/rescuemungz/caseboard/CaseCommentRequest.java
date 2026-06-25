package com.ssafy.rescuemungz.caseboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseCommentRequest(
        @NotBlank(message = "댓글을 입력해 주세요.")
        @Size(max = 1000, message = "댓글은 1000자 이내로 입력해 주세요.")
        String content,
        Long parentId
) {
}
