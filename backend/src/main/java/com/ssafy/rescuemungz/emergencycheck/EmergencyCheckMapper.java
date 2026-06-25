package com.ssafy.rescuemungz.emergencycheck;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface EmergencyCheckMapper {
    @Insert("""
            INSERT INTO emergency_checks(user_id, pet_id, occurred_at, repeat_count, suspected_food_id,
                suspected_food_text, symptom_note, symptom_tags, structured_input, photo_urls, risk_level, risk_reason,
                recommended_action, analysis_result, is_immediate_vet)
            VALUES(#{userId}, #{petId}, #{occurredAt}, #{repeatCount}, #{suspectedFoodId},
                #{suspectedFoodText}, #{symptomNote}, #{symptomTags}, #{structuredInput}, #{photoUrls}, #{riskLevel}, #{riskReason},
                #{recommendedAction}, #{analysisResult}, #{immediateVet})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EmergencyCheck check);

    @Select("""
            SELECT c.id, c.user_id, c.pet_id, p.name AS pet_name, c.occurred_at, c.repeat_count,
                   c.suspected_food_id, f.food_name AS suspected_food_name, c.suspected_food_text,
                   c.symptom_note, c.symptom_tags, c.structured_input, c.photo_urls, c.risk_level, c.risk_reason,
                   c.recommended_action, c.analysis_result, c.is_immediate_vet AS immediate_vet, c.created_at
            FROM emergency_checks c
            LEFT JOIN pets p ON p.id = c.pet_id
            LEFT JOIN food_safety f ON f.id = c.suspected_food_id
            WHERE c.user_id = #{userId}
            ORDER BY c.id DESC
            """)
    List<EmergencyCheck> findByUserId(long userId);

    @Select("""
            SELECT c.id, c.user_id, c.pet_id, p.name AS pet_name, c.occurred_at, c.repeat_count,
                   c.suspected_food_id, f.food_name AS suspected_food_name, c.suspected_food_text,
                   c.symptom_note, c.symptom_tags, c.structured_input, c.photo_urls, c.risk_level, c.risk_reason,
                   c.recommended_action, c.analysis_result, c.is_immediate_vet AS immediate_vet, c.created_at
            FROM emergency_checks c
            LEFT JOIN pets p ON p.id = c.pet_id
            LEFT JOIN food_safety f ON f.id = c.suspected_food_id
            WHERE c.id = #{id} AND c.user_id = #{userId}
            """)
    EmergencyCheck findByIdAndUserId(@Param("id") long id, @Param("userId") long userId);

    @Delete("DELETE FROM emergency_checks WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") long id, @Param("userId") long userId);
}
