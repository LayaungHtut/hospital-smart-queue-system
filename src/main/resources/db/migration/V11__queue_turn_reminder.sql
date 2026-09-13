-- ============================================================
-- V11__queue_turn_reminder.sql
-- Tracks whether a waiting patient has already been sent the
-- "your turn is coming up" reminder, so QueueExpiryScheduler can
-- send it exactly once per queue entry instead of every time it runs.
-- ============================================================

ALTER TABLE queue ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;
