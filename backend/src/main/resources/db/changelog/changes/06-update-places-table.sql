--liquibase formatted sql

--changeset ihor:add-trip-id-column
ALTER TABLE places ADD COLUMN trip_id VARCHAR(36);

--changeset ihor:add-fk-trip-id
ALTER TABLE places ADD CONSTRAINT fk_places_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id);