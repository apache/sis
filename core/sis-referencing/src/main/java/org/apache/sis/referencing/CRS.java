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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.internal.metadata.ReferencingUtilities;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.cs.DefaultEllipsoidalCS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Static;

import static java.util.Collections.singletonMap;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Static methods working on {@linkplain CoordinateReferenceSystem Coordinate Reference Systems}.
 * The methods defined in this class can be grouped in two categories:
 *
 * <ul>
 *   <li>Factory methods, the most notable one being {@link #forCode(String)}.</li>
 *   <li>Methods providing information, like {@link #isHorizontalCRS(CoordinateReferenceSystem)}.</li>
 * </ul>
 *
 * {@section Note on kinds of CRS}
 * The {@link #getSingleComponents(CoordinateReferenceSystem)} method decomposes an arbitrary CRS into a flat
 * list of single components. In such flat list, vertical and temporal components can easily be identified by
 * {@code instanceof} checks. But identifying the horizontal component is not as easy. The list below suggests
 * ways to classify the components:
 *
 * <ul>
 *   <li><code>if (crs instanceof TemporalCRS)</code> determines if the CRS is for the temporal component.</li>
 *   <li><code>if (crs instanceof VerticalCRS)</code> determines if the CRS is for the vertical component.</li>
 *   <li><code>if (CRS.{@linkplain #isHorizontalCRS(CoordinateReferenceSystem) isHorizontalCRS}(crs))</code>
 *       determines if the CRS is for the horizontal component.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
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
     *   <caption>Minimal set of supported authority codes</caption>
     *   <tr><th>Code</th>      <th>Enum</th>                            <th>CRS Type</th>      <th>Description</th></tr>
     *   <tr><td>CRS:27</td>    <td>{@link CommonCRS#NAD27  NAD27}</td>  <td>Geographic</td>    <td>Like EPSG:4267 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:83</td>    <td>{@link CommonCRS#NAD83  NAD83}</td>  <td>Geographic</td>    <td>Like EPSG:4269 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>CRS:84</td>    <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic</td>    <td>Like EPSG:4326 except for (<var>longitude</var>, <var>latitude</var>) axis order</td></tr>
     *   <tr><td>EPSG:4047</td> <td>{@link CommonCRS#SPHERE SPHERE}</td> <td>Geographic</td>    <td>GRS 1980 Authalic Sphere</td></tr>
     *   <tr><td>EPSG:4230</td> <td>{@link CommonCRS#ED50   ED50}</td>   <td>Geographic</td>    <td>European Datum 1950</td></tr>
     *   <tr><td>EPSG:4258</td> <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geographic</td>    <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4267</td> <td>{@link CommonCRS#NAD27  NAD27}</td>  <td>Geographic</td>    <td>North American Datum 1927</td></tr>
     *   <tr><td>EPSG:4269</td> <td>{@link CommonCRS#NAD83  NAD83}</td>  <td>Geographic</td>    <td>North American Datum 1983</td></tr>
     *   <tr><td>EPSG:4322</td> <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geographic</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4326</td> <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4936</td> <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geocentric</td>    <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4937</td> <td>{@link CommonCRS#ETRS89 ETRS89}</td> <td>Geographic 3D</td> <td>European Terrestrial Reference Frame 1989</td></tr>
     *   <tr><td>EPSG:4978</td> <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geocentric</td>    <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4979</td> <td>{@link CommonCRS#WGS84  WGS84}</td>  <td>Geographic 3D</td> <td>World Geodetic System 1984</td></tr>
     *   <tr><td>EPSG:4984</td> <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geocentric</td>    <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:4985</td> <td>{@link CommonCRS#WGS72  WGS72}</td>  <td>Geographic 3D</td> <td>World Geodetic System 1972</td></tr>
     *   <tr><td>EPSG:5715</td> <td>{@link CommonCRS.Vertical#DEPTH  DEPTH}</td> <td>Vertical</td> <td>Mean Sea Level depth</td></tr>
     *   <tr><td>EPSG:5714</td> <td>{@link CommonCRS.Vertical#MEAN_SEA_LEVEL MEAN_SEA_LEVEL}</td> <td>Vertical</td> <td>Mean Sea Level height</td></tr>
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
     *
     * @category factory
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
         * Delegate to the factory for the code space of the given code. If no authority factory
         * is available, or if the factory failed to create the CRS, delegate to CommonCRS. Note
         * that CommonCRS is not expected to succeed if the real EPSG factory threw an exception,
         * so we will log a message at the warning level in such case.
         */
        CRSAuthorityFactory factory = null; // TODO
        if (factory != null) try {
            return factory.createCoordinateReferenceSystem(value);
        } catch (FactoryException failure) {
            final CoordinateReferenceSystem crs = CommonCRS.forCode(authority, value, failure);
            Logging.unexpectedException(CRS.class, "forCode", failure); // See above comment.
            return crs;
        } else {
            return CommonCRS.forCode(authority, value, null);
        }
    }

    /**
     * Returns the valid geographic area for the given coordinate reference system, or {@code null} if unknown.
     * This method explores the {@linkplain CoordinateReferenceSystem#getDomainOfValidity() domain of validity}
     * associated with the given CRS. If more than one geographic bounding box is found, then they will be
     * {@linkplain org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#add(GeographicBoundingBox)
     * added} together.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The geographic area, or {@code null} if none.
     *
     * @see #getEnvelope(CoordinateReferenceSystem)
     * @see Extents#getGeographicBoundingBox(Extent)
     *
     * @category information
     */
    public static GeographicBoundingBox getGeographicBoundingBox(final CoordinateReferenceSystem crs) {
        return (crs != null) ? Extents.getGeographicBoundingBox(crs.getDomainOfValidity()) : null;
    }

    /**
     * Returns {@code true} if the given CRS is horizontal. The current implementation considers a
     * CRS as horizontal if it is two-dimensional and comply with one of the following conditions:
     *
     * <ul>
     *   <li>It is an instance of {@link GeographicCRS}.</li>
     *   <li>It is an instance of {@link ProjectedCRS}.</li>
     * </ul>
     *
     * In case of doubt, this method conservatively returns {@code false}.
     *
     * @todo Future SIS implementation may extend the above condition list. For example a radar station could
     *       use a polar coordinate system in a <code>DerivedCRS</code> instance based on a projected CRS.
     *       See <a href="http://issues.apache.org/jira/browse/SIS-161">SIS-161</a>.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return {@code true} if the given CRS is non-null and comply with one of the above conditions,
     *         or {@code false} otherwise.
     *
     * @see #getHorizontalComponent(CoordinateReferenceSystem)
     *
     * @category information
     */
    public static boolean isHorizontalCRS(final CoordinateReferenceSystem crs) {
        if (crs instanceof GeographicCRS || crs instanceof ProjectedCRS) {
            return crs.getCoordinateSystem().getDimension() == 2;
        }
        return false;
    }

    /**
     * Returns the first horizontal coordinate reference system found in the given CRS, or {@code null} if there is
     * none. If the given CRS is already horizontal according {@link #isHorizontalCRS(CoordinateReferenceSystem)},
     * then this method returns it as-is. Otherwise if the given CRS is compound, then this method searches for the
     * first horizontal component in the order of the {@linkplain #getSingleComponents(CoordinateReferenceSystem)
     * single components list}.
     *
     * <p>In the special case where a three-dimensional geographic CRS is found, this method will create a
     * two-dimensional geographic CRS without the vertical axis.</p>
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The first horizontal CRS, or {@code null} if none.
     *
     * @category information
     */
    public static SingleCRS getHorizontalComponent(final CoordinateReferenceSystem crs) {
        if (isHorizontalCRS(crs)) {
            return (SingleCRS) crs;
        }
        if (crs instanceof GeographicCRS) {
            EllipsoidalCS cs = ((GeographicCRS) crs).getCoordinateSystem();
            final int i = AxisDirections.indexOfColinear(cs, AxisDirection.UP);
            if (i >= 0) {
                final CoordinateSystemAxis xAxis = cs.getAxis(i > 0 ? 0 : 1);
                final CoordinateSystemAxis yAxis = cs.getAxis(i > 1 ? 1 : 2);
                cs = CommonCRS.DEFAULT.geographic().getCoordinateSystem();
                if (!Utilities.equalsIgnoreMetadata(cs.getAxis(0), xAxis) ||
                    !Utilities.equalsIgnoreMetadata(cs.getAxis(1), yAxis))
                {
                    // We can not reuse the name of the existing CS, because it typically
                    // contains text about axes including the axis that we just dropped.
                    cs = new DefaultEllipsoidalCS(singletonMap(EllipsoidalCS.NAME_KEY, "Ellipsoidal 2D"), xAxis, yAxis);
                }
                return new DefaultGeographicCRS(IdentifiedObjects.getProperties(crs), ((GeographicCRS) crs).getDatum(), cs);
            }
        }
        if (crs instanceof CompoundCRS) {
            final CompoundCRS cp = (CompoundCRS) crs;
            for (final CoordinateReferenceSystem c : cp.getComponents()) {
                final SingleCRS candidate = getHorizontalComponent(c);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first vertical coordinate reference system found in the given CRS, or {@code null} if there is none.
     * If the given CRS is already an instance of {@code VerticalCRS}, then this method returns it as-is.
     * Otherwise if the given CRS is compound, then this method searches for the first vertical component
     * in the order of the {@linkplain #getSingleComponents(CoordinateReferenceSystem) single components list}.
     *
     * {@section Height in a three-dimensional geographic CRS}
     * In ISO 19111 model, ellipsoidal heights are indissociable from geographic CRS because such heights
     * without their (<var>latitude</var>, <var>longitude</var>) locations make little sense. Consequently
     * a standard-conformant library should return {@code null} when asked for the {@code VerticalCRS}
     * component of a geographic CRS. This is what {@code getVerticalComponent(…)} does when the
     * {@code allowCreateEllipsoidal} argument is {@code false}.
     *
     * <p>However in some exceptional cases, handling ellipsoidal heights like any other kind of heights
     * may simplify the task. For example when computing <em>difference</em> between heights above the
     * same datum, the impact of ignoring locations may be smaller (but not necessarily canceled).
     * Orphan {@code VerticalCRS} may also be useful for information purpose like labeling a plot axis.
     * If the caller feels confident that ellipsoidal heights are safe for his task, he can set the
     * {@code allowCreateEllipsoidal} argument to {@code true}. In such case, this {@code getVerticalComponent(…)}
     * method will create a temporary {@code VerticalCRS} from the first three-dimensional {@code GeographicCRS}
     * <em>in last resort</em>, only if it can not find an existing {@code VerticalCRS} instance.
     * <strong>Note that this is not a valid CRS according ISO 19111</strong> — use with care.</p>
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @param  allowCreateEllipsoidal {@code true} for allowing the creation of orphan CRS for ellipsoidal heights.
     *         The recommended value is {@code false}.
     * @return The first vertical CRS, or {@code null} if none.
     *
     * @category information
     */
    public static VerticalCRS getVerticalComponent(final CoordinateReferenceSystem crs,
            final boolean allowCreateEllipsoidal)
    {
        if (crs instanceof VerticalCRS) {
            return (VerticalCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            final CompoundCRS cp = (CompoundCRS) crs;
            boolean a = false;
            do { // Executed at most twice.
                for (final CoordinateReferenceSystem c : cp.getComponents()) {
                    final VerticalCRS candidate = getVerticalComponent(c, a);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            } while ((a = !a) == allowCreateEllipsoidal);
        }
        if (allowCreateEllipsoidal && crs instanceof GeographicCRS) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            final int i = AxisDirections.indexOfColinear(cs, AxisDirection.UP);
            if (i >= 0) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                VerticalCRS c = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                if (!c.getCoordinateSystem().getAxis(0).equals(axis)) {
                    final Map<String,?> properties = IdentifiedObjects.getProperties(c);
                    c = new DefaultVerticalCRS(properties, c.getDatum(), new DefaultVerticalCS(properties, axis));
                }
                return c;
            }
        }
        return null;
    }

    /**
     * Returns the first temporal coordinate reference system found in the given CRS, or {@code null} if there is none.
     * If the given CRS is already an instance of {@code TemporalCRS}, then this method returns it as-is.
     * Otherwise if the given CRS is compound, then this method searches for the first temporal component
     * in the order of the {@linkplain #getSingleComponents(CoordinateReferenceSystem) single components list}.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The first temporal CRS, or {@code null} if none.
     *
     * @category information
     */
    public static TemporalCRS getTemporalComponent(final CoordinateReferenceSystem crs) {
        if (crs instanceof TemporalCRS) {
            return (TemporalCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            final CompoundCRS cp = (CompoundCRS) crs;
            for (final CoordinateReferenceSystem c : cp.getComponents()) {
                final TemporalCRS candidate = getTemporalComponent(c);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
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
     * <div class="note"><b>Example:</b>
     * Apache SIS allows 4-dimensional (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>)
     * coordinate reference system to be built in two different ways as shown below:
     *
     * <table class="compact" summary="Illustration of a compound CRS.">
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
     * </td></tr></table>
     *
     * This method guaranteed that the returned list is a flat one as shown on the right side.
     * Note that such flat lists are the only one allowed by ISO/OGC standards for compound CRS.
     * The hierarchical structure is an Apache SIS flexibility.</div>
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
                singles = new ArrayList<SingleCRS>(elements.size());
                ReferencingUtilities.getSingleComponents(elements, singles);
            }
        } else {
            // Intentional CassCastException here if the crs is not a SingleCRS.
            singles = Collections.singletonList((SingleCRS) crs);
        }
        return singles;
    }
}
