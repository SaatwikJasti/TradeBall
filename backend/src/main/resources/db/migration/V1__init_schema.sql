-- V1__init_schema.sql
-- TradeBall relational schema

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE players (
    id              BIGSERIAL PRIMARY KEY,
    external_id     VARCHAR(64)  NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    position        VARCHAR(16),
    team            VARCHAR(16),
    age             INTEGER,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_players_external_id UNIQUE (external_id)
);

CREATE INDEX idx_players_name ON players (last_name, first_name);
CREATE INDEX idx_players_team ON players (team);
CREATE INDEX idx_players_position ON players (position);
CREATE INDEX idx_players_active ON players (active);

CREATE TABLE player_stats (
    id                      BIGSERIAL PRIMARY KEY,
    player_id               BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    season                  INTEGER      NOT NULL,
    games_played            INTEGER      NOT NULL DEFAULT 0,
    points                  DOUBLE PRECISION NOT NULL DEFAULT 0,
    rebounds                DOUBLE PRECISION NOT NULL DEFAULT 0,
    assists                 DOUBLE PRECISION NOT NULL DEFAULT 0,
    steals                  DOUBLE PRECISION NOT NULL DEFAULT 0,
    blocks                  DOUBLE PRECISION NOT NULL DEFAULT 0,
    three_pointers          DOUBLE PRECISION NOT NULL DEFAULT 0,
    field_goal_percentage   DOUBLE PRECISION NOT NULL DEFAULT 0,
    free_throw_percentage   DOUBLE PRECISION NOT NULL DEFAULT 0,
    turnovers               DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_stats_player_season UNIQUE (player_id, season)
);

CREATE INDEX idx_player_stats_season ON player_stats (season);

CREATE TABLE rosters (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rosters_user_id ON rosters (user_id);

CREATE TABLE roster_players (
    roster_id       BIGINT NOT NULL REFERENCES rosters(id) ON DELETE CASCADE,
    player_id       BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    PRIMARY KEY (roster_id, player_id)
);

CREATE INDEX idx_roster_players_player_id ON roster_players (player_id);

CREATE TABLE trade_evaluations (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    score           INTEGER      NOT NULL,
    verdict         VARCHAR(32)  NOT NULL,
    explanation     TEXT,
    model_version   VARCHAR(64)  NOT NULL,
    CONSTRAINT fk_trade_evaluations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_trade_evaluations_verdict CHECK (verdict IN ('GREAT_TRADE', 'FAIR_TRADE', 'POOR_TRADE')),
    CONSTRAINT ck_trade_evaluations_score CHECK (score >= 0 AND score <= 100)
);

CREATE INDEX idx_trade_evaluations_user_id ON trade_evaluations (user_id);
CREATE INDEX idx_trade_evaluations_created_at ON trade_evaluations (created_at DESC);

CREATE TABLE trade_evaluation_players (
    id                      BIGSERIAL PRIMARY KEY,
    trade_evaluation_id     BIGINT NOT NULL REFERENCES trade_evaluations(id) ON DELETE CASCADE,
    player_id               BIGINT NOT NULL REFERENCES players(id),
    direction               VARCHAR(16) NOT NULL,
    fantasy_score           DOUBLE PRECISION NOT NULL,
    CONSTRAINT ck_tep_direction CHECK (direction IN ('INCOMING', 'OUTGOING'))
);

CREATE INDEX idx_tep_evaluation_id ON trade_evaluation_players (trade_evaluation_id);

CREATE TABLE trade_evaluation_categories (
    id                      BIGSERIAL PRIMARY KEY,
    trade_evaluation_id     BIGINT NOT NULL REFERENCES trade_evaluations(id) ON DELETE CASCADE,
    category                VARCHAR(16) NOT NULL,
    incoming_value          DOUBLE PRECISION NOT NULL,
    outgoing_value          DOUBLE PRECISION NOT NULL,
    delta                   DOUBLE PRECISION NOT NULL,
    z_score_delta           DOUBLE PRECISION NOT NULL,
    impact                  VARCHAR(16) NOT NULL,
    CONSTRAINT ck_tec_impact CHECK (impact IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE'))
);

CREATE INDEX idx_tec_evaluation_id ON trade_evaluation_categories (trade_evaluation_id);

CREATE TABLE data_sync_jobs (
    id                  BIGSERIAL PRIMARY KEY,
    type                VARCHAR(32)  NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMP WITH TIME ZONE,
    records_processed   INTEGER      NOT NULL DEFAULT 0,
    records_failed      INTEGER      NOT NULL DEFAULT 0,
    error_message       TEXT,
    CONSTRAINT ck_sync_type CHECK (type IN ('PLAYERS', 'STATS')),
    CONSTRAINT ck_sync_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'PARTIAL'))
);

CREATE INDEX idx_data_sync_jobs_started_at ON data_sync_jobs (started_at DESC);

