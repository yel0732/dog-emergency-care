package com.ssafy.rescuemungz.emergencycheck;

import java.time.LocalDateTime;

public class EmergencyCheck {
    private Long id;
    private Long userId;
    private Long petId;
    private String petName;
    private LocalDateTime occurredAt;
    private Integer repeatCount;
    private Long suspectedFoodId;
    private String suspectedFoodName;
    private String suspectedFoodText;
    private String symptomNote;
    private String symptomTags;
    private String structuredInput;
    private String photoUrls;
    private String riskLevel;
    private String riskReason;
    private String recommendedAction;
    private String analysisResult;
    private Boolean immediateVet;
    private LocalDateTime createdAt;
    private String headline;
    private java.util.List<String> immediateActions = java.util.List.of();
    private java.util.List<String> avoidActions = java.util.List.of();
    private java.util.List<String> observationChecklist = java.util.List.of();
    private java.util.List<String> escalationCriteria = java.util.List.of();
    private java.util.List<String> optionalQuestions = java.util.List.of();
    private java.util.List<EvidenceSummary> evidenceSummary = java.util.List.of();
    private String disclaimer;
    private AvailableActions availableActions;
    private Boolean fallbackUsed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public Integer getRepeatCount() { return repeatCount; }
    public void setRepeatCount(Integer repeatCount) { this.repeatCount = repeatCount; }
    public Long getSuspectedFoodId() { return suspectedFoodId; }
    public void setSuspectedFoodId(Long suspectedFoodId) { this.suspectedFoodId = suspectedFoodId; }
    public String getSuspectedFoodName() { return suspectedFoodName; }
    public void setSuspectedFoodName(String suspectedFoodName) { this.suspectedFoodName = suspectedFoodName; }
    public String getSuspectedFoodText() { return suspectedFoodText; }
    public void setSuspectedFoodText(String suspectedFoodText) { this.suspectedFoodText = suspectedFoodText; }
    public String getSymptomNote() { return symptomNote; }
    public void setSymptomNote(String symptomNote) { this.symptomNote = symptomNote; }
    public String getSymptomTags() { return symptomTags; }
    public void setSymptomTags(String symptomTags) { this.symptomTags = symptomTags; }
    public String getStructuredInput() { return structuredInput; }
    public void setStructuredInput(String structuredInput) { this.structuredInput = structuredInput; }
    public String getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(String photoUrls) { this.photoUrls = photoUrls; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }
    public Boolean getImmediateVet() { return immediateVet; }
    public void setImmediateVet(Boolean immediateVet) { this.immediateVet = immediateVet; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public java.util.List<String> getImmediateActions() { return immediateActions; }
    public void setImmediateActions(java.util.List<String> immediateActions) { this.immediateActions = immediateActions == null ? java.util.List.of() : immediateActions; }
    public java.util.List<String> getAvoidActions() { return avoidActions; }
    public void setAvoidActions(java.util.List<String> avoidActions) { this.avoidActions = avoidActions == null ? java.util.List.of() : avoidActions; }
    public java.util.List<String> getObservationChecklist() { return observationChecklist; }
    public void setObservationChecklist(java.util.List<String> observationChecklist) { this.observationChecklist = observationChecklist == null ? java.util.List.of() : observationChecklist; }
    public java.util.List<String> getEscalationCriteria() { return escalationCriteria; }
    public void setEscalationCriteria(java.util.List<String> escalationCriteria) { this.escalationCriteria = escalationCriteria == null ? java.util.List.of() : escalationCriteria; }
    public java.util.List<String> getOptionalQuestions() { return optionalQuestions; }
    public void setOptionalQuestions(java.util.List<String> optionalQuestions) { this.optionalQuestions = optionalQuestions == null ? java.util.List.of() : optionalQuestions; }
    public java.util.List<EvidenceSummary> getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(java.util.List<EvidenceSummary> evidenceSummary) { this.evidenceSummary = evidenceSummary == null ? java.util.List.of() : evidenceSummary; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public AvailableActions getAvailableActions() { return availableActions; }
    public void setAvailableActions(AvailableActions availableActions) { this.availableActions = availableActions; }
    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
}
