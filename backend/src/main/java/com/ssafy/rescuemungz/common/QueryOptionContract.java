package com.ssafy.rescuemungz.common;

import java.util.List;
import java.util.Set;

public final class QueryOptionContract {
    public static final String DEFAULT_SORT = "latest";
    public static final String DEFAULT_DIRECTION = "desc";

    public static final List<String> VIDEO_SORTS = List.of("latest", "bookmarks", "title", "category");
    public static final List<String> CASE_SORTS = List.of("latest", "title", "category", "views", "comments", "followers");
    public static final List<String> DIRECTIONS = List.of("desc", "asc");

    public static final Set<String> VIDEO_SORT_SET = Set.copyOf(VIDEO_SORTS);
    public static final Set<String> CASE_SORT_SET = Set.copyOf(CASE_SORTS);
    public static final Set<String> DIRECTION_SET = Set.copyOf(DIRECTIONS);

    private QueryOptionContract() {
    }
}
