package com.ssafy.rescuemungz.emergencyvideo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmergencyVideoRequest(
        @NotBlank(message = "영상 제목을 입력해 주세요.")
        @Size(max = 160, message = "영상 제목은 160자 이내로 입력해 주세요.")
        String title,

        @NotBlank(message = "카테고리를 입력해 주세요.")
        @Size(max = 50, message = "카테고리는 50자 이내로 입력해 주세요.")
        String category,

        @Size(max = 120, message = "증상은 120자 이내로 입력해 주세요.")
        String symptom,

        @Size(max = 2000, message = "설명은 2000자 이내로 입력해 주세요.")
        String description,

        @NotBlank(message = "YouTube URL을 입력해 주세요.")
        @Size(max = 500, message = "YouTube URL은 500자 이내로 입력해 주세요.")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?(youtube\\.com/(watch\\?v=|embed/|shorts/)|youtu\\.be/).+",
                message = "YouTube 영상 URL 형식으로 입력해 주세요."
        )
        String youtubeUrl
) {
}
