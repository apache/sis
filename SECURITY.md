# Security Policy

Apache SIS does not store credentials (user logins or passwords).
JDBC URLs for connecting to databases or paths to opened data files
may appear in log records or (only if the JavaFX application is used) in Java preferences.
Prerequisites for running Apache SIS safely are listed below.


## Assumptions

URLs for database connections should not contain passwords.
Instead, `DataSource`, JNDI or web application containers
[can be used](https://sis.apache.org/epsg.html#existing).

If Apache SIS is connected to a MySQL or MariaDB database,
the [SQL mode](https://mariadb.com/docs/server/server-management/variables-and-modes/sql_mode)
should contain `NO_BACKSLASH_ESCAPES`.
No particular setting is needed for PostgreSQL, DuckDB, HSQL, H2 or Derby databases.


## Guarantees

Some file formats can contain references (usually as URLs) to auxiliary files:

* `PARAMETERFILE` elements in Well Known Text (WKT),
* `xlink:href` attributes in Geographic Markup Language (GML),
* Any coordinate operation working with a datum shift grid such as NADCON or NTv2,
* Landsat scenes with bands stored in separate TIFF images.

By default, Apache SIS opens referenced files only if they are on the same host
and in the same directory as, or in a sub-directory of, the referencing file.
In some special cases such as datum shift grids, Apache SIS may also open files
in the local directory identified by the `SIS_DATA` environment variable.
There is no fallback if `SIS_DATA` is not defined.


## Reporting a Vulnerability

Apache SIS follows the [Apache Software Foundation security process](https://www.apache.org/security/).
Please report suspected vulnerabilities privately to `security@apache.org`;
do not open public GitHub issues or pull requests for security reports.
