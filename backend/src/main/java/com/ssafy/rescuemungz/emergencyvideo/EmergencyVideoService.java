package com.ssafy.rescuemungz.emergencyvideo;

import com.ssafy.rescuemungz.common.NotFoundException;
import com.ssafy.rescuemungz.common.QueryOptionContract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmergencyVideoService {
    private final EmergencyVideoMapper videoMapper;

    public EmergencyVideoService(EmergencyVideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    @Transactional
    public EmergencyVideo create(EmergencyVideoRequest request) {
        EmergencyVideo video = toVideo(request);
        videoMapper.insert(video);
        return find(video.getId(), 0);
    }

    public EmergencyVideoPageResponse search(String category, String keyword, String sort, String direction, int page, int size, long viewerId) {
        String cleanCategory = normalizeCategory(category);
        String cleanKeyword = blankToNull(keyword);
        int cleanPage = normalizePage(page);
        int cleanSize = normalizeSize(size);
        String cleanSort = normalizeSort(sort);
        String cleanDirection = normalizeDirection(direction);
        String orderBy = orderBy(cleanSort, cleanDirection, false);
        int offset = cleanPage * cleanSize;
        List<EmergencyVideo> items = videoMapper.search(cleanCategory, cleanKeyword, viewerId, orderBy, cleanSize, offset);
        long total = videoMapper.count(cleanCategory, cleanKeyword);
        return EmergencyVideoPageResponse.of(items, total, cleanPage, cleanSize, cleanSort, cleanDirection);
    }

    public EmergencyVideoPageResponse bookmarkedVideos(String category, String keyword, String sort, String direction, int page, int size, long userId) {
        String cleanCategory = normalizeCategory(category);
        String cleanKeyword = blankToNull(keyword);
        int cleanPage = normalizePage(page);
        int cleanSize = normalizeSize(size);
        String cleanSort = normalizeSort(sort);
        String cleanDirection = normalizeDirection(direction);
        String orderBy = orderBy(cleanSort, cleanDirection, true);
        int offset = cleanPage * cleanSize;
        List<EmergencyVideo> items = videoMapper.findBookmarkedByUser(userId, cleanCategory, cleanKeyword, orderBy, cleanSize, offset);
        long total = videoMapper.countBookmarkedByUser(userId, cleanCategory, cleanKeyword);
        return EmergencyVideoPageResponse.of(items, total, cleanPage, cleanSize, cleanSort, cleanDirection);
    }

    public EmergencyVideo find(long id, long viewerId) {
        EmergencyVideo video = videoMapper.findById(id, viewerId);
        if (video == null) {
            throw new NotFoundException("Emergency video not found.");
        }
        return video;
    }

    @Transactional
    public EmergencyVideo update(long id, EmergencyVideoRequest request) {
        EmergencyVideo video = toVideo(request);
        video.setId(id);
        if (videoMapper.update(video) == 0) {
            throw new NotFoundException("Emergency video not found.");
        }
        return find(id, 0);
    }

    @Transactional
    public void delete(long id) {
        if (videoMapper.delete(id) == 0) {
            throw new NotFoundException("Emergency video not found.");
        }
    }

    @Transactional
    public EmergencyVideo bookmark(long videoId, long userId) {
        find(videoId, userId);
        videoMapper.bookmark(videoId, userId);
        return find(videoId, userId);
    }

    @Transactional
    public EmergencyVideo unbookmark(long videoId, long userId) {
        find(videoId, userId);
        videoMapper.unbookmark(videoId, userId);
        return find(videoId, userId);
    }

    private EmergencyVideo toVideo(EmergencyVideoRequest request) {
        EmergencyVideo video = new EmergencyVideo();
        video.setTitle(request.title().trim());
        video.setCategory(normalizeRequiredCategory(request.category()));
        video.setSymptom(blankToNull(request.symptom()));
        video.setDescription(blankToNull(request.description()));
        video.setYoutubeUrl(request.youtubeUrl().trim());
        return video;
    }

    private String normalizeRequiredCategory(String category) {
        String normalized = normalizeCategory(category);
        if (normalized == null) {
            throw new IllegalArgumentException("카테고리를 입력해 주세요.");
        }
        return normalized;
    }

    private String normalizeCategory(String category) {
        String value = blankToNull(category);
        if (value == null) return null;
        return switch (value) {
            case "구토/소화기 증상", "설사/소화기 증상" -> "구토/설사/소화기 증상";
            case "이물섭취/응급처치", "이물섭취", "이물 섭취", "이물 섭취/응급처치", "중독/위험물질" -> "이물섭취/위험물질";
            default -> value;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size < 1) return 6;
        return Math.min(size, 50);
    }

    private String normalizeSort(String sort) {
        String value = blankToNull(sort);
        if (value == null) {
            return QueryOptionContract.DEFAULT_SORT;
        }
        if (!QueryOptionContract.VIDEO_SORT_SET.contains(value)) {
            throw new IllegalArgumentException("영상 정렬 기준은 latest, title, category, bookmarks 중 하나여야 합니다.");
        }
        return value;
    }

    private String normalizeDirection(String direction) {
        String value = blankToNull(direction);
        if (value == null) {
            return QueryOptionContract.DEFAULT_DIRECTION;
        }
        value = value.toLowerCase();
        if (!QueryOptionContract.DIRECTION_SET.contains(value)) {
            throw new IllegalArgumentException("정렬 방향은 asc 또는 desc만 사용할 수 있습니다.");
        }
        return value;
    }

    private String orderBy(String sort, String direction, boolean bookmarkedOnly) {
        String safeDirection = "asc".equals(direction) ? "ASC" : "DESC";
        return switch (sort) {
            case "title" -> "v.title " + safeDirection;
            case "category" -> "v.category " + safeDirection;
            case "bookmarks" -> "(SELECT COUNT(*) FROM video_bookmarks b2 WHERE b2.video_id = v.id) " + safeDirection;
            default -> bookmarkedOnly ? "b.created_at " + safeDirection : "v.created_at " + safeDirection;
        };
    }
}
