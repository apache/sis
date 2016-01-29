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
package org.apache.sis.referencing.factory;

import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.crs.AbstractCRS;

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link AuthorityFactoryProxy} implementation.
 * This test uses {@link CommonAuthorityFactory} as a simple factory implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(CommonAuthorityFactoryTest.class)
public final strictfp class AuthorityFactoryProxyTest extends TestCase {
    /**
     * Ensures that the most specific interfaces appear first in the list of proxies.
     */
    @Test
    public void testProxies() {
        for (int i=1; i<AuthorityFactoryProxy.PROXIES.length; i++) {
            final Class<?> generic = AuthorityFactoryProxy.PROXIES[i].type;
            for (int j=0; j<i; j++) {
                assertFalse(AuthorityFactoryProxy.PROXIES[j].type.isAssignableFrom(generic));
            }
        }
    }

    /**
     * Tests {@link AuthorityFactoryProxy#getInstance(Class)}.
     */
    @Test
    public void testGetInstance() {
        assertEquals(ProjectedCRS.class,              AuthorityFactoryProxy.getInstance(ProjectedCRS.class)        .type);
        assertEquals(ProjectedCRS.class,              AuthorityFactoryProxy.getInstance(DefaultProjectedCRS.class) .type);
        assertEquals(GeographicCRS.class,             AuthorityFactoryProxy.getInstance(GeographicCRS.class)       .type);
        assertEquals(GeographicCRS.class,             AuthorityFactoryProxy.getInstance(DefaultGeographicCRS.class).type);
        assertEquals(DerivedCRS.class,                AuthorityFactoryProxy.getInstance(DefaultDerivedCRS.class)   .type);
        assertEquals(GeodeticDatum.class,             AuthorityFactoryProxy.getInstance(DefaultGeodeticDatum.class).type);
        assertEquals(CoordinateReferenceSystem.class, AuthorityFactoryProxy.getInstance(AbstractCRS.class)         .type);
        assertEquals(CoordinateSystem.class,          AuthorityFactoryProxy.getInstance(CoordinateSystem.class)    .type);
        assertEquals(CoordinateSystemAxis.class,      AuthorityFactoryProxy.getInstance(CoordinateSystemAxis.class).type);
        assertEquals(PrimeMeridian.class,             AuthorityFactoryProxy.getInstance(PrimeMeridian.class)       .type);
        assertEquals(Ellipsoid.class,                 AuthorityFactoryProxy.getInstance(Ellipsoid.class)           .type);
        assertEquals(Datum.class,                     AuthorityFactoryProxy.getInstance(Datum.class)               .type);
    }

    /**
     * Tests {@link AuthorityFactoryProxy#specialize(String)}.
     */
    @Test
    public void testSpecialize() {
        final AuthorityFactoryProxy<IdentifiedObject> base = AuthorityFactoryProxy.OBJECT;
        assertEquals(CoordinateReferenceSystem.class, base.specialize("CRS")      .type);
        assertEquals(CoordinateSystem.class,          base.specialize("CS")       .type);
        assertEquals(CoordinateSystemAxis.class,      base.specialize("aXis")     .type);
        assertEquals(PrimeMeridian.class,             base.specialize("Meridian") .type);
        assertEquals(Ellipsoid.class,                 base.specialize("ellipsoid").type);
        assertEquals(Datum.class,                     base.specialize("datum")    .type);

        assertEquals(GeodeticDatum.class, AuthorityFactoryProxy.GEODETIC_DATUM.specialize("datum").type);
        assertNull(AuthorityFactoryProxy.COORDINATE_SYSTEM.specialize("datum"));
    }

    /**
     * Tests {@link AuthorityFactoryProxy#createFromAPI(AuthorityFactory, String)}.
     * We use the {@link CommonAuthorityFactory} for testing purpose.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testCreateFromAPI() throws FactoryException {
        final CRSAuthorityFactory factory = new CommonAuthorityFactory();
        final CoordinateReferenceSystem expected = factory.createCoordinateReferenceSystem("83");
        AuthorityFactoryProxy<?> proxy;
        /*
         * Try the proxy using the 'createGeographicCRS', 'createCoordinateReferenceSystem'
         * and 'createObject' methods. The later uses a generic implementation, while the
         * first two should use specialized implementations.
         */
        proxy = AuthorityFactoryProxy.getInstance(GeographicCRS.class);
        assertSame(AuthorityFactoryProxy.GEOGRAPHIC_CRS, proxy);
        assertSame(expected, proxy.createFromAPI(factory, "83"));
        assertSame(expected, proxy.createFromAPI(factory, "CRS:83"));

        proxy = AuthorityFactoryProxy.getInstance(CoordinateReferenceSystem.class);
        assertSame(AuthorityFactoryProxy.CRS, proxy);
        assertSame(expected, proxy.createFromAPI(factory, "83"));
        assertSame(expected, proxy.createFromAPI(factory, "CRS:83"));

        proxy = AuthorityFactoryProxy.getInstance(IdentifiedObject.class);
        assertSame(AuthorityFactoryProxy.OBJECT, proxy);
        assertSame(expected, proxy.createFromAPI(factory, "83"));
        assertSame(expected, proxy.createFromAPI(factory, "CRS:83"));
        /*
         * Try using the 'createProjectedCRS' method, which should not
         * be supported for the CRS factory (at least not for code "83").
         */
        proxy = AuthorityFactoryProxy.getInstance(ProjectedCRS.class);
        assertSame(AuthorityFactoryProxy.PROJECTED_CRS, proxy);
        try {
            assertSame(expected, proxy.createFromAPI(factory, "83"));
            fail("Should not have created a CRS of the wrong type.");
        } catch (FactoryException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message.contains("83"));
            assertTrue(message.contains("GeographicCRS"));
            assertTrue(message.contains("ProjectedCRS"));
        }
    }
}
