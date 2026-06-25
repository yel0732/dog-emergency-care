package com.ssafy.rescuemungz.emergencycheck;

import java.util.List;

public record EmergencyGuideResponse(
        String emergencyLevel,
        String headline,
        List<String> immediateActions,
        List<String> avoidActions,
        List<String> observationChecklist,
        List<String> escalationCriteria,
        List<String> optionalQuestions,
        List<EvidenceSummary> evidenceSummary,
        String disclaimer,
        AvailableActions availableActions,
        boolean fallbackUsed
) {
}
