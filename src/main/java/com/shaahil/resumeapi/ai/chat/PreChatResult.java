package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;

public record PreChatResult(Route route, String replyText, SearchOutcome search) {
}
