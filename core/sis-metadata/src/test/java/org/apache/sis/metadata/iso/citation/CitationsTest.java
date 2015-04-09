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
package org.apache.sis.metadata.iso.citation;

import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.metadata.iso.citation.Citations.*;
import static org.junit.Assert.assertSame;


/**
 * Tests {@link Citations}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class CitationsTest extends TestCase {
    /**
     * Tests {@link Citations#fromName(String)}.
     */
    @Test
    public void testFromName() {
        assertSame(ISO,      fromName("ISO"));
        assertSame(OGC,      fromName(Constants.OGC));
        assertSame(EPSG,     fromName(Constants.EPSG));  // This one is important.
        assertSame(EPSG,     fromName(Constants.IOGP));  // This one is not really needed (and maybe not strictly correct).
        assertSame(SIS,      fromName(Constants.SIS));
        assertSame(ESRI,     fromName("ESRI"));
        assertSame(ORACLE,   fromName("Oracle"));
        assertSame(NETCDF,   fromName("NetCDF"));
        assertSame(GEOTIFF,  fromName("GeoTIFF"));
        assertSame(PROJ4,    fromName("Proj.4"));
        assertSame(PROJ4,    fromName("Proj4"));
        assertSame(MAP_INFO, fromName("MapInfo"));
        assertSame(S57,      fromName("S-57"));
        assertSame(S57,      fromName("S57"));
        assertSame(ISBN,     fromName("ISBN"));
        assertSame(ISSN,     fromName("ISSN"));
    }
}
