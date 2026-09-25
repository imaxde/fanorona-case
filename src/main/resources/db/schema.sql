CREATE TABLE players (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    lookup_name TEXT NOT NULL UNIQUE
);

CREATE TABLE player_stats (
    player_id INTEGER PRIMARY KEY REFERENCES players(id),
    games INTEGER NOT NULL DEFAULT 0 CHECK (games >= 0),
    wins INTEGER NOT NULL DEFAULT 0 CHECK (wins >= 0),
    losses INTEGER NOT NULL DEFAULT 0 CHECK (losses >= 0),
    CHECK (games >= wins + losses)
);

CREATE TABLE games (
    id INTEGER PRIMARY KEY,
    played_on TEXT NOT NULL,
    white_player_id INTEGER NOT NULL REFERENCES players(id),
    black_player_id INTEGER NOT NULL REFERENCES players(id),
    outcome TEXT NOT NULL CHECK (outcome IN ('WHITE_WIN', 'BLACK_WIN', 'DRAW')),
    board_width INTEGER NOT NULL CHECK (board_width > 0),
    board_height INTEGER NOT NULL CHECK (board_height > 0),
    initial_position TEXT NOT NULL,
    CHECK (white_player_id <> black_player_id)
);

CREATE TABLE moves (
    game_id INTEGER NOT NULL REFERENCES games(id),
    move_index INTEGER NOT NULL CHECK (move_index >= 0),
    side TEXT NOT NULL CHECK (side IN ('WHITE', 'BLACK')),
    PRIMARY KEY (game_id, move_index)
);

CREATE TABLE actions (
    game_id INTEGER NOT NULL,
    move_index INTEGER NOT NULL,
    action_index INTEGER NOT NULL CHECK (action_index >= 0),
    kind TEXT NOT NULL CHECK (kind IN ('PAIKA', 'APPROACH', 'WITHDRAWAL')),
    from_x INTEGER NOT NULL,
    from_y INTEGER NOT NULL,
    to_x INTEGER NOT NULL,
    to_y INTEGER NOT NULL,
    PRIMARY KEY (game_id, move_index, action_index),
    FOREIGN KEY (game_id, move_index) REFERENCES moves(game_id, move_index)
);

CREATE INDEX games_by_white ON games(white_player_id);
CREATE INDEX games_by_black ON games(black_player_id);
