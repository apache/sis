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
 * GIMI store.
 *
 * @author  Johann Sorel (Geomatys)
 */
module org.apache.sis.storage.geoheif {
    // Dependencies used in public API.
    requires transitive org.apache.sis.referencing;
    requires transitive org.apache.sis.storage;

    exports org.apache.sis.storage.isobmff;

    uses org.apache.sis.storage.isobmff.BoxRegistry;

    provides org.apache.sis.storage.DataStoreProvider
            with org.apache.sis.storage.geoheif.GeoHeifStoreProvider;

    provides org.apache.sis.storage.isobmff.BoxRegistry
            with org.apache.sis.storage.isobmff.gimi.GIMI,
                 org.apache.sis.storage.isobmff.video.ISO14496_10,
                 org.apache.sis.storage.isobmff.base.ISO14496_12,
                 org.apache.sis.storage.isobmff.mpeg.ISO23001_17,
                 org.apache.sis.storage.isobmff.image.ISO23008_12;

}
