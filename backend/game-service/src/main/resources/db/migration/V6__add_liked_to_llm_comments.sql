ALTER TABLE llm_comments
    ADD COLUMN IF NOT EXISTS liked BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_llm_comments_liked ON llm_comments (liked);
