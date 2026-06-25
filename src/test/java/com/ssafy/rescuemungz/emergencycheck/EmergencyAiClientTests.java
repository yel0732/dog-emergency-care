package com.ssafy.rescuemungz.emergencycheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyAiClientTests {
    private static final GmsProperties PROPERTIES = new GmsProperties(
            "real-gms-key",
            "https://gms.ssafy.io/gmsapi",
            "/api.openai.com/v1/chat/completions",
            "gpt-4o-mini",
            1
    );

    @Test
    void guideKeepsBackendRiskLevelWhenAiReturnsDifferentLevel() {
        EmergencyAiClient client = clientReturning("""
                {
                  "emergencyLevel": "관찰",
                  "headline": "현재는 지켜보셔도 됩니다.",
                  "immediateActions": ["물을 조금 먹이세요."],
                  "avoidActions": ["임의 처방하지 마세요.", "억지로 먹이지 마세요."],
                  "observationChecklist": ["호흡과 의식 변화를 확인하세요."],
                  "escalationCriteria": ["증상이 악화되면 바로 연락하세요."],
                  "optionalQuestions": ["지금 바로 내원해야 하는지 문의하세요."],
                  "disclaimer": "안내"
                }
                """);

        EmergencyGuideResponse guide = client.guide(
                request(),
                null,
                null,
                "위험",
                true,
                StructuredSymptomInput.empty("초콜릿을 먹고 구토했습니다."),
                List.of(),
                new AvailableActions(true, true, null, null, null)
        ).orElseThrow();

        assertThat(guide.emergencyLevel()).isEqualTo("위험");
        assertThat(guide.immediateActions().get(0)).contains("동물병원");
        assertThat(guide.avoidActions()).noneMatch(value -> value.contains("처방"));
    }

    @Test
    void structureCleansAndBoundsAiOutput() {
        EmergencyAiClient client = clientReturning("""
                {
                  "symptomKeywords": ["구토", "구토", "무기력"],
                  "foodOrToxinKeywords": ["초콜릿"],
                  "suspectedFoodText": "초콜릿",
                  "occurredTimeText": "1시간 전",
                  "repeatCount": 99,
                  "observedSigns": ["침 흘림"],
                  "missingInfo": ["섭취량 확인 필요"],
                  "originalSummary": "초콜릿을 먹고 구토했습니다."
                }
                """);

        StructuredSymptomInput structured = client.structure(request()).orElseThrow();

        assertThat(structured.symptomKeywords()).containsExactly("구토", "무기력");
        assertThat(structured.repeatCount()).isEqualTo(30);
        assertThat(structured.foodOrToxinKeywords()).containsExactly("초콜릿");
    }

    @Test
    void returnsEmptyWhenSpringAiGatewayFails() {
        EmergencyAiClient client = new EmergencyAiClient(PROPERTIES, new ObjectMapper(), prompt -> Optional.empty());

        assertThat(client.structure(request())).isEmpty();
    }

    private EmergencyAiClient clientReturning(String json) {
        return new EmergencyAiClient(PROPERTIES, new ObjectMapper(), prompt -> Optional.of(json));
    }

    private EmergencyCheckRequest request() {
        return new EmergencyCheckRequest(
                1L,
                null,
                2,
                null,
                "1시간 전",
                null,
                "초콜릿",
                "두 조각",
                "초콜릿을 먹고 두 번 구토했습니다.",
                List.of("구토"),
                List.of("무기력"),
                List.of()
        );
    }
}
