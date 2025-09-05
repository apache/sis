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
import java.nio.file.Files

group = "org.apache.sis"
// The version is specified in `gradle.properties`.

val pathToFX = System.getenv("PATH_TO_FX")
if (pathToFX == null) {
    throw GradleException("For compiling the Apache SIS optional sub-project, the PATH_TO_FX environment variable must be set.")
}
if (!File(pathToFX, "javafx.base.jar").isFile()) {
    throw GradleException("The directory specified by the PATH_TO_FX environment variable shall contain \"javafx.*.jar\" files.")
}

/*
 * This project uses a custom Gradle plugin for building a project with Module Source Hierarchy.
 * See the Gradle build script in the `endorsed` directory for more information.
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
var mainDepPath = file("../endorsed/build/classes/java/main")
var testDepPath = file("../endorsed/build/classes/java/test")
dependencies {
    // Mandatory dependencies
    api           (libs.units)
    api           (libs.geoapi)
    implementation(libs.jaxb.api)                  // Transitive dependency.
    runtimeOnly   (libs.jaxb.impl)
    api           (files(File(pathToFX, "javafx.base.jar")))
    api           (files(File(pathToFX, "javafx.graphics.jar")))
    api           (files(File(pathToFX, "javafx.controls.jar")))
    api           (files(File(pathToFX, "javafx.web.jar")))
    runtimeOnly   (files(File(pathToFX, "javafx.media.jar")))
    api           (files("${mainDepPath}/org.apache.sis.util"))
    api           (files("${mainDepPath}/org.apache.sis.metadata"))
    api           (files("${mainDepPath}/org.apache.sis.referencing"))
    implementation(files("${mainDepPath}/org.apache.sis.referencing.gazetteer"))
    api           (files("${mainDepPath}/org.apache.sis.feature"))
    api           (files("${mainDepPath}/org.apache.sis.storage"))
    implementation(files("${mainDepPath}/org.apache.sis.storage.xml"))
    runtimeOnly   (files("${mainDepPath}/org.apache.sis.storage.netcdf"))
    runtimeOnly   (files("${mainDepPath}/org.apache.sis.storage.geotiff"))
    runtimeOnly   (files("${mainDepPath}/org.apache.sis.storage.earthobservation"))
    api           (files("${mainDepPath}/org.apache.sis.portrayal"))
    api           (drivers.derby.core)
    api           (drivers.derby.tools)

    // Test dependencies
    testImplementation(drivers.postgres)
    testImplementation(tests.junit5)
    testRuntimeOnly   (tests.jupiter)
    testRuntimeOnly   (libs.jts.core)
    testRuntimeOnly   (libs.esri.geometry)
}

/*
 * Compile main and tests classes from Module Source Hierarchy.
 * The test classes require some additional dependencies declared in the `org.apache.sis.test.optional` module.
 */
var srcDir = file("src")            // Must be the same as the hard-coded value in `BuildHelper.java`.
tasks.compileJava {
    dependsOn(":endorsed:compileJava")
    options.release.set(22)         // The version of both Java source code and compiled byte code.
}
tasks.compileTestJava {
    options.compilerArgs.add("-source")         // "source", not "release", because we accept any target version.
    options.compilerArgs.add("22")
    patchForTests(options.compilerArgs);
    srcDir.list().forEach {
        addRead(options.compilerArgs, it, "org.apache.sis.test.optional,org.junit.jupiter.api")
    }
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
 * This is for making internal packages accessible to JUnit or to some test classes.
 */
fun addExport(args : MutableList<String>, module : String, pkg : String, consumers : String) {
    args.add("--add-exports")
    args.add(module + '/' + pkg + '=' + consumers)
}

/*
 * Adds a JVM argument for patching a module with test classes.
 */
fun patchModuleWithTests(args : MutableList<String>, module : String) {
    args.add("--patch-module")
    args.add("${module}=${testDepPath}/${module}")
}

/*
 * Add compiler and runtime options for patching the Apache SIS main modules with the test classes.
 * The same options are required for both compiling and executing the tests.
 */
fun patchForTests(args : MutableList<String>) {
    patchModuleWithTests(args, "org.apache.sis.util")
    patchModuleWithTests(args, "org.apache.sis.metadata")
    patchModuleWithTests(args, "org.apache.sis.feature")

    addRead(args, "org.apache.sis.referencing.database", "org.apache.sis.referencing.epsg");
    addRead(args, "org.apache.sis.referencing.epsg",     "org.postgresql.jdbc");

    // ――――――――――――― Module name ――――――――――――――――――――――― Package to export ―――――――――――――――
    addExport(args, "org.apache.sis.util",              "org.apache.sis.test",
                    "org.apache.sis.gui," +
                    "org.apache.sis.referencing.epsg," +
                    "org.apache.sis.referencing.database")
    addExport(args, "org.apache.sis.metadata",          "org.apache.sis.metadata.sql.privy",
                    "org.apache.sis.referencing.epsg")
}

/*
 * Download the FontGIS glyphs if not already present — https://viglino.github.io/font-gis/
 * The license is OFL-1.1 (SIL Open Font License), classified by ASF as Category B:
 * the file may be included in binary-only form in convenience binaries,
 * but shall not be included in source releases.
 */
fun downloadFontGIS() {
    val targetFile = File(file("build"), "classes/java/main/org.apache.sis.gui/org/apache/sis/gui/internal/font-gis.ttf")
    if (!targetFile.exists()) {
        val archiveFolder = File(file("cache"), "fontgis")
        val archiveFile   = File(archiveFolder, "fontgis.tgz")
        val archiveEntry  = File(archiveFolder, "package/fonts/font-gis.ttf")
        val archiveURL    = "https://registry.npmjs.org/font-gis/-/font-gis-1.0.5.tgz"
        if (!archiveEntry.exists()) {
            if (!archiveFile.exists()) {
                archiveFolder.mkdirs()
                println("Downloading " + archiveURL)
                ant.invokeMethod("get", mapOf("src" to archiveURL, "dest" to archiveFile))
            }
            ant.invokeMethod("untar", mapOf("src"  to archiveFile, "dest" to archiveFolder, "compression" to "gzip"))
        }
        targetFile.getParentFile().mkdirs()
        Files.createLink(targetFile.toPath(), archiveEntry.toPath())
    }
}

/*
 * Adds symbolic links to EPSG license if those optional data are present.
 */
fun addLicenseEPSG() {
    var buildDir = file("build")
    if (buildDir.exists()) {
        var targetFile = File(buildDir, "classes/java/main/org.apache.sis.referencing.epsg/META-INF/LICENSE")
        if (!targetFile.exists()) {
            val sourceFile = File(file("src"), "org.apache.sis.referencing.epsg/main/org/apache/sis/referencing/factory/sql/epsg/LICENSE.txt")
            if (sourceFile.exists()) {
                var realPath = sourceFile.toPath().toRealPath()
                Files.createLink(targetFile.toPath(), realPath)
                targetFile = File(file("build"), "classes/java/main/org.apache.sis.referencing.database/META-INF/LICENSE")
                Files.createLink(targetFile.toPath(), realPath)
            }
        }
    }
}

/*
 * Discover and execute JUnit-based tests.
 */
tasks.test {
    val args = mutableListOf("-enableassertions")
    args.add("--enable-native-access")
    args.add("org.apache.sis.storage.gdal")
    patchForTests(args);
    addRead  (args, "org.apache.sis.util",                                "ALL-UNNAMED")
    addExport(args, "org.apache.sis.util", "org.apache.sis.test",         "ALL-UNNAMED")
    addExport(args, "org.apache.sis.gui",  "org.apache.sis.gui.internal", "ALL-UNNAMED")
    args.add("--add-opens")
    args.add("org.apache.sis.metadata/org.apache.sis.metadata.sql=org.apache.sis.referencing.database")
    setAllJvmArgs(args)
    testLogging {
        events("FAILED", "STANDARD_OUT", "STANDARD_ERROR")
        setExceptionFormat("FULL")
    }
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
}

/*
 * Configuration of some META-INF/MANIFEST.MF attributes.
 * Other attributes are hard-coded in `../buildSrc`.
 */
tasks.jar {
    addLicenseEPSG();
    downloadFontGIS();
    manifest {
        attributes["Main-Class"] = "org.apache.sis.gui.DataViewer"
    }
}

/*
 * Configuration of the modules to deploy as Maven artifacts.
 */
publishing {
    publications {
        create<MavenPublication>("epsg") {
            var module = "org.apache.sis.referencing.epsg"
            groupId    = "org.apache.sis.non-free"
            artifactId = "sis-epsg"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "EPSG dataset for Apache SIS"
                description = "The EPSG geodetic dataset provides definitions for thousands of Coordinate Reference Systems (CRS), " +
                              "together with parameter values for thousands of Coordinate Operations between various pairs of CRS. " +
                              "This module contains the SQL scripts for creating a local copy of EPSG geodetic dataset. " +
                              "EPSG is maintained by the IOGP Surveying &amp; Positioning Committee and reproduced in this module " +
                              "with same content. See https://epsg.org/ for more information."
                licenses {
                    license {
                        // Not included in source code, user must download explicitly.
                        // name = "EPSG terms of use"
                        url = "https://epsg.org/terms-of-use.html"
                        distribution = "manual"
                    }
                    license {
                        // name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
            }
        }
        create<MavenPublication>("database") {
            var module = "org.apache.sis.referencing.database"
            groupId    = "org.apache.sis.non-free"
            artifactId = "sis-embedded-data"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Data in embedded environment"
                description = "Provides non-free data, including the EPSG geodetic dataset, in a single read-only JAR file. " +
                              "This module contains a copy of EPSG geodetic dataset in an embedded Apache Derby database. " +
                              "Having this artifact on the module path avoid the need to set the 'SIS_DATA' environment variable " +
                              "for using the Coordinate Reference Systems (CRS) and Coordinate Operations defined by EPSG. " +
                              "EPSG is maintained by the IOGP Surveying &amp; Positioning Committee and reproduced in this module " +
                              "with same content. See https://epsg.org/ for more information."
                licenses {
                    license {
                        // Not included in source code, user must download explicitly.
                        // name = "EPSG terms of use"
                        url = "https://epsg.org/terms-of-use.html"
                        distribution = "manual"
                    }
                    license {
                        // name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
            }
        }
        create<MavenPublication>("gui") {
            var module = "org.apache.sis.gui"
            groupId    = "org.apache.sis.application"
            artifactId = "sis-javafx"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS application for JavaFX"
                description = "Client application for JavaFX. " +
                              "This module requires the JavaFX environment to be pre-installed. " +
                              "See https://openjfx.io/openjfx-docs/#install-javafx for details."
            }
        }
        create<MavenPublication>("storage.gdal") {
            var module = "org.apache.sis.storage.gdal"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-gdal"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS storage using GDAL through Panama"
                description = "Read and write files using the GDAL library. " +
                              "This module assumes that GDAL is pre-installed."
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
        sign(publishing.publications["storage.gdal"])
        sign(publishing.publications["database"])
        sign(publishing.publications["epsg"])
        sign(publishing.publications["gui"])
    }
}
