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

import java.io.Serializable;
import net.jcip.annotations.Immutable;

import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.extent.GeographicBoundingBox;


/**
 * Immutable representation of an {@linkplain Envelope envelope}.
 * This class is final in order to ensure that the immutability contract can not be broken
 * (assuming not using <cite>Java Native Interface</cite> or reflections).
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
@Immutable
public final class ImmutableEnvelope extends ArrayEnvelope implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5593936512712449234L;

    /**
     * Creates an immutable envelope with the values of the given envelope.
     * This constructor can be used when the given envelope is known to not
     * be an instance of {@code ImmutableEnvelope}. In case of doubt,
     * consider using {@link #castOrCopy(Envelope)} instead.
     *
     * @param envelope The envelope to copy.
     *
     * @see #castOrCopy(Envelope)
     */
    public ImmutableEnvelope(final Envelope envelope) {
        super(envelope);
    }

    /**
     * Constructs a new envelope with the same data than the specified geographic bounding box.
     * The coordinate reference system is set to {@code "CRS:84"}.
     *
     * @param box The bounding box to copy.
     */
    public ImmutableEnvelope(final GeographicBoundingBox box) {
        super(box);
    }

    /**
     * Creates an immutable envelope with the ordinate values of the given envelope but
     * a different CRS. This method does <strong>not</strong> reproject the given envelope.
     * It just assign the given CRS to this envelope without any check, except for the CRS
     * dimension.
     *
     * <p>The main purpose of this method is to assign a non-null CRS when the envelope to
     * copy has a null CRS.</p>
     *
     * @param crs      The CRS to assign to this envelope, or {@code null}.
     * @param envelope The envelope from which to copy ordinate values.
     * @throws MismatchedDimensionException If the dimension of the given CRS is not equals
     *         to the dimension of the given envelope.
     */
    public ImmutableEnvelope(final CoordinateReferenceSystem crs, final Envelope envelope)
            throws MismatchedDimensionException
    {
        super(envelope);
        this.crs = crs;
        AbstractDirectPosition.ensureDimensionMatch(crs, getDimension());
    }

    /**
     * Constructs a new envelope initialized to the values parsed from the given string in
     * <cite>Well Known Text</cite> (WKT) format. The given string is typically a {@code BOX}
     * element like below:
     *
     * {@preformat wkt
     *     BOX(-180 -90, 180 90)
     * }
     *
     * However this constructor is lenient to other geometry types like {@code POLYGON}.
     * See the javadoc of the {@link GeneralEnvelope#GeneralEnvelope(String) GeneralEnvelope}
     * constructor for more information.
     *
     * @param  crs The coordinate reference system, or {@code null} if none.
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws IllegalArgumentException If the given string can not be parsed.
     * @throws MismatchedDimensionException If the dimension of the given CRS is not equals
     *         to the dimension of the parsed envelope.
     */
    public ImmutableEnvelope(final CoordinateReferenceSystem crs, final String wkt)
            throws IllegalArgumentException, MismatchedDimensionException
    {
        super(wkt);
        this.crs = crs;
        AbstractDirectPosition.ensureDimensionMatch(crs, getDimension());
    }

    /**
     * Returns the given envelope as an {@code ImmutableEnvelope} instance. If the given envelope
     * is already an instance of {@code ImmutableEnvelope}, then it is returned unchanged.
     * Otherwise the coordinate values and the CRS of the given envelope are copied in a
     * new envelope.
     *
     * @param  envelope The envelope to cast, or {@code null}.
     * @return The values of the given envelope as an {@code ImmutableEnvelope} instance.
     *
     * @see GeneralEnvelope#castOrCopy(Envelope)
     */
    public static ImmutableEnvelope castOrCopy(final Envelope envelope) {
        if (envelope == null || envelope instanceof ImmutableEnvelope) {
            return (ImmutableEnvelope) envelope;
        }
        return new ImmutableEnvelope(envelope);
    }
}
