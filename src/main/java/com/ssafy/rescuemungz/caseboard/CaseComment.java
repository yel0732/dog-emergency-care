package com.ssafy.rescuemungz.caseboard;

import java.time.LocalDateTime;

public class CaseComment {
    private Long id;
    private Long postId;
    private Long parentId;
    private Long userId;
    private String userName;
    private String userProfileImageUrl;
    private Boolean followingAuthor;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserProfileImageUrl() { return userProfileImageUrl; }
    public void setUserProfileImageUrl(String userProfileImageUrl) { this.userProfileImageUrl = userProfileImageUrl; }
    public Boolean getFollowingAuthor() { return followingAuthor; }
    public void setFollowingAuthor(Boolean followingAuthor) { this.followingAuthor = followingAuthor; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
