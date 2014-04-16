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
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultCompoundCS;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.ReferencingUtilities;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Workaround;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.internal.referencing.WKTUtilities.toFormattable;


/**
 * A CRS describing the position of points through two or more independent coordinate reference systems.
 * This class is often used for defining 4-dimensional (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>)
 * coordinate reference systems as an aggregation of simpler CRS. Below is two examples of such aggregations:
 *
 * <table class="compact" summary="Illustration of a compound CRS.">
 * <tr><th>Flat list</th><th>Hierarchical structure</th></tr>
 * <tr><td><blockquote>
 *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
 *   <code>  ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
 *   <code>  ├─VerticalCRS</code> — (<var>z</var>)<br>
 *   <code>  └─TemporalCRS</code> — (<var>t</var>)
 * </blockquote></td><td><blockquote>
 *   <code>CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>)<br>
 *   <code>  ├─CompoundCRS</code> — (<var>x</var>, <var>y</var>, <var>z</var>)<br>
 *   <code>  │   ├─ProjectedCRS</code> — (<var>x</var>, <var>y</var>)<br>
 *   <code>  │   └─VerticalCRS</code> — (<var>z</var>)<br>
 *   <code>  └─TemporalCRS</code> — (<var>t</var>)
 * </blockquote></td></tr>
 * </table>
 *
 * Strictly speaking, only the flat list on the left side is allowed by OGC/ISO specifications.
 * However Apache SIS relaxes this rule by allowing hierarchies as shown on the right side. This
 * flexibility allows SIS to preserve information about the (<var>x</var>,<var>y</var>,<var>z</var>)
 * part (e.g. the EPSG identifier) that would otherwise been lost. Users can obtain the list of their
 * choice by invoking {@link #getSingleComponents()} or {@link #getComponents()} respectively.
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and all {@link CoordinateReferenceSystem} instances given to the constructor are also immutable.
 * Unless otherwise noted in the javadoc, this condition holds if all components were created using only
 * SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@XmlType(name="CompoundCRSType")
@XmlRootElement(name = "CompoundCRS")
public class DefaultCompoundCRS extends AbstractCRS implements CompoundCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2656710314586929287L;

    /**
     * The coordinate reference systems in this compound CRS.
     * May be the same reference than {@link #singles}.
     */
    private final List<? extends CoordinateReferenceSystem> components;

    /**
     * A decomposition of the CRS list into the single elements.
     * Computed by {@link #computeSingleCRS(List)} on construction or deserialization.
     */
    private transient List<SingleCRS> singles;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultCompoundCRS() {
        components = null;
    }

    /**
     * Constructs a compound CRS from the given properties and CRS.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
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
     * @param components The sequence of coordinate reference systems making this compound CRS.
     */
    public DefaultCompoundCRS(final Map<String,?> properties, final CoordinateReferenceSystem... components) {
        super(properties, createCoordinateSystem(components));
        this.components = copy(Arrays.asList(components));
        // 'singles' is computed by the above method call.
    }

    /**
     * Returns a compound coordinate system for the specified array of CRS objects.
     * This method is a work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.7")
    private static CoordinateSystem createCoordinateSystem(final CoordinateReferenceSystem[] components) {
        ensureNonNull("components", components);
        if (components.length < 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.TooFewArguments_2, 2, components.length));
        }
        final CoordinateSystem[] cs = new CoordinateSystem[components.length];
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem crs = components[i];
            ensureNonNullElement("components", i, crs);
            cs[i] = crs.getCoordinateSystem();
        }
        return new DefaultCompoundCS(cs);
    }

    /**
     * Computes the {@link #singles} field from the given CRS list and returns {@code true}
     * if it has the same content.
     */
    private boolean computeSingleCRS(List<? extends CoordinateReferenceSystem> crs) {
        singles = new ArrayList<>(crs.size());
        final boolean identical = ReferencingUtilities.getSingleComponents(crs, singles);
        singles = UnmodifiableArrayList.wrap(singles.toArray(new SingleCRS[singles.size()]));
        return identical;
    }

    /**
     * Returns an unmodifiable copy of the given list. As a side effect, this method computes the
     * {@linkplain singles} list. If it appears that the list of {@code SingleCRS} is equal to the
     * given list, then it is returned in other to share the same list in both {@link #components}
     * and {@link #singles} references.
     *
     * <p><strong>WARNING:</strong> this method is invoked by constructors <em>before</em>
     * the {@linkplain #components} field is set. Do not use this field in this method.</p>
     */
    private List<? extends CoordinateReferenceSystem> copy(List<? extends CoordinateReferenceSystem> components) {
        if (computeSingleCRS(components)) {
            components = singles; // Shares the same list.
        } else {
            components = UnmodifiableArrayList.wrap(components.toArray(new CoordinateReferenceSystem[components.size()]));
        }
        return components;
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
    protected DefaultCompoundCRS(final CompoundCRS crs) {
        super(crs);
        if (crs instanceof DefaultCompoundCRS) {
            final DefaultCompoundCRS that = (DefaultCompoundCRS) crs;
            this.components = that.components;
            this.singles    = that.singles;
        } else {
            this.components = copy(crs.getComponents());
            // 'singles' is computed by the above method call.
        }
    }

    /**
     * Returns a SIS CRS implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCompoundCRS castOrCopy(final CompoundCRS object) {
        return (object == null) || (object instanceof DefaultCompoundCRS)
                ? (DefaultCompoundCRS) object : new DefaultCompoundCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code CompoundCRS.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code CompoundCRS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code CompoundCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends CompoundCRS> getInterface() {
        return CompoundCRS.class;
    }

    /**
     * Compound CRS do not have datum.
     */
    @Override
    final Datum getDatum() {
        return null;
    }

    /**
     * Returns the ordered list of coordinate reference systems.
     * This is the list of CRS given at construction time.
     * This list may contains other {@code CompoundCRS} instances, as described in class Javadoc.
     * For a flattened list of {@link SingleCRS} instances, see {@link #getSingleComponents()}.
     *
     * @return The coordinate reference systems as an unmodifiable list.
     */
    @Override
    @SuppressWarnings("unchecked") // We are safe if the list is read-only.
    public List<CoordinateReferenceSystem> getComponents() {
        return (List<CoordinateReferenceSystem>) components;
    }

    /**
     * Returns the ordered list of single coordinate reference systems. If this compound CRS contains
     * other compound CRS, then all of them are expanded in a sequence of {@code SingleCRS} objects.
     * See class Javadoc for more information.
     *
     * @return The single coordinate reference systems as an unmodifiable list.
     *
     * @see org.apache.sis.referencing.CRS#getSingleComponents(CoordinateReferenceSystem)
     */
    public List<SingleCRS> getSingleComponents() {
        return singles;
    }

    /**
     * Computes the single CRS list on deserialization.
     */
    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (components instanceof CheckedContainer<?>) {
            final Class<?> type = ((CheckedContainer<?>) components).getElementType();
            if (type == SingleCRS.class) {
                singles = (List<SingleCRS>) components;
                return;
            }
        }
        computeSingleCRS(components);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public synchronized DefaultCompoundCRS forConvention(final AxesConvention convention) {
        ensureNonNull("convention", convention);
        final Map<AxesConvention,AbstractCRS> derived = derived();
        DefaultCompoundCRS crs = (DefaultCompoundCRS) derived.get(convention);
        if (crs == null) {
            crs = this;
            boolean changed = false;
            final List<? extends CoordinateReferenceSystem> components =
                    (convention != AxesConvention.NORMALIZED) ? this.components : singles;
            final CoordinateReferenceSystem[] newComponents = new CoordinateReferenceSystem[components.size()];
            for (int i=0; i<newComponents.length; i++) {
                CoordinateReferenceSystem component = components.get(i);
                AbstractCRS m = castOrCopy(component);
                if (m != (m = m.forConvention(convention))) {
                    component = m;
                    changed = true;
                }
                newComponents[i] = component;
            }
            if (changed) {
                if (convention == AxesConvention.NORMALIZED) {
                    Arrays.sort(newComponents, SubTypes.BY_TYPE); // This array typically has less than 4 elements.
                }
                crs = new DefaultCompoundCRS(IdentifiedObjects.getProperties(this, IDENTIFIERS_KEY), newComponents);
            }
            derived.put(convention, crs);
        }
        return crs;
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    return components.equals(((DefaultCompoundCRS) object).components);
                }
                default: {
                    return deepEquals(getComponents(), ((CompoundCRS) object).getComponents(), mode);
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + 31*components.hashCode();
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code CompoundCRS[…]} element.
     *
     * {@section WKT validity}
     * The WKT version 2 format restricts compound CRS to the following components in that order:
     *
     * <ul>
     *   <li>A mandatory horizontal CRS (only one of two-dimensional {@code GeographicCRS}
     *       or {@code ProjectedCRS} or {@code EngineeringCRS}).</li>
     *   <li>Optionally followed by a {@code VerticalCRS} or a {@code ParametricCRS} (but not both).</li>
     *   <li>Optionally followed by a {@code TemporalCRS}.</li>
     * </ul>
     *
     * SIS does not check if this CRS is compliant with the above-cited restrictions.
     *
     * @return {@code "CompoundCRS"} (WKT 2) or {@code "Compd_CS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, null);
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = convention.majorVersion() == 1;
        for (final CoordinateReferenceSystem element :
                (isWKT1 || convention == Convention.INTERNAL) ? components : singles)
        {
            formatter.newLine();
            formatter.append(toFormattable(element));
        }
        formatter.newLine(); // For writing the ID[…] element on its own line.
        return isWKT1 ? "Compd_CS" : "CompoundCRS";
    }
}
