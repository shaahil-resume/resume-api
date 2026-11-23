CREATE TABLE chat_requests (
    id             BIGSERIAL        PRIMARY KEY,
    occurred_at    TIMESTAMPTZ      NOT NULL,
    outcome        VARCHAR(20)      NOT NULL,
    best_score     DOUBLE PRECISION NOT NULL,
    chunks_kept    INTEGER          NOT NULL,
    latency_ms     BIGINT           NOT NULL,
    retries        INTEGER          NOT NULL,
    prompt_version VARCHAR(20)      NOT NULL
);

CREATE INDEX idx_chat_requests_occurred_at ON chat_requests (occurred_at);
