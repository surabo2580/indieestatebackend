CREATE INDEX IF NOT EXISTS idx_reels_feed_active_status_created
    ON reels (is_active, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_videos_status_created
    ON videos (status, created_at);
