package com.ssafy.rescuemungz.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력해 주세요.")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$", message = "아이디는 영문으로 시작하고 영문, 숫자, 밑줄만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 60, message = "비밀번호는 8~60자로 입력해 주세요.")
        @Pattern(
                regexp = "^(?=\\S+$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,60}$",
                message = "비밀번호는 공백 없이 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(min = 2, max = 50, message = "이름은 2~50자로 입력해 주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z\\s]{2,50}$", message = "이름은 한글 또는 영문으로 입력해 주세요.")
        String name,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = 2, max = 50, message = "닉네임은 2~50자로 입력해 주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_\\s]{2,50}$", message = "닉네임은 한글, 영문, 숫자, 밑줄만 사용할 수 있습니다.")
        String nickname,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
        @Size(max = 255, message = "이메일은 255자 이내로 입력해 주세요.")
        String email
) {
}
