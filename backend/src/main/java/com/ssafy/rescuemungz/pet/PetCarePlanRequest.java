package com.ssafy.rescuemungz.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PetCarePlanRequest(
        @NotNull(message = "반려견을 선택해 주세요.")
        Long petId,

        Long emergencyCheckId,

        @NotBlank(message = "계획 제목을 입력해 주세요.")
        @Size(min = 2, max = 160, message = "계획 제목은 2~160자로 입력해 주세요.")
        String title,

        @NotBlank(message = "계획 카테고리를 입력해 주세요.")
        @Size(max = 40, message = "계획 카테고리는 40자 이내로 입력해 주세요.")
        String category,

        @NotNull(message = "기록 날짜를 선택해 주세요.")
        LocalDate planDate,

        @Size(max = 500, message = "메모는 500자 이내로 입력해 주세요.")
        String memo,

        Boolean completed
) {
}
