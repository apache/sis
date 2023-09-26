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

/*
 * "org.apache.sis.buildtools" is a custom Gradle plugin for building a project with Module Source Hierarchy.
 * See the Gradle build script in the `endorsed` directory for more information.
 */
plugins {
    `java-library`
    `maven-publish`     // For local deployment only. Not to be published on Maven Central.
    id("org.apache.sis.buildtools")
    antlr
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
    api(files("../endorsed/build/classes/java/main/org.apache.sis.util"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.metadata"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.referencing"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.feature"))
    api(files("../endorsed/build/classes/java/main/org.apache.sis.storage"))

    // Test dependencies
    testImplementation(tests.geoapi)
    testImplementation(tests.junit5)
    testImplementation(tests.junit4)
    testRuntimeOnly   (tests.junit)
    testRuntimeOnly   (tests.junitLauncher)
    /*
     * Dependencies used only by incubated modules. The dependencies are not declared
     * in the global `settings.gradle.kts` because incubated modules are not released.
     */
    implementation(group = "org.antlr",       name = "antlr4-maven-plugin", version = "4.11.1")
    compileOnly   (group = "jakarta.servlet", name = "jakarta.servlet-api", version = "6.0.0")
    compileOnly   (group = "org.osgi",        name = "osgi.core",           version = "8.0.0")
    antlr         (group = "org.antlr",       name = "antlr4",              version = "4.11.1")
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
    options.release.set(11)         // The version of both Java source code and compiled byte code.
}
tasks.compileTestJava {
    srcDir.list().forEach {
        addRead(options.compilerArgs, it, "org.apache.sis.test.incubator,org.junit.jupiter.api,junit")
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
 * Discover and execute JUnit-based tests.
 */
tasks.test {
    val args = mutableListOf("-enableassertions")
    setAllJvmArgs(args)
    testLogging {
        events("FAILED", "STANDARD_OUT", "STANDARD_ERROR")
        setExceptionFormat("FULL")
    }
}

/*
 * Configuration of the modules to deploy as Maven artifacts.
 */
publishing {
    publications {
        create<MavenPublication>("storage.shapefile") {
            groupId    = "org.apache.sis.storage"
            artifactId = "sis-shapefile"
            artifact(file("${buildDir}/libs/org.apache.sis.storage.shapefile.jar"))
            pom {
                name        = "Apache SIS Shapefile storage"
                description = "Read and write files in the Shapefile format."
            }
        }
        create<MavenPublication>("webapp") {
            groupId    = "org.apache.sis.application"
            artifactId = "sis-webapp"
            artifact(file("${buildDir}/libs/org.apache.sis.webapp.jar"))
            pom {
                name        = "Apache SIS web services layer"
                description = "Placeholder for future developments."
            }
        }
    }
}
