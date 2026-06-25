package com.ssafy.rescuemungz.caseboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.rescuemungz.common.ForbiddenException;
import com.ssafy.rescuemungz.common.NotFoundException;
import com.ssafy.rescuemungz.common.QueryOptionContract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CaseBoardService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final CasePostMapper postMapper;
    private final CaseCommentMapper commentMapper;
    private final ObjectMapper objectMapper;

    public CaseBoardService(CasePostMapper postMapper, CaseCommentMapper commentMapper, ObjectMapper objectMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.objectMapper = objectMapper;
    }

    public CasePostPageResponse search(String category, String keyword, Long authorId, String sort, String direction, boolean followingOnly, int page, int size, long viewerId) {
        String cleanCategory = blankToNull(category);
        String cleanKeyword = blankToNull(keyword);
        int cleanPage = normalizePage(page);
        int cleanSize = normalizeSize(size);
        String cleanSort = normalizeSort(sort);
        String cleanDirection = normalizeDirection(direction);
        int offset = cleanPage * cleanSize;
        List<CasePost> items = hydrate(postMapper.search(cleanCategory, cleanKeyword, authorId, followingOnly, viewerId, cleanSort, cleanDirection, cleanSize, offset));
        long total = postMapper.count(cleanCategory, cleanKeyword, authorId, followingOnly, viewerId);
        return CasePostPageResponse.of(items, total, cleanPage, cleanSize, cleanSort, cleanDirection);
    }

    @Transactional
    public CasePost create(long userId, CasePostRequest request) {
        CasePost post = toPost(request);
        post.setUserId(userId);
        postMapper.insert(post);
        return find(post.getId(), false, userId);
    }

    @Transactional
    public CasePost find(long id, boolean increaseView, long viewerId) {
        if (increaseView) {
            postMapper.increaseViewCount(id);
        }
        CasePost post = postMapper.findById(id, viewerId);
        if (post == null) {
            throw new NotFoundException("Case post not found.");
        }
        hydrate(post);
        return post;
    }

    @Transactional
    public CasePost update(long id, long userId, CasePostRequest request) {
        assertPostOwner(id, userId);
        CasePost post = toPost(request);
        post.setId(id);
        post.setUserId(userId);
        if (postMapper.update(post) == 0) {
            throw new NotFoundException("Case post not found.");
        }
        return find(id, false, userId);
    }

    @Transactional
    public void delete(long id, long userId, boolean admin) {
        if (!admin) {
            assertPostOwner(id, userId);
        } else {
            findPostExists(id);
        }
        int deleted = admin ? postMapper.deleteById(id) : postMapper.delete(id, userId);
        if (deleted == 0) {
            throw new NotFoundException("Case post not found.");
        }
    }

    public List<CaseComment> comments(long postId, long viewerId) {
        findPostExists(postId);
        return commentMapper.findByPostId(postId, viewerId);
    }

    @Transactional
    public CaseComment createComment(long postId, long userId, CaseCommentRequest request) {
        findPostExists(postId);
        Long parentId = request.parentId();
        if (parentId != null) {
            CaseComment parent = findComment(parentId);
            if (!parent.getPostId().equals(postId)) {
                throw new IllegalArgumentException("Reply target does not belong to this post.");
            }
            if (parent.getParentId() != null) {
                throw new IllegalArgumentException("Replies can be added to top-level comments only.");
            }
        }
        CaseComment comment = new CaseComment();
        comment.setPostId(postId);
        comment.setParentId(parentId);
        comment.setUserId(userId);
        comment.setContent(request.content().trim());
        commentMapper.insert(comment);
        return findComment(comment.getId());
    }

    @Transactional
    public CaseComment updateComment(long id, long userId, CaseCommentRequest request) {
        assertCommentOwner(id, userId);
        CaseComment comment = new CaseComment();
        comment.setId(id);
        comment.setUserId(userId);
        comment.setContent(request.content().trim());
        if (commentMapper.update(comment) == 0) {
            throw new NotFoundException("Case comment not found.");
        }
        return findComment(id);
    }

    @Transactional
    public void deleteComment(long id, long userId, boolean admin) {
        if (!admin) {
            assertCommentOwner(id, userId);
        } else {
            findComment(id);
        }
        int deleted = admin ? commentMapper.deleteWithRepliesById(id) : commentMapper.deleteWithReplies(id, userId);
        if (deleted == 0) {
            throw new NotFoundException("Case comment not found.");
        }
    }

    private CaseComment findComment(long id) {
        CaseComment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new NotFoundException("Case comment not found.");
        }
        return comment;
    }

    private void assertPostOwner(long id, long userId) {
        CasePost post = postMapper.findById(id, userId);
        if (post == null) {
            throw new NotFoundException("게시글을 찾을 수 없습니다.");
        }
        if (!post.getUserId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글만 수정하거나 삭제할 수 있습니다.");
        }
    }

    private void assertCommentOwner(long id, long userId) {
        CaseComment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new NotFoundException("댓글을 찾을 수 없습니다.");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다.");
        }
    }

    private void findPostExists(long id) {
        if (postMapper.findById(id, 0) == null) {
            throw new NotFoundException("Case post not found.");
        }
    }

    private CasePost toPost(CasePostRequest request) {
        CasePost post = new CasePost();
        post.setTitle(request.title().trim());
        post.setCategory(request.category().trim());
        post.setContent(request.content().trim());
        List<String> images = sanitizeImages(request.imageUrls());
        post.setImageUrls(images);
        post.setImageUrlsJson(toJson(images));
        return post;
    }

    private List<CasePost> hydrate(List<CasePost> posts) {
        posts.forEach(this::hydrate);
        return posts;
    }

    private void hydrate(CasePost post) {
        post.setImageUrls(fromJson(post.getImageUrlsJson()));
    }

    private List<String> sanitizeImages(List<String> urls) {
        if (urls == null) return List.of();
        return urls.stream()
                .map(url -> url == null ? "" : url.trim())
                .filter(url -> !url.isBlank())
                .limit(5)
                .toList();
    }

    private String toJson(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls == null ? List.of() : urls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> parsed = objectMapper.readValue(json, STRING_LIST);
            return parsed == null ? List.of() : parsed;
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
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
        if (!QueryOptionContract.CASE_SORT_SET.contains(value)) {
            throw new IllegalArgumentException("게시판 정렬 기준은 latest, title, category, views, comments, followers 중 하나여야 합니다.");
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

}
