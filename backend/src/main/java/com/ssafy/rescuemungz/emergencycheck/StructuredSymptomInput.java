package com.ssafy.rescuemungz.emergencycheck;

import java.util.List;

public record StructuredSymptomInput(
        List<String> symptomKeywords,
        List<String> foodOrToxinKeywords,
        String suspectedFoodText,
        String occurredTimeText,
        Integer repeatCount,
        List<String> observedSigns,
        List<String> missingInfo,
        String originalSummary
) {
    public static StructuredSymptomInput empty(String summary) {
        return new StructuredSymptomInput(List.of(), List.of(), null, null, null, List.of(), List.of("확인 필요"), summary);
    }
}
