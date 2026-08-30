--liquibase formatted sql
--changeset ihor:add-tags-and-participants
CREATE TABLE tags (
    tag_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (tag_id),
    CONSTRAINT uq_tags_name UNIQUE (name)
);

CREATE TABLE trip_tags (
    trip_id VARCHAR(36) NOT NULL,
    tag_id VARCHAR(36) NOT NULL,
    CONSTRAINT pk_trip_tags PRIMARY KEY (trip_id, tag_id),
    CONSTRAINT fk_trip_tags_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_tags_tag_id FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

CREATE TABLE trip_participants (
    trip_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    CONSTRAINT pk_trip_participants PRIMARY KEY (trip_id, user_id),
    CONSTRAINT fk_trip_participants_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_participants_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
