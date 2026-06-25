package com.ssafy.rescuemungz.emergencyvideo;

import com.ssafy.rescuemungz.auth.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency-videos")
public class EmergencyVideoController {
    private final EmergencyVideoService videoService;

    public EmergencyVideoController(EmergencyVideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<EmergencyVideo> create(@Valid @RequestBody EmergencyVideoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoService.create(request));
    }

    @GetMapping
    public EmergencyVideoPageResponse search(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "latest") String sort,
                                             @RequestParam(defaultValue = "desc") String direction,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "6") int size) {
        return videoService.search(category, keyword, sort, direction, page, size, AuthUtil.optionalUserIdOrZero());
    }

    @GetMapping("/bookmarks")
    public EmergencyVideoPageResponse bookmarks(@RequestParam(required = false) String category,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "latest") String sort,
                                                @RequestParam(defaultValue = "desc") String direction,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "6") int size) {
        return videoService.bookmarkedVideos(category, keyword, sort, direction, page, size, AuthUtil.requiredUserId());
    }

    @GetMapping("/{id}")
    public EmergencyVideo find(@PathVariable long id) {
        return videoService.find(id, AuthUtil.optionalUserIdOrZero());
    }

    @PutMapping("/{id}")
    public EmergencyVideo update(@PathVariable long id, @Valid @RequestBody EmergencyVideoRequest request) {
        return videoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        videoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/bookmark")
    public EmergencyVideo bookmark(@PathVariable long id) {
        return videoService.bookmark(id, AuthUtil.requiredUserId());
    }

    @DeleteMapping("/{id}/bookmark")
    public EmergencyVideo unbookmark(@PathVariable long id) {
        return videoService.unbookmark(id, AuthUtil.requiredUserId());
    }
}
