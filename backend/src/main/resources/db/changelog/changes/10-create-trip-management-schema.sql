--liquibase formatted sql
--changeset ihor:create-trip-management-schema
DROP TABLE IF EXISTS trip_places;

ALTER TABLE trips DROP FOREIGN KEY fk_trips_user_id;
ALTER TABLE trips DROP COLUMN start_date;
ALTER TABLE trips DROP COLUMN end_date;
ALTER TABLE trips RENAME COLUMN user_id TO owner_id;
ALTER TABLE trips ADD CONSTRAINT fk_trips_owner_id FOREIGN KEY (owner_id) REFERENCES users(user_id);
ALTER TABLE trips ADD COLUMN description TEXT;
ALTER TABLE trips ADD COLUMN budget DECIMAL(10, 2);
ALTER TABLE trips ADD COLUMN currency VARCHAR(10);
ALTER TABLE trips ADD COLUMN cover_url VARCHAR(500);
ALTER TABLE trips ADD COLUMN is_public BOOLEAN DEFAULT false;

ALTER TABLE places MODIFY COLUMN address VARCHAR(500);
ALTER TABLE places MODIFY COLUMN latitude DECIMAL(10, 8) NOT NULL;
ALTER TABLE places MODIFY COLUMN longitude DECIMAL(11, 8) NOT NULL;

CREATE TABLE trip_days (
    day_id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    day_number INT NOT NULL,
    date DATE,
    CONSTRAINT pk_trip_days PRIMARY KEY (day_id),
    CONSTRAINT fk_trip_days_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
);

CREATE TABLE itineraries (
    itinerary_id VARCHAR(36) NOT NULL,
    day_id VARCHAR(36) NOT NULL,
    place_id VARCHAR(36) NOT NULL,
    visit_order INT NOT NULL,
    notes TEXT,
    time_spent_minutes INT,
    CONSTRAINT pk_itineraries PRIMARY KEY (itinerary_id),
    CONSTRAINT fk_itineraries_day_id FOREIGN KEY (day_id) REFERENCES trip_days(day_id) ON DELETE CASCADE,
    CONSTRAINT fk_itineraries_place_id FOREIGN KEY (place_id) REFERENCES places(place_id)
);
