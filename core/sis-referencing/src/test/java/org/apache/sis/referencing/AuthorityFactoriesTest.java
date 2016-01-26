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
package org.apache.sis.referencing;

import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link AuthorityFactories}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class AuthorityFactoriesTest extends TestCase {
    /**
     * Tests creation of {@code CRS:84} from various codes.
     *
     * @throws FactoryException if a CRS:84 creation failed.
     */
    @Test
    public void testCRS84() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        final GeographicCRS crs = factory.createGeographicCRS("CRS:84");
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:CRS::84"));
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:CRS:1.3:84"));
        assertSame(crs, factory.createGeographicCRS("URN:OGC:DEF:CRS:CRS:1.3:84"));
        assertSame(crs, factory.createGeographicCRS("URN:OGC:DEF:CRS:CRS::84"));
        assertSame(crs, factory.createGeographicCRS("urn:x-ogc:def:crs:CRS:1.3:84"));
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:OGC:1.3:CRS84"));

        // Following are just wrappers for above factory.
        assertSame(crs, CRS.forCode("urn:ogc:def:crs:CRS:1.3:84"));
        assertSame(crs, CRS.forCode("urn:ogc:def:crs:OGC:1.3:CRS84"));
        assertSame(crs, CRS.forCode("CRS:84"));
        assertSame(crs, CRS.forCode("OGC:CRS84"));

        assertNotDeepEquals(crs, CRS.forCode("CRS:83"));
    }
}
