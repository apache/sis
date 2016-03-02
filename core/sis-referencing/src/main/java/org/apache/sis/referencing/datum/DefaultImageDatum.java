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
package org.apache.sis.referencing.datum;

import java.util.Map;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.datum.ImageDatum;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Defines the origin of an image coordinate reference system. An image datum is used in a local
 * context only. For an image datum, the anchor point is usually either the centre of the image
 * or the corner of the image.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.referencing.crs.DefaultImageCRS
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createImageDatum(String)
 */
@XmlType(name = "ImageDatumType")
@XmlRootElement(name = "ImageDatum")
public class DefaultImageDatum extends AbstractDatum implements ImageDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4304193511244150936L;

    /**
     * Specification of the way the image grid is associated with the image data attributes.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setPixelInCell(PixelInCell)}</p>
     *
     * @see #getPixelInCell()
     */
    private PixelInCell pixelInCell;

    /**
     * Creates an image datum from the given properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
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
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#ANCHOR_POINT_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getAnchorPoint()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#REALIZATION_EPOCH_KEY}</td>
     *     <td>{@link java.util.Date}</td>
     *     <td>{@link #getRealizationEpoch()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties  The properties to be given to the identified object.
     * @param pixelInCell The way the image grid is associated with the image data attributes.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createImageDatum(Map, PixelInCell)
     */
    public DefaultImageDatum(final Map<String,?> properties, final PixelInCell pixelInCell) {
        super(properties);
        this.pixelInCell = pixelInCell;
        ensureNonNull("pixelInCell", pixelInCell);
    }

    /**
     * Creates a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     *
     * @see #castOrCopy(ImageDatum)
     */
    protected DefaultImageDatum(final ImageDatum datum) {
        super(datum);
        pixelInCell = datum.getPixelInCell();
    }

    /**
     * Returns a SIS datum implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultImageDatum castOrCopy(final ImageDatum object) {
        return (object == null) || (object instanceof DefaultImageDatum)
                ? (DefaultImageDatum) object : new DefaultImageDatum(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ImageDatum.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ImageDatum}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with
     * their own set of interfaces.</div>
     *
     * @return {@code ImageDatum.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ImageDatum> getInterface() {
        return ImageDatum.class;
    }

    /**
     * Specification of the way the image grid is associated with the image data attributes.
     *
     * @return The way image grid is associated with image data attributes.
     */
    @Override
    @XmlElement(required = true)
    public PixelInCell getPixelInCell() {
        return pixelInCell;
    }

    /**
     * Compares this datum with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                return Objects.equals(pixelInCell, ((DefaultImageDatum) object).pixelInCell);
            }
            default: {
                return Objects.equals(getPixelInCell(), ((ImageDatum) object).getPixelInCell());
            }
        }
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
        return super.computeHashCode() + Objects.hashCode(pixelInCell);
    }

    /**
     * Formats this datum as a <cite>Well Known Text</cite> {@code ImageDatum[…]} element.
     *
     * <div class="note"><b>Compatibility note:</b>
     * {@code ImageDatum} is defined in the WKT 2 specification only.</div>
     *
     * @return {@code "ImageDatum"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#81">WKT 2 specification §12.2</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        final Convention convention = formatter.getConvention();
        if (convention == Convention.INTERNAL) {
            formatter.append(getPixelInCell());         // This is an extension compared to ISO 19162.
        } else if (convention.majorVersion() == 1) {
            formatter.setInvalidWKT(this, null);
        }
        return formatter.shortOrLong(WKTKeywords.IDatum, WKTKeywords.ImageDatum);
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
     * Constructs a new datum in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultImageDatum() {
    }

    /**
     * Invoked by JAXB only at unmarshalling time.
     *
     * @see #getPixelInCell()
     */
    private void setPixelInCell(final PixelInCell value) {
        if (pixelInCell == null) {
            pixelInCell = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultImageDatum.class, "setPixelInCell", "pixelInCell");
        }
    }
}
