package com.ssafy.rescuemungz.user;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper {
    @Insert("""
            INSERT IGNORE INTO user_follows(follower_id, following_id)
            VALUES(#{followerId}, #{followingId})
            """)
    int follow(@Param("followerId") long followerId, @Param("followingId") long followingId);

    @Delete("DELETE FROM user_follows WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int unfollow(@Param("followerId") long followerId, @Param("followingId") long followingId);

    @Select("""
            SELECT EXISTS(
                SELECT 1 FROM user_follows
                WHERE follower_id = #{followerId} AND following_id = #{followingId}
            )
            """)
    boolean isFollowing(@Param("followerId") long followerId, @Param("followingId") long followingId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE following_id = #{userId}")
    int countFollowers(long userId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE follower_id = #{userId}")
    int countFollowing(long userId);

    @Select("""
            SELECT u.id, u.nickname, u.profile_image AS profile_image_url,
                   COALESCE(fc.follower_count, 0) AS follower_count,
                   COALESCE(fgc.following_count, 0) AS following_count,
                   CASE WHEN vf.following_id IS NULL THEN FALSE ELSE TRUE END AS following,
                   f.created_at AS followed_at
            FROM user_follows f
            JOIN users u ON u.id = f.following_id
            LEFT JOIN (
                SELECT following_id, COUNT(*) AS follower_count
                FROM user_follows
                GROUP BY following_id
            ) fc ON fc.following_id = u.id
            LEFT JOIN (
                SELECT follower_id, COUNT(*) AS following_count
                FROM user_follows
                GROUP BY follower_id
            ) fgc ON fgc.follower_id = u.id
            LEFT JOIN user_follows vf ON vf.follower_id = #{viewerId} AND vf.following_id = u.id
            WHERE f.follower_id = #{viewerId}
              AND u.deleted_at IS NULL
            ORDER BY f.created_at DESC, u.id DESC
            """)
    List<UserFollowResponse> findFollowing(long viewerId);

    @Select("""
            SELECT u.id, u.nickname, u.profile_image AS profile_image_url,
                   COALESCE(fc.follower_count, 0) AS follower_count,
                   COALESCE(fgc.following_count, 0) AS following_count,
                   CASE WHEN vf.following_id IS NULL THEN FALSE ELSE TRUE END AS following,
                   f.created_at AS followed_at
            FROM user_follows f
            JOIN users u ON u.id = f.follower_id
            LEFT JOIN (
                SELECT following_id, COUNT(*) AS follower_count
                FROM user_follows
                GROUP BY following_id
            ) fc ON fc.following_id = u.id
            LEFT JOIN (
                SELECT follower_id, COUNT(*) AS following_count
                FROM user_follows
                GROUP BY follower_id
            ) fgc ON fgc.follower_id = u.id
            LEFT JOIN user_follows vf ON vf.follower_id = #{viewerId} AND vf.following_id = u.id
            WHERE f.following_id = #{viewerId}
              AND u.deleted_at IS NULL
            ORDER BY f.created_at DESC, u.id DESC
            """)
    List<UserFollowResponse> findFollowers(long viewerId);
}
