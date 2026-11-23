package com.shaahil.resumeapi.ai.chat;

public record ChatEvent(String type, String data) {

    public static ChatEvent token(String data) {
        return new ChatEvent("token", data);
    }

    public static ChatEvent citations(String data) {
        return new ChatEvent("citations", data);
    }

    public static ChatEvent done() {
        return new ChatEvent("done", "");
    }
}
