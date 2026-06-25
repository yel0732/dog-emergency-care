package com.ssafy.rescuemungz.videoreview;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VideoReviewMapper {
    @Insert("""
            INSERT INTO video_comments(video_id, user_id, rating, content)
            VALUES(#{videoId}, #{userId}, #{rating}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VideoReview review);

    @Select("""
            SELECT r.id, r.video_id, r.user_id, u.nickname AS user_name, r.rating, r.content, r.created_at, r.updated_at
            FROM video_comments r
            JOIN users u ON u.id = r.user_id
            WHERE r.video_id = #{videoId} AND r.deleted_at IS NULL
            ORDER BY r.id DESC
            """)
    List<VideoReview> findByVideoId(long videoId);

    @Select("""
            SELECT r.id, r.video_id, r.user_id, u.nickname AS user_name, r.rating, r.content, r.created_at, r.updated_at
            FROM video_comments r
            JOIN users u ON u.id = r.user_id
            WHERE r.video_id = #{videoId}
              AND r.user_id = #{userId}
              AND r.deleted_at IS NULL
            """)
    VideoReview findActiveByVideoAndUser(@Param("videoId") long videoId, @Param("userId") long userId);

    @Select("""
            SELECT r.id, r.video_id, r.user_id, u.nickname AS user_name, r.rating, r.content, r.created_at, r.updated_at
            FROM video_comments r
            JOIN users u ON u.id = r.user_id
            WHERE r.id = #{id} AND r.deleted_at IS NULL
            """)
    VideoReview findById(long id);

    @Update("""
            UPDATE video_comments
            SET rating=#{rating}, content=#{content}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND user_id=#{userId} AND deleted_at IS NULL
            """)
    int update(VideoReview review);

    @Delete("UPDATE video_comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL")
    int delete(@Param("id") long id, @Param("userId") long userId);

    @Delete("UPDATE video_comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int deleteById(long id);
}
