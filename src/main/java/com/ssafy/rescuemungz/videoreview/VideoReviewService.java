package com.ssafy.rescuemungz.videoreview;

import com.ssafy.rescuemungz.common.ForbiddenException;
import com.ssafy.rescuemungz.common.NotFoundException;
import com.ssafy.rescuemungz.emergencyvideo.EmergencyVideoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VideoReviewService {
    private final VideoReviewMapper reviewMapper;
    private final EmergencyVideoService videoService;

    public VideoReviewService(VideoReviewMapper reviewMapper, EmergencyVideoService videoService) {
        this.reviewMapper = reviewMapper;
        this.videoService = videoService;
    }

    @Transactional
    public VideoReview create(long videoId, long userId, VideoReviewRequest request) {
        videoService.find(videoId, 0);
        VideoReview review = toReview(request);
        review.setVideoId(videoId);
        review.setUserId(userId);
        reviewMapper.insert(review);
        return find(review.getId());
    }

    public List<VideoReview> findByVideo(long videoId) {
        videoService.find(videoId, 0);
        return reviewMapper.findByVideoId(videoId);
    }

    public VideoReview find(long id) {
        VideoReview review = reviewMapper.findById(id);
        if (review == null) {
            throw new NotFoundException("Review not found.");
        }
        return review;
    }

    @Transactional
    public VideoReview update(long id, long userId, VideoReviewRequest request) {
        assertReviewOwner(id, userId);
        VideoReview review = toReview(request);
        review.setId(id);
        review.setUserId(userId);
        if (reviewMapper.update(review) == 0) {
            throw new NotFoundException("Review not found.");
        }
        return find(id);
    }

    @Transactional
    public void delete(long id, long userId, boolean admin) {
        if (!admin) {
            assertReviewOwner(id, userId);
        } else {
            find(id);
        }
        int deleted = admin ? reviewMapper.deleteById(id) : reviewMapper.delete(id, userId);
        if (deleted == 0) {
            throw new NotFoundException("Review not found.");
        }
    }

    private VideoReview toReview(VideoReviewRequest request) {
        VideoReview review = new VideoReview();
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        return review;
    }

    private void assertReviewOwner(long id, long userId) {
        VideoReview review = reviewMapper.findById(id);
        if (review == null) {
            throw new NotFoundException("Review not found.");
        }
        if (!review.getUserId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 리뷰만 수정하거나 삭제할 수 있습니다.");
        }
    }
}
