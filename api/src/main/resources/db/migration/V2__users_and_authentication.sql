-- Users and email/password authentication (ADR-004).

CREATE TABLE users (
    id             uuid                     PRIMARY KEY,
    -- Stored trimmed and lower-cased by the application.
    email          varchar(320)             NOT NULL,
    email_verified boolean                  NOT NULL DEFAULT false,
    display_name   varchar(100)             NOT NULL,
    created_at     timestamp with time zone NOT NULL,
    CONSTRAINT users_email_key UNIQUE (email)
);

-- Kept apart from users: someone who signs in only with Google or Apple has no row here.
CREATE TABLE password_credentials (
    user_id       uuid                     PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    -- Spring Security format, e.g. {argon2}$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>.
    password_hash varchar(255)             NOT NULL,
    updated_at    timestamp with time zone NOT NULL
);

-- Only a SHA-256 hash of each refresh token is stored. A family is one sign-in session;
-- each refresh replaces the token with a new one in the same family.
CREATE TABLE refresh_tokens (
    id         uuid                     PRIMARY KEY,
    user_id    uuid                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(64)              NOT NULL,
    family_id  uuid                     NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at    timestamp with time zone,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);
