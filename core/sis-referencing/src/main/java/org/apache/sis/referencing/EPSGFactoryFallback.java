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

import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Fallback;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Debug;


/**
 * The authority factory to use as a fallback when the real EPSG factory is not available.
 * We use this factory in order to guarantee that the minimal set of CRS codes documented
 * in the {@link CRS#forCode(String)} method javadoc is always available.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@Fallback
final class EPSGFactoryFallback extends GeodeticAuthorityFactory implements CRSAuthorityFactory {
    /**
     * Whether to disallow {@code CommonCRS} to use {@link org.apache.sis.referencing.factory.sql.EPSGFactory}
     * (in which case {@code CommonCRS} will fallback on hard-coded values).
     * This field should always be {@code false}, except for debugging purposes.
     */
    @Debug
    static final boolean FORCE_HARDCODED = false;

    /**
     * The singleton instance.
     */
    static final CRSAuthorityFactory INSTANCE = new EPSGFactoryFallback();

    /**
     * The authority, created when first needed.
     */
    private Citation authority;

    /**
     * Constructor for the singleton instance.
     */
    private EPSGFactoryFallback() {
    }

    /**
     * Returns the EPSG authority with only a modification in the title of emphasing that this is a subset
     * of EPSG dataset.
     */
    @Override
    public synchronized Citation getAuthority() {
        if (authority == null) {
            final DefaultCitation c = new DefaultCitation(Citations.EPSG);
            c.setTitle(new SimpleInternationalString("Subset of " + c.getTitle().toString(Locale.ENGLISH)));
            authority = c;
        }
        return authority;
    }

    /**
     * Returns the namespace of EPSG codes.
     *
     * @return The {@code "EPSG"} string in a singleton map.
     */
    @Override
    public Set<String> getCodeSpaces() {
        return Collections.singleton(Constants.EPSG);
    }

    /**
     * Returns the list of EPSG codes available.
     */
    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) {
        final boolean geographic = type.isAssignableFrom(GeographicCRS.class);
        final boolean geocentric = type.isAssignableFrom(GeocentricCRS.class);
        final boolean projected  = type.isAssignableFrom(ProjectedCRS .class);
        final Set<String> codes = new LinkedHashSet<String>();
        for (final CommonCRS crs : CommonCRS.values()) {
            if (geographic) {
                add(codes, crs.geographic);
                add(codes, crs.geo3D);
            }
            if (geocentric) {
                add(codes, crs.geocentric);
            }
            if (projected && (crs.northUTM != 0 || crs.southUTM != 0)) {
                for (int zone = crs.firstZone; zone <= crs.lastZone; zone++) {
                    if (crs.northUTM != 0) codes.add(Integer.toString(crs.northUTM + zone));
                    if (crs.southUTM != 0) codes.add(Integer.toString(crs.southUTM + zone));
                }
            }
        }
        if (type.isAssignableFrom(VerticalCRS.class)) {
            for (final CommonCRS.Vertical candidate : CommonCRS.Vertical.values()) {
                if (candidate.isEPSG) {
                    codes.add(Integer.toString(candidate.crs));
                }
            }
        }
        return codes;
    }

    /**
     * Adds the given value to the given set, provided that the value is different than zero.
     * Zero is used as a sentinel value in {@link CommonCRS} meaning "no EPSG code".
     */
    private static void add(final Set<String> codes, final int value) {
        if (value != 0) {
            codes.add(Integer.toString(value));
        }
    }

    /**
     * Returns a coordinate reference system for the given EPSG code. This method is invoked
     * as a fallback when {@link CRS#forCode(String)} can not create a CRS for a given code.
     */
    @Override
    public IdentifiedObject createObject(final String code) throws NoSuchAuthorityCodeException {
        NumberFormatException cause = null;
        try {
            /*
             * Parse the value after the last ':'. We do not bother to verify if the part before ':' is legal
             * (e.g. "EPSG:4326", "EPSG::4326", "urn:ogc:def:crs:epsg::4326", etc.) because this analysis has
             * already be done by MultiAuthoritiesFactory. We nevertheless skip the prefix in case this factory
             * is used directly (not through MultiAuthoritiesFactory), which should be rare. The main case is
             * when using the factory returned by AuthorityFactories.fallback(…).
             */
            final int n = Integer.parseInt(CharSequences.trimWhitespaces(code,
                            code.lastIndexOf(DefaultNameSpace.DEFAULT_SEPARATOR) + 1,
                            code.length()).toString());
            for (final CommonCRS crs : CommonCRS.values()) {
                if (n == crs.geographic) return crs.geographic();
                if (n == crs.geocentric) return crs.geocentric();
                if (n == crs.geo3D)      return crs.geographic3D();
                final double latitude;
                int zone;
                if (crs.northUTM != 0 && (zone = n - crs.northUTM) >= crs.firstZone && zone <= crs.lastZone) {
                    latitude = +1;
                } else if (crs.southUTM != 0 && (zone = n - crs.southUTM) >= crs.firstZone && zone <= crs.lastZone) {
                    latitude = -1;
                } else {
                    continue;
                }
                return crs.UTM(latitude, TransverseMercator.centralMeridian(zone));
            }
            for (final CommonCRS.Vertical candidate : CommonCRS.Vertical.values()) {
                if (candidate.isEPSG && candidate.crs == n) {
                    return candidate.crs();
                }
            }
        } catch (NumberFormatException e) {
            cause = e;
        }
        final String authority = Constants.EPSG + " subset";
        final NoSuchAuthorityCodeException e = new NoSuchAuthorityCodeException(Errors.format(
                Errors.Keys.NoSuchAuthorityCode_3, authority, CoordinateReferenceSystem.class, code), authority, code);
        e.initCause(cause);
        throw e;
    }
}
