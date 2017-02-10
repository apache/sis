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
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
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
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Fallback;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Debug;


/**
 * The authority factory to use as a fallback when the real EPSG factory is not available.
 * We use this factory in order to guarantee that the minimal set of CRS codes documented
 * in the {@link CRS#forCode(String)} method javadoc is always available.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
@Fallback
final class EPSGFactoryFallback extends GeodeticAuthorityFactory implements CRSAuthorityFactory, DatumAuthorityFactory {
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
    static final EPSGFactoryFallback INSTANCE = new EPSGFactoryFallback();

    /**
     * Kinds of object created by this factory. Used as bitmask.
     */
    private static final int CRS = 1, DATUM = 2, ELLIPSOID = 4, PRIME_MERIDIAN = 8;

    /**
     * The authority to report in exceptions. Not necessarily the same than the {@link #authority} title.
     */
    private static final String AUTHORITY = Constants.EPSG + "-subset";

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
            c.setTitle(Vocabulary.formatInternational(Vocabulary.Keys.SubsetOf_1, c.getTitle()));
            authority = c;
        }
        return authority;
    }

    /**
     * Returns the namespace of EPSG codes.
     *
     * @return the {@code "EPSG"} string in a singleton map.
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
        final boolean pm         = type.isAssignableFrom(PrimeMeridian.class);
        final boolean ellipsoid  = type.isAssignableFrom(Ellipsoid    .class);
        final boolean datum      = type.isAssignableFrom(GeodeticDatum.class);
        final boolean geographic = type.isAssignableFrom(GeographicCRS.class);
        final boolean geocentric = type.isAssignableFrom(GeocentricCRS.class);
        final boolean projected  = type.isAssignableFrom(ProjectedCRS .class);
        final Set<String> codes = new LinkedHashSet<>();
        if (pm) codes.add(StandardDefinitions.GREENWICH);
        for (final CommonCRS crs : CommonCRS.values()) {
            if (ellipsoid)  add(codes, crs.ellipsoid);
            if (datum)      add(codes, crs.datum);
            if (geocentric) add(codes, crs.geocentric);
            if (geographic) {
                add(codes, crs.geographic);
                add(codes, crs.geo3D);
            }
            if (projected && (crs.northUTM != 0 || crs.southUTM != 0)) {
                for (int zone = crs.firstZone; zone <= crs.lastZone; zone++) {
                    if (crs.northUTM != 0) codes.add(Integer.toString(crs.northUTM + zone));
                    if (crs.southUTM != 0) codes.add(Integer.toString(crs.southUTM + zone));
                }
            }
        }
        final boolean vertical = type.isAssignableFrom(VerticalCRS  .class);
        final boolean vdatum   = type.isAssignableFrom(VerticalDatum.class);
        if (vertical || vdatum) {
            for (final CommonCRS.Vertical candidate : CommonCRS.Vertical.values()) {
                if (candidate.isEPSG) {
                    if (vertical) codes.add(Integer.toString(candidate.crs));
                    if (vdatum)   codes.add(Integer.toString(candidate.datum));
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
     * Returns a prime meridian for the given EPSG code.
     */
    @Override
    public PrimeMeridian createPrimeMeridian(final String code) throws NoSuchAuthorityCodeException {
        return (PrimeMeridian) predefined(code, PRIME_MERIDIAN);
    }

    /**
     * Returns an ellipsoid for the given EPSG code.
     */
    @Override
    public Ellipsoid createEllipsoid(final String code) throws NoSuchAuthorityCodeException {
        return (Ellipsoid) predefined(code, ELLIPSOID);
    }

    /**
     * Returns a datum for the given EPSG code.
     */
    @Override
    public Datum createDatum(final String code) throws NoSuchAuthorityCodeException {
        return (Datum) predefined(code, DATUM);
    }

    /**
     * Returns a coordinate reference system for the given EPSG code. This method is invoked
     * as a fallback when {@link CRS#forCode(String)} can not create a CRS for a given code.
     */
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws NoSuchAuthorityCodeException {
        return (CoordinateReferenceSystem) predefined(code, CRS);
    }

    /**
     * Returns a coordinate reference system, datum or ellipsoid for the given EPSG code.
     */
    @Override
    public IdentifiedObject createObject(final String code) throws NoSuchAuthorityCodeException {
        return predefined(code, -1);
    }

    /**
     * Implementation of all {@code createFoo(String)} methods in this fallback class.
     *
     * @param  code  the EPSG code.
     * @param  kind  any combination of {@link #CRS}, {@link #DATUM}, {@link #ELLIPSOID} or {@link #PRIME_MERIDIAN} bits.
     * @return the requested object.
     * @throws NoSuchAuthorityCodeException if no matching object has been found.
     */
    private IdentifiedObject predefined(String code, final int kind) throws NoSuchAuthorityCodeException {
        try {
            /*
             * Parse the value after the last ':'. We do not bother to verify if the part before ':' is legal
             * (e.g. "EPSG:4326", "EPSG::4326", "urn:ogc:def:crs:epsg::4326", etc.) because this analysis has
             * already be done by MultiAuthoritiesFactory. We nevertheless skip the prefix in case this factory
             * is used directly (not through MultiAuthoritiesFactory), which should be rare. The main case is
             * when using the factory returned by AuthorityFactories.fallback(…).
             */
            code = CharSequences.trimWhitespaces(code, code.lastIndexOf(DefaultNameSpace.DEFAULT_SEPARATOR) + 1, code.length()).toString();
            final int n = Integer.parseInt(code);
            if ((kind & PRIME_MERIDIAN) != 0  &&  n == Constants.EPSG_GREENWICH) {
                return CommonCRS.WGS84.primeMeridian();
            }
            for (final CommonCRS crs : CommonCRS.values()) {
                /*
                 * In a complete EPSG dataset we could have an ambiguity below because the same code can be used
                 * for datum, ellipsoid and CRS objects. However in the particular case of this EPSG-subset, we
                 * ensured that there is no such collision - see CommonCRSTest.ensureNoCodeCollision().
                 */
                if ((kind & ELLIPSOID) != 0  &&  n == crs.ellipsoid) return crs.ellipsoid();
                if ((kind & DATUM)     != 0  &&  n == crs.datum)     return crs.datum();
                if ((kind & CRS) != 0) {
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
                    return crs.UTM(latitude, TransverseMercator.Zoner.UTM.centralMeridian(zone));
                }
            }
            if ((kind & (DATUM | CRS)) != 0) {
                for (final CommonCRS.Vertical candidate : CommonCRS.Vertical.values()) {
                    if (candidate.isEPSG) {
                        if ((kind & DATUM) != 0  &&  candidate.datum == n) return candidate.datum();
                        if ((kind & CRS)   != 0  &&  candidate.crs   == n) return candidate.crs();
                    }
                }
            }
        } catch (NumberFormatException cause) {
            final NoSuchAuthorityCodeException e = new NoSuchAuthorityCodeException(Resources.format(
                    Resources.Keys.NoSuchAuthorityCode_3, Constants.EPSG, toClass(kind), code), AUTHORITY, code);
            e.initCause(cause);
            throw e;
        }
        throw new NoSuchAuthorityCodeException(Resources.format(Resources.Keys.NoSuchAuthorityCodeInSubset_4,
                Constants.EPSG, toClass(kind), code, "http://sis.apache.org/epsg.html"), AUTHORITY, code);
    }

    /**
     * Returns the interface for the given {@link #CRS}, {@link #DATUM}, {@link #ELLIPSOID} or {@link #PRIME_MERIDIAN}
     * constant. This is used for formatting error message only.
     */
    private static Class<?> toClass(final int kind) {
        switch (kind) {
            case CRS:            return CoordinateReferenceSystem.class;
            case DATUM:          return Datum.class;
            case ELLIPSOID:      return Ellipsoid.class;
            case PRIME_MERIDIAN: return PrimeMeridian.class;
            default:             return IdentifiedObject.class;
        }
    }
}
