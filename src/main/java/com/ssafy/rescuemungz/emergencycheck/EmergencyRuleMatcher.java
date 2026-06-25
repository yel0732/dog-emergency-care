package com.ssafy.rescuemungz.emergencycheck;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EmergencyRuleMatcher {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final Set<String> CRITICAL_SINGLE_KEYWORDS = Set.of(
            "피토", "피 토", "혈변", "검은변", "타르변", "창백한 잇몸", "하얀 잇몸", "쓰러짐",
            "호흡곤란", "파란 잇몸", "혀가 파래", "발작", "경련", "의식소실", "자일리톨", "초콜릿", "초코", "코코아", "카카오",
            "포도", "건포도", "양파", "마늘", "부동액", "쥐약", "복부팽만", "헛구역질",
            "하임리히", "질식", "기도폐쇄", "목에 걸", "켁켁", "캑캑", "컥컥", "숨 못 쉬", "숨쉬기 힘들"
    );
    private static final Set<String> GENERIC_SUPPORT_KEYWORDS = Set.of(
            "구토", "무기력", "식욕저하", "식욕부진", "설사", "기침", "발열", "허약", "헐떡",
            "침흘림", "떨림", "비틀", "강아지", "축 처져", "안절부절", "갈증"
    );
    private static final Set<String> CONTEXT_KEYWORDS = Set.of(
            "더위", "뜨거워", "차 안", "아스팔트", "초콜릿", "초코", "코코아", "카카오", "자일리톨", "포도", "건포도", "양파",
            "마늘", "부동액", "쥐약", "바닷물", "소금물", "남조류", "녹조", "수영장", "염소",
            "미접종", "중성화 안", "질분비물", "발정", "구근", "약", "세제", "화학물질",
            "간식", "먹다가", "삼켰", "이물질", "목에 걸", "하임리히", "기도폐쇄", "질식",
            "눈", "안구", "각막", "충혈"
    );
    private static final Set<String> DURATION_KEYWORDS = Set.of(
            "하루", "이틀", "사흘", "24시간", "48시간", "72시간", "계속", "반복", "여러 번", "몇 번"
    );

    private final EmergencyRuleMapper ruleMapper;
    private final EmergencyEvidenceMapper evidenceMapper;
    private final ObjectMapper objectMapper;

    public EmergencyRuleMatcher(EmergencyRuleMapper ruleMapper, EmergencyEvidenceMapper evidenceMapper, ObjectMapper objectMapper) {
        this.ruleMapper = ruleMapper;
        this.evidenceMapper = evidenceMapper;
        this.objectMapper = objectMapper;
    }

    public List<MatchedEmergencyRule> match(EmergencyCheckRequest request, StructuredSymptomInput structured, FoodSafety food) {
        String haystack = buildHaystack(request, structured, food);
        Map<Long, EmergencyEvidence> evidences = new HashMap<>();
        for (EmergencyEvidence evidence : evidenceMapper.findAll()) {
            evidences.put(evidence.getId(), evidence);
        }
        List<MatchedEmergencyRule> matches = new ArrayList<>();
        for (EmergencyRule rule : ruleMapper.findAll()) {
            List<String> keywords = keywords(rule);
            List<String> matched = keywords.stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .filter(keyword -> haystack.contains(TextSafety.normalize(keyword)))
                    .distinct()
                    .limit(10)
                    .toList();
            if (!isMeaningfulMatch(rule, matched, haystack)) {
                continue;
            }
            EmergencyEvidence evidence = evidences.get(rule.getEvidenceId());
            if (evidence != null) {
                BigDecimal score = BigDecimal.valueOf(matched.size() * 10L + Math.max(rule.getPriority() == null ? 0 : rule.getPriority(), 0));
                matches.add(new MatchedEmergencyRule(rule, evidence, score, matched));
            }
        }
        return matches.stream()
                .sorted(Comparator.comparing((MatchedEmergencyRule m) -> riskRank(m.rule().getRiskLevel())).reversed()
                        .thenComparing(m -> m.rule().getPriority() == null ? 0 : m.rule().getPriority(), Comparator.reverseOrder())
                        .thenComparing(m -> m.rule().getId()))
                .limit(5)
                .toList();
    }

    private boolean isMeaningfulMatch(EmergencyRule rule, List<String> matched, String haystack) {
        if (matched.isEmpty()) return false;
        boolean hasCritical = matched.stream().anyMatch(this::isCriticalSingleKeyword);
        if (hasCritical) return true;
        boolean hasContext = containsAnyNormalized(haystack, CONTEXT_KEYWORDS);
        boolean allGeneric = matched.stream().allMatch(this::isGenericSupportKeyword);
        String ruleText = safe(rule.getRuleName()) + " " + safe(rule.getCategory()) + " " + safe(rule.getTriggerCondition());
        boolean complexDisease = containsAny(ruleText, "내출혈", "빈혈", "쇼크", "중독", "열사병", "자궁축농증", "파보", "디스템퍼", "GDV", "위확장", "위염전", "렙토스피라", "감염");
        if (!isDanger(rule.getRiskLevel())) {
            if (allGeneric && complexDisease) return false;
            if (allGeneric && containsAny(ruleText, "하루", "이틀", "지속") && !containsAnyNormalized(haystack, DURATION_KEYWORDS)) return false;
            return true;
        }
        if (allGeneric) return false;
        if (complexDisease) {
            return hasContext && matched.size() >= 2;
        }
        return matched.size() >= 2 && hasContext;
    }

    private boolean isCriticalSingleKeyword(String keyword) {
        String normalized = TextSafety.normalize(keyword);
        return CRITICAL_SINGLE_KEYWORDS.stream().map(TextSafety::normalize).anyMatch(normalized::contains);
    }

    private boolean isGenericSupportKeyword(String keyword) {
        String normalized = TextSafety.normalize(keyword);
        return GENERIC_SUPPORT_KEYWORDS.stream().map(TextSafety::normalize)
                .anyMatch(generic -> normalized.contains(generic) || generic.contains(normalized));
    }

    private boolean containsAnyNormalized(String haystack, Set<String> needles) {
        return needles.stream().map(TextSafety::normalize).anyMatch(haystack::contains);
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private String buildHaystack(EmergencyCheckRequest request, StructuredSymptomInput structured, FoodSafety food) {
        Set<String> values = new LinkedHashSet<>();
        values.add(request.symptomNote());
        values.add(request.suspectedFoodText());
        values.add(request.exposureAmount());
        if (request.symptomTags() != null) values.addAll(request.symptomTags());
        if (request.redFlags() != null) values.addAll(request.redFlags());
        if (structured != null) {
            values.add(structured.originalSummary());
            values.add(structured.suspectedFoodText());
            if (structured.symptomKeywords() != null) values.addAll(structured.symptomKeywords());
            if (structured.foodOrToxinKeywords() != null) values.addAll(structured.foodOrToxinKeywords());
            if (structured.observedSigns() != null) values.addAll(structured.observedSigns());
        }
        if (food != null) {
            values.add(food.getFoodName());
            values.add(food.getRiskCondition());
        }
        String normalized = TextSafety.normalize(String.join(" ", values.stream().filter(v -> v != null && !v.isBlank()).toList()));
        return normalized + " " + inferredKeywords(normalized);
    }

    private String inferredKeywords(String normalized) {
        List<String> inferred = new ArrayList<>();
        if (containsAny(normalized, "하임리히", "목에걸", "목에 걸", "켁켁", "캑캑", "컥컥", "기도", "기도폐쇄")) {
            inferred.add("질식 기도폐쇄 하임리히 목에 걸 켁켁 캑캑 숨 못 쉬 호흡곤란 이물질");
        }
        if (containsAny(normalized, "숨쉬기힘들", "숨쉬기 힘들", "숨 쉬기 힘들", "숨을못", "숨을 못", "숨못쉬", "숨 못 쉬", "호흡곤란")) {
            inferred.add("호흡곤란 숨 못 쉬 기도폐쇄");
        }
        if (containsAny(normalized, "간식먹", "먹다가", "삼켰", "삼킴") && containsAny(normalized, "켁켁", "캑캑", "컥컥", "목", "숨", "하임리히")) {
            inferred.add("질식 목에 걸 이물질 기도폐쇄 하임리히");
        }
        if (containsAny(normalized, "초코", "초콜릿", "초콜렛", "쵸코", "코코아", "카카오", "브라우니")) {
            inferred.add("초콜릿 초코 초콜렛 코코아 카카오 카페인 methylxanthine 다크초콜릿 베이킹초콜릿 주방 식품 독소");
        }
        return String.join(" ", inferred);
    }

    private List<String> keywords(EmergencyRule rule) {
        try {
            List<String> parsed = objectMapper.readValue(rule.getSymptomKeywords(), STRING_LIST);
            return parsed == null ? List.of() : parsed;
        } catch (Exception ex) {
            return List.of(rule.getRuleName(), rule.getCategory());
        }
    }

    static int riskRank(String riskLevel) {
        if ("위험".equals(riskLevel)) return 4;
        if ("주의".equals(riskLevel)) return 3;
        if ("정보부족".equals(riskLevel)) return 2;
        return 1;
    }

    static boolean isDanger(String riskLevel) {
        return "위험".equals(riskLevel);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
