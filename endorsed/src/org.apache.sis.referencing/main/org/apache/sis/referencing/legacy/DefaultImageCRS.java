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
package org.apache.sis.referencing.legacy;

import java.util.Map;
import java.util.Objects;
import java.lang.reflect.Field;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.crs.SingleCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.internal.shared.NilReferencingObject;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.metadata.internal.shared.ImplementationHelper;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.referencing.datum.ImageDatum;


/**
 * A 2-dimensional engineering coordinate reference system applied to locations in images.
 * Image coordinate reference systems are treated as a separate sub-type because a separate
 * user community exists for images with its own terms of reference.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain DefaultImageDatum Image}.<br>
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlType(name = "ImageCRSType", propOrder = {
    "cartesianCS",
    "affineCS",
    "datum"
})
@XmlRootElement(name = "ImageCRS")
@SuppressWarnings("deprecation")
public final class DefaultImageCRS extends AbstractCRS implements ImageCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7222610270977351462L;

    /**
     * The datum, or {@code null} if the <abbr>CRS</abbr> is associated only to a datum ensemble.
     *
     * <p><b>Consider this field as final!</b>
     * This field is non-final only for construction convenience and for unmarshalling.</p>
     *
     * @see #getDatum()
     */
    @SuppressWarnings("serial")
    private ImageDatum datum;

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
     * @param  datum       the datum.
     * @param  cs          the coordinate system.
     */
    public DefaultImageCRS(final Map<String,?> properties,
                           final ImageDatum    datum,
                           final AffineCS      cs)
    {
        super(properties, cs);
        this.datum = Objects.requireNonNull(datum);
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
     * @see #castOrCopy(ImageCRS)
     */
    protected DefaultImageCRS(final ImageCRS crs) {
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
    public static DefaultImageCRS castOrCopy(final ImageCRS object) {
        return (object == null) || (object instanceof DefaultImageCRS)
                ? (DefaultImageCRS) object : new DefaultImageCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     *
     * @return the coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends ImageCRS> getInterface() {
        return ImageCRS.class;
    }

    /**
     * Returns the datum.
     *
     * @return the datum.
     */
    @Override
    @XmlElement(name = "imageDatum", required = true)
    public ImageDatum getDatum() {
        return datum;
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

    /**
     * Compares this coordinate reference system with the specified object for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    whether to perform a strict or lenient comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, ComparisonMode mode) {
        if (super.equals(object, mode)) {
            if (mode == ComparisonMode.STRICT) {
                final var that = (DefaultImageCRS) object;
                return Objects.equals(datum, that.datum);
            }
            final var that = (SingleCRS) object;
            return Utilities.deepEquals(getDatum(), that.getDatum(), mode);
        }
        return false;
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(datum);
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
    @SuppressWarnings("unused")
    private DefaultImageCRS() {
        super(Map.of(NAME_KEY, NilReferencingObject.UNNAMED), CommonCRS.Engineering.DISPLAY.crs().getCoordinateSystem());
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
    @SuppressWarnings("unused")
    private void setDatum(final ImageDatum value) {
        if (datum == null) {
            datum = value;
        } else {
            ImplementationHelper.propertyAlreadySet(getClass(), "setDatum", "imageDatum");
        }
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
    @SuppressWarnings("unused")
    @XmlElement(name = "cartesianCS")
    private CartesianCS getCartesianCS() {
        final AffineCS cs = getCoordinateSystem();
        return (cs instanceof CartesianCS) ? (CartesianCS) cs : null;
    }

    @SuppressWarnings("unused")
    @XmlElement(name = "affineCS")
    private AffineCS getAffineCS() {
        final AffineCS cs = getCoordinateSystem();
        return (cs instanceof CartesianCS) ? null : cs;
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCartesianCS()
     */
    @SuppressWarnings("unused")
    private void setCartesianCS(final CartesianCS cs) {
        setAffineCS(cs);
    }

    @SuppressWarnings("unused")
    private void setAffineCS(final AffineCS cs) {
        try {
            Field coordinateSystem = AbstractCRS.class.getDeclaredField("coordinateSystem");
            coordinateSystem.setAccessible(true);
            coordinateSystem.set(this, cs);
        } catch (ReflectiveOperationException e) {
            throw new BackingStoreException(e);
        }
    }
}
