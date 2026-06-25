package com.ssafy.rescuemungz.pet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PetRequest(
        @NotBlank(message = "반려견 이름을 입력해 주세요.")
        @Size(min = 1, max = 50, message = "반려견 이름은 1~50자로 입력해 주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s]{1,50}$", message = "반려견 이름은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String name,

        @Size(max = 60, message = "견종은 60자 이내로 입력해 주세요.")
        String breed,

        @Min(value = 0, message = "나이는 0살 이상으로 입력해 주세요.")
        @Max(value = 40, message = "나이는 40살 이하로 입력해 주세요.")
        Integer age,

        @DecimalMin(value = "0.1", message = "체중은 0.1kg 이상으로 입력해 주세요.")
        @DecimalMax(value = "120.0", message = "체중은 120kg 이하로 입력해 주세요.")
        BigDecimal weight,

        @Pattern(regexp = "^(UNKNOWN|MALE|FEMALE)$", message = "성별은 남아, 여아, 선택 안 함 중 하나로 선택해 주세요.")
        String gender,

        Boolean neutered,

        @Size(max = 255, message = "알레르기는 255자 이내로 입력해 주세요.")
        String allergies,

        @Size(max = 255, message = "기저질환은 255자 이내로 입력해 주세요.")
        String diseases
) {
}
