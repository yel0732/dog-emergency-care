package com.ssafy.rescuemungz.emergencycheck;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyGuideFactoryTests {
    private final EmergencyGuideFactory factory = new EmergencyGuideFactory();

    @Test
    void fallbackIncludesHospitalFirstForDanger() {
        EmergencyGuideResponse guide = factory.fallback("위험", "호흡곤란", null, List.of(match()),
                new AvailableActions(true, true, "호흡기 증상", 10L, "영상 확인으로 병원 연락이나 이동을 늦추지 마세요."), true);

        assertThat(guide.fallbackUsed()).isTrue();
        assertThat(guide.immediateActions().get(0)).contains("동물병원");
        assertThat(guide.availableActions().showHospitalButton()).isTrue();
        assertThat(guide.evidenceSummary()).hasSize(1);
        assertThat(guide.evidenceSummary().get(0).evidenceId()).isEqualTo(3L);
    }

    private MatchedEmergencyRule match() {
        EmergencyRule rule = new EmergencyRule();
        rule.setId(7L);
        rule.setRuleName("중독 룰");
        rule.setRiskLevel("위험");
        rule.setPriority(20);
        rule.setRecommendedAction("섭취 물질과 시간을 확인하고 병원에 연락하세요.");
        EmergencyEvidence evidence = new EmergencyEvidence();
        evidence.setId(3L);
        evidence.setSourceOrg("ASPCA");
        evidence.setSourceTitle("Poison Control");
        evidence.setConditionText("독성 물질 섭취");
        evidence.setActionText("병원 연락");
        evidence.setDoNotAction("임의 구토 유도 금지");
        evidence.setSourceUrl("https://example.com");
        return new MatchedEmergencyRule(rule, evidence, BigDecimal.TEN, List.of("초콜릿"));
    }
}
