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


### Updates the Data Definition scripts

Verify that the new SQL scripts downloaded from EPSG defines the same tables as the previous version:

```bash
cd _<directory containing EPSG scripts of previous version>_
diff PostgreSQL_Table_Script.sql $EPSG_SCRIPTS/PostgreSQL_Table_Script.sql
diff PostgreSQL_FKey_Script.sql  $EPSG_SCRIPTS/PostgreSQL_FKey_Script.sql
```

If there are some changes, port them manually to the {@code Tables.sql} and {@code FKeys.sql} scripts.
The [page listing the changes](./Changes.html) gives information about the changes to expert or to reproduce.
Then, execute the `main` method of the `org.apache.sis.referencing.factory.sql.epsg.*Updater` classes
located in the test directory of the `org.apache.sis.non-free:sis-epsg` Maven sub-project.
Adjust version numbers as needed in the following commands:

```bash
cd <path to a local copy of http://svn.apache.org/repos/asf/sis/data/non-free/EPSG/>
export NON_FREE_DIR=$PWD

cd _<path to SIS project directory>_
gradle clean test jar
export CLASSPATH=~/.m2/repository/org/apache/derby/derby/10.14.2.0/derby-10.14.2.0.jar
export CLASSPATH=~/.m2/repository/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar:$CLASSPATH
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
EXPORT SIS_TEST_OPTIONS=epsg,extensive,postgresql
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
