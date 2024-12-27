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
rootProject.name  = "Apache SIS on GeoAPI 3.0"
val geoapiVersion = "3.0.2"

/*
 * The sub-projects to include in the build.
 * They are directory names relative to this file.
 */
include("parent")
include("endorsed")
include("incubator")
if (System.getenv("PATH_TO_FX") != null) {
    include("optional")
}

/*
 * Central place where to declare the repositories inherited by all sub-projects.
 * Sub-projects can override by declaring their own `repositories` block.
 */
dependencyResolutionManagement {
    /*
     * The repositories from where to download the JAR files declared in the `dependencies` section.
     * The Maven local repository should be avoided for security reasons (Gradle has its own cache).
     */
    repositories {
        mavenCentral()
        maven {
            name = "UCAR"
            url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases")
            content {
                includeGroup("edu.ucar")        // Restrict usage to those dependencies.
            }
            mavenContent {
                releasesOnly()
            }
        }
    }
    /*
     * All dependencies used by sub-projects, together with their versions.
     * For most dependencies (except tests), we assume semantic versioning.
     * The preferred versions are the ones that we tested.
     */
    versionCatalogs {
        create("libs") {
            library("geoapi",        "org.opengis",            "geoapi")              .version {strictly(geoapiVersion)}
            library("units",         "javax.measure",          "unit-api")            .version {strictly("[2.1, 3.0[");  prefer("2.1.3")}
            library("jaxb.api",      "jakarta.xml.bind",       "jakarta.xml.bind-api").version {strictly("[4.0, 5.0[");  prefer("4.0.2")}
            library("jaxb.impl",     "org.glassfish.jaxb",     "jaxb-runtime")        .version {strictly("[4.0, 5.0[");  prefer("4.0.5")}
            library("yasson",        "org.eclipse",            "yasson")              .version {strictly("[3.0, 4.0[");  prefer("3.0.3")}
            library("jts.core",      "org.locationtech.jts",   "jts-core")            .version {strictly("[1.15, 2.0["); prefer("1.19.0")}
            library("esri.geometry", "com.esri.geometry",      "esri-geometry-api")   .version {strictly("[2.0, 3.0[");  prefer("2.2.4")}
            library("libreoffice",   "org.libreoffice",        "libreoffice")         .version {strictly("[7.0, 8.0[");  prefer("7.6.7")}
            library("ucar",          "edu.ucar",               "cdm-core")            .version {strictly("[5.0, 6.0[");  prefer("5.5.3")}
            library("aws.s3",        "software.amazon.awssdk", "s3")                  .version {strictly("[2.0, 3.0[");  prefer("2.29.37")}
        }
        create("tests") {
            library("geoapi",        "org.opengis",            "geoapi-conformance")     .version {strictly(geoapiVersion)}
            library("junit5",        "org.junit.jupiter",      "junit-jupiter-api")      .version {strictly("5.10.3")}
            library("jupiter",       "org.junit.jupiter",      "junit-jupiter-engine")   .version {strictly("5.10.3")}
            library("jama",          "gov.nist.math",          "jama")                   .version {strictly("1.0.3")}
            library("geographiclib", "net.sf.geographiclib",   "GeographicLib-Java")     .version {strictly("2.0")}
            library("slf4j",         "org.slf4j",              "slf4j-jdk14").version {
                prefer("1.7.28")            // Should match the version used by UCAR.
            }
        }
        create("drivers") {
            version("derby") {
                strictly("[10.0, 11.0[")
                prefer("10.15.2.0")         // 10.15 is the last series compatible with JDK 11.
                // If the derby version is updated, search for that version number in Javadoc.
            }
            // Derby vulnerabiliy: https://nvd.nist.gov/vuln/detail/CVE-2022-46337
            // Fix would require an upgrade to Java 21.
            library("derby.core",    "org.apache.derby",       "derby")      .versionRef("derby")
            library("derby.tools",   "org.apache.derby",       "derbytools") .versionRef("derby")
            library("postgres",      "org.postgresql",         "postgresql") .version {prefer("42.7.3")}
            library("hsql",          "org.hsqldb",             "hsqldb")     .version {strictly("[2.0, 3.0["); prefer("2.7.3")}
            library("h2",            "com.h2database",         "h2")         .version {strictly("[2.0, 3.0["); prefer("2.3.230")}
        }
    }
}
