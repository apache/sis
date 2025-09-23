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
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.io.wkt.Formatter;

// Specific to the geoapi-4.0 branch:
import org.apache.sis.referencing.datum.DefaultImageDatum;


/**
 * A 2-dimensional engineering coordinate reference system applied to locations in images.
 * Image coordinate reference systems are treated as a separate sub-type because a separate
 * user community exists for images with its own terms of reference.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultImageDatum Image}.<br>
 * <b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultAffineCS Affine}.
 * </p>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @deprecated The {@code ImageCRS} class has been removed in ISO 19111:2019.
 *             It is replaced by {@code EngineeringCRS}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.datum.DefaultImageDatum
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createImageCRS(String)
 *
 * @since 0.4
 */
@Deprecated(since="1.5", forRemoval=true)   // Actually to be moved to an internal package for GML and WKT purposes.
@XmlType(name = "ImageCRSType", propOrder = {
    "cartesianCS",
    "affineCS",
    "datum"
})
@XmlRootElement(name = "ImageCRS")
public final class DefaultImageCRS extends AbstractSingleCRS<DefaultImageDatum> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7222610270977351462L;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
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
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum.
     * @param  cs          the coordinate system.
     */
    public DefaultImageCRS(final Map<String,?> properties,
                           final DefaultImageDatum datum,
                           final AffineCS cs)
    {
        super(properties, DefaultImageDatum.class, datum, null, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    private DefaultImageCRS(final DefaultImageCRS original, final AbstractCS cs) {
        super(original, null, cs);
    }

    /**
     * Returns the datum.
     *
     * @return the datum.
     */
    @Override
    @XmlElement(name = "imageDatum", required = true)
    public DefaultImageDatum getDatum() {
        return super.getDatum();
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
     */
    @Override
    public AffineCS getCoordinateSystem() {
        return (AffineCS) super.getCoordinateSystem();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultImageCRS forConvention(final AxesConvention convention) {
        return (DefaultImageCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultImageCRS(this, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code ImageCRS[…]} element.
     *
     * <h4>Compatibility note</h4>
     * {@code ImageCRS} are defined in the WKT 2 specification only.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "ImageCRS"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        if (formatter.getConvention().majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return WKTKeywords.ImageCRS;
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
    private DefaultImageCRS() {
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
    private void setDatum(final DefaultImageDatum value) {
        setDatum("imageDatum", value);
    }

    /**
     * Used by JAXB only (invoked by reflection).
     * Only one of {@code getCartesianCS()} and {@link #getAffineCS()} can return a non-null value.
     *
     * <h4>Implementation note</h4>
     * The usual way to handle {@code <xs:choice>} with JAXB is to annotate a single method like below:
     *
     * {@snippet lang="java" :
     *     @Override
     *     @XmlElements({
     *       @XmlElement(name = "cartesianCS", type = DefaultCartesianCS.class),
     *       @XmlElement(name = "affineCS",    type = DefaultAffineCS.class)
     *     })
     *     public AffineCS getCoordinateSystem() {
     *         return super.getCoordinateSystem();
     *     }
     * }
     *
     * However, our attempts to apply this approach worked for {@code DefaultParameterValue} but not for this class:
     * for an unknown reason, the unmarshalled CS object is empty.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-166">SIS-166</a>
     */
    @XmlElement(name = "cartesianCS") private CartesianCS getCartesianCS() {return getCoordinateSystem(CartesianCS.class);}
    @XmlElement(name = "affineCS")    private AffineCS    getAffineCS()    {return getCoordinateSystem(AffineCS.class);}

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCartesianCS()
     */
    private void setCartesianCS(final CartesianCS cs) {setCoordinateSystem("cartesianCS", cs);}
    private void setAffineCS   (final AffineCS    cs) {setCoordinateSystem("affineCS",    cs);}
}
