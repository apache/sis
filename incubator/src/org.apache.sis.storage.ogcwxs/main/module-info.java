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
 * Data stores for the <abbr>OGC</abbr> web services of the W*S family
 * (<abbr>WMS</abbr>, <abbr>WMTS</abbr>, <abbr>WFS</abbr>, <abbr>WCS</abbr>, …).
 *
 * @author Johann Sorel (Geomatys)
 */
module org.apache.sis.storage.ogcwxs {
    // Dependencies used in public API.
    requires transitive org.apache.sis.storage;
    requires transitive java.net.http;

    // Dependencies internal to the implementation.
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.openapitools.jackson.nullable;

    // Uncomment when the package contains at least one class.
    // exports org.apache.sis.storage.ogcwxs;
    exports org.apache.sis.storage.web.service;
    exports org.apache.sis.storage.web.openapi;
}
