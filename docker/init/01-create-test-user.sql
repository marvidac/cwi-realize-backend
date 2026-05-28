CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    username VARCHAR(80) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    current_token_hash VARCHAR(128),
    token_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO users (name, email, username, password, created_at)
VALUES (
    'Usuário Teste',
    'usuario.teste@example.com',
    'usuario.teste',
    '$2b$10$seE0pquOBNog0aeG2xhmlOn5/kiaFsHZGPiz4zJ8rpOGe15NSV9pG',
    NOW()
)
ON CONFLICT (username) DO NOTHING;
