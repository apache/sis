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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Static;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Static methods working on {@linkplain CoordinateReferenceSystem Coordinate Reference Systems}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
public final class CRS extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CRS() {
    }

    /**
     * Returns the Coordinate Reference System for the given authority code.
     * The set of available codes depends on the {@link CRSAuthorityFactory} instances available on the classpath.
     * There is many thousands of CRS defined by EPSG authority or by other authorities.
     * The following table lists a very small subset of codes which are guaranteed to be available
     * on any installation of Apache SIS version 0.4 or above:
     *
     * <blockquote><table class="sis">
     *   <tr><th>Code</th>      <th>Enum</th>                                  <th>CRS Type</th>   <th>Description</th></tr>
     *   <tr><td>CRS:27</td>    <td>{@link GeodeticObjects#NAD27  NAD27}</td>  <td>Geographic</td> <td>Like EPSG:4267 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:83</td>    <td>{@link GeodeticObjects#NAD83  NAD83}</td>  <td>Geographic</td> <td>Like EPSG:4269 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:84</td>    <td>{@link GeodeticObjects#WGS84  WGS84}</td>  <td>Geographic</td> <td>Like EPSG:4326 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>EPSG:4047</td> <td>{@link GeodeticObjects#SPHERE SPHERE}</td> <td>Geographic</td> <td>GRS 1980 Authalic Sphere</td></tr>
     *   <tr><td>EPSG:4230</td> <td>{@link GeodeticObjects#ED50   ED50}</td>   <td>Geographic</td> <td>European Datum 1950</td></tr>
     *   <tr><td>EPSG:4258</td> <td>{@link GeodeticObjects#ETRS89 ETRS89}</td> <td>Geographic</td> <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4267</td> <td>{@link GeodeticObjects#NAD27  NAD27}</td>  <td>Geographic</td> <td>North American Datum 1927</td></tr>
     *   <tr><td>EPSG:4269</td> <td>{@link GeodeticObjects#NAD83  NAD83}</td>  <td>Geographic</td> <td>North American Datum 1983</td></tr>
     *   <tr><td>EPSG:4322</td> <td>{@link GeodeticObjects#WGS72  WGS72}</td>  <td>Geographic</td> <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4326</td> <td>{@link GeodeticObjects#WGS84  WGS84}</td>  <td>Geographic</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:5715</td> <td>{@link GeodeticObjects.Vertical#DEPTH  DEPTH}</td> <td>Vertical</td> <td>Mean Sea Level depth</td></tr>
     *   <tr><td>EPSG:5714</td> <td>{@link GeodeticObjects.Vertical#MEAN_SEA_LEVEL MEAN_SEA_LEVEL}</td> <td>Vertical</td> <td>Mean Sea Level height</td></tr>
     * </table></blockquote>
     *
     * This method accepts also the URN and URL syntax.
     * For example the following codes are considered equivalent to {@code "EPSG:4326"}:
     * <ul>
     *   <li>{@code "urn:ogc:def:crs:EPSG::4326"}</li>
     *   <li>{@code "http://www.opengis.net/gml/srs/epsg.xml#4326"}</li>
     * </ul>
     *
     * @param  code The authority code.
     * @return The Coordinate Reference System for the given authority code.
     * @throws NoSuchAuthorityCodeException If there is no known CRS associated to the given code.
     * @throws FactoryException if the CRS creation failed for an other reason.
     */
    public static CoordinateReferenceSystem forCode(final String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        ensureNonNull("code", code);
        final String authority;
        final String value;
        final DefinitionURI uri = DefinitionURI.parse(code);
        if (uri != null) {
            final String type = uri.type;
            if (type != null && !type.equalsIgnoreCase("crs")) {
                throw new NoSuchIdentifierException(Errors.format(Errors.Keys.UnknownType_1, type), type);
            }
            authority = uri.authority;
            value = uri.code;
        } else {
            final int s = code.indexOf(DefinitionURI.SEPARATOR);
            authority = CharSequences.trimWhitespaces(code.substring(0, Math.max(0, s)));
            value = CharSequences.trimWhitespaces(code.substring(s + 1));
        }
        if (authority == null || authority.isEmpty()) {
            throw new NoSuchIdentifierException(Errors.format(Errors.Keys.MissingAuthority_1, code), code);
        }
        /*
         * Code below this point is a temporary implementation to
         * be removed after we ported the EPSG authority factory.
         */
        NumberFormatException cause = null;
        try {
            if (authority.equalsIgnoreCase("CRS")) {
                switch (Integer.parseInt(value)) {
                    case 27: return GeodeticObjects.NAD27.normalizedGeographic();
                    case 83: return GeodeticObjects.NAD83.normalizedGeographic();
                    case 84: return GeodeticObjects.WGS84.normalizedGeographic();
                }
            } else if (authority.equalsIgnoreCase("EPSG")) {
                final int n = Integer.parseInt(value);
                for (final GeodeticObjects candidate : GeodeticObjects.values()) {
                    if (candidate.geographic == n) {
                        return candidate.geographic();
                    }
                }
                for (final GeodeticObjects.Vertical candidate : GeodeticObjects.Vertical.values()) {
                    if (candidate.isEPSG && candidate.crs == n) {
                        return candidate.crs();
                    }
                }
            } else {
                throw new NoSuchIdentifierException(Errors.format(Errors.Keys.UnknownAuthority_1, authority), authority);
            }
        } catch (NumberFormatException e) {
            cause = e;
        }
        final NoSuchAuthorityCodeException e = new NoSuchAuthorityCodeException(
                Errors.format(Errors.Keys.NoSuchAuthorityCode_3, authority, CoordinateReferenceSystem.class, value),
                authority, value, code);
        e.initCause(cause);
        throw e;
    }

    /**
     * Returns the ordered list of single coordinate reference systems for the specified CRS.
     * This method performs the following choices:
     *
     * <ul>
     *   <li>If the given CRS is null, returns an empty list.</li>
     *   <li>If the given CRS is an instance of {@link SingleCRS}, returns that instance in a singleton list.</li>
     *   <li>If the given CRS is an instance of {@link CompoundCRS}, returns a flattened list of its
     *       {@linkplain DefaultCompoundCRS#getComponents() components}. Some components may themselves be
     *       other {@code CompoundCRS} instances, in which case those compound CRS are also expanded in their
     *       list of {@code SingleCRS} components.</li>
     *   <li>Otherwise throws a {@code ClassCastException}.</li>
     * </ul>
     *
     * {@example Apache SIS allows 4-dimensional (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>)
     * coordinate reference system to be built in two different ways as shown below:
     *
     * <p><table class="compact">
     * <tr><th>Hierarchical structure</th><th>Flat list</th></tr>
     * <tr><td><blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>)<br>
     *   <code>  │   ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  │   └─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote></td><td><blockquote>
     *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
     *   <code>  ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
     *   <code>  ├─VerticalCRS</code> — (<var>z</var>)<br>
     *   <code>  └─TemporalCRS</code> — (<var>t</var>)
     * </blockquote>
     * </td></tr></table></p>
     *
     * This method guaranteed that the returned list is a flat one as shown on the right side.
     * Note that such flat lists are the only one allowed by ISO/OGC standards for compound CRS.
     * The hierarchical structure is an Apache SIS flexibility.}
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The single coordinate reference systems, or an empty list if the given CRS is {@code null}.
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     *
     * @see DefaultCompoundCRS#getSingleComponents()
     */
    public static List<SingleCRS> getSingleComponents(final CoordinateReferenceSystem crs) {
        final List<SingleCRS> singles;
        if (crs == null) {
            singles = Collections.emptyList();
        } else if (crs instanceof CompoundCRS) {
            if (crs instanceof DefaultCompoundCRS) {
                singles = ((DefaultCompoundCRS) crs).getSingleComponents();
            } else {
                final List<CoordinateReferenceSystem> elements = ((CompoundCRS) crs).getComponents();
                singles = new ArrayList<>(elements.size());
                ReferencingUtilities.getSingleComponents(elements, singles);
            }
        } else {
            // Intentional CassCastException here if the crs is not a SingleCRS.
            singles = Collections.singletonList((SingleCRS) crs);
        }
        return singles;
    }
}
