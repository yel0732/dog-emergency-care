package com.ssafy.rescuemungz.emergencycheck;

public class EmergencyRule {
    private Long id;
    private String category;
    private String ruleName;
    private String symptomKeywords;
    private String triggerCondition;
    private String riskLevel;
    private Boolean immediateVet;
    private String recommendedAction;
    private Long evidenceId;
    private Integer priority;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getSymptomKeywords() { return symptomKeywords; }
    public void setSymptomKeywords(String symptomKeywords) { this.symptomKeywords = symptomKeywords; }
    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String triggerCondition) { this.triggerCondition = triggerCondition; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getImmediateVet() { return immediateVet; }
    public void setImmediateVet(Boolean immediateVet) { this.immediateVet = immediateVet; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public Long getEvidenceId() { return evidenceId; }
    public void setEvidenceId(Long evidenceId) { this.evidenceId = evidenceId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
