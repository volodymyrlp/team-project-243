ALTER TABLE places
    ADD COLUMN trip_id VARCHAR(36),
    ADD CONSTRAINT fk_places_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id);