--liquibase formatted sql
--changeset ihor:update-places-table
ALTER TABLE places ADD COLUMN trip_id VARCHAR(36);
ALTER TABLE places ADD CONSTRAINT fk_places_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id);