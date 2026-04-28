package com.company.benchmarkengine.application.controller.vo;

public class BenchmarkAlertLinkageVO {

    private final String alertId;
    private final String alertType;
    private final String alertLevel;
    private final String alertStatus;
    private final String notifyStatus;
    private final String summary;
    private final String detailPath;
    private final String linkageMode;
    private final String notificationLogId;

    public BenchmarkAlertLinkageVO(String alertId,
                                   String alertType,
                                   String alertLevel,
                                   String alertStatus,
                                   String notifyStatus,
                                   String summary,
                                   String detailPath,
                                   String linkageMode,
                                   String notificationLogId) {
        this.alertId = alertId;
        this.alertType = alertType;
        this.alertLevel = alertLevel;
        this.alertStatus = alertStatus;
        this.notifyStatus = notifyStatus;
        this.summary = summary;
        this.detailPath = detailPath;
        this.linkageMode = linkageMode;
        this.notificationLogId = notificationLogId;
    }

    public String getAlertId() {
        return alertId;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public String getNotifyStatus() {
        return notifyStatus;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetailPath() {
        return detailPath;
    }

    public String getLinkageMode() {
        return linkageMode;
    }

    public String getNotificationLogId() {
        return notificationLogId;
    }
}
