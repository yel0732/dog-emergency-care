package com.ssafy.rescuemungz.emergencycheck;

public class EmergencyEvidence {
    private Long id;
    private String category;
    private String symptomKeyword;
    private String conditionText;
    private String actionText;
    private String doNotAction;
    private String sourceTitle;
    private String sourceOrg;
    private String sourceUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSymptomKeyword() { return symptomKeyword; }
    public void setSymptomKeyword(String symptomKeyword) { this.symptomKeyword = symptomKeyword; }
    public String getConditionText() { return conditionText; }
    public void setConditionText(String conditionText) { this.conditionText = conditionText; }
    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }
    public String getDoNotAction() { return doNotAction; }
    public void setDoNotAction(String doNotAction) { this.doNotAction = doNotAction; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
    public String getSourceOrg() { return sourceOrg; }
    public void setSourceOrg(String sourceOrg) { this.sourceOrg = sourceOrg; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
