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
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.datum.ImageDatum;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import org.apache.sis.internal.jdk7.Objects;


/**
 * Defines the origin of an image coordinate reference system. An image datum is used in a local
 * context only. For an image datum, the anchor point is usually either the centre of the image
 * or the corner of the image.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@Immutable
@XmlType(name = "ImageDatumType")
@XmlRootElement(name = "ImageDatum")
public class DefaultImageDatum extends AbstractDatum implements ImageDatum {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4304193511244150936L;

    /**
     * Specification of the way the image grid is associated with the image data attributes.
     */
    private final PixelInCell pixelInCell;

    /**
     * Constructs a new datum with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param datum The datum to copy.
     */
    public DefaultImageDatum(final ImageDatum datum) {
        super(datum);
        pixelInCell = datum.getPixelInCell();
    }

    /**
     * Constructs an image datum from a name.
     *
     * @param name The datum name.
     * @param pixelInCell the way the image grid is associated with the image data attributes.
     */
    public DefaultImageDatum(final String name, final PixelInCell pixelInCell) {
        this(Collections.singletonMap(NAME_KEY, name), pixelInCell);
    }

    /**
     * Constructs an image datum from a set of properties. The properties map is given
     * unchanged to the {@linkplain AbstractDatum#AbstractDatum(Map) super-class constructor}.
     *
     * @param properties  Set of properties. Should contains at least {@code "name"}.
     * @param pixelInCell the way the image grid is associated with the image data attributes.
     */
    public DefaultImageDatum(final Map<String,?> properties, final PixelInCell pixelInCell) {
        super(properties);
        this.pixelInCell = pixelInCell;
        ensureNonNull("pixelInCell", pixelInCell);
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
     * Specification of the way the image grid is associated with the image data attributes.
     *
     * @return The way image grid is associated with image data attributes.
     */
    @Override
    public PixelInCell getPixelInCell() {
        return pixelInCell;
    }

    /**
     * Compares this datum with the specified object for equality.
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
                    final DefaultImageDatum that = (DefaultImageDatum) object;
                    return Objects.equals(this.pixelInCell, that.pixelInCell);
                }
                default: {
                    if (!(object instanceof ImageDatum)) break;
                    final ImageDatum that = (ImageDatum) object;
                    return Objects.equals(getPixelInCell(), that.getPixelInCell());
                }
            }
        }
        return false;
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        /*
         * The "^ (int) serialVersionUID" is an arbitrary change applied to the hash code value in order to
         * differentiate this ImageDatum implementation from implementations of other GeoAPI interfaces.
         */
        int code = super.hashCode(mode) ^ (int) serialVersionUID;
        switch (mode) {
            case STRICT: {
                code += Objects.hashCode(pixelInCell);
                break;
            }
            default: {
                code += Objects.hashCode(getPixelInCell());
                break;
            }
        }
        return code;
    }

    /**
     * Format the inner part of a <cite>Well Known Text</cite> (WKT) element.
     * {@code ImageDatum} are defined in the WKT 2 specification only.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name.
     */
    @Override
    public String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        formatter.append(pixelInCell);
        formatter.setInvalidWKT("ImageDatum");
        return "GENDATUM"; // Generic datum (WKT 2)
    }
}
