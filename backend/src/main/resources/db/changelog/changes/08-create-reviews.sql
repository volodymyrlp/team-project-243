--liquibase formatted sql
--changeset ihor:create-reviews-table
CREATE TABLE reviews (
    review_id VARCHAR(36) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    user_id VARCHAR(36),
    content TEXT NOT NULL,
    rating INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reviews PRIMARY KEY (review_id),
    CONSTRAINT fk_reviews_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);
