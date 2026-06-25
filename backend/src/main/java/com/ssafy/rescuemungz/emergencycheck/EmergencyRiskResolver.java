package com.ssafy.rescuemungz.emergencycheck;

import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class EmergencyRiskResolver {
    private static final Set<String> GENERIC_SYMPTOMS = Set.of(
            "구토", "무기력", "설사", "식욕저하", "식욕부진", "기침", "발열", "허약", "헐떡"
    );
    private static final Set<String> REASSURING_DETAILS = Set.of(
            "한 번", "1번", "한번", "피는 없", "피 없음", "분홍", "잘 걷", "물도 마", "정상", "괜찮",
            "반응", "의식 정상", "잇몸은 분홍", "지금은"
    );
    private static final Set<String> LOW_RISK_VOMITING_DETAILS = Set.of(
            "한 번", "1번", "한번", "1회", "한 차례", "한차례"
    );
    private static final Set<String> LOW_RISK_RECOVERY_DETAILS = Set.of(
            "지금은", "이후 정상", "잘 놀", "뛰어다", "활력은 괜찮", "활력 정상", "밥도 먹", "물도 마",
            "호흡 정상", "숨은 정상", "의식 정상", "괜찮", "멀쩡"
    );
    private static final Set<String> LOW_RISK_VOMITING_EXCLUSIONS = Set.of(
            "포도", "건포도", "초콜릿", "코코아", "자일리톨", "무설탕", "양파", "마늘", "부추", "알코올",
            "커피", "카페인", "약", "진통제", "수면제", "감기약", "쥐약", "살충제", "세제", "락스",
            "비료", "제초제", "방부제", "전자담배", "액상", "독성", "중독", "이물", "삼킴", "뼈",
            "가시", "꼬치", "장난감", "양말", "옥수수대", "복부팽만", "배가 빵빵", "헛구역질",
            "호흡곤란", "숨을 못", "파란", "창백", "하얀 잇몸", "혈변", "피가", "피 섞", "피를", "출혈", "검은변", "무기력",
            "축 처", "경련", "발작", "실신", "쓰러", "의식", "고열", "열사병", "저체온", "통증"
    );

    public String resolveRisk(FoodSafety food, List<MatchedEmergencyRule> matches, StructuredSymptomInput structured) {
        return resolveRisk(null, food, matches, structured);
    }

    public String resolveRisk(EmergencyCheckRequest request, FoodSafety food, List<MatchedEmergencyRule> matches, StructuredSymptomInput structured) {
        String ruleRisk = matches.stream()
                .map(match -> match.rule().getRiskLevel())
                .max(Comparator.comparingInt(EmergencyRuleMatcher::riskRank))
                .orElse(null);
        String foodRisk = foodRisk(food);
        String risk = maxRisk(ruleRisk, foodRisk);
        if (("주의".equals(risk) || "정보부족".equals(risk)) && isLowRiskSingleVomiting(request, structured, food, matches)) {
            return "관찰";
        }
        if (risk != null) return risk;
        if (isLowRiskSingleVomiting(request, structured, food, matches)) {
            return "관찰";
        }
        return "정보부족";
    }

    public boolean immediateVet(String riskLevel, FoodSafety food, List<MatchedEmergencyRule> matches) {
        return "위험".equals(riskLevel)
                || (food != null && Boolean.TRUE.equals(food.getImmediateVet()))
                || matches.stream().anyMatch(match -> Boolean.TRUE.equals(match.rule().getImmediateVet()));
    }

    public String reason(String riskLevel, FoodSafety food, List<MatchedEmergencyRule> matches) {
        if (!matches.isEmpty()) {
            MatchedEmergencyRule first = matches.get(0);
            String signals = publicSignals(first.matchedKeywords());
            if ("위험".equals(riskLevel)) {
                return "입력 내용에서 즉시 확인이 필요한 위험 신호(%s)가 확인되어 위험 단계로 분류했습니다."
                        .formatted(signals);
            }
            return "%s 단계로 분류했습니다. 확인된 신호: %s. 참고 근거는 아래 근거 내용에서 확인할 수 있습니다."
                    .formatted(riskLevel, signals);
        }
        if (food != null) {
            return "%s의 음식 안전 정보가 %s 단계로 등록되어 있어 현재 상황의 참고 근거로 사용했습니다."
                    .formatted(food.getFoodName(), food.getRiskLevel());
        }
        if ("관찰".equals(riskLevel)) {
            return "현재 입력에서는 즉시 병원으로 연결할 강한 위험 신호가 확인되지 않았습니다. 증상 변화와 악화 기준을 관찰해 주세요.";
        }
        if ("정보부족".equals(riskLevel)) {
            return "현재 입력만으로는 응급도 판단에 필요한 핵심 정보가 부족합니다. 아래 질문에 답을 덧붙여 다시 상담해 주세요.";
        }
        return "현재 입력만으로는 응급도 안내에 필요한 정보가 부족합니다. 안전을 위해 추가 확인이 필요합니다.";
    }

    public String recommendedAction(String riskLevel, List<MatchedEmergencyRule> matches) {
        if ("정보부족".equals(riskLevel)) {
            return "아래 질문에 대한 답을 기존 증상 메모에 덧붙여 다시 상담해 주세요.";
        }
        if ("위험".equals(riskLevel)) {
            return "즉시 가까운 동물병원으로 이동하세요. 이동 중 필요하면 병원에 연락해 도착 전 안내를 받으세요.";
        }
        if (!matches.isEmpty()) return TextSafety.politeGuardianTone(publicAction(matches.get(0).rule().getRecommendedAction()));
        if ("주의".equals(riskLevel)) return "증상 변화와 반복 횟수를 기록하고 악화 시 병원에 연락하세요.";
        return "부족한 정보를 확인하고 호흡, 의식, 구토·설사 반복 여부를 관찰하세요.";
    }

    private String foodRisk(FoodSafety food) {
        if (food == null) return null;
        if (Boolean.TRUE.equals(food.getImmediateVet()) || "위험".equals(food.getRiskLevel())) return "위험";
        if ("주의".equals(food.getRiskLevel())) return "주의";
        if ("정보부족".equals(food.getRiskLevel())) return "정보부족";
        return null;
    }

    private String maxRisk(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return EmergencyRuleMatcher.riskRank(a) >= EmergencyRuleMatcher.riskRank(b) ? a : b;
    }

    private boolean needsMoreInformation(EmergencyCheckRequest request, StructuredSymptomInput structured) {
        String text = TextSafety.normalize(String.join(" ",
                request == null ? "" : safe(request.symptomNote()),
                request == null ? "" : safe(request.suspectedFoodText()),
                request == null || request.symptomTags() == null ? "" : String.join(" ", request.symptomTags()),
                structured == null || structured.originalSummary() == null ? "" : structured.originalSummary(),
                structured == null || structured.symptomKeywords() == null ? "" : String.join(" ", structured.symptomKeywords()),
                structured == null || structured.observedSigns() == null ? "" : String.join(" ", structured.observedSigns())
        ));
        boolean hasGenericSymptom = GENERIC_SYMPTOMS.stream().map(TextSafety::normalize).anyMatch(text::contains);
        boolean hasReassuringDetail = REASSURING_DETAILS.stream().map(TextSafety::normalize).anyMatch(text::contains);
        boolean structuredMissing = structured == null
                || (structured.missingInfo() != null && !structured.missingInfo().isEmpty())
                || (structured.symptomKeywords() == null || structured.symptomKeywords().isEmpty());
        return (hasGenericSymptom && !hasReassuringDetail) || structuredMissing;
    }

    private boolean isLowRiskSingleVomiting(EmergencyCheckRequest request, StructuredSymptomInput structured, FoodSafety food,
                                            List<MatchedEmergencyRule> matches) {
        if (request == null) return false;
        if (food != null && ("위험".equals(food.getRiskLevel()) || Boolean.TRUE.equals(food.getImmediateVet()))) return false;
        if (matches.stream().anyMatch(match -> "위험".equals(match.rule().getRiskLevel())
                || Boolean.TRUE.equals(match.rule().getImmediateVet()))) return false;
        if (request.repeatCount() != null && request.repeatCount() > 1) return false;
        if (request.redFlags() != null && !request.redFlags().isEmpty()) return false;

        String text = TextSafety.normalize(String.join(" ",
                safe(request.symptomNote()),
                safe(request.suspectedFoodText()),
                safe(request.exposureAmount()),
                request.symptomTags() == null ? "" : String.join(" ", request.symptomTags())
        ));
        boolean vomiting = text.contains(TextSafety.normalize("구토")) || text.contains(TextSafety.normalize("토했"));
        boolean singleEpisode = request.repeatCount() != null && request.repeatCount() <= 1
                || LOW_RISK_VOMITING_DETAILS.stream().map(TextSafety::normalize).anyMatch(text::contains);
        boolean recovered = LOW_RISK_RECOVERY_DETAILS.stream().map(TextSafety::normalize).anyMatch(text::contains);
        boolean excluded = LOW_RISK_VOMITING_EXCLUSIONS.stream().map(TextSafety::normalize).anyMatch(text::contains);
        return vomiting && singleEpisode && recovered && !excluded;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String publicSignals(List<String> values) {
        if (values == null || values.isEmpty()) return "입력 증상";
        List<String> signals = values.stream()
                .map(this::publicAction)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return signals.isEmpty() ? "입력 증상" : String.join(", ", signals);
    }

    private String publicAction(String value) {
        if (value == null || value.isBlank()) return value;
        return value
                .replace("후두마비는 응급 치료가 필요한 상황일 수 있으므로", "호흡 문제가 갑자기 악화될 수 있으므로")
                .replace("후두마비", "호흡 문제")
                .replace("내출혈", "출혈 또는 순환 이상")
                .replace("파보바이러스", "감염성 위장 질환")
                .replace("디스템퍼", "전염성 감염")
                .replace("자궁축농증", "생식기 감염");
    }
}
