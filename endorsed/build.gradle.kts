/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
group = "org.apache.sis"
// The version is specified in `gradle.properties`.

/*
 * This project uses a custom Gradle plugin for building a project with Module Source Hierarchy as specified
 * in https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html#directory-hierarchies documentation.
 * The expected hierarchy is:
 *
 *   endorsed
 *     ├─ build
 *     └─ src
 *         ├─ org.apache.sis.metadata
 *         │    ├─ main
 *         │    │   ├─ module-info.java
 *         │    │   └─ org/apache/sis/metadata/…
 *         │    └─ test
 *         │        └─ org/apache/sis/metadata/…
 *         ├─ org.apache.sis.referencing
 *         │    ├─ main
 *         │    │   ├─ module-info.java
 *         │    │   └─ org/apache/sis/referencing/…
 *         │    └─ test
 *         │        └─ org/apache/sis/referencing/…
 *         └─ etc.
 */
plugins {
    `java-library`
    `maven-publish`
    signing
    id("net.linguica.maven-settings") version "0.5"
    id("org.apache.sis.buildtools")
}

/*
 * All JAR files that may potentially be used by a module.
 * For each Apache SIS module, the dependencies actually
 * used are declared in the `module-info.java` file.
 */
dependencies {
    // Mandatory dependencies
    api(libs.units)
    api(libs.geoapi)
    api(libs.jaxb.api)

    // Optional dependencies
    runtimeOnly(libs.jaxb.impl)
    compileOnly(libs.jaxb.impl)                 // For avoiding compiler warnings. Not read by SIS modules.
    compileOnly(libs.jts.core)
    compileOnly(libs.esri.geometry)
    compileOnly(libs.libreoffice)
    compileOnly(libs.ucar)
    compileOnly(libs.aws.s3)
    compileOnly(drivers.postgres)
    runtimeOnly(drivers.derby.core)
    runtimeOnly(drivers.derby.tools)

    // Test dependencies
    testImplementation(tests.junit5)
    testImplementation(tests.geoapi)
    testImplementation(tests.jama)
    testImplementation(tests.geographiclib)
    testImplementation(libs.jts.core)           // We need to repeat optional dependencies.
    testImplementation(libs.esri.geometry)      // Idem.
    testImplementation(libs.libreoffice)
    testImplementation(libs.ucar)
    testImplementation(libs.aws.s3)
    testImplementation(drivers.derby.tools)
    testImplementation(drivers.derby.core)
    testImplementation(drivers.postgres)
    testImplementation(drivers.hsql)
    testImplementation(drivers.h2)

    // For test execution
    testRuntimeOnly(tests.jupiter)
    testRuntimeOnly(tests.slf4j)
}

/*
 * Compile main and tests classes from Module Source Hierarchy.
 * The test classes require some additional dependencies declared in the `org.apache.sis.test.endorsed` module.
 */
var srcDir = file("src")            // Must be the same as the hard-coded value in `BuildHelper.java`.
tasks.compileJava {
    dependsOn(":geoapi:rebuild")
    options.release.set(11)         // The version of both Java source code and compiled byte code.
}
tasks.compileTestJava {
    options.compilerArgs.add("-source")         // "source", not "release", because we accept any target version.
    options.compilerArgs.add("16")              // Minimal Java version required by some API that the tests use.
    srcDir.list().forEach {
        addRead(options.compilerArgs, it, "org.apache.sis.test.endorsed,org.junit.jupiter.api")
    }
    addExportForTests(options.compilerArgs)
}

/*
 * Adds a JVM argument for adding dependencies to a module.
 * This is for dependencies not declared in `module-info`
 * but needed for test compilation or test execution.
 */
fun addRead(args : MutableList<String>, module : String, dependencies : String) {
    args.add("--add-reads")
    args.add(module + '=' + dependencies)
}

/*
 * Adds a JVM argument for making an internal package accessible to another module.
 * This is for making internal packages accessible to JUnit or to some test classes
 * defined in other modules.
 */
fun addExport(args : MutableList<String>, module : String, pkg : String, consumers : String) {
    args.add("--add-exports")
    args.add(module + '/' + pkg + '=' + consumers)
}

/*
 * Add compiler and runtime options for patching the Apache SIS main modules with the test classes.
 * The same options are required for both compiling and executing the tests.
 */
fun addExportForTests(args : MutableList<String>) {
    addRead(args, "org.apache.sis.metadata",    "org.apache.derby.tools,com.h2database,org.hsqldb")
    addRead(args, "org.apache.sis.referencing", "jama,GeographicLib.Java")
    addRead(args, "org.apache.sis.storage",     "esri.geometry.api")
    addRead(args, "org.apache.sis.storage.xml", "esri.geometry.api")

    var allModules = srcDir.list().joinToString(separator=",") + ",ALL-UNNAMED"

    // ――――――――――――― Module name ――――――――――――――――――――――― Package to export ―――――――――――――――
    addExport(args, "org.apache.sis.util",              "org.apache.sis.test",
                     allModules)

    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.test.mock",
                    "org.apache.sis.referencing")

    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.xml.test",
                    "org.apache.sis.referencing," +
                    "org.apache.sis.storage.xml," +
                    "org.apache.sis.profile.france")

    addExport(args, "org.apache.sis.storage",           "org.apache.sis.storage.test",
                    "org.apache.sis.storage.geotiff," +
                    "org.apache.sis.storage.netcdf")
    /*
     * Some test classes need access to more internal packages than requested by the main classes.
     * The following lines may need to be edited when export statements are added or removed in a
     * module-info.java file of main source code, or when a test class starts using or stop using
     * an internal API.
     */
    // ――――――――――――― Module name ――――――――――――――――――――――― Package to export ―――――――――――――――
    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.metadata.internal.shared",
                    "org.apache.sis.referencing.gazetteer")

    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.metadata.xml",
                    "org.apache.sis.storage," +
                    "org.apache.sis.console")

    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.xml.internal.shared",
                    "org.apache.sis.storage.geotiff")

    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.xml.bind.gcx",
                    "org.apache.sis.referencing")

    addExport(args, "org.apache.sis.referencing",       "org.apache.sis.referencing.internal",
                    "org.apache.sis.openoffice")

    addExport(args, "org.apache.sis.feature",           "org.apache.sis.feature.internal.shared",
                    "org.apache.sis.storage.sql")

    addExport(args, "org.apache.sis.feature",           "org.apache.sis.geometry.wrapper.jts",
                    "org.apache.sis.storage.sql," +
                    "org.apache.sis.portrayal")
}

/*
 * Discover and execute JUnit-based tests.
 */
tasks.test {
    val args = mutableListOf("-enableassertions")
    addExportForTests(args)
    /*
     * JAXB implementation needs permissions to do reflection.
     */
    args.add("--add-opens")
    args.add("org.apache.sis.metadata/org.apache.sis.test.mock=jakarta.xml.bind")
    setAllJvmArgs(args)
    /*
     * Request full stack trace in case of failure. The short stack trace is not enough
     * for identifying the failure cause.
     */
    testLogging {
        events("FAILED", "STANDARD_OUT", "STANDARD_ERROR")
        setExceptionFormat("FULL")
    }
    systemProperty("java.awt.headless", "true")
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
}

/*
 * Javadoc generation. Most of the configuration is specified in the `../buildSrc` plugin.
 * This file contains only the subproject-dependent parts.
 */
tasks.javadoc {
    setTitle("Apache SIS ${version} API")
}

/*
 * Configuration of some META-INF/MANIFEST.MF attributes.
 * Other attributes are hard-coded in `../buildSrc`.
 */
tasks.jar {
    manifest {
        attributes["Main-Class"]            = "org.apache.sis.console.Command"
        attributes["RegistrationClassName"] = "org.apache.sis.openoffice.Registration"
    }
}

/*
 * Configuration of the modules to deploy as Maven artifacts.
 */
publishing {
    publications {
        create<MavenPublication>("util") {
            var module = "org.apache.sis.util"
            groupId    = "org.apache.sis.core"
            artifactId = "sis-utility"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS utilities"
                description = "Units of measurement and miscellaneous utility methods required by Apache SIS."
            }
        }
        create<MavenPublication>("metadata") {
            var module = "org.apache.sis.metadata"
            groupId    = "org.apache.sis.core"
            artifactId = "sis-metadata"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS metadata"
                description = "Implementations of metadata derived from ISO 19115. " +
                              "This module provides both an implementation of the metadata interfaces defined in GeoAPI, " +
                              "and a framework for handling those metadata through Java reflection."
            }
        }
        create<MavenPublication>("referencing") {
            var module = "org.apache.sis.referencing"
            groupId    = "org.apache.sis.core"
            artifactId = "sis-referencing"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS referencing"
                description = "Implementations of Coordinate Reference Systems (CRS), " +
                              "conversion and transformation services derived from ISO 19111."
            }
        }
        create<MavenPublication>("referencing.gazetteer") {
            var module = "org.apache.sis.referencing.gazetteer"
            groupId    = "org.apache.sis.core"
            artifactId = "sis-referencing-by-identifiers"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS referencing by geographic identifiers"
                description = "Implementations of Spatial Reference Systems using Geographic Identifiers " +
                              "gazetteer services derived from ISO 19112."
            }
        }
        create<MavenPublication>("feature") {
            var module = "org.apache.sis.feature"
            groupId    = "org.apache.sis.core"
            artifactId = "sis-feature"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS features"
                description = "Representations of geographic features. " +
                              "Includes access to both vector and raster data."
            }
        }
        create<MavenPublication>("portrayal") {
            var module = "org.apache.sis.portrayal"
            groupId    = "org.apache.sis.core"
            artifactId = "sis-portrayal"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS portrayal"
                description = "Symbology and map representations, together with a rendering engine for display."
            }
        }
        create<MavenPublication>("storage") {
            var module = "org.apache.sis.storage"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-storage"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS common storage"
                description = "Provides the interfaces and base classes to be implemented by various storage formats."
            }
        }
        create<MavenPublication>("storage.xml") {
            var module = "org.apache.sis.storage.xml"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-xmlstore"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS XML storage"
                description = "Read and write files in the GPX format."
            }
        }
        create<MavenPublication>("storage.netcdf") {
            var module = "org.apache.sis.storage.netcdf"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-netcdf"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS netCDF storage"
                description = "Bridge between netCDF Climate and Forecast (CF) convention and ISO 19115 metadata."
            }
        }
        create<MavenPublication>("storage.geotiff") {
            var module = "org.apache.sis.storage.geotiff"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-geotiff"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS GeoTIFF storage"
                description = "Cloud Optimized GeoTIFF reader and bridge to ISO 19115 metadata."
            }
        }
        create<MavenPublication>("storage.earthobservation") {
            var module = "org.apache.sis.storage.earthobservation"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-earth-observation"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS Earth Observation storage"
                description = "Read a directory of Landsat files as a single resource."
            }
        }
        create<MavenPublication>("storage.sql") {
            var module = "org.apache.sis.storage.sql"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-sqlstore"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS SQL storage"
                description = "Read and write features from SQL databases."
            }
        }
        create<MavenPublication>("cloud.aws") {
            var module = "org.apache.sis.cloud.aws"
            groupId    = "org.apache.sis.cloud"
            artifactId = "sis-cloud-aws"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS storage from Amazon AWS S3"
                description = "Provides access to Amazon AWS S3 storage from Apache SIS data stores."
            }
        }
        create<MavenPublication>("profile.france") {
            var module = "org.apache.sis.profile.france"
            groupId    = "org.apache.sis.profiles"
            artifactId = "sis-french-profile"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS French profiles"
                description = "Extensions to ISO-19115 metadata mandated by the French government."
            }
        }
        create<MavenPublication>("profile.japan") {
            var module = "org.apache.sis.profile.japan"
            groupId    = "org.apache.sis.profiles"
            artifactId = "sis-japan-profile"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS Japanese profiles"
                description = "Extensions to netCDF reader for file formats published by Japanese Aerospace Exploration Agency (JAXA)."
            }
        }
        create<MavenPublication>("console") {
            var module = "org.apache.sis.console"
            groupId    = "org.apache.sis.application"
            artifactId = "sis-console"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS console"
                description = "Console application."
            }
        }
        create<MavenPublication>("openoffice") {
            var module = "org.apache.sis.openoffice"
            groupId    = "org.apache.sis.application"
            artifactId = "sis-openoffice"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Bridges to Apache OpenOffice or LibreOffice"
                description = "Provides some Apache SIS functionalities as Apache OpenOffice addins. " +
                              "For example, addins provide coordinate operation services as formulas " +
                              "inside the Calc spreadsheet."
            }
        }
    }
    /* Following block is currently repeated in all sub-projects. */
    repositories {
        maven {
            val stage = if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"
            name = providers.gradleProperty("${stage}Id").get()
            url = uri(providers.gradleProperty("${stage}URL").get())
        }
    }
}

signing {
    useGpgCmd()
    if (System.getProperty("org.apache.sis.releaseVersion") != null) {
        sign(publishing.publications["util"])
        sign(publishing.publications["metadata"])
        sign(publishing.publications["referencing"])
        sign(publishing.publications["referencing.gazetteer"])
        sign(publishing.publications["feature"])
        sign(publishing.publications["portrayal"])
        sign(publishing.publications["storage"])
        sign(publishing.publications["storage.sql"])
        sign(publishing.publications["storage.xml"])
        sign(publishing.publications["storage.netcdf"])
        sign(publishing.publications["storage.geotiff"])
        sign(publishing.publications["storage.earthobservation"])
        sign(publishing.publications["cloud.aws"])
        sign(publishing.publications["profile.france"])
        sign(publishing.publications["profile.japan"])
        sign(publishing.publications["console"])
        sign(publishing.publications["openoffice"])
    }
}
