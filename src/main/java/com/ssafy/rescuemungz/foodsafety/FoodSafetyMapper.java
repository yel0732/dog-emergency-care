package com.ssafy.rescuemungz.foodsafety;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FoodSafetyMapper {
    @Select("""
            SELECT f.id, f.food_name, f.risk_level, f.danger_reason, f.observed_symptoms, f.response,
                   f.dose_note, f.risk_condition, f.source_references AS references_json, f.is_immediate_vet, f.updated_at
            FROM food_safety f
            WHERE #{keyword} IS NULL
               OR LOWER(f.food_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
               OR EXISTS (
                   SELECT 1
                   FROM food_aliases a
                   WHERE a.food_id = f.id
                     AND LOWER(a.alias_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
               )
            ORDER BY
                CASE
                    WHEN #{keyword} IS NULL THEN 10
                    WHEN LOWER(f.food_name) = LOWER(#{keyword}) THEN 1
                    WHEN EXISTS (
                        SELECT 1
                        FROM food_aliases a
                        WHERE a.food_id = f.id
                          AND LOWER(a.alias_name) = LOWER(#{keyword})
                    ) THEN 2
                    WHEN LOWER(f.food_name) LIKE CONCAT(LOWER(#{keyword}), '%') THEN 3
                    WHEN EXISTS (
                        SELECT 1
                        FROM food_aliases a
                        WHERE a.food_id = f.id
                          AND LOWER(a.alias_name) LIKE CONCAT(LOWER(#{keyword}), '%')
                    ) THEN 4
                    ELSE 5
                END,
                CASE f.risk_level
                    WHEN '위험' THEN 1
                    WHEN '주의' THEN 2
                    WHEN '안전' THEN 3
                    WHEN '정보부족' THEN 3
                    ELSE 4
                END,
                f.food_name
            """)
    List<FoodSafety> search(@Param("keyword") String keyword);

    @Select("""
            SELECT id, food_name, risk_level, danger_reason, observed_symptoms, response,
                   dose_note, risk_condition, source_references AS references_json, is_immediate_vet, updated_at
            FROM food_safety
            WHERE id = #{id}
            """)
    FoodSafety findById(long id);

    @Select("""
            SELECT id, food_name, risk_level, danger_reason, observed_symptoms, response,
                   dose_note, risk_condition, source_references AS references_json, is_immediate_vet, updated_at
            FROM food_safety
            WHERE LOWER(food_name) = LOWER(#{foodName})
            LIMIT 1
            """)
    FoodSafety findByFoodNameExact(@Param("foodName") String foodName);

    @Select("""
            SELECT f.id, f.food_name, f.risk_level, f.danger_reason, f.observed_symptoms, f.response,
                   f.dose_note, f.risk_condition, f.source_references AS references_json, f.is_immediate_vet, f.updated_at
            FROM food_aliases a
            JOIN food_safety f ON f.id = a.food_id
            WHERE LOWER(a.alias_name) = LOWER(#{aliasName})
            LIMIT 1
            """)
    FoodSafety findByAliasExact(@Param("aliasName") String aliasName);

    @Select("""
            SELECT alias_name
            FROM food_aliases
            WHERE food_id = #{foodId}
            ORDER BY alias_name
            """)
    List<String> findAliases(long foodId);
}
