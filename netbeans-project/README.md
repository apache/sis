# NetBeans project files

This directory contains project configuration files for Apache NetBeans IDE.
As an alternative to opening the Gradle project, NetBeans users can open the
Ant project provided in this directory. This alternative is provided because
NetBeans 18 does not support well Module Source Hierarchy in Gradle projects.
This directory may be removed in a future version if NetBeans support of Gradle
is improved to the point where _Module Source Hierarchy_ become well supported.


## Prerequisites
[Apache Ivy](https://ant.apache.org/ivy/) must be downloaded.
Linux users can do that with their package manager,
for example by executing `dnf install apache-ivy` on the command line.
The path to `ivy.jar` must be specified to NetBeans Ant configuration.
The menu can be found there (NetBeans 18):

* Tools > Options > Java > Ant > Classpath

If Ivy was installed by a Linux package manager,
the path to declare may be `/usr/share/java/ivy.jar`.

[JavaFX](https://openjfx.io/) must be downloaded
(that GPL dependency is optional when using the Gradle build,
but this directory is a non-official workaround for developing SIS in NetBeans).
The `nbproject/private/private.properties` file must be edited with the addition
of a `javafx.module.path` property like below
(replace `${PATH_TO_FX}/lib` by the actual path to the directory containing the JAR files):

```
javafx.module.path=${PATH_TO_FX}/lib
```


### GeoAPI 3.1/4.0 development branches
If developing on the `geoapi-3.1` or `geoapi-4.0` branch instead of `main`,
then `gradle compileJava` should be executed on the command-line before to
open the NetBeans project, in order to install the right GeoAPI development
snapshot. This step is not necessary if developing on the `main` branch.


## Known limitations
We did not found yet how to get the tests running.
For running some tests in NetBeans, create Java files
in the `src-local/org.apache.sis.test.uncommitted` module
with a `main` method invoking the actual method to test.
This is not convenient, but we hope that it will become simpler in the future
if Gradle and NetBeans support of _Module Source Hierarchy_ is gradually improved.


## Note for maintainers
NetBeans upgrades causes the `nbproject/build-impl.xml` file to be overwritten.
When it happens, the new file should be manually edited as below:

* In the `<javac …/>` task, replace the `source` attribute by `release` and remove the `target` attribute.
