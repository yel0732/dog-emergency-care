package com.ssafy.rescuemungz.videoreview;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VideoReviewController {
    private final VideoReviewService reviewService;

    public VideoReviewController(VideoReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/emergency-videos/{videoId}/reviews")
    public ResponseEntity<VideoReview> create(@PathVariable long videoId, @Valid @RequestBody VideoReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(videoId, AuthUtil.requiredUserId(), request));
    }

    @GetMapping("/api/emergency-videos/{videoId}/reviews")
    public List<VideoReview> findByVideo(@PathVariable long videoId) {
        return reviewService.findByVideo(videoId);
    }

    @GetMapping("/api/video-reviews/{reviewId}")
    public VideoReview find(@PathVariable long reviewId) {
        return reviewService.find(reviewId);
    }

    @PutMapping("/api/video-reviews/{reviewId}")
    public VideoReview update(@PathVariable long reviewId, @Valid @RequestBody VideoReviewRequest request) {
        return reviewService.update(reviewId, AuthUtil.requiredUserId(), request);
    }

    @DeleteMapping("/api/video-reviews/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable long reviewId) {
        reviewService.delete(reviewId, AuthUtil.requiredUserId(), AuthUtil.currentUserIsAdmin());
        return ResponseEntity.noContent().build();
    }
}
