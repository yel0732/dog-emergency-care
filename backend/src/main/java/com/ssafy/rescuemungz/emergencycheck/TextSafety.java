package com.ssafy.rescuemungz.emergencycheck;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class TextSafety {
    private static final Pattern HTML = Pattern.compile("<[^>]*>|script\\s*:", Pattern.CASE_INSENSITIVE);
    private static final List<String> INJECTION_HINTS = List.of(
            "ignore previous", "system prompt", "api key", "jwt", "db 전체", "시스템 프롬프트",
            "이전 지시", "무시해", "비밀번호", "다른 사용자"
    );

    private TextSafety() {
    }

    static String clean(String value, int maxLength) {
        if (value == null) return null;
        String cleaned = HTML.matcher(value).replaceAll(" ").replace('\u0000', ' ').trim();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength).trim();
        }
        return cleaned;
    }

    static boolean suspicious(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        return INJECTION_HINTS.stream().anyMatch(lower::contains) || HTML.matcher(value).find();
    }

    static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}]+", "")
                .trim();
    }

    static String politeGuardianTone(String value) {
        String cleaned = clean(value, 1000);
        if (cleaned == null || cleaned.isBlank()) return cleaned;
        return cleaned
                .replace("대상이다", "대상입니다")
                .replace("중요하다", "중요합니다")
                .replace("필요하다", "필요합니다")
                .replace("가능하다", "가능합니다")
                .replace("권장한다", "권장합니다")
                .replace("권장된다", "권장됩니다")
                .replace("분류된다", "분류됩니다")
                .replace("제시된다", "제시됩니다")
                .replace("보고된다", "보고됩니다")
                .replace("문의한다", "문의하세요")
                .replace("상담한다", "상담하세요")
                .replace("확인한다", "확인합니다")
                .replace("연락한다", "연락합니다")
                .replace("방문한다", "방문하세요")
                .replace("진행한다", "진행합니다")
                .replace("관찰한다", "관찰합니다")
                .replace("준비한다", "준비합니다")
                .replace("포함한다", "포함합니다")
                .replace("사용한다", "사용합니다")
                .replace("연결한다", "연결합니다")
                .replace("분류한다", "분류합니다")
                .replace("관리한다", "관리합니다")
                .replace("해야 한다", "해야 합니다")
                .replace("해야한다", "해야 합니다")
                .replace("높다", "높습니다")
                .replace("낮다", "낮습니다")
                .replace("않음", "않습니다")
                .replace("있음", "있습니다")
                .replace("피한다", "피하세요")
                .replace("기록한다", "기록하세요")
                .replace("낮춘다", "낮춥니다")
                .replace("확인받는다", "확인받으세요")
                .replace("위험 신호다", "위험 신호입니다")
                .replace("응급이다", "응급입니다")
                .replace("위험이 크다", "위험이 큽니다")
                .replace("보관한다", "보관하세요")
                .replace("이동한다", "이동하세요")
                .replace("하지 않는다", "하지 마세요")
                .replace("말라", "마세요")
                .replace("할 수 있다", "할 수 있습니다")
                .replace("될 수 있다", "될 수 있습니다")
                .replace("일으킬 수 있다", "일으킬 수 있습니다")
                .replace("이어질 수 있다", "이어질 수 있습니다")
                .replace("진행할 수 있다", "진행할 수 있습니다")
                .replace("나타날 수 있다", "나타날 수 있습니다")
                .replace("수 있다", "수 있습니다")
                .replace("수 없다", "수 없습니다")
                .replaceAll("([가-힣\\s]+[을를]) 한다([.。\\s]|$)", "$1 하세요$2")
                .replaceAll("이다([.。\\s]|$)", "입니다$1")
                .trim();
    }
}
