package com.scaleset.cfbuilder.autoscaling;

public class NotificationConfiguration {
    private String[] notificationTypes;
    private String topicARN;

    public NotificationConfiguration(String topicARN, String[] notificationTypes) {
        this.notificationTypes = notificationTypes;
        this.topicARN = topicARN;
    }

    public String[] getNotificationTypes() {
        return notificationTypes;
    }

    public void setNotificationTypes(String[] notificationTypes) {
        this.notificationTypes = notificationTypes;
    }

    public String getTopicARN() {
        return topicARN;
    }

    public void setTopicARN(String topicARN) {
        this.topicARN = topicARN;
    }
}
