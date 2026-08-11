--liquibase formatted sql
--changeset antigravity:create-trip-places-table
CREATE TABLE trip_places (
    trip_place_id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    place_id VARCHAR(36) NOT NULL,
    day_number INT NOT NULL,
    order_index INT NOT NULL,
    CONSTRAINT pk_trip_places PRIMARY KEY (trip_place_id),
    CONSTRAINT fk_trip_places_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id),
    CONSTRAINT fk_trip_places_place_id FOREIGN KEY (place_id) REFERENCES places(place_id)
);
