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

/**
 * Coverage-json store.
 *
 * @todo Consider renaming packages as {@code org.apache.sis.storage.json.coverage} in order to group all JSON formats.
 *       Having the exact format name "coveragejson" in package name is redundant with {@code CoverageJsonStore} class name.
 *       Furthermore, the "Coverage-json" description appears right after module name in Javadoc. Having a single "json"
 *       root allow Javadoc to detect more easily the related modules and organize them accordingly.
 *
 * @author  Johann Sorel (Geomatys)
 */
module org.apache.sis.storage.json {
    requires static org.locationtech.jts;
    // Dependencies used in public API.
    requires transitive org.apache.sis.referencing;
    requires transitive org.apache.sis.storage;

    // Dependencies internal to the implementation.
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    provides org.apache.sis.storage.DataStoreProvider
        with org.apache.sis.storage.coveragejson.CoverageJsonStoreProvider;

    exports org.apache.sis.storage.json;
    exports org.apache.sis.storage.coveragejson;
    exports org.apache.sis.storage.coveragejson.binding;
}
