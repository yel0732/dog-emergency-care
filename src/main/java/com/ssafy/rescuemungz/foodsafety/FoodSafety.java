package com.ssafy.rescuemungz.foodsafety;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FoodSafety {
    private Long id;
    private String foodName;
    private String riskLevel;
    private String dangerReason;
    private String observedSymptoms;
    private String response;
    private String doseNote;
    private String riskCondition;
    private String referencesJson;
    private List<String> references = new ArrayList<>();
    private List<FoodSafetyReference> referenceLinks = new ArrayList<>();
    private List<String> aliases = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private Boolean immediateVet;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getDangerReason() { return dangerReason; }
    public void setDangerReason(String dangerReason) { this.dangerReason = dangerReason; }
    public String getObservedSymptoms() { return observedSymptoms; }
    public void setObservedSymptoms(String observedSymptoms) { this.observedSymptoms = observedSymptoms; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getDoseNote() { return doseNote; }
    public void setDoseNote(String doseNote) { this.doseNote = doseNote; }
    public String getRiskCondition() { return riskCondition; }
    public void setRiskCondition(String riskCondition) { this.riskCondition = riskCondition; }
    public String getReferencesJson() { return referencesJson; }
    public void setReferencesJson(String referencesJson) { this.referencesJson = referencesJson; }
    public List<String> getReferences() { return references; }
    public void setReferences(List<String> references) { this.references = references == null ? new ArrayList<>() : references; }
    public List<FoodSafetyReference> getReferenceLinks() { return referenceLinks; }
    public void setReferenceLinks(List<FoodSafetyReference> referenceLinks) { this.referenceLinks = referenceLinks == null ? new ArrayList<>() : referenceLinks; }
    public List<String> getAliases() { return aliases; }
    public void setAliases(List<String> aliases) { this.aliases = aliases == null ? new ArrayList<>() : aliases; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags == null ? new ArrayList<>() : tags; }
    public Boolean getImmediateVet() { return immediateVet; }
    public void setImmediateVet(Boolean immediateVet) { this.immediateVet = immediateVet; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
