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
 * GeoHEIF / GIMI store.
 * The <abbr>HEIF</abbr> abbreviation stands for <q>High Efficiency Image Format</q>
 * and <abbr>GIMI</abbr> stands for <dfn><abbr title="Geospatial Intelligence">GEOINT</abbr> Imagery Media for
 * <abbr title="Intelligence, Surveillance, and Reconnaissance">ISR</abbr></dfn>.
 * This format is described by the following set of standards:
 *
 * <ul>
 *   <li>ISO/IEC 14496-12 — ISO Base Media File Format (<abbr>ISOBMFF</abbr>)</li>
 *   <li>ISO/IEC 23008-12 — High Efficiency Image File Format (<abbr>HEIF</abbr>)</li>
 * </ul>
 *
 * This module supports only a subset of the data structures ("boxes") defined in above standards.
 * It focuses on the data structures needed for georeferenced images.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
module org.apache.sis.storage.geoheif {
    // Dependencies used in public API.
    requires transitive org.apache.sis.referencing;
    requires transitive org.apache.sis.storage;

    exports org.apache.sis.storage.geoheif;

    uses org.apache.sis.storage.isobmff.BoxRegistry;

    provides org.apache.sis.storage.DataStoreProvider
            with org.apache.sis.storage.geoheif.GeoHeifStoreProvider;
}
