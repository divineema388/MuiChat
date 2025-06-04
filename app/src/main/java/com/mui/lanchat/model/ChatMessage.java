package com.mui.lanchat.model;

import com.google.gson.Gson;

public class ChatMessage {
    private String senderIp;
    private String message;
    private long timestamp;

    public ChatMessage(String senderIp, String message) {
        this.senderIp = senderIp;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
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
        return new Gson().toJson(this);
    }

    // Convert JSON string to ChatMessage object
    public static ChatMessage fromJson(String jsonString) {
        return new Gson().fromJson(jsonString, ChatMessage.class);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
               "senderIp='" + senderIp + '\'' +
               ", message='" + message + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}