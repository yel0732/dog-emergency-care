package com.ssafy.rescuemungz.emergencycheck;

public record AvailableActions(
        boolean showHospitalButton,
        boolean showVideoButton,
        String videoCategoryCode,
        Long emergencyCheckId,
        String warningMessage
) {
}
