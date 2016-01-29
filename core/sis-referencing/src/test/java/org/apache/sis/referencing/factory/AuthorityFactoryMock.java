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

import java.util.Set;
import java.util.LinkedHashSet;
import javax.measure.unit.Unit;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.crs.HardCodedCRS;

import static org.junit.Assert.*;


/**
 * A pseudo-authority factory with hard-coded objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@org.apache.sis.internal.jdk7.AutoCloseable
public final strictfp class AuthorityFactoryMock extends GeodeticAuthorityFactory implements CRSAuthorityFactory,
        CSAuthorityFactory, DatumAuthorityFactory, CoordinateOperationAuthorityFactory
{
    /**
     * The authority.
     */
    private final Citation authority;

    /**
     * {@code true} if this factory has been closed by
     * an explicit call to the {@link #close()} method.
     */
    private boolean closed;

    /**
     * Creates a new factory for the given authority.
     *
     * @param authority The title of the authority to declare.
     * @param version   The version, or {@code null} if none.
     */
    @SuppressWarnings("serial")
    public AuthorityFactoryMock(final String authority, final String version) {
        this.authority = new SimpleCitation(authority) {
            @Override public InternationalString getEdition() {
                return (version != null) ? new SimpleInternationalString(version) : null;
            }
        };
    }

    /**
     * Returns the authority built from the title given at construction time.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Add the string representations of the given values into the {@code codes} set.
     * This is a helper method for {@link #getAuthorityCodes(Class)}.
     * Not an efficient approach but okay for testing purpose.
     */
    private static void add(final Set<String> codes, final int... values) {
        for (final int value : values) {
            final String s = Integer.toString(value);
            assertTrue(s, codes.add(s));
        }
    }

    /**
     * Returns the authority codes for the given type.
     */
    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) {
        assertFalse("This factory has been closed.", isClosed());
        final Set<String> codes = new LinkedHashSet<String>();
        if (type.isAssignableFrom(GeocentricCRS.class)) add(codes, 4979);
        if (type.isAssignableFrom(GeographicCRS.class)) add(codes, 84, 4326);
        if (type.isAssignableFrom(PrimeMeridian.class)) add(codes, 8901, 8903, 8914);
        if (type.isAssignableFrom(GeodeticDatum.class)) add(codes, 6326, 6322, 6807, 6301, 6612, 6047);
        if (type.isAssignableFrom(VerticalDatum.class)) add(codes, 5100);
        if (type.isAssignableFrom(VerticalCRS.class))   add(codes, 5714, 9905);
        return codes;
    }

    /**
     * Returns the geodetic object for the given code.
     *
     * @throws NoSuchAuthorityCodeException if the given code is unknown.
     */
    @Override
    public IdentifiedObject createObject(final String code) throws NoSuchAuthorityCodeException {
        assertFalse("This factory has been closed.", isClosed());
        final int n;
        try {
            n = Integer.parseInt(trimNamespace(code));
        } catch (NumberFormatException e) {
            throw new NoSuchAuthorityCodeException(e.toString(), "MOCK", code);
        }
        switch (n) {
            case   84: return HardCodedCRS.WGS84;
            case 4326: return HardCodedCRS.WGS84_φλ;
            case 4979: return HardCodedCRS.GEOCENTRIC;
            case 5714: return HardCodedCRS.GRAVITY_RELATED_HEIGHT;
            case 9905: return HardCodedCRS.DEPTH;
            case 8901: return HardCodedDatum.GREENWICH;
            case 8903: return HardCodedDatum.PARIS;
            case 8914: return HardCodedDatum.PARIS_RGS;
            case 6326: return HardCodedDatum.WGS84;
            case 6322: return HardCodedDatum.WGS72;
            case 6807: return HardCodedDatum.NTF;
            case 6301: return HardCodedDatum.TOKYO;
            case 6612: return HardCodedDatum.JGD2000;
            case 6047: return HardCodedDatum.SPHERE;
            case 5100: return HardCodedDatum.MEAN_SEA_LEVEL;
            default: throw new NoSuchAuthorityCodeException(code, authority.getTitle().toString(), code);
        }
    }

    /**
     * Returns the unit of measurement for the given code.
     *
     * @return The unit of measurement.
     * @throws NoSuchAuthorityCodeException if the given code is unknown.
     */
    @Override
    public Unit<?> createUnit(final String code) throws NoSuchAuthorityCodeException {
        assertFalse("This factory has been closed.", isClosed());
        final int n;
        try {
            n = Integer.parseInt(trimNamespace(code));
        } catch (NumberFormatException e) {
            throw new NoSuchAuthorityCodeException(e.toString(), "MOCK", code);
        }
        final Unit<?> unit = Units.valueOfEPSG(n);
        if (unit == null) {
            throw new NoSuchAuthorityCodeException(code, authority.getTitle().toString(), code);
        }
        return unit;
    }

    /**
     * Returns the spatial extent for the given code.
     *
     * @return The spatial extent.
     * @throws NoSuchAuthorityCodeException if the given code is unknown.
     */
    @Override
    public Extent createExtent(final String code) throws NoSuchAuthorityCodeException {
        assertFalse("This factory has been closed.", isClosed());
        final int n;
        try {
            n = Integer.parseInt(trimNamespace(code));
        } catch (NumberFormatException e) {
            throw new NoSuchAuthorityCodeException(e.toString(), "MOCK", code);
        }
        switch (n) {
            case 1262: return Extents.WORLD;
            default: throw new NoSuchAuthorityCodeException(code, authority.getTitle().toString(), code);
        }
    }

    /**
     * Returns {@code true} if this factory has been closed
     * by an explicit call to the {@link #close()} method.
     */
    final synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Flags this factory as closed.
     */
    public synchronized void close() {
        closed = true;
    }
}
