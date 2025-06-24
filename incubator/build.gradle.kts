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
 * This project uses a custom Gradle plugin for building a project with Module Source Hierarchy.
 * See the Gradle build script in the `endorsed` directory for more information.
 */
plugins {
    antlr
    `java-library`
    `maven-publish`     // For local deployment only. Not to be published on Maven Central.
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
    api(libs.jaxb.api)                  // Transitive dependency.
    api(libs.jts.core)                  // Should be an optional dependency.
    api(libs.esri.geometry)             // Idem.
    api(libs.yasson)
    api(files("../endorsed/build/classes/java/main/org.apache.sis.util"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.metadata"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.referencing"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.feature"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.storage"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.storage.sql"))

    // Test dependencies
    testImplementation(tests.junit5)
    testRuntimeOnly   (tests.jupiter)
    /*
     * Dependencies used only by incubated modules. The dependencies are not declared
     * in the global `settings.gradle.kts` because incubated modules are not released.
     */
    implementation(group = "org.antlr",       name = "antlr4-maven-plugin", version = "4.13.2")
    implementation(group = "org.xerial",      name = "sqlite-jdbc",         version = "3.45.1.0")
    compileOnly   (group = "jakarta.servlet", name = "jakarta.servlet-api", version = "6.1.0")
    compileOnly   (group = "org.osgi",        name = "osgi.core",           version = "8.0.0")
    antlr         (group = "org.antlr",       name = "antlr4",              version = "4.13.2")
}

/*
 * Generate ANTLR4 source files, then compile main and tests classes from Module Source Hierarchy.
 * The test classes require some additional dependencies declared in the `org.apache.sis.test.incubator` module.
 *
 * Note: as of Gradle 8.2, the ANTLR task does not work well with arbitrary source directories.
 * We have to keep default convention even if it does not fit well in Module Source Hierarchy.
 * Another problem is that we didn't found the right compiler options for combining a directory
 * of generated sources with the main sources in a Module Source Hierarchy, so we have to write
 * the output directly in main source directory. This is hopefully temporary, since we hope to
 * replace ANTLR generated code by hand-written code in a future version.
 */
var srcDir = file("src")        // Must be the same as the hard-coded value in `BuildHelper.java`.
tasks.compileJava {
    dependsOn(":endorsed:compileJava")
    options.release.set(22)         // The version of both Java source code and compiled byte code.
}
tasks.compileTestJava {
    options.compilerArgs.add("-source")         // "source", not "release", because we accept any target version.
    options.compilerArgs.add("22")
    srcDir.list().forEach {
        addRead(options.compilerArgs, it, "org.apache.sis.test.incubator,org.junit.jupiter.api")
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
    /*
     * Some test classes need access to more internal packages than requested by the main classes.
     * The following lines may need to be edited when export statements are added or removed in a
     * module-info.java file of main source code, or when a test class starts using or stop using
     * an internal API.
     */
    // ――――――――――――― Module name ――――――――――――――――――――――― Package to export ―――――――――――――――
    addExport(args, "org.apache.sis.feature",           "org.apache.sis.geometry.wrapper",
                    "org.apache.sis.storage.geopackage")
}

/*
 * Discover and execute JUnit-based tests.
 */
tasks.test {
    val args = mutableListOf("-enableassertions")
    args.add("--enable-native-access")
    args.add("org.apache.sis.storage.gsf")
    addExportForTests(args)
    setAllJvmArgs(args)
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
 * Configuration of the modules to deploy as Maven artifacts.
 */
publishing {
    publications {
        create<MavenPublication>("storage.shapefile") {
            var module = "org.apache.sis.storage.shapefile"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-shapefile"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS Shapefile storage"
                description = "Read and write files in the Shapefile format."
            }
        }
        create<MavenPublication>("storage.geoheif") {
            var module = "org.apache.sis.storage.geoheif"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-geoheif"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS GeoHEIF Coverage storage"
                description = "Read files in GeoHEIF format."
            }
        }
        create<MavenPublication>("storage.geopackage") {
            var module = "org.apache.sis.storage.geopackage"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-geopackage"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS GeoPackage storage"
                description = "Read and write files in the GeoPackage format."
            }
        }
        create<MavenPublication>("storage.gsf") {
            var module = "org.apache.sis.storage.gsf"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-gsf"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS LibGSF Panama binding"
                description = "Read files in GSF format."
            }
        }
        create<MavenPublication>("storage.coveragejson") {
            var module = "org.apache.sis.storage.coveragejson"
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-coveragejson"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS JSON Coverage storage"
                description = "Read and write files in the JSON Coverage format."
            }
        }
        create<MavenPublication>("webapp") {
            var module = "org.apache.sis.webapp"
            groupId    = "org.apache.sis.application"
            artifactId = "sis-webapp"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS web services layer"
                description = "Placeholder for future developments."
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
