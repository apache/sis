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

import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.Resources;


/**
 * Applies union or intersection operations on a sequence of envelopes.
 * This utility class infers a common coordinate reference system for performing the reduce operation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see Envelopes#union(Envelope...)
 * @see Envelopes#intersect(Envelope...)
 */
class EnvelopeReducer {
    /**
     * A reducer performing the {@linkplain GeneralEnvelope#add(Envelope) union} operation.
     *
     * @see Envelopes#union(Envelope...)
     */
    static final EnvelopeReducer UNION = new EnvelopeReducer("union");

    /**
     * A reducer performing the {@linkplain GeneralEnvelope#intersect(Envelope) intersection} operation.
     *
     * @see Envelopes#intersect(Envelope...)
     */
    static final EnvelopeReducer INTERSECT = new EnvelopeReducer("intersect") {
        @Override void reduce(GeneralEnvelope result, Envelope other) {
            result.intersect(other);
        }

        @Override void reduce(DefaultGeographicBoundingBox result, GeographicBoundingBox other) {
            result.intersect(other);
        }
    };

    /**
     * The public method from {@link Envelopes} which is using this envelope reducer.
     * Used for logging purposes in case of error during the calculation of the envelope
     * to use as a hint for finding an operation between a pair of CRS.
     */
    private final String caller;

    /**
     * Creates a new reducer. We should have a singleton instance for each type of reduce operation.
     */
    EnvelopeReducer(final String caller) {
        this.caller = caller;
    }

    /**
     * Applies the reduce operation on the given {@code result} envelope.
     * The {@code result} envelope is modified in-place.
     */
    void reduce(GeneralEnvelope result, Envelope other) {
        result.add(other);
    }

    /**
     * Applies the reduce operation on the given {@code result} bounding box.
     * The {@code result} envelope is modified in-place.
     */
    void reduce(DefaultGeographicBoundingBox result, GeographicBoundingBox other) {
        result.add(other);
    }

    /**
     * Reduces all given envelopes, transforming them to a common CRS if necessary.
     * If all envelopes use the same CRS (ignoring metadata) or if the CRS of all envelopes is {@code null},
     * then the reduce operation is performed without transforming any envelope. Otherwise all envelopes are
     * transformed to a {@linkplain CRS#suggestCommonTarget common CRS} before reduction.
     * The CRS of the returned envelope may different than the CRS of all given envelopes.
     *
     * @param  envelopes  the envelopes for which to perform the reduce operation. Null elements are ignored.
     * @return result of reduce operation, or {@code null} if the given array does not contain non-null elements.
     * @throws TransformException if this method cannot determine a common CRS, or if a transformation failed.
     */
    final GeneralEnvelope reduce(final Envelope[] envelopes) throws TransformException {
        /*
         * First, compute the unions or intersections of all envelopes having a common CRS
         * without performing any reprojection. In the common case where all envelopes use
         * the same CRS, this will result in an array having only one non-null element.
         */
        final var reduced = new GeneralEnvelope[envelopes.length];
        int count = 0;
merge:  for (final Envelope envelope : envelopes) {
            if (envelope != null) {
                final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
                for (int i=0; i<count; i++) {
                    final GeneralEnvelope previous = reduced[i];
                    if (CRS.equivalent(crs, previous.getCoordinateReferenceSystem())) {
                        reduce(previous, envelope);
                        continue merge;
                    }
                }
                reduced[count++] = new GeneralEnvelope(envelope);
            }
        }
        switch (count) {
            case 0: return null;
            case 1: return reduced[0];
        }
        /*
         * Compute the geographic bounding box of all remaining elements to reduce. This will be used for
         * choosing a common CRS. Note that if a warning is logged, ReferencingServices.setBounds(…) will
         * pretend that warning come from Envelopes.<caller>. This is related to Envelopes.findOperation(…)
         * since the purpose of this bounding box is to find a coordinate operation.
         */
        final ReferencingServices converter = ReferencingServices.getInstance();
        CoordinateReferenceSystem[]  crs  = new CoordinateReferenceSystem[count];
        DefaultGeographicBoundingBox more = new DefaultGeographicBoundingBox();
        DefaultGeographicBoundingBox bbox = null;
        for (int i=0; i<count; i++) {
            final GeneralEnvelope e = reduced[i];
            crs[i] = e.getCoordinateReferenceSystem();
            if (converter.setBounds(e, more, caller) != null) {         // See above comment about logging.
                if (bbox == null) {
                    bbox = more;
                    more = new DefaultGeographicBoundingBox();
                } else {
                    reduce(bbox, more);
                }
            }
        }
        /*
         * Now transform all remaining envelopes, so we can perform final reduction.
         */
        final CoordinateReferenceSystem target = CRS.suggestCommonTarget(bbox, crs);
        if (target == null) {
            throw new TransformException(Resources.format(Resources.Keys.CanNotFindCommonCRS));
        }
        GeneralEnvelope result = null;
        for (int i=0; i<count; i++) {
            final Envelope other = Envelopes.transform(reduced[i], target);
            if (result == null) {
                result = GeneralEnvelope.castOrCopy(other);
            } else {
                reduce(result, other);
            }
        }
        return result;
    }
}
