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
package org.apache.sis.referencing.gazetteer;

import java.util.Map;
import java.text.ParseException;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.resources.Errors;


/**
 * The Military Grid Reference System (MGRS).
 * The MGRS is the geocoordinate standard used by NATO militaries for locating points on the earth.
 * It is based on the Universal Transverse Mercator (UTM) and the polar stereographic projections.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe. However the {@code Coder} performing conversions
 * between labels and coordinates are not thread-safe; a new instance must be created for each thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see <a href="https://en.wikipedia.org/wiki/Military_Grid_Reference_System">Military Grid Reference System on Wikipedia</a>
 */
public class MilitaryGridReferenceSystem {
    /**
     * Creates a new Military Grid Reference System (MGRS).
     */
    public MilitaryGridReferenceSystem() {
    }

    /**
     * Conversions between direct positions and labels in the Military Grid Reference System (MGRS).
     * Each {@code Coder} instance can read labels at arbitrary precision, but formats at the
     * {@linkplain #setPrecision specified precision}.
     *
     * <div class="section">Immutability and thread safety</div>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    public static class Coder {
        /**
         * Number of digits to use for formatting the numerical part of a MGRS label.
         */
        private byte digits;

        /**
         * Cached information needed for building a MGRS label from a direct position in the given CRS.
         */
        private final Map<CoordinateReferenceSystem,MGRSEncoder> encoders;

        /**
         * A buffer where to create label, to be reused for each new label.
         */
        private final StringBuilder buffer;

        /**
         * Creates a new coder initialized to the default precision.
         */
        public Coder() {
            digits    = 5;                          // 1 meter precision.
            buffer    = new StringBuilder(12);      // Length of "4QFJ12345678" sample value.
            encoders  = new IdentityHashMap<>();
        }

        /**
         * Returns the precision of the labels formatted by this coder.
         * This method returns one of the following values:
         *
         * <table class="sis">
         *   <caption>MGRS label precisions</caption>
         *   <tr><th>Precision (m)</th>             <th>Label example</th></tr>
         *   <tr><td style="text-align:right">1</td> <td>4QFJ 12345 67890</td></tr>
         *   <tr><td style="text-align:right">10</td> <td>4QFJ 1234 6789</td></tr>
         *   <tr><td style="text-align:right">100</td> <td>4QFJ 123 678</td></tr>
         *   <tr><td style="text-align:right">1000</td> <td>4QFJ 12 67</td></tr>
         *   <tr><td style="text-align:right">10 000</td> <td>4QFJ 1 6</td></tr>
         *   <tr><td style="text-align:right">100 000</td> <td>4QFJ</td></tr>
         *   <tr><td style="text-align:right">(approximative) 1 000 000</td> <td>4Q</td></tr>
         * </table>
         *
         * Values smaller than 1 (e.g. 0.01 for a centimetre precision) may also be returned
         * if that value has been {@linkplain #setPrecision(double) explicitely set},
         * but sub-metric precision are usually not used with MGRS.
         *
         * @return precision of formatted labels in metres.
         */
        public double getPrecision() {
            return MathFunctions.pow10(MGRSEncoder.METRE_PRECISION_DIGITS - digits);
        }

        /**
         * Sets the desired precision of the labels formatted by this coder.
         * This method rounds the given precision to one of the power of 10
         * documented in the {@link #getPrecision()} method.
         *
         * @param  precision  the desired precision in metres.
         */
        public void setPrecision(final double precision) {
            final double p = Math.floor(Math.log10(precision));
            if (!Double.isFinite(p)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "precision", precision));
            }
            // The -3 is an arbitrary limit to millimetre precision.
            int n = Math.max(-3, Math.min(MGRSEncoder.METRE_PRECISION_DIGITS + 1, (int) p));
            digits = (byte) (MGRSEncoder.METRE_PRECISION_DIGITS - n);
        }

        /**
         * Encodes the given position into a MGRS label.
         * The given position must have a CRS associated to it.
         *
         * @param  position  the coordinate to encode.
         * @return MGRS encoding of the given position.
         * @throws TransformException if an error occurred while transforming the given coordinate to a MGRS label.
         */
        public String encode(final DirectPosition position) throws TransformException {
            ArgumentChecks.ensureNonNull("position", position);
            final CoordinateReferenceSystem crs = position.getCoordinateReferenceSystem();
            MGRSEncoder encoder = encoders.get(crs);
            if (encoder == null) try {
                encoder = new MGRSEncoder(crs);
                if (encoders.put(crs, encoder) != null) {
                    throw new ConcurrentModificationException();            // Opportunistic check.
                }
            } catch (FactoryException e) {
                throw new TransformException(e.toString(), e);
            }
            buffer.setLength(0);
            encoder.encode(position, digits, buffer);
            return buffer.toString();
        }

        /**
         * Decodes the given MGRS label into a position.
         *
         * @param  label  MGRS string to decode.
         * @return a new position with the longitude at ordinate 0 and latitude at ordinate 1.
         * @throws ParseException if an error occurred while parsing the given string.
         */
        public DirectPosition decode(final String label) throws ParseException {
            return null;
        }
    }
}
