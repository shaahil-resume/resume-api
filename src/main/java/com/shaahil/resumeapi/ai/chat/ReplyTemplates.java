package com.shaahil.resumeapi.ai.chat;

public class ReplyTemplates {

    private static final String EXAMPLES = " Examples: What have I built with Spring Boot? "
            + "Where am I based? What are my main skills?";

    private ReplyTemplates() {
    }

    public static String welcome() {
        return "Hi! I can answer questions about my experience, projects and skills." + EXAMPLES;
    }

    public static String fallback() {
        return "Sorry, I couldn't answer that just now. Please try again in a moment, "
                + "or ask me directly through the contact form.";
    }

    public static String declineBelowFloor() {
        return "I only answer questions about my work and background." + EXAMPLES;
    }
}
