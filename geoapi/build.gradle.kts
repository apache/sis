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
tasks.register<Exec>("rebuild") {
    setWorkingDir(file("snapshot"))
    commandLine("mvn", "clean", "install")
    /*
     * The following are used by Gradle for deciding if the GeoAPI project needs to be rebuilt.
     * We declare only the modules of interest to Apache SIS. Changes in other modules will not
     * trigger a rebuild.
     */
    inputs.dir("snapshot/geoapi/src/main")
    inputs.dir("snapshot/geoapi-pending/src/main")
    inputs.dir("snapshot/geoapi-conformance/src/main")

    outputs.file("snapshot/geoapi-pending/target/geoapi-pending-4.0-SNAPSHOT.jar")
    outputs.file("snapshot/geoapi-conformance/target/geoapi-conformance-4.0-SNAPSHOT.jar")
}
