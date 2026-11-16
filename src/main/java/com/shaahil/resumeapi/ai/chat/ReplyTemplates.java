package com.shaahil.resumeapi.ai.chat;

public class ReplyTemplates {

    private static final String EXAMPLES = " Examples: What has he built with Spring Boot? "
            + "Where is he based? What are his main skills?";

    private ReplyTemplates() {
    }

    public static String welcome() {
        return "Hi! I answer questions about Shaahil's experience, projects and skills." + EXAMPLES;
    }

    public static String declineBelowFloor() {
        return "I only answer questions about Shaahil's work and background." + EXAMPLES;
    }
}
