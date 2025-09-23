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
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.measure.Units;


/**
 * A 1-dimensional coordinate system for heights or depths of points.
 * OGC/ISO do not provide an exact definition, since the complexity of vertical CS fall outside the scope of ISO 19111.
 * Some examples of vertical coordinate systems are:
 *
 * <ul>
 *   <li>Height in metres from a surface dependent on the Earth's gravity field</li>
 *   <li>Height or depth represented by an atmospheric or water pressure measurement.</li>
 *   <li>Depth represented by the time needed for an echo signal to travel.</li>
 *   <li>"Sigma-level depth" (used in oceanography) as a dimensionless quantity.</li>
 * </ul>
 *
 * The above examples show the variety of units of measurement which can be associated to vertical positions.
 *
 * <table class="sis">
 * <caption>Permitted associations</caption>
 * <tr>
 *   <th>Used with CRS</th>
 *   <th>Permitted axis names</th>
 * </tr><tr>
 *   <td>{@linkplain org.apache.sis.referencing.crs.DefaultVerticalCRS Vertical}</td>
 *   <td>“Gravity-related height” or “Depth”</td>
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
 *
 * @see org.apache.sis.referencing.crs.DefaultVerticalCRS
 * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createVerticalCS(String)
 *
 * @since 0.4
 */
@XmlType(name = "VerticalCSType")
@XmlRootElement(name = "VerticalCS")
public class DefaultVerticalCS extends AbstractCS implements VerticalCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1201155778896630499L;

    /**
     * Constructs a coordinate system from a set of properties.
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
     * @param  properties  the properties to be given to the identified object.
     * @param  axis        the single axis (e.g. “height” or “depth”).
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createVerticalCS(Map, CoordinateSystemAxis)
     */
    public DefaultVerticalCS(final Map<String,?> properties, final CoordinateSystemAxis axis) {
        super(properties, axis);
    }

    /**
     * Creates a new CS derived from the specified one, but with different axis order or unit.
     *
     * @see #createForAxes(String, CoordinateSystemAxis[])
     */
    private DefaultVerticalCS(DefaultVerticalCS original, String name, CoordinateSystemAxis[] axes) {
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
     * @see #castOrCopy(VerticalCS)
     */
    protected DefaultVerticalCS(final VerticalCS original) {
        super(original);
    }

    /**
     * Returns a SIS coordinate system implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVerticalCS castOrCopy(final VerticalCS object) {
        return (object == null) || (object instanceof DefaultVerticalCS)
                ? (DefaultVerticalCS) object : new DefaultVerticalCS(object);
    }

    /**
     * Returns {@code VALID} if the given argument values are allowed for this coordinate system,
     * or an {@code INVALID_*} error code otherwise. This method is invoked at construction time.
     * The current implementation accepts only temporal directions (i.e. {@link AxisDirection#UP}
     * and {@link AxisDirection#DOWN}).
     */
    @Override
    final int validateAxis(final AxisDirection direction, Unit<?> unit) {
        if (!AxisDirections.isVertical(direction)) {
            return INVALID_DIRECTION;
        }
        unit = unit.getSystemUnit();
        if (unit.equals(Units.METRE)   ||       // Most usual case.
            unit.equals(Units.PASCAL)  ||       // Height or depth estimated by the atmospheric or ocean pressure.
            unit.equals(Units.SECOND)  ||       // Depth estimated by the time needed for an echo to travel.
            unit.equals(Units.UNITY))           // Sigma-level (percentage from sea surface to ocean floor).
        {
            return VALID;
        }
        return INVALID_UNIT;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code VerticalCS.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code VerticalCS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with
     * their own set of interfaces.
     *
     * @return {@code VerticalCS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends VerticalCS> getInterface() {
        return VerticalCS.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultVerticalCS forConvention(final AxesConvention convention) {
        return (DefaultVerticalCS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate system with different axes.
     */
    @Override
    final AbstractCS createForAxes(final String name, final CoordinateSystemAxis[] axes) {
        switch (axes.length) {
            case 1: return new DefaultVerticalCS(this, name, axes);
            default: throw unexpectedDimension(axes, 1, 1);
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
    private DefaultVerticalCS() {
    }
}
