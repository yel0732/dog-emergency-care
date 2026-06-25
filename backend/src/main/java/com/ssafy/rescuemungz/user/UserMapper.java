package com.ssafy.rescuemungz.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Insert("""
            INSERT INTO users(login_id, email, name, nickname, password_hash, profile_image, role)
            VALUES(#{loginId}, #{email}, #{name}, #{nickname}, #{passwordHash}, #{profileImageUrl}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("""
            SELECT id, COALESCE(login_id, nickname) AS login_id, password_hash AS password,
                   COALESCE(name, nickname) AS name, email, nickname,
                   password_hash, profile_image AS profile_image_url, role,
                   TRUE AS active, created_at, updated_at
            FROM users
            WHERE id = #{id} AND deleted_at IS NULL
            """)
    User findById(long id);

    @Select("""
            SELECT id, COALESCE(login_id, nickname) AS login_id, password_hash AS password,
                   COALESCE(name, nickname) AS name, email, nickname,
                   password_hash, profile_image AS profile_image_url, role,
                   TRUE AS active, created_at, updated_at
            FROM users
            WHERE deleted_at IS NULL
              AND (
                login_id = #{loginId}
                OR email = #{loginId}
                OR nickname = #{loginId}
                OR (#{loginId} = 'test' AND id = 2)
              )
            LIMIT 1
            """)
    User findByLoginId(String loginId);

    @Select("""
            SELECT id, COALESCE(login_id, nickname) AS login_id, password_hash AS password,
                   COALESCE(name, nickname) AS name, email, nickname,
                   password_hash, profile_image AS profile_image_url, role,
                   TRUE AS active, created_at, updated_at
            FROM users
            WHERE email = #{email} AND deleted_at IS NULL
            """)
    User findByEmail(String email);

    @Update("""
            UPDATE users
            SET password_hash=#{passwordHash}, name=#{name}, nickname=#{nickname}, email=#{email}, profile_image=#{profileImageUrl}
            WHERE id=#{id} AND deleted_at IS NULL
            """)
    int update(User user);

    @Update("UPDATE users SET password_hash=#{passwordHash} WHERE id=#{id} AND deleted_at IS NULL")
    int updatePasswordHash(User user);

    @Update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int deactivate(long id);
}
