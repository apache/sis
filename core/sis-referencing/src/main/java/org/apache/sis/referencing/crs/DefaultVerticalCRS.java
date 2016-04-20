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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A 1-dimensional coordinate reference system used for recording heights or depths.
 * Vertical CRSs make use of the direction of gravity to define the concept of height or depth,
 * but the relationship with gravity may not be straightforward.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultVerticalDatum Vertical}.<br>
 * <b>Used with coordinate system type:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultVerticalCS Vertical}.
 * </p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
 * @see org.apache.sis.referencing.cs.DefaultVerticalCS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createVerticalCRS(String)
 */
@XmlType(name = "VerticalCRSType", propOrder = {
    "coordinateSystem",
    "datum"
})
@XmlRootElement(name = "VerticalCRS")
public class DefaultVerticalCRS extends AbstractCRS implements VerticalCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3565878468719941800L;

    /**
     * The datum.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDatum(VerticalDatum)}</p>
     *
     * @see #getDatum()
     */
    private VerticalDatum datum;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
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
     * @param datum The datum.
     * @param cs The coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createVerticalCRS(Map, VerticalDatum, VerticalCS)
     */
    public DefaultVerticalCRS(final Map<String,?> properties,
                              final VerticalDatum datum,
                              final VerticalCS    cs)
    {
        super(properties, cs);
        ensureNonNull("datum", datum);
        this.datum = datum;
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param crs The coordinate reference system to copy.
     *
     * @see #castOrCopy(VerticalCRS)
     */
    protected DefaultVerticalCRS(final VerticalCRS crs) {
        super(crs);
        datum = crs.getDatum();
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values than the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultVerticalCRS castOrCopy(final VerticalCRS object) {
        return (object == null) || (object instanceof DefaultVerticalCRS)
                ? (DefaultVerticalCRS) object : new DefaultVerticalCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code VerticalCRS.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code VerticalCRS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code VerticalCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends VerticalCRS> getInterface() {
        return VerticalCRS.class;
    }

    /**
     * Returns the datum.
     *
     * @return The datum.
     */
    @Override
    @XmlElement(name = "verticalDatum", required = true)
    public VerticalDatum getDatum() {
        return datum;
    }

    /**
     * Returns the coordinate system.
     *
     * @return The coordinate system.
     */
    @Override
    @XmlElement(name = "verticalCS", required = true)
    public VerticalCS getCoordinateSystem() {
        return (VerticalCS) super.getCoordinateSystem();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultVerticalCRS forConvention(final AxesConvention convention) {
        return (DefaultVerticalCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new DefaultVerticalCRS(properties, datum, (VerticalCS) cs);
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code VerticalCRS[…]} element.
     *
     * @return {@code "VerticalCRS"} (WKT 2) or {@code "Vert_CS"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#69">WKT 2 specification §10</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return (formatter.getConvention().majorVersion() == 1) ? WKTKeywords.Vert_CS
               : isBaseCRS(formatter) ? WKTKeywords.BaseVertCRS
                 : formatter.shortOrLong(WKTKeywords.VertCRS, WKTKeywords.VerticalCRS);
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultVerticalCRS() {
        /*
         * The datum and the coordinate system are mandatory for SIS working. We do not verify their presence
         * here because the verification would have to be done in an 'afterMarshal(…)' method and throwing an
         * exception in that method causes the whole unmarshalling to fail.  But the SC_CRS adapter does some
         * verifications.
         */
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getDatum()
     */
    private void setDatum(final VerticalDatum value) {
        if (datum == null) {
            datum = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultVerticalCRS.class, "setDatum", "verticalDatum");
        }
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCoordinateSystem()
     */
    private void setCoordinateSystem(final VerticalCS cs) {
        setCoordinateSystem("verticalCS", cs);
    }
}
