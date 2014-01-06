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
package org.apache.sis.referencing.crs;

import java.util.Map;
import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * Abstract coordinate reference system, usually defined by a coordinate system and a datum.
 * {@code AbstractCRS} can have an arbitrary number of dimensions. The actual dimension of a
 * given instance can be determined as below:
 *
 * {@preformat java
 *   int dimension = crs.getCoordinateSystem().getDimension();
 * }
 *
 * However most subclasses restrict the allowed number of dimensions.
 *
 * {@section Instantiation}
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 * An exception to this rule may occur when it is not possible to identify the exact CRS type.
 *
 * {@section Immutability and thread safety}
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code CoordinateReferenceSystem} instances
 * created using only SIS factories and static constants can be shared by many objects and passed between threads
 * without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see AbstractCS
 * @see org.apache.sis.referencing.datum.AbstractDatum
 */
@XmlType(name="AbstractCRSType")
@XmlRootElement(name = "AbstractCRS")
@XmlSeeAlso({
    DefaultGeodeticCRS.class,
    DefaultVerticalCRS.class,
    DefaultTemporalCRS.class,
    DefaultEngineeringCRS.class,
    DefaultImageCRS.class,
    DefaultCompoundCRS.class
})
public class AbstractCRS extends AbstractReferenceSystem implements CoordinateReferenceSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7433284548909530047L;

    /**
     * The coordinate system.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setCoordinateSystem(CoordinateSystem)}</p>
     *
     * @see #getCoordinateSystem()
     */
    private CoordinateSystem coordinateSystem;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractCRS() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
    }

    /**
     * Creates a coordinate reference system from the given properties and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
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
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param cs The coordinate system.
     */
    public AbstractCRS(final Map<String,?> properties, final CoordinateSystem cs) {
        super(properties);
        ensureNonNull("cs", cs);
        coordinateSystem = cs;
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param crs The coordinate reference system to copy.
     */
    protected AbstractCRS(final CoordinateReferenceSystem crs) {
        super(crs);
        coordinateSystem = crs.getCoordinateSystem();
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code CoordinateReferenceSystem.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateReferenceSystem> getInterface() {
        return CoordinateReferenceSystem.class;
    }

    /**
     * Returns the datum, or {@code null} if none.
     *
     * This property does not exist in {@code CoordinateReferenceSystem} interface — it is defined in the
     * {@link SingleCRS} sub-interface instead. But Apache SIS does not define an {@code AbstractSingleCRS} class
     * in order to simplify our class hierarchy, so we provide a datum getter in this class has a hidden property.
     * Subclasses implementing {@code SingleCRS} (basically all SIS subclasses except {@link DefaultCompoundCRS})
     * will override this method with public access and more specific return type.
     */
    Datum getDatum() {
        return null;
    }

    /**
     * Returns the coordinate system.
     *
     * @return The coordinate system.
     */
    @Override
    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Returns the coordinate system if it is of the given type, or {@code null} otherwise.
     * This method is invoked by subclasses that can accept more than one CS type.
     */
    @SuppressWarnings("unchecked")
    final <T extends CoordinateSystem> T getCoordinateSystem(final Class<T> type) {
        final CoordinateSystem cs = coordinateSystem;
        if (type.isInstance(cs)) {
            // Special case for AfficeCS: must ensure that the cs is not the CartesianCS subtype.
            if (type != AffineCS.class || !(cs instanceof CartesianCS)) {
                return (T) cs;
            }
        }
        return null;
    }

    /**
     * Sets the coordinate system to the given value. This method is invoked only by JAXB at
     * unmarshalling time and can be invoked only if the coordinate system has never been set.
     *
     * {@note It was easy to put JAXB annotations directly on datum fields in subclasses because each CRS
     *        type can be associated to only one datum type. But we do not have this convenience for
     *        coordinate systems, where the same CRS may accept more than one kind of CS.
     *        In GML, the different kinds of CS are marshalled in different XML elements.}
     *
     * @param  name The property name, used only in case of error message to format.
     * @throws IllegalStateException If the coordinate system has already been set.
     */
    final void setCoordinateSystem(final String name, final CoordinateSystem cs) {
        if (cs != null && ReferencingUtilities.canSetProperty(name, coordinateSystem != null)) {
            coordinateSystem = cs;
        }
    }

    /**
     * Returns the unit used for all axis, or {@code null} if not all axis uses the same unit.
     * This method is often used for formatting according  Well Know Text (WKT) version 1.
     */
    final Unit<?> getUnit() {
        return ReferencingUtilities.getUnit(coordinateSystem);
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties are
     * compared including the {@linkplain #getDomainOfValidity() domain of validity} and
     * the {@linkplain #getScope() scope}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final Datum datum = getDatum();
            switch (mode) {
                case STRICT: {
                    final AbstractCRS that = (AbstractCRS) object;
                    return Objects.equals(datum, that.getDatum()) &&
                           Objects.equals(coordinateSystem, that.coordinateSystem);
                }
                default: {
                    return deepEquals(datum,
                                      (object instanceof SingleCRS) ? ((SingleCRS) object).getDatum() : null, mode) &&
                           deepEquals(getCoordinateSystem(),
                                      ((CoordinateReferenceSystem) object).getCoordinateSystem(), mode);
                }
            }
        }
        return false;
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(getDatum(), coordinateSystem);
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT)</a> element.
     * The default implementation writes the following elements:
     *
     * <ul>
     *   <li>The datum, if any.</li>
     *   <li>The unit if all axes use the same unit. Otherwise the unit is omitted and the WKT format
     *       is {@linkplain Formatter#setInvalidWKT(IdentifiedObject) flagged as invalid}.</li>
     *   <li>All {@linkplain #getCoordinateSystem() coordinate system}'s axis.</li>
     * </ul>
     *
     * @param  formatter The formatter to use.
     * @return The name of the WKT element type (e.g. {@code "GEOGCS"}).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatDefaultWKT(formatter);
        // Will declares the WKT as invalid.
        return super.formatTo(formatter);
    }

    /**
     * Default implementation of {@link #formatTo(Formatter)}.
     * For {@link DefaultEngineeringCRS} and {@link DefaultVerticalCRS} use only.
     */
    final void formatDefaultWKT(final Formatter formatter) {
        formatter.append(getDatum());
        final Unit<?> unit = getUnit();
        formatter.append(unit);
        final CoordinateSystem cs = coordinateSystem;
        final int dimension = cs.getDimension();
        for (int i=0; i<dimension; i++) {
            formatter.append(cs.getAxis(i));
        }
        if (unit == null) {
            formatter.setInvalidWKT(cs);
        }
    }
}
