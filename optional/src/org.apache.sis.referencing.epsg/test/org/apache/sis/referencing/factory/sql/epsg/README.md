# EPSG dataset update procedure

The `org.apache.sis.referencing.factory.sql.epsg` package in the `non-free:sis-epsg` Maven artifact
provides SQL scripts for installing a local copy of the [EPSG geodetic dataset](https://epsg.org/).
This dataset provides definitions for thousands of Coordinate Reference Systems (CRS),
together with parameter values for thousands of Coordinate Operations between various pairs of CRS.
EPSG is maintained by the [International Association of Oil and Gas Producers](https://www.iogp.org/) (IOGP)
Surveying & Positioning Committee and is subject to [EPSG terms of use](https://epsg.org/terms-of-use.html).
Because of incompatibilities between EPSG terms of use and Apache 2 license, the EPSG geodetic dataset is not distributed
by default with Apache SIS. A copy of the dataset is provided in a separated module in a separated source code repository.
The Maven identifier of that module is `org.apache.sis.non-free:sis-epsg` and the source repository is located at
http://svn.apache.org/repos/asf/sis/data/non-free/sis-epsg.
The EPSG scripts are copied in that module with identical content, but in a more compact format.

This `org.apache.sis.referencing.factory.sql.epsg` package in `endorsed/org.opengis.sis.referencing` module
contains only tools for maintaining the `non-free/org.apache.sis.referencing.epsg` module.
This package is provided only in the **test** directory, not in the main directory, because the
`org.apache.sis.referencing.factory.sql.epsg` package name is reserved by the `non-free/org.apache.sis.referencing.epsg` module.
The `endorsed/org.apache.sis.referencing` module should not distribute anything in packages owned by other modules.
However, it is okay to use those package names in directories that are not part of the distribution, like tests.
We put those tools here for easier maintainance when the core of Apache SIS is modified.


## How to apply EPSG geodetic dataset updates

This page explains how to convert the SQL scripts published by EPSG into the more compact form used by Apache SIS.
The compact form is about half the size of the original files. Compaction is achieved by avoiding redundant statements.
This conversion applies only to the data types, the integrity constraints and the way the SQL scripts are written.
No data value should be altered. Steps to follow:

Download the latest SQL scripts for PostgreSQL from https://epsg.org/ (require registration).
Unzip in the directory of your choice and remember the path to that directory:

```
unzip EPSG-PSQL-export-_<version>_.zip
export EPSG_SCRIPTS=$PWD
```

If a copy of the original SQL scripts (as downloaded from EPSG) for the previous version is still available,
and if the following commands report no difference, then jump to "execute main" step.

```
cd _<directory containing EPSG scripts of previous version>_
diff PostgreSQL_Table_Script.sql $EPSG_SCRIPTS/PostgreSQL_Table_Script.sql
diff PostgreSQL_FKey_Script.sql  $EPSG_SCRIPTS/PostgreSQL_FKey_Script.sql
```

Otherwise, move to the directory which contains the Apache SIS scripts:

```
cd <SIS_HOME>/non-free/sis-epsg/src/main/resources/org/apache/sis/referencing/factory/sql/epsg/
```

Overwrite `Tables.sql` and `FKeys.sql` with the new SQL scripts.
Do not overwrite `Data.sql` and `Indexes.sql`:

```
cp $EPSG_SCRIPTS/PostgreSQL_Table_Script.sql Tables.sql
cp $EPSG_SCRIPTS/PostgreSQL_FKey_Script.sql  FKeys.sql
```

Open the `Tables.sql` file for edition:

* Keep the header comments that existed in the overwritten file.
* In the statement creating the `coordinateaxis` table,
  add the `NOT NULL` constraint to the `coord_axis_code` column.
* In the statement creating the `change` table,
  remove the `UNIQUE` constraint on the `change_id` column
  and add a `CONSTRAINT pk_change PRIMARY KEY (change_id)` line instead.
* In the statement creating the `epsg_datum` table,
  change the type of the `realization_epoch` column to `DATE`.
* Change the type of `ellipsoid_shape`, `reverse_op`, `param_sign_reversal`
  `show_crs`, `show_operation` and all `deprecated` fields from `SMALLINT`
  (or sometimes `VARCHAR(3)`) to `BOOLEAN`.
* Change the type of every `table_name` columns from `VARCHAR(80)` to `epsg_table_name`.
* Change the type of `coord_ref_sys_kind` column from `VARCHAR(24)` to `epsg_crs_kind`.
* Change the type of `coord_sys_type` column from `VARCHAR(24)` to `epsg_cs_kind`.
* Change the type of `datum_type` column from `VARCHAR(24)` to `epsg_datum_kind`.
* Suppress trailing spaces and save.

Usually this results in no change at all compared to the previous script (ignoring white spaces),
in which case the maintainer can just revert the changes in order to preserve the formatting.
Then open the `FKeys.sql` file for edition:

* At the end of all `ALTER TABLE` statement, append `ON UPDATE RESTRICT ON DELETE RESTRICT`.
* Suppress trailing spaces and save.

In most cases this results in unmodified `FKeys.sql` file compared to the previous version.


### Main
Execute the `main` method of the `org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter` class
located in the test directory of `sis-referencing` module
(adjust version numbers as needed; we may provide an easier way after migration to Jigsaw modules):

```
cd _<path to SIS project directory>_
mvn clean install
export CLASSPATH=~/.m2/repository/org/apache/derby/derby/10.14.2.0/derby-10.14.2.0.jar
export CLASSPATH=$PWD/core/sis-metadata/target/test-classes:$CLASSPATH
export CLASSPATH=$PWD/target/binaries/sis-referencing-1.x-SNAPSHOT.jar:$CLASSPATH
export CLASSPATH=$PWD/core/sis-metadata/target/test-classes:$CLASSPATH
export CLASSPATH=$PWD/core/sis-referencing/target/test-classes:$CLASSPATH
cd <path to local copy of http://svn.apache.org/repos/asf/sis/data/non-free/>
java org.apache.sis.referencing.factory.sql.epsg.DataScriptFormatter $EPSG_SCRIPTS/PostgreSQL_Data_Script.sql \
     sis-epsg/src/main/resources/org/apache/sis/referencing/factory/sql/epsg/Data.sql
```

Run the tests. It it convenient to run `org.apache.sis.referencing.factory.sql.EPSGInstallerTest`
in an IDE first, for easier debugging if some changes in database structure or content broke some code.
Then the whole Apache SIS project should be [tested extensively](https://sis.apache.org/source.html#tests),
preferably with a PostgreSQL server ready to accept local connections to `SpatialMetadataTest` database:

```
EXPORT SIS_TEST_OPTIONS=extensive,postgresql
gradle test
```

Regenerate the HTML pages listing available CRS and coordinate operation methods.
Those pages will be copied into the
[site/content/tables/](http://svn.apache.org/repos/asf/sis/site/trunk/content/tables/)
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
