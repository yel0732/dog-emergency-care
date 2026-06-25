package com.ssafy.rescuemungz.emergencycheck;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VetReportMapper {
    @Insert("""
            INSERT INTO vet_reports(check_id, risk_summary, situation_summary, immediate_actions, avoid_actions,
                observation_checklist, escalation_criteria, hospital_message, optional_questions, evidence_summary,
                llm_response_json, pet_snapshot, symptom_snapshot)
            VALUES(#{checkId}, #{riskSummary}, #{situationSummary}, #{immediateActions}, #{avoidActions},
                #{observationChecklist}, #{escalationCriteria}, #{hospitalMessage}, #{optionalQuestions}, #{evidenceSummary},
                #{llmResponseJson}, #{petSnapshot}, #{symptomSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VetReport report);

    @Select("""
            SELECT id, check_id, risk_summary, situation_summary, immediate_actions, avoid_actions,
                   observation_checklist, escalation_criteria, hospital_message, optional_questions,
                   optional_questions AS vet_questions, evidence_summary, llm_response_json,
                   saved_at, pet_snapshot, symptom_snapshot
            FROM vet_reports
            WHERE check_id = #{checkId}
            """)
    VetReport findByCheckId(long checkId);
}
