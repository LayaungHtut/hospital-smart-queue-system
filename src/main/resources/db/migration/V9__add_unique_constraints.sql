-- Add unique constraints for staff and admin tables
CREATE UNIQUE INDEX IF NOT EXISTS idx_staff_phone_unique 
    ON staff(phone) WHERE phone IS NOT NULL AND phone <> '';
CREATE UNIQUE INDEX IF NOT EXISTS idx_staff_email_unique 
    ON staff(email) WHERE email IS NOT NULL AND email <> '';
CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_email_unique 
    ON admin_user(email) WHERE email IS NOT NULL AND email <> '';
