--liquibase formatted sql
--changeset ihor:create-places-table
CREATE TABLE places (
    place_id VARCHAR(36) NOT NULL,
    external_place_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    CONSTRAINT pk_places PRIMARY KEY (place_id)
);
