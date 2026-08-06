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
package org.apache.sis.referencing.dggs;

/**
 * Reference system establishing a specific association of zone identifiers to zones for one or more
 * discrete global grid hierarchy.
 * <p>
 * Synonym of “zonal identifier reference system” and “zone indexing scheme”.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#term-zirs
 * @see https://www.mdpi.com/2220-9964/4/1/320 for the different kind of indexing
 * @see https://docs.ogc.org/per/23-010.html#_the_pyxis_indexing for pyxis indexing
 * @see https://defs.opengis.net/prez/catalogs/ogc-cat:register/col/def:dggrs for DGGRS ZIRS definitions
 */
public interface ZonalReferenceSystem {

    /**
     * @return identifier of the zonal reference system
     */
    String getIdentifier();

    /**
     * @return description of the zonal reference system
     */
    String getDescription();

    /**
     * @return true if create ZonalIdentifiers have a Long type representation
     */
    boolean supportUInt64Form();
}
