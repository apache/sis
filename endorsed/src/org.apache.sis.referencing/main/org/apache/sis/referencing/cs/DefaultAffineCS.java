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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.measure.Unit;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.measure.Units;


/**
 * A 2- or 3-dimensional coordinate system with straight axes that are not necessarily orthogonal.
 *
 * <table class="sis">
 * <caption>Permitted associations</caption>
 * <tr>
 *   <th>Used with CRS</th>
 *   <th>Permitted axis names</th>
 * </tr><tr>
 *   <td>{@linkplain org.apache.sis.referencing.crs.DefaultEngineeringCRS Engineering}</td>
 *   <td>unspecified</td>
 * </tr></table>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and the {@link CoordinateSystemAxis} instances given to the constructor are also immutable. Unless otherwise
 * noted in the javadoc, this condition holds if all components were created using only SIS factories and static
 * constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.4
 */
@XmlType(name = "AffineCSType")
@XmlRootElement(name = "AffineCS")
public class DefaultAffineCS extends AbstractCS implements AffineCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7977674229369042440L;

    /**
     * Constructs a two-dimensional coordinate system from a set of properties.
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
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties  the properties to be given to the identified object.
     * @param axis0       the first axis.
     * @param axis1       the second axis.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createAffineCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    public DefaultAffineCS(final Map<String,?>   properties,
                           final CoordinateSystemAxis axis0,
                           final CoordinateSystemAxis axis1)
    {
        super(properties, axis0, axis1);
    }

    /**
     * Constructs a three-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the superclass constructor.
     *
     * @param properties  the properties to be given to the identified object.
     * @param axis0       the first axis.
     * @param axis1       the second axis.
     * @param axis2       the third axis.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createAffineCS(Map, CoordinateSystemAxis, CoordinateSystemAxis, CoordinateSystemAxis)
     */
    public DefaultAffineCS(final Map<String,?>   properties,
                           final CoordinateSystemAxis axis0,
                           final CoordinateSystemAxis axis1,
                           final CoordinateSystemAxis axis2)
    {
        super(properties, axis0, axis1, axis2);
    }

    /**
     * Creates a new CS derived from the specified one, but with different axis order or unit.
     *
     * @see #createForAxes(String, CoordinateSystemAxis[])
     */
    DefaultAffineCS(DefaultAffineCS original, String name, CoordinateSystemAxis[] axes) {
        super(original, name, axes);
    }

    /**
     * Creates a new coordinate system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  original  the coordinate system to copy.
     *
     * @see #castOrCopy(AffineCS)
     */
    protected DefaultAffineCS(final AffineCS original) {
        super(original);
    }

    /**
     * Returns a SIS coordinate system implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * <p>This method checks for the {@link CartesianCS} sub-interface. If that interface is found,
     * then this method delegates to the corresponding {@code castOrCopy} static method.</p>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultAffineCS castOrCopy(final AffineCS object) {
        if (object instanceof CartesianCS) {
            return DefaultCartesianCS.castOrCopy((CartesianCS) object);
        }
        return (object == null) || (object instanceof DefaultAffineCS)
                ? (DefaultAffineCS) object : new DefaultAffineCS(object);
    }

    /**
     * Returns {@code VALID} if the given argument values are allowed for this coordinate system,
     * or an {@code INVALID_*} error code otherwise. This method is invoked at construction time.
     *
     * <p>The current implementation rejects all directions that are known to be non-spatial, not
     * for grids and not for display. We conservatively accept all others axis directions because
     * some of them are created from strings like "South along 90°E".</p>
     *
     * <p>This method accepts linear units, but also accepts the dimensionless units because the
     * later are used for grid and display coordinates.</p>
     */
    @Override
    final int validateAxis(final AxisDirection direction, final Unit<?> unit) {
        if (!AxisDirections.isSpatialOrUserDefined(direction, true)) {
            return INVALID_DIRECTION;
        }
        if (!Units.isLinear(unit) && !Units.UNITY.equals(unit) && !Units.PIXEL.equals(unit)) {
            return INVALID_UNIT;
        }
        return VALID;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code AffineCS.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the affine coordinate system interface implemented by this class.
     */
    @Override
    public Class<? extends AffineCS> getInterface() {
        return AffineCS.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultAffineCS forConvention(final AxesConvention convention) {
        return (DefaultAffineCS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate system with different axes.
     * This method shall be overridden by all {@code AffineCS} subclasses in this package.
     */
    @Override
    AbstractCS createForAxes(final String name, final CoordinateSystemAxis[] axes) {
        switch (axes.length) {
            case 2: // Fall through
            case 3: return new DefaultAffineCS(this, name, axes);
            default: throw unexpectedDimension(axes, 2, 3);
        }
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs a new coordinate system in which every attributes are set to a null or empty value.
     * <strong>This is not a valid object.</strong> This constructor is strictly reserved to JAXB,
     * which will assign values to the fields using reflection.
     */
    DefaultAffineCS() {
    }
}
