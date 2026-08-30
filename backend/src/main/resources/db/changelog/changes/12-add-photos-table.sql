--liquibase formatted sql
--changeset ihor:add-photos-table
CREATE TABLE photos (
    photo_id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    uploader_id VARCHAR(36) NOT NULL,
    url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_photos PRIMARY KEY (photo_id),
    CONSTRAINT fk_photos_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_photos_uploader_id FOREIGN KEY (uploader_id) REFERENCES users(user_id) ON DELETE CASCADE
);
