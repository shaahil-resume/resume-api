package com.shaahil.resumeapi.analytics;

import com.shaahil.resumeapi.ai.ChatCompleted;
import com.shaahil.resumeapi.ai.ChatOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One row per chat request. Holds counts and timings only, never the question or the answer. */
@Entity
@Table(name = "chat_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatOutcome outcome;

    @Column(nullable = false)
    private double bestScore;

    @Column(nullable = false)
    private int chunksKept;

    @Column(nullable = false)
    private long latencyMs;

    @Column(nullable = false)
    private int retries;

    @Column(nullable = false, length = 20)
    private String promptVersion;

    static ChatRequestLog from(ChatCompleted event) {
        ChatRequestLog log = new ChatRequestLog();
        log.occurredAt = event.occurredAt();
        log.outcome = event.outcome();
        log.bestScore = event.bestScore();
        log.chunksKept = event.chunksKept();
        log.latencyMs = event.latencyMs();
        log.retries = event.retries();
        log.promptVersion = event.promptVersion();
        return log;
    }
}
