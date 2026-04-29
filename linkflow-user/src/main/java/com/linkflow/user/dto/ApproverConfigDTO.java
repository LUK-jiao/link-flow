package com.linkflow.user.dto;

public class ApproverConfigDTO {
    private String campaignType;
    private Long approverId;
    private Integer approverLevel;

    public String getCampaignType() {
        return campaignType;
    }

    public void setCampaignType(String campaignType) {
        this.campaignType = campaignType;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public Integer getApproverLevel() {
        return approverLevel;
    }

    public void setApproverLevel(Integer approverLevel) {
        this.approverLevel = approverLevel;
    }
}