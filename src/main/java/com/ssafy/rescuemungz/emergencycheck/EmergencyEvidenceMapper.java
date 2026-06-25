package com.ssafy.rescuemungz.emergencycheck;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmergencyEvidenceMapper {
    @Select("""
            SELECT id, category, symptom_keyword, condition_text, action_text, do_not_action,
                   source_title, source_org, source_url
            FROM emergency_evidence
            """)
    List<EmergencyEvidence> findAll();
}
