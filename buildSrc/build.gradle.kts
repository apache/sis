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
plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("buildTools") {
            id = "org.apache.sis.buildtools"
            implementationClass = "org.apache.sis.buildtools.gradle.BuildHelper"
        }
    }
}

/*
 * All dependencies used by the plugin.
 */
repositories {
    mavenCentral()
}
dependencies {
    implementation (group = "org.apache.commons", name = "commons-compress",        version = "1.23.0")
    testCompileOnly(group = "junit",              name = "junit",                   version = "4.13.2")
    testRuntimeOnly(group = "org.junit.vintage",  name = "junit-vintage-engine",    version = "5.9.3")
    testRuntimeOnly(group = "org.junit.platform", name = "junit-platform-launcher", version = "1.9.3")
}

/*
 * Compile main classes from Package Hierarchy.
 */
tasks.compileJava {
    options.encoding = "UTF-8"      // The character encoding to be used when reading source files.
    options.setDeprecation(true)    // Whether to log details of usage of deprecated members or classes.
}
tasks.compileTestJava {
    options.encoding = "UTF-8"
    options.setDeprecation(true)
}

/*
 * Discover and execute JUnit-based tests.
 */
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "SKIPPED", "FAILED", "STANDARD_OUT", "STANDARD_ERROR")
        setExceptionFormat("FULL")
    }
}
