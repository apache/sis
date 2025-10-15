# Changes in EPSG scripts

This page summarizes the changes applied by Apache SIS on the SQL scripts published by EPSG.
For more information about the use of EPSG geodetic dataset in Apache SIS, see [README](./README.md).

## Tables

Additions:
* Comments in a header for explaining the origin of the file.
* In the "Coordinate Axis" table, `NOT NULL` constraint added to the `coord_axis_code` column.
* `COLLATE "Ignore Accent and Case"` added in definitions of columns for object names or aliases.

Removal of unused tables or columns:
* The "Change" table and the `change_id` column in all tables.
* The `information_source`, `data_source` and `revision_date` columns in all tables.
* The `crs_scope`, `coord_op_scope`, `datum_scope` and `area_of_use_code` columns (deprecated).

Change of data types:
* Type of `realization_epoch` and `publication_date` columns changed to `DATE` (in the "Datum" table).
* Type of `epsg_usage` column changed from `SERIAL` to `INTEGER NOT NULL`.
* Type of `table_name` column in all tables changed from `VARCHAR(80)` to "Table Name".
* Type of `coord_ref_sys_kind` column changed from `VARCHAR(24)` to "CRS Kind".
* Type of `coord_sys_type` column changed from `VARCHAR(24)` to "CS Kind".
* Type of `datum_type` column changed from `VARCHAR(24)` to "Datum Kind".
* Type of `supersession_type` column changed from `VARCHAR(50)` to "Supersession Type".
* Type of `ellipsoid_shape`, `reverse_op`, `param_sign_reversal`, `show_crs`, `show_operation`
  and all `deprecated` columns changed from `SMALLINT` (or sometimes `VARCHAR(3)`) to `BOOLEAN`.
* Change all `FLOAT` types to `DOUBLE PRECISION` because Apache SIS reads all numbers as `double` type.
  This change avoids spurious digits in the conversions from `float` to `double`.

Changes that do not impact data:
* Rename `epsg_` table names to the camel case convention used by Apache SIS.
* Use a different column order for keeping related columns close to each other.
* Suppress trailing `NULL` (not to be confused with `NOT NULL`) as they are implicit.

**Maintenance note:** if some values were added in any enumeration, check the maximal
length of the `VARCHAR` replacements in the `EPSGInstaller.identifierReplacements` map.
If some new columns have their type changed to the Boolean or double-precision type,
some hard-coded values in the `DataScriptFormatter` class may need to be modified,
in particular the `booleanColumns` and `doubleColumns` collections.


## Foreigner keys

* Remove the `fk_change_id` foreigner key.
* At the end of all `ALTER TABLE` statement, append `ON UPDATE RESTRICT ON DELETE RESTRICT`.
