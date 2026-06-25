package com.ssafy.rescuemungz.emergencycheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyRuleMatcherTests {

    @Test
    void possibleFoodSymptomsDoNotBecomeCurrentSymptomSignals() {
        EmergencyRuleMatcher matcher = new EmergencyRuleMatcher(
                () -> List.of(rule(13L, "발작 기록", "[\"발작\"]", "주의", 1L)),
                () -> List.of(evidence(1L)),
                new ObjectMapper()
        );
        FoodSafety chocolate = new FoodSafety();
        chocolate.setFoodName("초콜릿");
        chocolate.setRiskCondition("초콜릿 섭취");
        chocolate.setObservedSymptoms("구토, 설사, 떨림, 발작");

        List<MatchedEmergencyRule> matches = matcher.match(request(), StructuredSymptomInput.empty(request().symptomNote()), chocolate);

        assertThat(matches).isEmpty();
    }

    @Test
    void eyeRubbingAndRednessCanMatchEyeEmergencyRule() {
        EmergencyRuleMatcher matcher = new EmergencyRuleMatcher(
                () -> List.of(rule(9L, "눈 부상/이물질",
                        "[\"눈\", \"눈을 비벼\", \"빨게\", \"충혈\"]", "위험", 9L)),
                () -> List.of(evidence(9L)),
                new ObjectMapper()
        );
        EmergencyCheckRequest request = new EmergencyCheckRequest(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "갑자기 눈을 비벼요. 확인해보니까 좀 빨게졌어요.",
                List.of(),
                List.of(),
                List.of()
        );

        List<MatchedEmergencyRule> matches = matcher.match(request, StructuredSymptomInput.empty(request.symptomNote()), null);

        assertThat(matches).extracting(match -> match.rule().getId()).contains(9L);
        assertThat(matches.get(0).matchedKeywords()).contains("눈", "눈을 비벼", "빨게");
    }

    private EmergencyCheckRequest request() {
        return new EmergencyCheckRequest(
                1L,
                null,
                null,
                null,
                null,
                null,
                "초콜릿",
                null,
                "아직 특별한 증상은 없지만 먹은 게 걱정돼요.",
                List.of(),
                List.of(),
                List.of()
        );
    }

    private EmergencyRule rule(Long id, String name, String keywords, String riskLevel, Long evidenceId) {
        EmergencyRule rule = new EmergencyRule();
        rule.setId(id);
        rule.setCategory("신경");
        rule.setRuleName(name);
        rule.setSymptomKeywords(keywords);
        rule.setTriggerCondition("발작이 언급된 경우");
        rule.setRiskLevel(riskLevel);
        rule.setImmediateVet(false);
        rule.setRecommendedAction("발작 시간을 기록하세요.");
        rule.setEvidenceId(evidenceId);
        rule.setPriority(50);
        return rule;
    }

    private EmergencyEvidence evidence(Long id) {
        EmergencyEvidence evidence = new EmergencyEvidence();
        evidence.setId(id);
        evidence.setCategory("신경");
        evidence.setSourceOrg("테스트");
        evidence.setSourceTitle("테스트 근거");
        evidence.setConditionText("발작 기록");
        evidence.setActionText("기록");
        evidence.setDoNotAction("붙잡지 않기");
        evidence.setSourceUrl("https://example.com");
        return evidence;
    }
}
