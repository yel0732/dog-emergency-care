package com.ssafy.rescuemungz.caseboard;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CaseCommentMapper {
    @Insert("""
            INSERT INTO community_comments(post_id, parent_id, user_id, content)
            VALUES(#{postId}, #{parentId}, #{userId}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CaseComment comment);

    @Select("""
            SELECT c.id, c.post_id, c.parent_id, c.user_id, u.nickname AS user_name,
                   u.profile_image AS user_profile_image_url,
                   EXISTS(SELECT 1 FROM user_follows f WHERE f.following_id = c.user_id AND f.follower_id = #{viewerId}) AS following_author,
                   c.content, c.created_at, c.updated_at
            FROM community_comments c
            JOIN users u ON u.id = c.user_id
            WHERE c.post_id = #{postId} AND c.deleted_at IS NULL AND u.deleted_at IS NULL
            ORDER BY COALESCE(c.parent_id, c.id) ASC, CASE WHEN c.parent_id IS NULL THEN 0 ELSE 1 END ASC, c.id ASC
            """)
    List<CaseComment> findByPostId(@Param("postId") long postId, @Param("viewerId") long viewerId);

    @Select("""
            SELECT c.id, c.post_id, c.parent_id, c.user_id, u.nickname AS user_name,
                   u.profile_image AS user_profile_image_url, c.content, c.created_at, c.updated_at
            FROM community_comments c
            JOIN users u ON u.id = c.user_id
            WHERE c.id = #{id} AND c.deleted_at IS NULL AND u.deleted_at IS NULL
            """)
    CaseComment findById(long id);

    @Update("""
            UPDATE community_comments
            SET content=#{content}
            WHERE id=#{id} AND user_id=#{userId} AND deleted_at IS NULL
            """)
    int update(CaseComment comment);

    @Update("UPDATE community_comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL")
    int delete(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE community_comments SET deleted_at = CURRENT_TIMESTAMP WHERE (id = #{id} OR parent_id = #{id}) AND deleted_at IS NULL")
    int deleteWithReplies(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE community_comments SET deleted_at = CURRENT_TIMESTAMP WHERE (id = #{id} OR parent_id = #{id}) AND deleted_at IS NULL")
    int deleteWithRepliesById(long id);
}
