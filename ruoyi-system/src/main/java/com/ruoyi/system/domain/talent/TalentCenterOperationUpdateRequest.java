package com.ruoyi.system.domain.talent;

public class TalentCenterOperationUpdateRequest
{
    private String expectedStatus;
    private String status;
    private String preference;
    private String followUpNote;
    private Boolean settled;
    private String confirmationId;
    private String confirmedAt;

    public String getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(String value) { expectedStatus = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getPreference() { return preference; }
    public void setPreference(String value) { preference = value; }
    public String getFollowUpNote() { return followUpNote; }
    public void setFollowUpNote(String value) { followUpNote = value; }
    public Boolean getSettled() { return settled; }
    public void setSettled(Boolean value) { settled = value; }
    public String getConfirmationId() { return confirmationId; }
    public void setConfirmationId(String value) { confirmationId = value; }
    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String value) { confirmedAt = value; }
}
