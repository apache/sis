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
import java.util.Collections;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultCompoundCS;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.jaxb.referencing.SC_CRS;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;


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
 * <div class="section">Component order</div>
 * ISO 19162 restricts compound CRS to the following components in that order:
 * <ul>
 *   <li>A mandatory horizontal CRS (only one of two-dimensional {@code GeographicCRS} or {@code ProjectedCRS} or {@code EngineeringCRS}).</li>
 *   <li>Optionally followed by a {@code VerticalCRS} or a {@code ParametricCRS} (but not both).</li>
 *   <li>Optionally followed by a {@code TemporalCRS}.</li>
 * </ul>
 *
 * SIS currently does not enforce those restrictions. In particular:
 * <ul>
 *   <li>Components may appear in different order.
 *   <li>{@code VerticalCRS} + {@code TemporalCRS} (without horizontal component) is accepted.</li>
 *   <li>{@code GeocentricCRS} or three-dimensional {@code GeographicCRS} can be combined with {@code TemporalCRS}.</li>
 * </ul>
 *
 * However users are encouraged to follow ISO 19162 restriction for better portability.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and all {@link CoordinateReferenceSystem} instances given to the constructor are also immutable.
 * Unless otherwise noted in the javadoc, this condition holds if all components were created using only
 * SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createCompoundCRS(String)
 */
@XmlType(name = "CompoundCRSType")
@XmlRootElement(name = "CompoundCRS")
public class DefaultCompoundCRS extends AbstractCRS implements CompoundCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2656710314586929287L;

    /**
     * The coordinate reference systems in this compound CRS.
     * May be the same reference than {@link #singles}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at construction and unmarshalling time by {@link #setComponents(List)}</p>
     */
    private List<? extends CoordinateReferenceSystem> components;

    /**
     * A decomposition of the CRS list into the single elements.
     * Computed by {@link #setSingleComponents(List)} on construction, deserialization or unmarshalling.
     */
    private transient List<SingleCRS> singles;

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
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param components The sequence of coordinate reference systems making this compound CRS.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createCompoundCRS(Map, CoordinateReferenceSystem...)
     */
    public DefaultCompoundCRS(final Map<String,?> properties, final CoordinateReferenceSystem... components) {
        super(properties, createCoordinateSystem(properties, components));
        setComponents(Arrays.asList(components));
        // 'singles' is computed by the above method call.
    }

    /**
     * Returns a compound coordinate system for the specified array of CRS objects.
     *
     * @param  properties The properties given to the constructor, or {@code null} if unknown.
     * @param  components The CRS components, usually singles but not necessarily.
     * @return The coordinate system for the given components.
     */
    private static CoordinateSystem createCoordinateSystem(final Map<String,?> properties,
            final CoordinateReferenceSystem[] components)
    {
        ArgumentChecks.ensureNonNull("components", components);
        if (components.length < 2) {
            throw new IllegalArgumentException(Errors.getResources(properties).getString(
                    Errors.Keys.TooFewArguments_2, 2, components.length));
        }
        final CoordinateSystem[] cs = new CoordinateSystem[components.length];
        for (int i=0; i<components.length; i++) {
            final CoordinateReferenceSystem crs = components[i];
            ArgumentChecks.ensureNonNullElement("components", i, crs);
            cs[i] = crs.getCoordinateSystem();
        }
        return new DefaultCompoundCS(cs);
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
            setComponents(crs.getComponents());
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
     * Computes the {@link #components} and {@link #singles} fields from the given CRS list.
     * If the two lists have the same content, then the two fields will reference the same list.
     *
     * @see #getComponents()
     */
    @SuppressWarnings("SuspiciousToArrayCall")
    private void setComponents(final List<? extends CoordinateReferenceSystem> crs) {
        if (setSingleComponents(crs)) {
            components = singles; // Shares the same list.
        } else {
            components = UnmodifiableArrayList.wrap(crs.toArray(new CoordinateReferenceSystem[crs.size()]));
        }
    }

    /**
     * Returns the ordered list of single coordinate reference systems. If this compound CRS contains
     * other compound CRS, then all of them are flattened in a sequence of {@code SingleCRS} objects.
     * See class Javadoc for more information.
     *
     * @return The single coordinate reference systems as an unmodifiable list.
     *
     * @see org.apache.sis.referencing.CRS#getSingleComponents(CoordinateReferenceSystem)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<SingleCRS> getSingleComponents() {
        return singles;
    }

    /**
     * Computes the {@link #singles} field from the given CRS list and returns {@code true}
     * if the given list was already a list of single CRS.
     *
     * <p><strong>WARNING:</strong> this method is invoked by <em>before</em> the {@linkplain #components}
     * field is set. Do not use that field in this method.</p>
     *
     * @see #getSingleComponents()
     */
    private boolean setSingleComponents(final List<? extends CoordinateReferenceSystem> crs) {
        final List<SingleCRS> flattened = new ArrayList<SingleCRS>(crs.size());
        final boolean identical = ReferencingUtilities.getSingleComponents(crs, flattened);
        singles = UnmodifiableArrayList.wrap(flattened.toArray(new SingleCRS[flattened.size()]));
        return identical;
    }

    /**
     * Computes the single CRS list on deserialization.
     *
     * @param  in The input stream from which to deserialize a compound CRS.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final List<? extends CoordinateReferenceSystem> components = this.components;
        if (components instanceof CheckedContainer<?>) {
            final Class<?> type = ((CheckedContainer<?>) components).getElementType();
            if (type == SingleCRS.class) {
                singles = (List<SingleCRS>) components;
                return;
            }
        }
        setSingleComponents(components);
    }

    /**
     * Returns {@code true} if the sequence of single components is conform to the ISO 19162 restrictions.
     * The <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#111">WKT 2 specification at §16.1</a>
     * restricts {@code CompoundCRS} to the following components in that order:
     *
     * <ul>
     *   <li>A mandatory horizontal CRS (only one of two-dimensional {@code GeographicCRS}
     *       or {@code ProjectedCRS} or {@code EngineeringCRS}).</li>
     *   <li>Optionally followed by a {@code VerticalCRS} or a {@code ParametricCRS} (but not both).</li>
     *   <li>Optionally followed by a {@code TemporalCRS}.</li>
     * </ul>
     *
     * This method verifies the above criterion with the following flexibilities:
     *
     * <ul>
     *   <li>Accepts three-dimensional {@code GeodeticCRS} followed by a {@code TemporalCRS}.</li>
     * </ul>
     *
     * This method does not verify recursively if the component are themselves standard compliant.
     * In particular, this method does not verify if the geographic CRS uses (latitude, longitude)
     * axes order as requested by ISO 19162.
     *
     * <p>This method is not yet public because of the above-cited limitations: a {@code true} return
     * value is not a guarantee that the CRS is really standard-compliant.</p>
     *
     * @return {@code true} if this CRS is "standard" compliant, except for the above-cited limitations.
     */
    @SuppressWarnings("fallthrough")
    static boolean isStandardCompliant(final List<? extends CoordinateReferenceSystem> singles) {
        if (Containers.isNullOrEmpty(singles)) {
            return false;
        }
        /*
         * 0 if we expect a horizontal CRS: Geographic2D, projected or engineering.
         * 1 if we expect a vertical or parametric CRS (but not both).
         * 2 if we expect a temporal CRS.
         * 3 if we do not expect any other CRS.
         */
        int state = 0;
        for (final CoordinateReferenceSystem crs : singles) {
            switch (state) {
                case 0: {
                    if (crs instanceof GeodeticCRS || crs instanceof ProjectedCRS || crs instanceof EngineeringCRS) {
                        switch (crs.getCoordinateSystem().getDimension()) {
                            case 2: state = 1; continue;    // Next CRS can be vertical, parametric or temporal.
                            case 3: state = 2; continue;    // Next CRS can only be temporal.
                        }
                    }
                    return false;
                }
                case 1: {
                    if (crs instanceof VerticalCRS) {   // TODO: accept also ParametricCRS here.
                        state = 2; continue;    // Next CRS can only be temporal.
                    }
                    // Fallthrough (the current CRS may be temporal)
                }
                case 2: {
                    if (crs instanceof TemporalCRS) {
                        state = 3; continue;    // Do not expect any other CRS.
                    }
                    // Fallthrough (unexpected CRS).
                }
                default: {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the given convention is {@link AxesConvention#CONVENTIONALLY_ORIENTED} or
     * {@link AxesConvention#NORMALIZED}, then this method will also reorder the components
     * with horizontal CRS (geodetic or projected) first, then vertical CRS, then temporal CRS.</p>
     *
     * @return {@inheritDoc}
     */
    @Override
    public synchronized DefaultCompoundCRS forConvention(final AxesConvention convention) {
        ArgumentChecks.ensureNonNull("convention", convention);
        DefaultCompoundCRS crs = (DefaultCompoundCRS) getCached(convention);
        if (crs == null) {
            crs = this;
            boolean changed = false;
            final boolean reorderCRS = convention.ordinal() <= AxesConvention.CONVENTIONALLY_ORIENTED.ordinal();
            final List<? extends CoordinateReferenceSystem> components = reorderCRS ? singles : this.components;
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
                if (reorderCRS) {
                    Arrays.sort(newComponents, SubTypes.BY_TYPE); // This array typically has less than 4 elements.
                }
                crs = new DefaultCompoundCRS(IdentifiedObjects.getProperties(this, IDENTIFIERS_KEY), newComponents);
            }
            crs = (DefaultCompoundCRS) setCached(convention, crs);
        }
        return crs;
    }

    /**
     * Should never be invoked since we override {@link AbstractCRS#forConvention(AxesConvention)}.
     */
    @Override
    final AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        throw new AssertionError();
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
                    return Utilities.deepEquals(getComponents(), ((CompoundCRS) object).getComponents(), mode);
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
     * <div class="section">WKT validity</div>
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
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#110">WKT 2 specification §16</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, null);
        final Convention convention = formatter.getConvention();
        final List<? extends CoordinateReferenceSystem> crs;
        final boolean isStandardCompliant;
        final boolean isWKT1 = convention.majorVersion() == 1;
        if (isWKT1 || convention == Convention.INTERNAL) {
            crs = getComponents();
            isStandardCompliant = true;                     // WKT 1 does not put any restriction.
        } else {
            crs = getSingleComponents();
            isStandardCompliant = isStandardCompliant(crs);
        }
        for (final CoordinateReferenceSystem element : crs) {
            formatter.newLine();
            formatter.append(WKTUtilities.toFormattable(element));
        }
        formatter.newLine();                                // For writing the ID[…] element on its own line.
        if (!isStandardCompliant) {
            formatter.setInvalidWKT(this, null);
        }
        return isWKT1 ? WKTKeywords.Compd_CS : WKTKeywords.CompoundCRS;
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
     * Constructs a new object in which every attributes are set to a null or empty value.
     * <strong>This is not a valid object.</strong> This constructor is strictly reserved
     * to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultCompoundCRS() {
        components = Collections.emptyList();
        singles    = Collections.emptyList();
        /*
         * At least one component CRS is mandatory for SIS working. We do not verify their presence here
         * because the verification would have to be done in an 'afterMarshal(…)' method and throwing an
         * exception in that method causes the whole unmarshalling to fail.  But the SC_CRS adapter does
         * some verifications (indirectly, by testing for coordinate system existence).
         */
    }

    /**
     * Returns the CRS components to marshal. We use this private methods instead than annotating
     * {@link #getSingleComponents()} directly for two reasons:
     *
     * <ul>
     *   <li>Use array instead of {@code List} in order to force JAXB to invoke the setter method.
     *       This setter is needed for performing additional work after setting the list of CRS.</li>
     *
     *   <li>Allow a slightly asymmetry: marshal {@code SingleCRS} components for compliance with
     *       the standard, but accept the more generic {@code CoordinateReferenceSystem} elements
     *       at unmarshalling time.</li>
     * </ul>
     */
    @XmlJavaTypeAdapter(SC_CRS.class)
    @XmlElement(name = "componentReferenceSystem", required = true)
    private CoordinateReferenceSystem[] getXMLComponents() {
        final List<SingleCRS> crs = getSingleComponents();
        return crs.toArray(new CoordinateReferenceSystem[crs.size()]);
    }

    /**
     * Invoked by JAXB for setting the components of this compound CRS.
     */
    private void setXMLComponents(final CoordinateReferenceSystem[] crs) {
        components = setSingleComponents(Arrays.asList(crs)) ? singles : UnmodifiableArrayList.wrap(crs);
        setCoordinateSystem("coordinateSystem", createCoordinateSystem(null, crs));
    }
}
