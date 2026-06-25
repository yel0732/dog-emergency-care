package com.ssafy.rescuemungz.emergencycheck;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface EmergencyCheckEvidenceMapper {
    @Insert("""
            INSERT IGNORE INTO emergency_check_evidences(check_id, evidence_id, rule_id, match_score, matched_keywords)
            VALUES(#{checkId}, #{evidenceId}, #{ruleId}, #{score}, #{matchedKeywords})
            """)
    int insert(@Param("checkId") long checkId,
               @Param("evidenceId") long evidenceId,
               @Param("ruleId") long ruleId,
               @Param("score") BigDecimal score,
               @Param("matchedKeywords") String matchedKeywords);
}
