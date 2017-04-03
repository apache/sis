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
package org.apache.sis.referencing.cs;

import java.util.Map;
import javax.measure.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.measure.Units;


/**
 * A 3-dimensional coordinate system with one distance measured from the origin and two angular coordinates.
 * Not to be confused with an {@linkplain DefaultEllipsoidalCS ellipsoidal coordinate system}
 * based on an ellipsoid "degenerated" into a sphere.
 *
 * <table class="sis">
 * <caption>Permitted associations</caption>
 * <tr>
 *   <th>Used with CRS</th>
 *   <th>Permitted axis names</th>
 * </tr><tr>
 *   <td>{@linkplain org.apache.sis.referencing.crs.DefaultGeocentricCRS Geocentric}</td>
 *   <td>“Spherical Latitude”, “ Spherical Longitude”, “Geocentric Radius”</td>
 * </tr><tr>
 *   <td>{@linkplain org.apache.sis.referencing.crs.DefaultEngineeringCRS Engineering}</td>
 *   <td>unspecified</td>
 * </tr></table>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and the {@link CoordinateSystemAxis} instances given to the constructor are also immutable. Unless otherwise
 * noted in the javadoc, this condition holds if all components were created using only SIS factories and static
 * constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.4
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createSphericalCS(String)
 *
 * @since 0.4
 * @module
 */
@XmlType(name = "SphericalCSType")
@XmlRootElement(name = "SphericalCS")
public class DefaultSphericalCS extends AbstractCS implements SphericalCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 196295996465774477L;

    /**
     * Creates a new coordinate system from an arbitrary number of axes. This constructor is for
     * implementations of the {@link #createForAxes(Map, CoordinateSystemAxis[])} method only,
     * because it does not verify the number of axes.
     */
    private DefaultSphericalCS(final Map<String,?> properties, final CoordinateSystemAxis[] axes) {
        super(properties, axes);
    }

    /**
     * Constructs a three-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  axis0       the first  axis (e.g. “Spherical latitude”).
     * @param  axis1       the second axis (e.g. “Spherical longitude”).
     * @param  axis2       the third  axis (e.g. “Geocentric radius”).
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createSphericalCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    public DefaultSphericalCS(final Map<String,?>   properties,
                              final CoordinateSystemAxis axis0,
                              final CoordinateSystemAxis axis1,
                              final CoordinateSystemAxis axis2)
    {
        super(properties, axis0, axis1, axis2);
    }

    /**
     * Creates a new coordinate system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  cs  the coordinate system to copy.
     *
     * @see #castOrCopy(SphericalCS)
     */
    protected DefaultSphericalCS(final SphericalCS cs) {
        super(cs);
    }

    /**
     * Returns a SIS coordinate system implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSphericalCS castOrCopy(final SphericalCS object) {
        return (object == null) || (object instanceof DefaultSphericalCS)
                ? (DefaultSphericalCS) object : new DefaultSphericalCS(object);
    }

    /**
     * Returns {@code VALID} if the given argument values are allowed for this coordinate system,
     * or an {@code INVALID_*} error code otherwise. This method is invoked at construction time.
     *
     * <p>The current implementation rejects all directions that are known to be non-spatial.</p>
     */
    @Override
    final int validateAxis(final AxisDirection direction, final Unit<?> unit) {
        if (!AxisDirections.isSpatialOrUserDefined(direction, false)) {
            return INVALID_DIRECTION;
        }
        if (!Units.isAngular(unit) && !Units.isLinear(unit)) {
            return INVALID_UNIT;
        }
        return VALID;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code SphericalCS.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code SphericalCS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code SphericalCS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends SphericalCS> getInterface() {
        return SphericalCS.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultSphericalCS forConvention(final AxesConvention convention) {
        return (DefaultSphericalCS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate system with different axes.
     */
    @Override
    final AbstractCS createForAxes(final Map<String,?> properties, final CoordinateSystemAxis[] axes) {
        switch (axes.length) {
            case 2: return new DefaultPolarCS(properties, axes);
            case 3: return new DefaultSphericalCS(properties, axes);
            default: throw unexpectedDimension(properties, axes, 2);
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new coordinate system in which every attributes are set to a null or empty value.
     * <strong>This is not a valid object.</strong> This constructor is strictly reserved to JAXB,
     * which will assign values to the fields using reflexion.
     */
    private DefaultSphericalCS() {
    }
}
