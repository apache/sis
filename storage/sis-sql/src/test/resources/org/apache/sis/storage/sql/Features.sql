
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements. See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.


-- Create a temporary database on PostgreSQL for testing SQL store.
-- The main table is "Cities", with associations to two other tables:
--
--   "Countries" through imported keys ("Cities" references "Countries")
--   "Parks" through exported keys ("Cities" is referenced by "Parks").

CREATE TABLE features."Countries" (
    code           CHARACTER(3)          NOT NULL,
    "native name"  CHARACTER VARYING(20) NOT NULL,

    CONSTRAINT "PK_Country" PRIMARY KEY (code)
);


CREATE TABLE features."Cities" (
    country        CHARACTER(3)          NOT NULL,
    "native name"  CHARACTER VARYING(20) NOT NULL,
    "translation"  CHARACTER VARYING(20) NOT NULL,
    population     INTEGER,

    CONSTRAINT "PK_City"    PRIMARY KEY (country, "native name"),
    CONSTRAINT "FK_Country" FOREIGN KEY (country) REFERENCES features."Countries"(code)
);


CREATE TABLE features."Parks" (
    country        CHARACTER(3)          NOT NULL,
    city           CHARACTER VARYING(20) NOT NULL,
    "native name"  CHARACTER VARYING(20) NOT NULL,
    "translation"  CHARACTER VARYING(20) NOT NULL,

    CONSTRAINT "PK_Park" PRIMARY KEY (country, city, "native name"),
    CONSTRAINT "FK_City" FOREIGN KEY (country, city) REFERENCES features."Cities"(country, "native name") ON DELETE CASCADE
);


COMMENT ON TABLE features."Cities"    IS 'The main table for this test.';
COMMENT ON TABLE features."Countries" IS 'Countries in which a city is located.';
COMMENT ON TABLE features."Parks"     IS 'Parks in cities.';



-- Add enough data for having at least two parks for a city.
-- The data intentionally use ideograms for testing encoding.

INSERT INTO features."Countries" (code, "native name") VALUES
    ('CAN', 'Canada'),
    ('FRA', 'France'),
    ('JPN', '日本');

INSERT INTO features."Cities" (country, "native name", "translation", population) VALUES
    ('CAN', 'Montréal', 'Montreal', 1704694),       -- Population in 2016
    ('CAN', 'Québec',   'Quebec',    531902),       -- Population in 2016
    ('FRA', 'Paris',    'Paris',    2206488),       -- Population in 2017
    ('JPN', '東京',     'Tōkyō',   13622267);       -- Population in 2016

INSERT INTO features."Parks" (country, city, "native name", "translation") VALUES
    ('CAN', 'Montréal', 'Mont Royal',           'Mount Royal'),
    ('FRA', 'Paris',    'Jardin des Tuileries', 'Tuileries Garden'),
    ('FRA', 'Paris',    'Jardin du Luxembourg', 'Luxembourg Garden'),
    ('JPN', '東京',     '代々木公園',           'Yoyogi-kōen'),
    ('JPN', '東京',     '新宿御苑',             'Shinjuku Gyoen');
