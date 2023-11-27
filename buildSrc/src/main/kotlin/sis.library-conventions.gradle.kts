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
version = "2.0-SNAPSHOT"

/*
 * "org.apache.sis.buildtools" is a custom Gradle plugin for building a project with Module Source Hierarchy
 * as specified in https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html#directory-hierarchies
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
}

/*
 * Configuration of the repositories where to deploy artifacts.
 */
publishing {
    repositories {
        maven {
            name = "Apache"
            url = uri(if (version.toString().endsWith("SNAPSHOT"))
                      "https://repository.apache.org/content/repositories/snapshots" else
                      "https://repository.apache.org/service/local/staging/deploy/maven2")
            credentials {
                val asfNexusUsername = providers.gradleProperty("asfNexusUsername")
                val asfNexusPassword = providers.gradleProperty("asfNexusPassword")
                if (asfNexusUsername.isPresent() && asfNexusPassword.isPresent()) {
                    username = asfNexusUsername.get()
                    password = asfNexusPassword.get()
                }
            }
        }
    }
}
