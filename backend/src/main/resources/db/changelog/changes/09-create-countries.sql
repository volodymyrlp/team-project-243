--liquibase formatted sql
--changeset ihor:create-countries-table
CREATE TABLE countries (
    country_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    popularity_score INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_countries PRIMARY KEY (country_id),
    CONSTRAINT uq_countries_name UNIQUE (name)
);
