package com.ssafy.rescuemungz.emergencycheck;

import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyRiskResolverTests {
    private final EmergencyRiskResolver resolver = new EmergencyRiskResolver();

    @Test
    void dangerRuleWinsOverCautionFood() {
        FoodSafety food = new FoodSafety();
        food.setRiskLevel("주의");
        MatchedEmergencyRule match = match("위험", 10, true);

        String risk = resolver.resolveRisk(food, List.of(match), StructuredSymptomInput.empty("구토와 호흡곤란"));

        assertThat(risk).isEqualTo("위험");
        assertThat(resolver.immediateVet(risk, food, List.of(match))).isTrue();
    }

    @Test
    void dangerousFoodCanRaiseRiskWithoutRuleMatch() {
        FoodSafety food = new FoodSafety();
        food.setFoodName("초콜릿");
        food.setRiskLevel("위험");
        food.setImmediateVet(true);

        String risk = resolver.resolveRisk(food, List.of(), StructuredSymptomInput.empty("초콜릿 섭취"));

        assertThat(risk).isEqualTo("위험");
        assertThat(resolver.immediateVet(risk, food, List.of())).isTrue();
    }

    @Test
    void missingCoreInformationDoesNotBecomeSafe() {
        String risk = resolver.resolveRisk(null, List.of(), StructuredSymptomInput.empty("잘 모르겠어요"));

        assertThat(risk).isEqualTo("정보부족");
    }

    @Test
    void singleVomitingWithClearRecoveryCanStayObservation() {
        EmergencyCheckRequest request = request(
                1,
                "새 간식을 먹고 한 번 토했지만 지금은 잘 놀아요.",
                "새 간식",
                List.of("구토")
        );
        MatchedEmergencyRule cautionMatch = match("주의", 3, false);

        String risk = resolver.resolveRisk(request, null, List.of(cautionMatch),
                StructuredSymptomInput.empty(request.symptomNote()));

        assertThat(risk).isEqualTo("관찰");
    }

    @Test
    void singleGrassVomitingWithRecoveryDoesNotBecomeInsufficient() {
        EmergencyCheckRequest request = request(
                1,
                "산책 중 풀을 먹고 한 번 토했어요. 이후 정상이에요.",
                "풀",
                List.of("구토")
        );

        String risk = resolver.resolveRisk(request, null, List.of(),
                StructuredSymptomInput.empty(request.symptomNote()));

        assertThat(risk).isEqualTo("관찰");
    }

    @Test
    void singleGrassVomitingWithUnknownFoodRecordCanStayObservation() {
        EmergencyCheckRequest request = request(
                1,
                "산책 중 풀을 먹고 한 번 토했어요. 이후 정상이에요.",
                "풀",
                List.of("구토")
        );
        FoodSafety food = new FoodSafety();
        food.setFoodName("풀");
        food.setRiskLevel("정보부족");
        food.setImmediateVet(false);

        String risk = resolver.resolveRisk(request, food, List.of(),
                StructuredSymptomInput.empty(request.symptomNote()));

        assertThat(risk).isEqualTo("관찰");
    }


    @Test
    void toxicFoodDoesNotBecomeObservationEvenWithSingleVomitingAndRecovery() {
        EmergencyCheckRequest request = request(
                1,
                "포도 1알을 먹었어요. 아직 멀쩡해 보여요.",
                "포도",
                List.of("구토")
        );
        FoodSafety food = new FoodSafety();
        food.setFoodName("포도");
        food.setRiskLevel("위험");
        food.setImmediateVet(true);

        String risk = resolver.resolveRisk(request, food, List.of(),
                StructuredSymptomInput.empty(request.symptomNote()));

        assertThat(risk).isEqualTo("위험");
    }

    private MatchedEmergencyRule match(String riskLevel, int priority, boolean immediateVet) {
        EmergencyRule rule = new EmergencyRule();
        rule.setId(1L);
        rule.setRuleName("호흡곤란 룰");
        rule.setRiskLevel(riskLevel);
        rule.setPriority(priority);
        rule.setImmediateVet(immediateVet);
        rule.setRecommendedAction("즉시 병원에 연락하세요.");
        EmergencyEvidence evidence = new EmergencyEvidence();
        evidence.setId(1L);
        evidence.setSourceOrg("AVMA");
        evidence.setSourceTitle("First Aid Tips");
        evidence.setConditionText("호흡곤란");
        evidence.setActionText("병원 연락");
        evidence.setDoNotAction("지연 금지");
        evidence.setSourceUrl("https://example.com");
        return new MatchedEmergencyRule(rule, evidence, BigDecimal.TEN, List.of("호흡곤란"));
    }

    private EmergencyCheckRequest request(Integer repeatCount, String symptomNote, String suspectedFoodText, List<String> tags) {
        return new EmergencyCheckRequest(
                1L,
                null,
                repeatCount,
                null,
                null,
                null,
                suspectedFoodText,
                null,
                symptomNote,
                tags,
                List.of(),
                List.of()
        );
    }
}
