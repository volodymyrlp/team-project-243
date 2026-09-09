--liquibase formatted sql
--changeset ihor:add-trip-fields
ALTER TABLE trips ADD COLUMN destination VARCHAR(255);
ALTER TABLE trips ADD COLUMN start_date DATE;
ALTER TABLE trips ADD COLUMN end_date DATE;
