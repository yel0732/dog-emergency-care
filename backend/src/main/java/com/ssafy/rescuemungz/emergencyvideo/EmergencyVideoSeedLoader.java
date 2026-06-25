package com.ssafy.rescuemungz.emergencyvideo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EmergencyVideoSeedLoader implements CommandLineRunner {
    private static final TypeReference<List<VideoSeed>> VIDEO_SEEDS = new TypeReference<>() {};
    private static final Map<String, String> CATEGORY_MAP = Map.of(
            "응급 기본", "응급상황 대처",
            "호흡/심장", "CPR/심폐소생술",
            "구토/소화기 증상", "구토/설사/소화기 증상",
            "설사/소화기 증상", "구토/설사/소화기 증상",
            "이물섭취/응급처치", "이물섭취/위험물질",
            "이물 섭취", "이물섭취/위험물질",
            "중독/위험물질", "이물섭취/위험물질"
    );
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "CPR/심폐소생술",
            "응급상황 대처",
            "발작/경련",
            "기도폐쇄/하임리히",
            "위험신호/건강체크",
            "구토/설사/소화기 증상",
            "이물섭취/위험물질",
            "호흡기 증상",
            "음식주의/중독",
            "약 복용/투약법"
    );

    private final EmergencyVideoMapper videoMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public EmergencyVideoSeedLoader(EmergencyVideoMapper videoMapper, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.videoMapper = videoMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        ensureVideoReviewRatingColumn();
        ClassPathResource resource = new ClassPathResource("emergency-videos.seed.json");
        if (!resource.exists()) return;
        try (InputStream input = resource.getInputStream()) {
            Set<String> loadedVideoIds = new HashSet<>();
            for (String url : videoMapper.findActiveYoutubeUrls()) {
                String videoId = youtubeVideoId(url);
                if (videoId != null) loadedVideoIds.add(videoId);
            }
            for (VideoSeed seed : objectMapper.readValue(input, VIDEO_SEEDS)) {
                if (seed.youtubeUrl() == null || seed.youtubeUrl().isBlank()) continue;
                String videoId = youtubeVideoId(seed.youtubeUrl());
                if (videoId != null && loadedVideoIds.contains(videoId)) continue;
                videoMapper.insertSeed(
                        seed.title(),
                        category(seed.category()),
                        seed.symptomTag(),
                        seed.description(),
                        seed.youtubeUrl(),
                        seed.source(),
                        seed.channelName(),
                        seed.publishedAt()
                );
                if (videoId != null) loadedVideoIds.add(videoId);
            }
        }
    }

    private void ensureVideoReviewRatingColumn() {
        if (!jdbcTemplate.queryForList("SHOW COLUMNS FROM video_comments LIKE 'rating'").isEmpty()) return;
        jdbcTemplate.execute("ALTER TABLE video_comments ADD COLUMN rating TINYINT NOT NULL DEFAULT 5 AFTER content");
    }

    private String category(String value) {
        String mapped = CATEGORY_MAP.getOrDefault(value, value);
        return VALID_CATEGORIES.contains(mapped) ? mapped : "응급상황 대처";
    }

    private String youtubeVideoId(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.endsWith("youtu.be")) {
                String path = uri.getPath();
                return path == null || path.length() <= 1 ? null : path.substring(1);
            }
            if (host.contains("youtube.com")) {
                String query = uri.getRawQuery();
                if (query == null) return null;
                for (String pair : query.split("&")) {
                    String[] parts = pair.split("=", 2);
                    if (parts.length == 2 && "v".equals(parts[0])) {
                        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private record VideoSeed(
            String title,
            String category,
            String symptomTag,
            String symptom,
            String description,
            String youtubeUrl,
            String source,
            String channelName,
            String publishedAt
    ) {
    }
}
