--liquibase formatted sql
--changeset antigravity:insert-dummy-user
INSERT INTO users (user_id, email, password_hash, full_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'test@example.com', 'dummy_hash', 'Test User');
