package com.ssafy.rescuemungz.pet;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PetCarePlanMapper {
    @Insert("""
            INSERT INTO care_diaries(user_id, pet_id, emergency_check_id, title, category, visit_date, memo)
            VALUES(#{userId}, #{petId}, #{emergencyCheckId}, #{title}, #{category}, #{planDate}, #{memo})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PetCarePlan plan);

    @Select("""
            SELECT cp.id, cp.user_id, cp.pet_id, p.name AS pet_name, cp.emergency_check_id, cp.title, cp.category,
                   cp.visit_date AS plan_date, cp.memo, FALSE AS completed, cp.created_at, cp.updated_at
            FROM care_diaries cp
            JOIN pets p ON p.id = cp.pet_id
            WHERE cp.user_id = #{userId} AND cp.deleted_at IS NULL
            ORDER BY cp.visit_date ASC, cp.id DESC
            """)
    List<PetCarePlan> findByUserId(long userId);

    @Select("""
            SELECT cp.id, cp.user_id, cp.pet_id, p.name AS pet_name, cp.emergency_check_id, cp.title, cp.category,
                   cp.visit_date AS plan_date, cp.memo, FALSE AS completed, cp.created_at, cp.updated_at
            FROM care_diaries cp
            JOIN pets p ON p.id = cp.pet_id
            WHERE cp.id = #{id} AND cp.deleted_at IS NULL
            """)
    PetCarePlan findById(long id);

    @Update("""
            UPDATE care_diaries
            SET pet_id=#{petId}, emergency_check_id=#{emergencyCheckId}, title=#{title}, category=#{category}, visit_date=#{planDate}, memo=#{memo}
            WHERE id=#{id} AND user_id=#{userId} AND deleted_at IS NULL
            """)
    int update(PetCarePlan plan);

    @Update("""
            UPDATE care_diaries
            SET updated_at = CURRENT_TIMESTAMP
            WHERE id=#{id} AND user_id=#{userId} AND deleted_at IS NULL
            """)
    int updateCompleted(@Param("id") long id, @Param("userId") long userId, @Param("completed") boolean completed);

    @Delete("UPDATE care_diaries SET deleted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL")
    int delete(@Param("id") long id, @Param("userId") long userId);
}
