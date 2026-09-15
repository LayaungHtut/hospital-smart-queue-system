-- ============================================================
-- V12__called_expiry_default.sql
-- A called patient (doctor pressed "Call Next") now has 5 minutes to show
-- up before the queue auto-expires, matching the smart queue engine
-- default requested for the CALLED -> no-show flow. Admins can still
-- change this value from Admin > Queue Settings.
-- ============================================================

UPDATE system_setting
SET setting_value = '5', updated_at = CURRENT_TIMESTAMP
WHERE setting_key = 'queue_expiry_minutes';

INSERT INTO system_setting (setting_key, setting_value, description, is_public)
VALUES ('queue_expiry_minutes', '5', 'Minutes a called patient has to show up before the queue expires', FALSE)
ON CONFLICT (setting_key) DO NOTHING;
