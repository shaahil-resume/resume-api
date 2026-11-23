package com.shaahil.resumeapi.ai;

import java.time.Instant;

/**
 * Published once per chat request so other modules (analytics) can record it without the
 * ai module knowing about them. It carries no question text, answer text or visitor address.
 *
 * @param occurredAt    when the request finished
 * @param outcome       how it ended
 * @param bestScore     best similarity score from retrieval, 0 when no search ran
 * @param chunksKept    number of knowledge chunks put in the prompt
 * @param latencyMs     time from request start to the last event
 * @param retries       model call retries before the first token
 * @param promptVersion prompt version in use
 * @param promptTokens  tokens the model counted in the prompt, null when not reported
 * @param completionTokens tokens the model counted in the reply, null when not reported
 */
public record ChatCompleted(Instant occurredAt, ChatOutcome outcome, double bestScore, int chunksKept,
                            long latencyMs, int retries, String promptVersion,
                            Integer promptTokens, Integer completionTokens) {
}
