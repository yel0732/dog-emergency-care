package com.ssafy.rescuemungz.emergencyvideo;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EmergencyVideoMapper {
    @Insert("""
            INSERT INTO videos(title, category, symptom_tag, video_description, youtube_url, source)
            VALUES(#{title}, #{category}, #{symptom}, #{description}, #{youtubeUrl}, '관리자 등록')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EmergencyVideo video);

    @Insert("""
            INSERT INTO videos(title, category, symptom_tag, video_description, youtube_url, source, channel_name, published_at)
            VALUES(#{title}, #{category}, #{symptom}, #{description}, #{youtubeUrl}, #{source}, #{channelName}, #{publishedAt})
            """)
    int insertSeed(@Param("title") String title,
                   @Param("category") String category,
                   @Param("symptom") String symptom,
                   @Param("description") String description,
                   @Param("youtubeUrl") String youtubeUrl,
                   @Param("source") String source,
                   @Param("channelName") String channelName,
                   @Param("publishedAt") String publishedAt);

    @Select("""
            SELECT youtube_url
            FROM videos
            WHERE deleted_at IS NULL
            """)
    List<String> findActiveYoutubeUrls();

    @Select("""
            SELECT v.id, v.title, v.category, v.symptom_tag AS symptom, v.video_description AS description, v.youtube_url,
                   (SELECT COUNT(*) FROM video_bookmarks b WHERE b.video_id = v.id) AS bookmark_count,
                   EXISTS(SELECT 1 FROM video_bookmarks b WHERE b.video_id = v.id AND b.user_id = #{viewerId}) AS bookmarked,
                   COALESCE((SELECT ROUND(AVG(r.rating), 1) FROM video_comments r WHERE r.video_id = v.id AND r.deleted_at IS NULL), 0) AS average_rating,
                   (SELECT COUNT(*) FROM video_comments r WHERE r.video_id = v.id AND r.deleted_at IS NULL) AS review_count,
                   v.created_at, v.updated_at
            FROM videos v
            WHERE (#{category} IS NULL OR category = #{category})
              AND v.deleted_at IS NULL
              AND (#{keyword} IS NULL OR v.title LIKE CONCAT('%', #{keyword}, '%') OR v.symptom_tag LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY ${orderBy}, v.id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<EmergencyVideo> search(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("viewerId") long viewerId,
            @Param("orderBy") String orderBy,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Select("""
            SELECT COUNT(*)
            FROM videos v
            WHERE (#{category} IS NULL OR v.category = #{category})
              AND v.deleted_at IS NULL
              AND (#{keyword} IS NULL OR v.title LIKE CONCAT('%', #{keyword}, '%') OR v.symptom_tag LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long count(@Param("category") String category, @Param("keyword") String keyword);

    @Select("""
            SELECT v.id, v.title, v.category, v.symptom_tag AS symptom, v.video_description AS description, v.youtube_url,
                   (SELECT COUNT(*) FROM video_bookmarks b WHERE b.video_id = v.id) AS bookmark_count,
                   EXISTS(SELECT 1 FROM video_bookmarks b WHERE b.video_id = v.id AND b.user_id = #{viewerId}) AS bookmarked,
                   COALESCE((SELECT ROUND(AVG(r.rating), 1) FROM video_comments r WHERE r.video_id = v.id AND r.deleted_at IS NULL), 0) AS average_rating,
                   (SELECT COUNT(*) FROM video_comments r WHERE r.video_id = v.id AND r.deleted_at IS NULL) AS review_count,
                   v.created_at, v.updated_at
            FROM videos v
            WHERE v.id = #{id} AND v.deleted_at IS NULL
            """)
    EmergencyVideo findById(@Param("id") long id, @Param("viewerId") long viewerId);

    @Select("""
            SELECT v.id, v.title, v.category, v.symptom_tag AS symptom, v.video_description AS description, v.youtube_url,
                   (SELECT COUNT(*) FROM video_bookmarks bx WHERE bx.video_id = v.id) AS bookmark_count,
                   TRUE AS bookmarked,
                   COALESCE((SELECT ROUND(AVG(r.rating), 1) FROM video_comments r WHERE r.video_id = v.id AND r.deleted_at IS NULL), 0) AS average_rating,
                   (SELECT COUNT(*) FROM video_comments r WHERE r.video_id = v.id AND r.deleted_at IS NULL) AS review_count,
                   v.created_at, v.updated_at
            FROM videos v
            JOIN video_bookmarks b ON b.video_id = v.id AND b.user_id = #{userId}
            WHERE v.deleted_at IS NULL
              AND (#{category} IS NULL OR v.category = #{category})
              AND (#{keyword} IS NULL OR v.title LIKE CONCAT('%', #{keyword}, '%') OR v.symptom_tag LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY ${orderBy}, v.id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<EmergencyVideo> findBookmarkedByUser(
            @Param("userId") long userId,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("orderBy") String orderBy,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Select("""
            SELECT COUNT(*)
            FROM videos v
            JOIN video_bookmarks b ON b.video_id = v.id AND b.user_id = #{userId}
            WHERE v.deleted_at IS NULL
              AND (#{category} IS NULL OR v.category = #{category})
              AND (#{keyword} IS NULL OR v.title LIKE CONCAT('%', #{keyword}, '%') OR v.symptom_tag LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countBookmarkedByUser(@Param("userId") long userId, @Param("category") String category, @Param("keyword") String keyword);

    @Update("""
            UPDATE videos
            SET title=#{title}, category=#{category}, symptom_tag=#{symptom}, video_description=#{description}, youtube_url=#{youtubeUrl}
            WHERE id=#{id}
            """)
    int update(EmergencyVideo video);

    @Delete("UPDATE videos SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int delete(long id);

    @Insert("""
            INSERT IGNORE INTO video_bookmarks(user_id, video_id)
            VALUES(#{userId}, #{videoId})
            """)
    int bookmark(@Param("videoId") long videoId, @Param("userId") long userId);

    @Delete("DELETE FROM video_bookmarks WHERE video_id = #{videoId} AND user_id = #{userId}")
    int unbookmark(@Param("videoId") long videoId, @Param("userId") long userId);
}
