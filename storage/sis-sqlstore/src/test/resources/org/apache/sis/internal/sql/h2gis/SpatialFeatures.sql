-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements. See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.


-- Create a temporary database on H2GIS for testing geometries.

CREATE ALIAS IF NOT EXISTS H2GIS_SPATIAL FOR "org.h2gis.functions.factory.H2GISFunctions.load";
CALL H2GIS_SPATIAL();

CREATE TABLE "SpatialData" (
    "geometry"   LINESTRING,
    "identifier" INT,

    CHECK ST_SRID("geometry") = 4326,
    CONSTRAINT "PK_SpatialData" PRIMARY KEY ("identifier")
);

INSERT INTO "SpatialData" VALUES
  (ST_GeomFromText('LINESTRING(32  2, 18  6, -3 13)', 4326), 3),
  (ST_GeomFromText('LINESTRING(67 12, 42 17, 45 25)', 4326), 1),
  (ST_GeomFromText('LINESTRING(94 84, 74 72, 82 69)', 4326), 8)
