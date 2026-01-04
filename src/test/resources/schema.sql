DROP TABLE IF EXISTS member;

CREATE TABLE member (
    id BINARY(16) PRIMARY KEY,
    member_id BINARY(16),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    password_updated_at TIMESTAMP,
    role INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    enabled BOOLEAN NOT NULL
);
