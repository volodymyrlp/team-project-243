--liquibase formatted sql
--changeset antigravity:create-trips-table
CREATE TABLE trips (
    trip_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_trips PRIMARY KEY (trip_id),
    CONSTRAINT fk_trips_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);
