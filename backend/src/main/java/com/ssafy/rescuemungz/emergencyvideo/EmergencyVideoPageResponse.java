package com.ssafy.rescuemungz.emergencyvideo;

import java.util.List;

public record EmergencyVideoPageResponse(
        List<EmergencyVideo> items,
        long total,
        int page,
        int size,
        int totalPages,
        String sort,
        String direction
) {
    public static EmergencyVideoPageResponse of(
            List<EmergencyVideo> items,
            long total,
            int page,
            int size,
            String sort,
            String direction
    ) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new EmergencyVideoPageResponse(items, total, page, size, totalPages, sort, direction);
    }
}
