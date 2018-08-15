--
-- Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
-- http://www.apache.org/licenses/LICENSE-2.0
--

--
-- Definition of a few types for referencing by coordinates (ISO 19111).
-- A database schema is already defined by the EPSG geodetic dataset.
-- This script adds only a few enumerations to be considered as a kind
-- of metadata.
--
CREATE TYPE metadata."AxisDirection" AS ENUM (
  'north', 'northNorthEast', 'northEast', 'eastNorthEast',
  'east',  'eastSouthEast',  'southEast', 'southSouthEast',
  'south', 'southSouthWest', 'southWest', 'westSouthWest',
  'west',  'westNorthWest',  'northWest', 'northNorthWest',
  'up',    'down',
  'geocentricX',    'geocentricY',    'geocentricZ',
  'columnPositive', 'columnNegative', 'rowPositive', 'rowNegative',
  'displayRight',   'displayLeft',    'displayUp',   'displayDown',
  'forward',        'aft',
  'port',           'starboard',
  'clockwise',      'counterClockwise',
  'towards',        'awayFrom',
  'future',         'past');
