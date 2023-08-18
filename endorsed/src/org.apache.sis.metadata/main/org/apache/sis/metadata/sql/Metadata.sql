--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--

--
-- Definition of main metadata table.
-- This script requires "Citations.sql" and "Contents.sql" to be executed first.
-- Current version contains very few columns. This will be expanded in future versions.
--
CREATE TABLE metadata."Metadata" (
  "ID"                   VARCHAR(15) NOT NULL PRIMARY KEY,
  "metadataIdentifier"   VARCHAR(15) REFERENCES metadata."Identifier"     ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT,
  "parentMetadata"       VARCHAR(15) REFERENCES metadata."Citation"       ("ID") ON UPDATE RESTRICT ON DELETE RESTRICT
);
