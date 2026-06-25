package com.ssafy.rescuemungz.emergencycheck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyCheckRequest(
        @NotNull(message = "반려견을 선택해 주세요.")
        Long petId,
        LocalDateTime occurredAt,
        @Min(value = 0, message = "반복 횟수는 0회 이상으로 입력해 주세요.")
        @Max(value = 30, message = "반복 횟수는 30회 이하로 입력해 주세요.")
        Integer repeatCount,
        @DecimalMin(value = "0.1", message = "현재 체중은 0.1kg 이상으로 입력해 주세요.")
        @DecimalMax(value = "120.0", message = "현재 체중은 120kg 이하로 입력해 주세요.")
        BigDecimal currentWeight,
        @Size(max = 80, message = "발생 시점은 80자 이내로 입력해 주세요.")
        String occurredTimeText,
        Long suspectedFoodId,
        @Size(max = 160, message = "의심 음식은 160자 이내로 입력해 주세요.")
        String suspectedFoodText,
        @Size(max = 160, message = "섭취량/노출량은 160자 이내로 입력해 주세요.")
        String exposureAmount,
        @NotBlank(message = "증상 내용을 입력해 주세요.")
        @Size(min = 5, max = 3000, message = "증상 내용은 5~3000자로 입력해 주세요.")
        String symptomNote,
        @Size(max = 10, message = "증상 태그는 최대 10개까지 선택할 수 있습니다.")
        List<@Size(max = 40, message = "증상 태그는 40자 이내로 입력해 주세요.") String> symptomTags,
        @Size(max = 8, message = "위험 신호는 최대 8개까지 선택할 수 있습니다.")
        List<@Size(max = 60, message = "위험 신호는 60자 이내로 입력해 주세요.") String> redFlags,
        @Size(max = 5, message = "사진은 최대 5개까지 등록할 수 있습니다.")
        List<@Size(max = 1500000, message = "사진 데이터는 1.5MB 이내로 등록해 주세요.") String> photoUrls
) {
}
