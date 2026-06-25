package com.ssafy.rescuemungz.emergencycheck;

import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class EmergencyGuideFactory {
    static final String DISCLAIMER = "이 안내는 수의사의 진단을 대체하지 않습니다. 상태가 나빠지거나 판단이 어렵다면 동물병원에 문의해 주세요.";
    static final String INSUFFICIENT_INFO_GUIDANCE = """
            현재 입력된 내용만으로는 응급 여부를 정확히 판단하기 어렵습니다.
            증상 정보가 충분하지 않은 경우에는 위험을 낮게 단정하기보다, 보호자가 주의 깊게 상태를 살필 수 있도록 주의 단계로 안내합니다.
            증상이 계속되거나 심해지거나, 평소와 다른 행동이 함께 보인다면 가까운 동물병원 또는 24시 응급병원에 문의해 주세요.
            본 안내는 참고용 정보이며, 수의사의 진단을 대체하지 않습니다.
            """.trim();
    private static final List<String> GUARDIAN_VET_QUESTIONS = List.of(
            "우리 강아지의 현재 상태가 응급 진료가 필요한 수준인가요?",
            "지금 보이는 증상으로 어떤 문제들을 우선 확인해야 하나요?",
            "병원에 도착하기 전까지 집에서 해도 되는 행동과 피해야 할 행동은 무엇인가요?",
            "진료 시 어떤 검사나 처치가 필요할 수 있나요?",
            "집으로 돌아간 뒤 어떤 변화가 생기면 즉시 다시 연락하거나 내원해야 하나요?"
    );

    public EmergencyGuideResponse fallback(String riskLevel, String reason, FoodSafety food, List<MatchedEmergencyRule> matches,
                                           AvailableActions actions, boolean fallbackUsed) {
        if ("정보부족".equals(riskLevel)) {
            return insufficientInfo(actions, fallbackUsed);
        }
        boolean danger = "위험".equals(riskLevel);
        List<EvidenceSummary> evidence = evidence(matches);
        List<String> immediate = unique(matches.stream().map(m -> m.rule().getRecommendedAction()).toList());
        if (food != null && food.getResponse() != null) immediate.add(food.getResponse());
        if (danger) immediate.add(0, "즉시 가까운 동물병원으로 이동하세요. 이동 중 필요하면 병원에 연락해 도착 전 안내를 받으세요.");
        if (immediate.isEmpty()) immediate.add("증상 발생 시각, 반복 횟수, 섭취한 물질을 기록하고 상태 변화를 관찰하세요.");

        List<String> avoid = unique(matches.stream().map(m -> m.evidence().getDoNotAction()).toList());
        avoid.add("수의사 지시 없이 임의로 구토를 유도하거나 사람 약을 먹이지 마세요.");

        List<String> observe = List.of("호흡 상태", "의식 상태", "구토 또는 설사 반복 여부", "잇몸 색과 활력 변화");
        List<String> escalation = danger
                ? List.of("호흡 곤란, 경련, 의식 저하, 지속 출혈이 있으면 즉시 병원으로 이동하세요.")
                : List.of("증상이 반복되거나 활력이 떨어지면 동물병원에 연락하세요.");
        List<String> questions = List.of(
                "우리 강아지의 현재 상태가 응급 진료가 필요한 수준인가요?",
                "지금 보이는 증상으로 어떤 문제들을 우선 확인해야 하나요?",
                "병원에 가기 전 집에서 해도 되는 행동과 하지 말아야 할 행동은 무엇인가요?",
                "이동 중 악화 신호가 보이면 어떻게 대응해야 하나요?"
        );
        String headline = danger ? "즉시 동물병원 방문이 필요할 수 있습니다." : "상태를 기록하며 병원 상담 기준을 확인하세요.";
        return new EmergencyGuideResponse(riskLevel, headline, immediate, avoid, observe, escalation, questions,
                evidence, DISCLAIMER, actions, fallbackUsed);
    }

    private EmergencyGuideResponse insufficientInfo(AvailableActions actions, boolean fallbackUsed) {
        List<String> questions = List.of(
                "구토물이나 변에 피가 섞여 있거나 검은 변이 보이면 어떤 검사가 필요한가요?",
                "잇몸 색은 분홍색인가요, 하얗거나 파랗게 보이나요?",
                "쓰러짐, 비틀거림, 경련, 호흡곤란, 의식 저하가 있나요?",
                "더운 곳에 오래 있었거나 초콜릿, 포도, 자일리톨, 약품 같은 위험 물질을 먹었을 가능성이 있나요?",
                "지금 물을 마시거나 걸을 수 있고 반응은 평소와 비슷한가요?"
        );
        List<String> immediate = List.of("증상이 계속되거나 심해지거나, 평소와 다른 행동이 함께 보인다면 가까운 동물병원 또는 24시 응급병원에 문의해 주세요.");
        List<String> avoid = List.of("피, 창백한 잇몸, 쓰러짐, 호흡곤란, 경련이 보이면 추가 입력을 기다리지 말고 동물병원으로 이동하세요.");
        List<String> observe = List.of("구토/설사 반복 횟수", "피 또는 검은색 분비물 여부", "잇몸 색", "호흡과 의식 상태", "위험 음식이나 물질 섭취 가능성");
        List<String> escalation = List.of("호흡곤란, 경련, 의식 저하, 지속 출혈, 창백하거나 파란 잇몸이 있으면 즉시 병원으로 이동하세요.");
        return new EmergencyGuideResponse(
                "정보부족",
                "판단에 필요한 정보가 더 필요합니다.",
                immediate,
                avoid,
                observe,
                escalation,
                questions,
                List.of(),
                DISCLAIMER,
                actions,
                fallbackUsed
        );
    }

    public List<EvidenceSummary> evidence(List<MatchedEmergencyRule> matches) {
        return matches.stream()
                .map(match -> new EvidenceSummary(
                        match.evidence().getId(),
                        match.rule().getId(),
                        match.evidence().getSourceOrg(),
                        publicText(match.evidence().getSourceTitle()),
                        guardianEvidenceText(match.evidence().getConditionText()),
                        guardianEvidenceText(match.evidence().getActionText()),
                        guardianAvoidText(match.evidence().getDoNotAction()),
                        match.evidence().getSourceUrl()
                ))
                .distinct()
                .toList();
    }

    public static List<String> guardianVetQuestions(String riskLevel) {
        if ("위험".equals(riskLevel)) {
            return List.of(
                    "우리 강아지를 지금 바로 응급 진료로 봐야 하는 상태인가요?",
                    "현재 증상에서 가장 먼저 확인해야 할 위험 신호는 무엇인가요?",
                    "병원으로 이동하는 동안 해도 되는 행동과 절대 피해야 할 행동은 무엇인가요?",
                    "도착 후 어떤 검사나 처치가 필요할 수 있나요?",
                    "이동 중 상태가 악화되면 어떻게 대응해야 하나요?"
            );
        }
        return GUARDIAN_VET_QUESTIONS;
    }

    public static List<String> guardianVetQuestionsIfNeeded(String riskLevel, List<String> questions) {
        if (questions == null || questions.isEmpty()) return guardianVetQuestions(riskLevel);
        boolean hasInterviewQuestion = questions.stream().anyMatch(EmergencyGuideFactory::isVetInterviewQuestion);
        return hasInterviewQuestion ? guardianVetQuestions(riskLevel) : questions;
    }

    private static boolean isVetInterviewQuestion(String question) {
        String text = TextSafety.normalize(question);
        return text.contains("언제시작")
                || text.contains("몇번")
                || text.contains("반복횟수")
                || text.contains("증상간격")
                || text.contains("최근먹")
                || text.contains("무엇을먹")
                || text.contains("사고")
                || text.contains("산책")
                || text.contains("기존질환")
                || text.contains("복용약")
                || text.contains("잇몸색")
                || text.contains("구토나설사")
                || text.contains("피나검은색")
                || text.contains("쓰러짐")
                || text.contains("비틀거림")
                || text.contains("먹었을가능성")
                || text.contains("물을마시")
                || text.contains("걸을수");
    }

    private static String publicText(String value) {
        if (value == null) return null;
        return value
                .replace("후두마비", "호흡 문제")
                .replace("내출혈", "출혈 또는 순환 이상")
                .replace("파보바이러스", "감염성 위장 질환")
                .replace("디스템퍼", "전염성 감염")
                .replace("자궁축농증", "생식기 감염")
                .replace("열사병", "고온 노출 후 체온 조절 이상")
                .replace("Laryngeal paralysis", "호흡기 증상 안내");
    }

    private static String guardianEvidenceText(String value) {
        String publicValue = publicText(rewriteKnownEvidenceText(value));
        if (publicValue == null || publicValue.isBlank()) return null;
        String normalized = TextSafety.normalize(publicValue);
        if (isInternalPolicyText(normalized)) {
            return "응급 안내는 진단을 대신하지 않으며, 상태가 나빠지거나 판단이 어렵다면 동물병원 상담이 필요합니다.";
        }
        return TextSafety.politeGuardianTone(publicValue);
    }

    private static String rewriteKnownEvidenceText(String value) {
        if (value == null) return null;
        String normalized = TextSafety.normalize(value);
        if (normalized.contains("부러진발톱출혈")
                && normalized.contains("타박상")
                && normalized.contains("염좌")
                && normalized.contains("절뚝임")) {
            return "가벼운 타박상이나 삐끗함도 통증, 부기, 절뚝임이 있으면 활동을 제한하고 상태를 살펴야 합니다. 통증이 심하거나 며칠 이상 지속되면 수의사 상담이 필요합니다.";
        }
        return value;
    }

    private static String guardianAvoidText(String value) {
        String publicValue = publicText(value);
        if (publicValue == null || publicValue.isBlank()) return null;
        String normalized = TextSafety.normalize(publicValue);
        if (isInternalPolicyText(normalized)) {
            return "응급처치만으로 해결하려 하지 말고, 수의사 상담이나 진료를 함께 받으세요.";
        }
        return TextSafety.politeGuardianTone(publicValue);
    }

    private static boolean isInternalPolicyText(String normalized) {
        return normalized.contains("진료를대체")
                || normalized.contains("진단대체")
                || normalized.contains("대체한다고안내")
                || normalized.contains("안내하지않는다")
                || normalized.contains("문구를항상포함")
                || normalized.contains("안내결과에는")
                || normalized.contains("응급룰")
                || normalized.contains("위험룰")
                || normalized.contains("룰로분류")
                || normalized.contains("룰과연결")
                || normalized.contains("룰설계")
                || normalized.contains("우선매칭")
                || normalized.contains("매칭대상")
                || normalized.contains("food_safety")
                || normalized.contains("emergency_rules")
                || normalized.contains("근거도출")
                || normalized.contains("판단룰")
                || normalized.contains("내부로직");
    }

    private List<String> unique(List<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = TextSafety.politeGuardianTone(TextSafety.clean(value, 240));
            if (cleaned != null && !cleaned.isBlank()) result.add(cleaned);
        }
        return new ArrayList<>(result);
    }
}
