package com.example.snakchatai.model;

import com.google.firebase.Timestamp;

public class CallLogModel {
    private String userId;
    private String username;
    private Timestamp timestamp;
    private boolean isOutgoing;

    public CallLogModel() {
    }

    public CallLogModel(String userId, String username, Timestamp timestamp, boolean isOutgoing) {
        this.userId = userId;
        this.username = username;
        this.timestamp = timestamp;
        this.isOutgoing = isOutgoing;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }
}
