package com.ssafy.rescuemungz.caseboard;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CasePostMapper {
    @Insert("""
            INSERT INTO community_posts(user_id, category, title, content, image_urls)
            VALUES(#{userId}, #{category}, #{title}, #{content}, #{imageUrlsJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CasePost post);

    @Select("""
            SELECT p.id, p.user_id, u.nickname AS user_name, u.profile_image AS user_profile_image_url,
                   p.category, p.title, p.content, CAST(p.image_urls AS CHAR) AS image_urls_json,
                   p.view_count,
                   (SELECT COUNT(*) FROM community_comments c WHERE c.post_id = p.id AND c.deleted_at IS NULL) AS comment_count,
                   (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = p.user_id) AS follower_count,
                   EXISTS(SELECT 1 FROM user_follows f WHERE f.following_id = p.user_id AND f.follower_id = #{viewerId}) AS following_author,
                   p.created_at, p.updated_at
            FROM community_posts p
            JOIN users u ON u.id = p.user_id
            WHERE p.deleted_at IS NULL
              AND u.deleted_at IS NULL
              AND (#{category} IS NULL OR p.category = #{category})
              AND (#{keyword} IS NULL OR p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{authorId} IS NULL OR p.user_id = #{authorId})
              AND (#{followingOnly} = FALSE OR EXISTS(SELECT 1 FROM user_follows f WHERE f.following_id = p.user_id AND f.follower_id = #{viewerId}))
            ORDER BY
              CASE WHEN #{sort} = 'latest' AND #{direction} = 'asc' THEN p.created_at END ASC,
              CASE WHEN #{sort} = 'latest' AND #{direction} = 'desc' THEN p.created_at END DESC,
              CASE WHEN #{sort} = 'title' AND #{direction} = 'asc' THEN p.title END ASC,
              CASE WHEN #{sort} = 'title' AND #{direction} = 'desc' THEN p.title END DESC,
              CASE WHEN #{sort} = 'category' AND #{direction} = 'asc' THEN p.category END ASC,
              CASE WHEN #{sort} = 'category' AND #{direction} = 'desc' THEN p.category END DESC,
              CASE WHEN #{sort} = 'views' AND #{direction} = 'asc' THEN p.view_count END ASC,
              CASE WHEN #{sort} = 'views' AND #{direction} = 'desc' THEN p.view_count END DESC,
              CASE WHEN #{sort} = 'comments' AND #{direction} = 'asc' THEN (SELECT COUNT(*) FROM community_comments c WHERE c.post_id = p.id AND c.deleted_at IS NULL) END ASC,
              CASE WHEN #{sort} = 'comments' AND #{direction} = 'desc' THEN (SELECT COUNT(*) FROM community_comments c WHERE c.post_id = p.id AND c.deleted_at IS NULL) END DESC,
              CASE WHEN #{sort} = 'followers' AND #{direction} = 'asc' THEN (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = p.user_id) END ASC,
              CASE WHEN #{sort} = 'followers' AND #{direction} = 'desc' THEN (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = p.user_id) END DESC,
              p.id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<CasePost> search(@Param("category") String category,
                          @Param("keyword") String keyword,
                          @Param("authorId") Long authorId,
                          @Param("followingOnly") boolean followingOnly,
                          @Param("viewerId") long viewerId,
                          @Param("sort") String sort,
                          @Param("direction") String direction,
                          @Param("size") int size,
                          @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*)
            FROM community_posts p
            JOIN users u ON u.id = p.user_id
            WHERE p.deleted_at IS NULL
              AND u.deleted_at IS NULL
              AND (#{category} IS NULL OR p.category = #{category})
              AND (#{keyword} IS NULL OR p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{authorId} IS NULL OR p.user_id = #{authorId})
              AND (#{followingOnly} = FALSE OR EXISTS(SELECT 1 FROM user_follows f WHERE f.following_id = p.user_id AND f.follower_id = #{viewerId}))
            """)
    long count(@Param("category") String category,
               @Param("keyword") String keyword,
               @Param("authorId") Long authorId,
               @Param("followingOnly") boolean followingOnly,
               @Param("viewerId") long viewerId);

    @Select("""
            SELECT p.id, p.user_id, u.nickname AS user_name, u.profile_image AS user_profile_image_url,
                   p.category, p.title, p.content, CAST(p.image_urls AS CHAR) AS image_urls_json,
                   p.view_count,
                   (SELECT COUNT(*) FROM community_comments c WHERE c.post_id = p.id AND c.deleted_at IS NULL) AS comment_count,
                   (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = p.user_id) AS follower_count,
                   EXISTS(SELECT 1 FROM user_follows f WHERE f.following_id = p.user_id AND f.follower_id = #{viewerId}) AS following_author,
                   p.created_at, p.updated_at
            FROM community_posts p
            JOIN users u ON u.id = p.user_id
            WHERE p.id = #{id} AND p.deleted_at IS NULL AND u.deleted_at IS NULL
            """)
    CasePost findById(@Param("id") long id, @Param("viewerId") long viewerId);

    @Update("""
            UPDATE community_posts
            SET category=#{category}, title=#{title}, content=#{content}, image_urls=#{imageUrlsJson}
            WHERE id=#{id} AND user_id=#{userId} AND deleted_at IS NULL
            """)
    int update(CasePost post);

    @Delete("UPDATE community_posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL")
    int delete(@Param("id") long id, @Param("userId") long userId);

    @Delete("UPDATE community_posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int deleteById(long id);

    @Update("UPDATE community_posts SET view_count = view_count + 1 WHERE id = #{id} AND deleted_at IS NULL")
    int increaseViewCount(long id);
}
