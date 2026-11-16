ALTER TABLE admin_users ADD COLUMN email VARCHAR(255);
ALTER TABLE admin_users ADD CONSTRAINT uk_admin_users_email UNIQUE (email);
