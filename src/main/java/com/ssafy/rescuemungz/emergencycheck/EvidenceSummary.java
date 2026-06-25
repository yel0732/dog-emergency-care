package com.ssafy.rescuemungz.emergencycheck;

public record EvidenceSummary(
        Long evidenceId,
        Long ruleId,
        String source,
        String title,
        String summary,
        String recommendedAction,
        String avoidAction,
        String sourceUrl
) {
}
