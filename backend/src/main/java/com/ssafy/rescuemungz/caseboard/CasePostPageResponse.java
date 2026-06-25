package com.ssafy.rescuemungz.caseboard;

import java.util.List;

public record CasePostPageResponse(
        List<CasePost> items,
        long total,
        int page,
        int size,
        int totalPages,
        String sort,
        String direction
) {
    public static CasePostPageResponse of(
            List<CasePost> items,
            long total,
            int page,
            int size,
            String sort,
            String direction
    ) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new CasePostPageResponse(items, total, page, size, totalPages, sort, direction);
    }
}
