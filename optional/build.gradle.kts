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
version = "1.4-SNAPSHOT"

val pathToFX = System.getenv("PATH_TO_FX")
if (pathToFX == null) {
    throw GradleException("For compiling the Apache SIS optional sub-project, the PATH_TO_FX environment variable must be set.")
}
if (!File(pathToFX, "javafx.base.jar").isFile()) {
    throw GradleException("The directory specified by the PATH_TO_FX environment variable shall contain \"javafx.*.jar\" files.")
}

/*
 * "org.apache.sis.buildtools" is a custom Gradle plugin for building a project with Module Source Hierarchy.
 * See the Gradle build script in the `endorsed` directory for more information.
 */
plugins {
    `java-library`
    `maven-publish`
    signing
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
    runtimeOnly   (drivers.derby.core)
    runtimeOnly   (drivers.derby.tools)

    // Test dependencies
    testImplementation(tests.geoapi)
    testImplementation(tests.junit5)
    testImplementation(tests.junit4)
    testRuntimeOnly   (tests.junit)
    testRuntimeOnly   (tests.junitLauncher)
}

/*
 * Compile main and tests classes from Module Source Hierarchy.
 * The test classes require some additional dependencies declared in the `org.apache.sis.test.optional` module.
 */
var srcDir = file("src")            // Must be the same as the hard-coded value in `BuildHelper.java`.
tasks.compileJava {
    dependsOn(":endorsed:compileJava")
    options.release.set(16)         // The version of both Java source code and compiled byte code.
}
tasks.compileTestJava {
    patchForTests(options.compilerArgs);
    srcDir.list().forEach {
        addRead(options.compilerArgs, it, "org.apache.sis.test.optional,org.junit.jupiter.api,junit")
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
    patchModuleWithTests(args, "org.apache.sis.feature")
    addExport(args, "org.apache.sis.util", "org.apache.sis.test", "org.apache.sis.gui")
}

/*
 * Discover and execute JUnit-based tests.
 */
tasks.test {
    val args = mutableListOf("-enableassertions")
    patchForTests(args);
    addRead  (args, "org.apache.sis.util",                                "ALL-UNNAMED")
    addExport(args, "org.apache.sis.util", "org.apache.sis.test",         "ALL-UNNAMED")
    addExport(args, "org.apache.sis.gui",  "org.apache.sis.gui.internal", "ALL-UNNAMED")
    setAllJvmArgs(args)
    testLogging {
        events("FAILED", "STANDARD_OUT", "STANDARD_ERROR")
        setExceptionFormat("FULL")
    }
}

/*
 * Configuration of some META-INF/MANIFEST.MF attributes.
 * Other attributes are hard-coded in `../buildSrc`.
 */
tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.apache.sis.gui.DataViewer"
    }
}

/*
 * Configuration of the modules to deploy as Maven artifacts.
 */
publishing {
    publications {
        create<MavenPublication>("gui") {
            var module = "org.apache.sis.gui"
            groupId    = "org.apache.sis.application"
            artifactId = "sis-javafx"
            artifact(layout.buildDirectory.file("libs/${module}.jar"))
            artifact(layout.buildDirectory.file("docs/${module}-sources.jar")) {classifier = "sources"}
            artifact(layout.buildDirectory.file("docs/${module}-javadoc.jar")) {classifier = "javadoc"}
            pom {
                name        = "Apache SIS application for JavaFX (optional)"
                description = "Client application for JavaFX. " +
                              "This module requires the JavaFX environment to be pre-installed. " +
                              "See https://openjfx.io/openjfx-docs/#install-javafx for details."
            }
        }
    }
}

signing {
    useGpgCmd()
    if (System.getProperty("org.apache.sis.releaseVersion") != null) {
        sign(publishing.publications["gui"])
    }
}
