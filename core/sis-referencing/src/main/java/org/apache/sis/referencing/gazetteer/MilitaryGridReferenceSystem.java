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
         * The precision as a power of 10.
         */
        private double precision;

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
            precision = 1;                          // 1 meter precision.
            buffer    = new StringBuilder(12);      // Length of "4QFJ12345678" sample value.
            encoders  = new IdentityHashMap<>();
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
            encoder.encode(position, precision, buffer);
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
