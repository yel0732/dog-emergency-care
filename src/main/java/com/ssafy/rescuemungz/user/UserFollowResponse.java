package com.ssafy.rescuemungz.user;

import java.time.LocalDateTime;

public class UserFollowResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private int followerCount;
    private int followingCount;
    private boolean following;
    private LocalDateTime followedAt;

    public UserFollowResponse() {
    }

    public UserFollowResponse(Long id, String nickname, String profileImageUrl, int followerCount, int followingCount, boolean following, LocalDateTime followedAt) {
        this.id = id;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.following = following;
        this.followedAt = followedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }
    public boolean isFollowing() { return following; }
    public void setFollowing(boolean following) { this.following = following; }
    public LocalDateTime getFollowedAt() { return followedAt; }
    public void setFollowedAt(LocalDateTime followedAt) { this.followedAt = followedAt; }
}
