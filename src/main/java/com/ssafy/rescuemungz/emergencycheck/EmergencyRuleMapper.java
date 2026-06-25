package com.ssafy.rescuemungz.emergencycheck;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmergencyRuleMapper {
    @Select("""
            SELECT id, category, rule_name, symptom_keywords, trigger_condition, risk_level,
                   is_immediate_vet, recommended_action, evidence_id, priority
            FROM emergency_rules
            ORDER BY priority DESC, id
            """)
    List<EmergencyRule> findAll();
}
