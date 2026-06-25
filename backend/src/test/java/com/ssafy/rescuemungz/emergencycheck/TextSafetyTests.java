package com.ssafy.rescuemungz.emergencycheck;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSafetyTests {

    @Test
    void politeGuardianToneConvertsNeedToDoEnding() {
        String text = TextSafety.politeGuardianTone(
                "며칠 이상 지속되면 수의사에게 연락해야 한다. 집에서 방치하지 않는다.");

        assertThat(text).contains("연락해야 합니다");
        assertThat(text).contains("방치하지 마세요");
        assertThat(text).doesNotContain("연락해야 한다");
    }

    @Test
    void politeGuardianToneConvertsPassiveRecommendationEnding() {
        String text = TextSafety.politeGuardianTone(
                "하루나 이틀 이상 지속되면 진료 예약이 권장된다.");

        assertThat(text).contains("진료 예약이 권장됩니다");
        assertThat(text).doesNotContain("권장된다");
    }

    @Test
    void politeGuardianToneConvertsOperationalAndRiskEndings() {
        String text = TextSafety.politeGuardianTone(
                "상위 독소 키워드는 우선 매칭 대상으로 관리한다. 진한 초콜릿일수록 위험도가 높다.");

        assertThat(text).contains("관리합니다");
        assertThat(text).contains("위험도가 높습니다");
        assertThat(text).doesNotContain("관리한다");
        assertThat(text).doesNotContain("높다");
    }
}
