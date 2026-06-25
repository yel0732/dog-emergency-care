package com.ssafy.rescuemungz.emergencycheck;

import java.math.BigDecimal;
import java.util.List;

public record MatchedEmergencyRule(
        EmergencyRule rule,
        EmergencyEvidence evidence,
        BigDecimal score,
        List<String> matchedKeywords
) {
}
