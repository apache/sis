
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements. See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.


-- Create a temporary database on PostgreSQL for testing SQL store.
-- The main table is "Cities", with associations to two other tables:
--
--   "Countries" through imported keys ("Cities" references "Countries")
--   "Parks" through exported keys ("Cities" is referenced by "Parks").

CREATE TABLE features."Countries" (
    "code"         CHARACTER(3)          NOT NULL,
    "native_name"  CHARACTER VARYING(20) NOT NULL,

    CONSTRAINT "PK_Country" PRIMARY KEY ("code")
);


CREATE TABLE features."Cities" (
    "country"      CHARACTER(3)          NOT NULL,
    "native_name"  CHARACTER VARYING(20) NOT NULL,
    "english_name" CHARACTER VARYING(20),
    "population"   INTEGER,

    CONSTRAINT "PK_City"    PRIMARY KEY ("country", "native_name"),
    CONSTRAINT "FK_Country" FOREIGN KEY ("country") REFERENCES features."Countries"("code")
);


CREATE TABLE features."Parks" (
    "country"      CHARACTER(3)          NOT NULL,
    "city"         CHARACTER VARYING(20) NOT NULL,
    "native_name"  CHARACTER VARYING(20) NOT NULL,
    "english_name" CHARACTER VARYING(20),

    CONSTRAINT "PK_Park" PRIMARY KEY ("country", "city", "native_name"),
    CONSTRAINT "FK_City" FOREIGN KEY ("country", "city") REFERENCES features."Cities"("country", "native_name") ON DELETE CASCADE
);


COMMENT ON TABLE features."Cities"    IS 'The main table for this test.';
COMMENT ON TABLE features."Countries" IS 'Countries in which a city is located.';
COMMENT ON TABLE features."Parks"     IS 'Parks in cities.';



-- Add enough data for having at least two parks for a city.
-- The data intentionally use ideograms for testing encoding.

INSERT INTO features."Countries" ("code", "native_name") VALUES
    ('CAN', 'Canada'),
    ('FRA', 'France'),
    ('JPN', '日本');

-- All numbers in the "population" columns are 2016 or 2017 data.
INSERT INTO features."Cities" ("country", "native_name", "english_name", "population") VALUES
    ('CAN', 'Montréal', 'Montreal', 1704694),
    ('CAN', 'Québec',   'Quebec',    531902),
    ('FRA', 'Paris',    'Paris',    2206488),
    ('JPN', '東京',     'Tōkyō',   13622267);

INSERT INTO features."Parks" ("country", "city", "native_name", "english_name") VALUES
    ('CAN', 'Montréal', 'Mont Royal',           'Mount Royal'),
    ('FRA', 'Paris',    'Jardin des Tuileries', 'Tuileries Garden'),
    ('FRA', 'Paris',    'Jardin du Luxembourg', 'Luxembourg Garden'),
    ('JPN', '東京',     '代々木公園',           'Yoyogi-kōen'),
    ('JPN', '東京',     '新宿御苑',             'Shinjuku Gyoen');
