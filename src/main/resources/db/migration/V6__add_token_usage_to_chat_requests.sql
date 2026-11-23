ALTER TABLE chat_requests ADD COLUMN prompt_tokens INTEGER;
ALTER TABLE chat_requests ADD COLUMN completion_tokens INTEGER;
ALTER TABLE chat_requests ADD COLUMN estimated_cost_usd NUMERIC(12, 6);
