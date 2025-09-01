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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatum;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.io.wkt.Formatter;

// Specific to the main branch:
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;


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
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.datum.DefaultVerticalDatum
 * @see org.apache.sis.referencing.cs.DefaultVerticalCS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createVerticalCRS(String)
 *
 * @since 0.4
 */
@XmlType(name = "VerticalCRSType", propOrder = {
    "coordinateSystem",
    "datum"
})
@XmlRootElement(name = "VerticalCRS")
public class DefaultVerticalCRS extends AbstractSingleCRS<VerticalDatum> implements VerticalCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5807645386129942811L;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
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
     *     <td>"domains"</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createVerticalCRS(Map, VerticalDatum, DefaultDatumEnsemble, VerticalCS)
     *
     * @since 1.5
     */
    public DefaultVerticalCRS(final Map<String,?> properties,
                              final VerticalDatum datum,
                              final DefaultDatumEnsemble<VerticalDatum> ensemble,
                              final VerticalCS cs)
    {
        super(properties, VerticalDatum.class, datum, ensemble, cs);
        checkDimension(1, 1, cs);
    }

    /**
     * @deprecated A {@code DefaultDatumEnsemble} argument has been added.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public DefaultVerticalCRS(final Map<String,?> properties,
                              final VerticalDatum datum,
                              final VerticalCS cs)
    {
        this(properties, datum, null, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    private DefaultVerticalCRS(final DefaultVerticalCRS original, final AbstractCS cs) {
        super(original, null, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     *
     * @see #castOrCopy(VerticalCRS)
     */
    protected DefaultVerticalCRS(final VerticalCRS crs) {
        super(crs);
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values as the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code VerticalCRS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code VerticalCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends VerticalCRS> getInterface() {
        return VerticalCRS.class;
    }

    /**
     * Returns an identification of a particular reference level surface used as a zero-height surface.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     *
     * @return the vertical datum, or {@code null} if this <abbr>CRS</abbr> is related to
     *         an object identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     */
    @Override
    @XmlElement(name = "verticalDatum", required = true)
    public VerticalDatum getDatum() {
        return super.getDatum();
    }

    /**
     * Returns a collection of datums which, for low accuracy requirements,
     * may be considered to be insignificantly different from each other.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a single {@linkplain #getDatum() datum}.
     *
     * <div class="warning"><b>Warning:</b> in a future SIS version, the return type may
     * be changed to the {@code org.opengis.referencing.datum.DatumEnsemble} interface.
     * This change is pending GeoAPI revision.</div>
     *
     * @return the datum ensemble, or {@code null} if this <abbr>CRS</abbr> is related
     *         to an object identified only by a single {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DefaultDatumEnsemble<VerticalDatum> getDatumEnsemble() {
        return super.getDatumEnsemble();
    }

    /**
     * Returns the datum or a view of the ensemble as a datum.
     * The {@code legacy} argument tells whether this method is invoked for formatting in a legacy <abbr>WKT</abbr> format.
     */
    @Override
    final VerticalDatum getDatumOrEnsemble(final boolean legacy) {
        return legacy ? DatumOrEnsemble.asDatum(this) : getDatum();
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
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
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultVerticalCRS(this, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code VerticalCRS[…]} element.
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
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
        setDatum("verticalDatum", value);
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
