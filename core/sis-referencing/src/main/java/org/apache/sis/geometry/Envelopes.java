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
package org.apache.sis.geometry;

/*
 * Do not add dependency to java.awt.Rectangle2D in this class, because not every platforms
 * support Java2D (e.g. Android),  or applications that do not need it may want to avoid to
 * force installation of the Java2D module (e.g. JavaFX/SWT).
 */
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.CRS;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.util.StringBuilders.trimFractionalPart;


/**
 * Utility methods for envelopes. This utility class is made up of static functions working
 * with arbitrary implementations of GeoAPI interfaces.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.metadata.iso.extent.Extents
 * @see CRS
 */
public final class Envelopes extends Static {
    /**
     * Enumeration of the 4 corners in an envelope, with repetition of the first point.
     * The values are (x,y) pairs with {@code false} meaning "minimal value" and {@code true}
     * meaning "maximal value". This is used by {@link #toPolygonWKT(Envelope)} only.
     */
    private static final boolean[] CORNERS = {
        false, false,
        false, true,
        true,  true,
        true,  false,
        false, false
    };

    /**
     * Do not allow instantiation of this class.
     */
    private Envelopes() {
    }

    /**
     * Returns the bounding box of a geometry defined in <cite>Well Known Text</cite> (WKT) format.
     * This method does not check the consistency of the provided WKT. For example it does not check
     * that every points in a {@code LINESTRING} have the same dimension. However this method
     * ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     *
     * <p>Example:</p>
     * <ul>
     *   <li>{@code BOX(-180 -90, 180 90)} (not really a geometry, but understood by many softwares)</li>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul>
     *
     * See {@link GeneralEnvelope#GeneralEnvelope(CharSequence)} for more information about the
     * parsing rules.
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @return The envelope of the given geometry.
     * @throws FactoryException If the given WKT can not be parsed.
     *
     * @see #toString(Envelope)
     * @see CRS#fromWKT(String)
     * @see org.apache.sis.io.wkt
     */
    public static Envelope fromWKT(final CharSequence wkt) throws FactoryException {
        ensureNonNull("wkt", wkt);
        try {
            return new GeneralEnvelope(wkt);
        } catch (IllegalArgumentException e) {
            throw new FactoryException(Errors.format(
                    Errors.Keys.UnparsableStringForClass_2, Envelope.class), e);
        }
    }

    /**
     * Formats the given envelope as a {@code BOX} element. The output is like below,
     * where <var>n</var> is the {@linkplain Envelope#getDimension() number of dimensions}
     * (omitted if equals to 2):
     *
     * <blockquote>{@code BOX}<var>n</var>{@code D(}{@linkplain Envelope#getLowerCorner() lower
     * corner}{@code ,} {@linkplain Envelope#getUpperCorner() upper corner}{@code )}</blockquote>
     *
     * {@note The <code>BOX</code> element is not part of the standard <cite>Well Known Text</cite>
     *        (WKT) format. However it is understood by many softwares, for example GDAL and PostGIS.}
     *
     * The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence)
     * parsed} by the {@code GeneralEnvelope} constructor.
     *
     * @param  envelope The envelope to format.
     * @return This envelope as a {@code BOX} or {@code BOX3D} (most typical dimensions) element.
     *
     * @see #fromWKT(CharSequence)
     * @see org.apache.sis.io.wkt
     */
    public static String toString(final Envelope envelope) {
        return AbstractEnvelope.toString(envelope, false);
    }

    /**
     * Formats the given envelope as a {@code POLYGON} element in the <cite>Well Known Text</cite>
     * (WKT) format. {@code POLYGON} can be used as an alternative to {@code BOX} when the element
     * needs to be considered as a standard WKT geometry.
     *
     * <p>The string returned by this method can be {@linkplain GeneralEnvelope#GeneralEnvelope(CharSequence)
     * parsed} by the {@code GeneralEnvelope} constructor.</p>
     *
     * @param  envelope The envelope to format.
     * @return The envelope as a {@code POLYGON} in WKT format.
     * @throws IllegalArgumentException if the given envelope can not be formatted.
     *
     * @see org.apache.sis.io.wkt
     */
    public static String toPolygonWKT(final Envelope envelope) throws IllegalArgumentException {
        /*
         * Get the dimension, ignoring the trailing ones which have infinite values.
         */
        int dimension = envelope.getDimension();
        while (dimension != 0) {
            final double length = envelope.getSpan(dimension - 1);
            if (!Double.isNaN(length) && !Double.isInfinite(length)) {
                break;
            }
            dimension--;
        }
        if (dimension < 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyEnvelope2D));
        }
        final StringBuilder buffer = new StringBuilder("POLYGON(");
        String separator = "(";
        for (int corner=0; corner<CORNERS.length; corner+=2) {
            for (int i=0; i<dimension; i++) {
                final double value;
                switch (i) {
                    case  0: // Fall through
                    case  1: value = CORNERS[corner+i] ? envelope.getMaximum(i) : envelope.getMinimum(i); break;
                    default: value = envelope.getMedian(i); break;
                }
                trimFractionalPart(buffer.append(separator).append(value));
                separator = " ";
            }
            separator = ", ";
        }
        return buffer.append("))").toString();
    }
}
