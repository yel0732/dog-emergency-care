package com.ssafy.rescuemungz.emergencycheck;

import java.time.LocalDateTime;

public class VetReport {
    private Long id;
    private Long checkId;
    private String riskSummary;
    private String situationSummary;
    private String immediateActions;
    private String avoidActions;
    private String observationChecklist;
    private String escalationCriteria;
    private String hospitalMessage;
    private String optionalQuestions;
    private String evidenceSummary;
    private String llmResponseJson;
    private String vetQuestions;
    private LocalDateTime savedAt;
    private String petSnapshot;
    private String symptomSnapshot;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCheckId() { return checkId; }
    public void setCheckId(Long checkId) { this.checkId = checkId; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public String getSituationSummary() { return situationSummary; }
    public void setSituationSummary(String situationSummary) { this.situationSummary = situationSummary; }
    public String getImmediateActions() { return immediateActions; }
    public void setImmediateActions(String immediateActions) { this.immediateActions = immediateActions; }
    public String getAvoidActions() { return avoidActions; }
    public void setAvoidActions(String avoidActions) { this.avoidActions = avoidActions; }
    public String getObservationChecklist() { return observationChecklist; }
    public void setObservationChecklist(String observationChecklist) { this.observationChecklist = observationChecklist; }
    public String getEscalationCriteria() { return escalationCriteria; }
    public void setEscalationCriteria(String escalationCriteria) { this.escalationCriteria = escalationCriteria; }
    public String getHospitalMessage() { return hospitalMessage; }
    public void setHospitalMessage(String hospitalMessage) { this.hospitalMessage = hospitalMessage; }
    public String getOptionalQuestions() { return optionalQuestions; }
    public void setOptionalQuestions(String optionalQuestions) {
        this.optionalQuestions = optionalQuestions;
        this.vetQuestions = optionalQuestions;
    }
    public String getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(String evidenceSummary) { this.evidenceSummary = evidenceSummary; }
    public String getLlmResponseJson() { return llmResponseJson; }
    public void setLlmResponseJson(String llmResponseJson) { this.llmResponseJson = llmResponseJson; }
    public String getVetQuestions() { return vetQuestions; }
    public void setVetQuestions(String vetQuestions) {
        this.vetQuestions = vetQuestions;
        this.optionalQuestions = vetQuestions;
    }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    public String getPetSnapshot() { return petSnapshot; }
    public void setPetSnapshot(String petSnapshot) { this.petSnapshot = petSnapshot; }
    public String getSymptomSnapshot() { return symptomSnapshot; }
    public void setSymptomSnapshot(String symptomSnapshot) { this.symptomSnapshot = symptomSnapshot; }
}
