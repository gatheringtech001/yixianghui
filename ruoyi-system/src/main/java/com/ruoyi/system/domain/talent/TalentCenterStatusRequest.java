package com.ruoyi.system.domain.talent;

public class TalentCenterStatusRequest
{
    private String actorUnionid;
    private String expectedStatus;
    private String status;
    private String confirmationId;
    private String confirmedAt;

    public String getActorUnionid() { return actorUnionid; }
    public void setActorUnionid(String actorUnionid) { this.actorUnionid = actorUnionid; }
    public String getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(String expectedStatus) { this.expectedStatus = expectedStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConfirmationId() { return confirmationId; }
    public void setConfirmationId(String confirmationId) { this.confirmationId = confirmationId; }
    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String confirmedAt) { this.confirmedAt = confirmedAt; }
}
