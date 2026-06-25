package com.ssafy.rescuemungz.caseboard;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CaseBoardController {
    private final CaseBoardService service;

    public CaseBoardController(CaseBoardService service) {
        this.service = service;
    }

    @GetMapping("/api/case-posts")
    public CasePostPageResponse search(@RequestParam(required = false) String category,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long authorId,
                                       @RequestParam(defaultValue = "latest") String sort,
                                       @RequestParam(defaultValue = "desc") String direction,
                                       @RequestParam(defaultValue = "false") boolean followingOnly,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "6") int size) {
        return service.search(category, keyword, authorId, sort, direction, followingOnly, page, size, AuthUtil.requiredUserId());
    }

    @PostMapping("/api/case-posts")
    public ResponseEntity<CasePost> create(@Valid @RequestBody CasePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(AuthUtil.requiredUserId(), request));
    }

    @GetMapping("/api/case-posts/{id}")
    public CasePost find(@PathVariable long id) {
        return service.find(id, true, AuthUtil.requiredUserId());
    }

    @PutMapping("/api/case-posts/{id}")
    public CasePost update(@PathVariable long id, @Valid @RequestBody CasePostRequest request) {
        return service.update(id, AuthUtil.requiredUserId(), request);
    }

    @DeleteMapping("/api/case-posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id, AuthUtil.requiredUserId(), AuthUtil.currentUserIsAdmin());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/case-posts/{postId}/comments")
    public List<CaseComment> comments(@PathVariable long postId) {
        return service.comments(postId, AuthUtil.requiredUserId());
    }

    @PostMapping("/api/case-posts/{postId}/comments")
    public ResponseEntity<CaseComment> createComment(@PathVariable long postId, @Valid @RequestBody CaseCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createComment(postId, AuthUtil.requiredUserId(), request));
    }

    @PutMapping("/api/case-comments/{commentId}")
    public CaseComment updateComment(@PathVariable long commentId, @Valid @RequestBody CaseCommentRequest request) {
        return service.updateComment(commentId, AuthUtil.requiredUserId(), request);
    }

    @DeleteMapping("/api/case-comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable long commentId) {
        service.deleteComment(commentId, AuthUtil.requiredUserId(), AuthUtil.currentUserIsAdmin());
        return ResponseEntity.noContent().build();
    }
}
