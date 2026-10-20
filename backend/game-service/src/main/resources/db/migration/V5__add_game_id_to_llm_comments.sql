ALTER TABLE llm_comments
    ADD COLUMN IF NOT EXISTS game_id UUID;

CREATE INDEX IF NOT EXISTS idx_llm_comments_game_id ON llm_comments (game_id);
