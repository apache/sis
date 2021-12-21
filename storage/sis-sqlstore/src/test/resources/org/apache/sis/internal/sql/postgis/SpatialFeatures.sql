-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements. See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.


-- Create a temporary database on PostgreSQL for testing geometries and rasters.
-- The "postgis_raster" extension must be installed before to execute this test.

SET search_path TO public;

CREATE TABLE features."SpatialData" (
    "filename" VARCHAR(20) NOT NULL,
    "image"    RASTER      NOT NULL,

    CONSTRAINT "PK_SpatialData" PRIMARY KEY ("filename")
);

INSERT INTO features."SpatialData" ("filename", "image")
  VALUES('raster-ushort.wkb', ('0100000200000000000000F43F000000000000044000000000000054C00000000000004EC0000000'
      || '00000000000000000000000000E6100000030004000600006F0079008300D300DD00E700370141014B019B01A501AF01060000'
      || '70007A008400D400DE00E800380142014C019C01A601B001')::raster);
