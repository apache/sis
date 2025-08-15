# EPSG dataset update procedure

The `org.apache.sis.referencing.factory.sql.epsg` package in the `non-free:sis-epsg` Maven artifact
provides SQL scripts for installing a local copy of the [EPSG geodetic dataset](https://epsg.org/).
That dataset provides definitions for thousands of Coordinate Reference Systems (CRS),
together with parameter values for thousands of Coordinate Operations between various pairs of CRS.
EPSG is maintained by the [International Association of Oil and Gas Producers](https://www.iogp.org/) (IOGP)
Surveying & Positioning Committee and is subject to [EPSG terms of use](https://epsg.org/terms-of-use.html).
Because of incompatibilities between EPSG terms of use and Apache 2 license,
the EPSG geodetic dataset is not distributed with Apache SIS source code or other bundles released on Apache web sites.
A modified copy of the dataset is provided in a [separated source code repository](https://svn.apache.org/repos/asf/sis/data/non-free/EPSG/)
for inclusion in the `org.apache.sis.non-free:sis-epsg` artifact only if links are provided as
[described in the main module](../../../../../../../../main/org/apache/sis/referencing/factory/sql/epsg/README.md).
The copy has the same content as the original EPSG scripts, but more compact and sometime with accent characters added.
Column order may also differ for more consistency (e.g., keeping `deprecated` as the last column in all tables).


## How to apply EPSG geodetic dataset updates

This page explains how to convert the SQL scripts published by EPSG into the more compact form used by Apache SIS.
The compact form is about half the size of the original files. Compaction is achieved by avoiding redundant statements.
This conversion applies only to the data types, the integrity constraints and the way that the SQL scripts are written.
No data value should be altered, accept for accented letters in some names. Steps to follow:


### Get the new EPSG scripts

Download the latest SQL scripts for PostgreSQL from https://epsg.org/ (require registration).
Unzip in the directory of your choice and remember the path to that directory:

```bash
unzip EPSG-<version>-PostgreSQL.zip
export EPSG_SCRIPTS=$PWD
```

Execute the scripts in the `public` schema of a PostgreSQL database on the local host.
This page assumes that the database name is "Referencing", but any other name can be used
if the argument given to `TableScriptUpdater` (later in this page) is adjusted accordingly.

If a copy of the original SQL scripts (as downloaded from EPSG) for the previous version is still available,
and if the following commands report no difference, then jump to "execute main" step.

```bash
cd _<directory containing EPSG scripts of previous version>_
diff PostgreSQL_Table_Script.sql $EPSG_SCRIPTS/PostgreSQL_Table_Script.sql
diff PostgreSQL_FKey_Script.sql  $EPSG_SCRIPTS/PostgreSQL_FKey_Script.sql
```

Otherwise, move to the directory which contains the Apache SIS scripts:

```bash
cd <path to a local copy of http://svn.apache.org/repos/asf/sis/data/non-free/EPSG/>
export NON_FREE_DIR=$PWD
```

Overwrite `Tables.sql` and `FKeys.sql` with the new SQL scripts.
Do not overwrite `Data.sql` yet:

```bash
cp $EPSG_SCRIPTS/PostgreSQL_Table_Script.sql Tables.sql
cp $EPSG_SCRIPTS/PostgreSQL_FKey_Script.sql  FKeys.sql
```


### Manual checks and editions

Open the `Tables.sql` file for edition:

* Keep the header comments that existed in the overwritten file.
* Remove the `"Change"` table and the `change_id` column in all tables. They are EPSG metadata unused by Apache SIS.
* Remove the `information_source`, `data_source` and `revision_date` columns in all tables. They are EPSG metadata unused by Apache SIS.
* Remove the `crs_scope`, `coord_op_scope`, `datum_scope` and `area_of_use_code` columns, which are deprecated.
* Keep the same column order than in the previous `Tables.sql`.
* Rename `epsg_` table names to the camel case convention used by Apache SIS.
* Suppress trailing `NULL` (not to be confused with `NOT NULL`) as they are implicit.
* In the statement creating the `coordinateaxis` table,
  add the `NOT NULL` constraint to the `coord_axis_code` column.
* In the statement creating the `epsg_datum` table,
  change the type of the `realization_epoch` and `publication_date` columns to `DATE`.
* Change the type of `ellipsoid_shape`, `reverse_op`, `param_sign_reversal`
  `show_crs`, `show_operation` and all `deprecated` fields from `SMALLINT`
  (or sometimes `VARCHAR(3)`) to `BOOLEAN`.
* Change all `FLOAT` types to `DOUBLE PRECISION` because Apache SIS reads all numbers as `double` type.
  This change avoids spurious digits in the conversions from `float` to `double`.
* Change the type of `epsg_usage` column from `SERIAL` to `INTEGER NOT NULL`.
* Change the type of every `table_name` columns from `VARCHAR(80)` to `"Table Name"`.
* Change the type of `coord_ref_sys_kind` column from `VARCHAR(24)` to `"CRS Kind"`.
* Change the type of `coord_sys_type` column from `VARCHAR(24)` to `"CS Kind"`.
* Change the type of `datum_type` column from `VARCHAR(24)` to `"Datum Kind"`.
* Change the type of `supersession_type` column from `VARCHAR(50)` to `"Supersession Type"`.
* If new enumeration values are added, check the maximal lengths of `VARCHAR` replacements in `EPSGInstaller`.
* Suppress trailing spaces and save.

Then open the `FKeys.sql` file for edition:

* Remove the `fk_change_id` foreigner key.
* At the end of all `ALTER TABLE` statement, append `ON UPDATE RESTRICT ON DELETE RESTRICT`.
* Suppress trailing spaces and save.

Usually, the above editions result in no change compared to the previous scripts (ignoring white spaces),
in which case the maintainer can just revert the changes in order to preserve the formatting.
However, if some changes are found in the schema, then hard-coded values in the `DataScriptFormatter` class may need
to be modified, in particular the `booleanColumnIndicesForTables` and `doubleColumnIndicesForTables` collections.


### Automatic updates after the manual checks

Execute the `main` method of the `org.apache.sis.referencing.factory.sql.epsg.*Updater` classes
located in the test directory of the `org.apache.sis.non-free:sis-epsg` Maven sub-project.
Adjust version numbers as needed in the following commands:

```bash
cd _<path to SIS project directory>_
gradle clean test jar
export CLASSPATH=~/.m2/repository/org/apache/derby/derby/10.14.2.0/derby-10.14.2.0.jar
export CLASSPATH=~/.m2/repository/javax/measure/unit-api/2.1.3/unit-api-2.1.3.jar:$CLASSPATH
export CLASSPATH=$PWD/geoapi/snapshot/geoapi/target/geoapi-3.0.2.jar:$CLASSPATH
export CLASSPATH=$PWD/endorsed/build/libs/org.apache.sis.referencing.jar:$CLASSPATH
export CLASSPATH=$PWD/endorsed/build/libs/org.apache.sis.metadata.jar:$CLASSPATH
export CLASSPATH=$PWD/endorsed/build/libs/org.apache.sis.util.jar:$CLASSPATH
export CLASSPATH=$PWD/endorsed/build/classes/java/test/org.apache.sis.referencing:$CLASSPATH
export CLASSPATH=$PWD/endorsed/build/classes/java/test/org.apache.sis.metadata:$CLASSPATH
export CLASSPATH=$PWD/optional/build/classes/java/test/org.apache.sis.referencing.epsg:$CLASSPATH

# From any directory
java org.apache.sis.referencing.factory.sql.epsg.TableScriptUpdater $NON_FREE_DIR/Tables.sql Referencing
java org.apache.sis.referencing.factory.sql.epsg.DataScriptUpdater  $EPSG_SCRIPTS/PostgreSQL_Data_Script.sql $NON_FREE_DIR/Data.sql
```


### Finalize

Run the tests. It it convenient to run `org.apache.sis.referencing.factory.sql.EPSGInstallerTest`
in an IDE first, for easier debugging if some changes in database structure or content broke some code.
Then the whole Apache SIS project should be [tested extensively](https://sis.apache.org/source.html#tests),
preferably with a PostgreSQL server ready to accept local connections to `SpatialMetadataTest` database:

```bash
EXPORT SIS_TEST_OPTIONS=extensive,postgresql
gradle test
```

Regenerate the HTML pages listing available CRS and coordinate operation methods.
Those pages will be copied into the
[site/content/tables/](https://github.com/apache/sis-site/tree/main/static/tables)
directory during the [release process](https://sis.apache.org/release-management.html#update-crs-list),
but for now the purpose is only to check if there is errors:

* Upgrade the `FACTORY.VERSION` value defined in the
  `org.apache.sis.referencing.report.CoordinateReferenceSystems` class, then execute that class.
  It can be executed from the IDE since the `main` method takes no argument.
  This class will write a `CoordinateReferenceSystems.html` file in current directory
  (the full path will be printed in the standard output).
* Execute the `org.apache.sis.referencing.report.CoordinateOperationMethods` class.
  It can be executed from the IDE since the `main` method takes no argument.
  This class will write a `CoordinateOperationMethods.html` file in current directory.

Open those generated HTML files in a web browser and verify the result.
