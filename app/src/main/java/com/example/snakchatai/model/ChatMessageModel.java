package com.example.snakchatai.model;

import com.google.firebase.Timestamp;

public class ChatMessageModel {
    private String message;
    private String senderId;
    private Timestamp timestamp;
    private String messageType;
    private boolean seen; // 1. Ye variable add kiya

    public ChatMessageModel() {
    }

    // 2. Ye constructor fix kiya (Isme ab 5 parameters hain)
    public ChatMessageModel(String message, String senderId, Timestamp timestamp, String messageType, boolean seen) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.seen = seen;
    }

    // Purana constructor backup ke liye
    public ChatMessageModel(String message, String senderId, Timestamp timestamp, String messageType) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.seen = false;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    // 3. Ye Getter aur Setter add kiye (Inke bina adapter error dega)
    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}