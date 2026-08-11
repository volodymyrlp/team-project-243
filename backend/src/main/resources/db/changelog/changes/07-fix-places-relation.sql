--liquibase formatted sql
--changeset ihor:fix-places-relation
ALTER TABLE places DROP CONSTRAINT fk_places_trip_id;
ALTER TABLE places DROP COLUMN trip_id;

