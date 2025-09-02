# Apache Spatial Information System (SIS)

Apache SIS is a Java language library for developing geospatial applications.
The library is an implementation of [OGC GeoAPI 3.0.2](http://www.geoapi.org/)
interfaces and can be used for desktop or server applications. SIS provides data
structures along with methods derived from the following international standards:

* Metadata                — ISO 19115-1, ISO 19115-2, ISO 19115-3, ISO 19157
* Referencing             — ISO 19111, ISO 19111-2, ISO 19112, OGC 01-009
* Features and filters    — ISO 19109, ISO 19143
* Units of measurement    — ISO 19103, JSR 385
* Data formats:
  * GeoTIFF               — OGC 19-008
  * NetCDF                — OGC 10-092, OGC 16-114
  * XML and GML           — ISO 19139, ISO 19136, OGC 01-009
  * WKT (Well-Known Text) — ISO 19162
  * CSV (Comma-separated values) — OGC 14-084

See the https://sis.apache.org/ web site for more information
and a more accurate list of standards.

SIS is a project of the [Apache Software Foundation](https://www.apache.org/).
Apache SIS, SIS, Apache, the Apache feather logo, and the Apache SIS
project logo are trademarks of The Apache Software Foundation.


## Build from sources

Running the library part of Apache SIS requires Java 11 or higher.
Running the JavaFX application part requires Java 22 or higher.
Building SIS requires Java 22 or higher
together with [Gradle](https://gradle.org/) 8 build system.
To build SIS, use the following command in this directory:

    gradle assemble

The JAR files will be located in the following directories,
together with their dependencies:

* `endorsed/build/libs/`  (core library)
* `optional/build/libs/`  (requires JavaFX)

If JAR files seem missing, try `gradle jar`.
For publishing to the local `~/.m2` repository
(for example, for use with Maven projects):

    gradle publishToMavenLocal


### Build the graphical application

If the [optional modules](optional/README.md) prerequisites are met,
a ZIP file containing a subset of Apache SIS modules and dependencies
is automatically built in the `optional/build/bundle/` sub-directory.
That ZIP file can be unzipped in any directory and the application is
launched by running the `./bin/sisfx` script.


## Getting Started

Information for running a [command-line tool](https://sis.apache.org/command-line.html)
or an [optional JavaFX application](https://sis.apache.org/javafx.html)
can be found on the web site. Java code examples for some common tasks
are given in the [How to…](https://sis.apache.org/howto.html) pages.
Complete API can be browsed in [online Javadoc](https://sis.apache.org/apidocs/index.html).


## License (see also LICENSE)

Collective work: Copyright 2010-2025 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Some Apache SIS subcomponents have dependencies subject to different
license terms. All those dependencies except GeoAPI, JSR-385 and JAXB API
are optional. Your use of those dependencies is subject to the terms and
conditions of the licenses listed in the NOTICE file.


## Mailing Lists

Discussion about SIS takes place on the following mailing lists:

* user@sis.apache.org    — about using SIS
* dev@sis.apache.org     — about developing SIS

Notification on all changes are sent to the following mailing lists:

* commits@sis.apache.org — about code changes
* issues@sis.apache.org  — about issue changes

The mailing lists are open to anyone and publicly archived.
You can subscribe the mailing lists by sending a message to
_LIST_-subscribe@sis.apache.org (for example user-subscribe@…).
To unsubscribe, send a message to _LIST_-unsubscribe@sis.apache.org.
For more instructions, send a message to _LIST_-help@sis.apache.org.


## Issue Tracker

If you encounter errors in SIS or want
to suggest an improvement or a new feature, please visit the
[SIS issue tracker](https://issues.apache.org/jira/browse/SIS).
There you can also find the latest information on known issues
and recent bug fixes and enhancements.
