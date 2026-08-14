-- Repeatable demo seed (runs only when the demo profile adds classpath:db/seed).
-- Idempotent: parent rows use ON CONFLICT; child rows are delete-then-reinsert.

INSERT INTO users (email, password_hash, role, created_at)
VALUES (
    'demo@example.com',
    '$2a$10$hoqm0.1wr2BXtdfjjxGFN.fwqItzsofLJNFOgXMuq.T28eCbvoprG',
    'USER',
    NOW()
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO pokemon (
    external_id, name, sprite_url, category, mass, local_name, region, created_at, updated_at
)
VALUES (
    1,
    'bulbasaur',
    'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
    'Seed Pokémon',
    69,
    'Bulby',
    'Kanto',
    NOW(),
    NOW()
)
ON CONFLICT (external_id) DO NOTHING;

DELETE FROM pokemon_ability
WHERE pokemon_id = (SELECT id FROM pokemon WHERE external_id = 1);

INSERT INTO pokemon_ability (pokemon_id, ability, position)
SELECT p.id, v.ability, v.position
FROM pokemon p
CROSS JOIN (VALUES ('overgrow', 0), ('chlorophyll', 1)) AS v(ability, position)
WHERE p.external_id = 1;

DELETE FROM pokemon_internal_tag
WHERE pokemon_id = (SELECT id FROM pokemon WHERE external_id = 1);

INSERT INTO pokemon_internal_tag (pokemon_id, tag, position)
SELECT p.id, v.tag, v.position
FROM pokemon p
CROSS JOIN (VALUES ('starter', 0), ('kanto', 1)) AS v(tag, position)
WHERE p.external_id = 1;

INSERT INTO pokemon (
    external_id, name, sprite_url, category, mass, local_name, region, created_at, updated_at
)
VALUES (
    4,
    'charmander',
    'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png',
    'Lizard Pokémon',
    85,
    'Ember',
    'Kanto',
    NOW(),
    NOW()
)
ON CONFLICT (external_id) DO NOTHING;

DELETE FROM pokemon_ability
WHERE pokemon_id = (SELECT id FROM pokemon WHERE external_id = 4);

INSERT INTO pokemon_ability (pokemon_id, ability, position)
SELECT p.id, v.ability, v.position
FROM pokemon p
CROSS JOIN (VALUES ('blaze', 0), ('solar-power', 1)) AS v(ability, position)
WHERE p.external_id = 4;

DELETE FROM pokemon_internal_tag
WHERE pokemon_id = (SELECT id FROM pokemon WHERE external_id = 4);

INSERT INTO pokemon_internal_tag (pokemon_id, tag, position)
SELECT p.id, v.tag, v.position
FROM pokemon p
CROSS JOIN (VALUES ('starter', 0), ('kanto', 1)) AS v(tag, position)
WHERE p.external_id = 4;

INSERT INTO pokemon (
    external_id, name, sprite_url, category, mass, local_name, region, created_at, updated_at
)
VALUES (
    7,
    'squirtle',
    'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png',
    'Tiny Turtle Pokémon',
    90,
    'Shellshock',
    'Kanto',
    NOW(),
    NOW()
)
ON CONFLICT (external_id) DO NOTHING;

DELETE FROM pokemon_ability
WHERE pokemon_id = (SELECT id FROM pokemon WHERE external_id = 7);

INSERT INTO pokemon_ability (pokemon_id, ability, position)
SELECT p.id, v.ability, v.position
FROM pokemon p
CROSS JOIN (VALUES ('torrent', 0), ('rain-dish', 1)) AS v(ability, position)
WHERE p.external_id = 7;

DELETE FROM pokemon_internal_tag
WHERE pokemon_id = (SELECT id FROM pokemon WHERE external_id = 7);

INSERT INTO pokemon_internal_tag (pokemon_id, tag, position)
SELECT p.id, v.tag, v.position
FROM pokemon p
CROSS JOIN (VALUES ('starter', 0), ('kanto', 1)) AS v(tag, position)
WHERE p.external_id = 7;
