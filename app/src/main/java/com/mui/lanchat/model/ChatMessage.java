package com.mui.lanchat.model;

import com.google.gson.Gson;

public class ChatMessage {
    private String senderIp;
    private String senderName; // <--- NEW FIELD
    private String message;
    private long timestamp;

    // Updated constructor to include senderName
    public ChatMessage(String senderIp, String senderName, String message) {
        this.senderIp = senderIp;
        this.senderName = senderName; // <--- ASSIGN NEW FIELD
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter for senderName
    public String getSenderName() {
        return senderName;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Convert ChatMessage object to JSON string
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Convert JSON string back to ChatMessage object
    public static ChatMessage fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ChatMessage.class);
    }
}