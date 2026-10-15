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
package org.apache.sis.geometries.internal.shared;

import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;


/**
 * An envelope whose positions can be given and obtained as vectors. Each method of {@link GeneralEnvelope}
 * taking a {@link DirectPosition} has an overload taking a {@link ReadOnly.Vector}, and each method producing
 * a {@code DirectPosition} has a counterpart producing a {@link Vector}, so that geometry algorithms working
 * on vectors do not have to convert them.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeometryEnvelope extends GeneralEnvelope {
    /**
     * Constructs an envelope of the given number of dimensions, with no reference system.
     * All coordinates are initialized to 0.
     *
     * @param dimension number of dimensions.
     */
    public GeometryEnvelope(final int dimension) {
        super(dimension);
    }

    /**
     * Constructs an envelope in the given reference system.
     * All coordinates are initialized to 0.
     *
     * @param crs reference system of the envelope.
     */
    public GeometryEnvelope(final CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * Constructs an envelope with the reference system and the coordinates of the given envelope.
     *
     * @param envelope the envelope to copy.
     */
    public GeometryEnvelope(final Envelope envelope) {
        super(envelope);
    }

    /**
     * Constructs an envelope defined by two corners given as direct positions.
     *
     * @param lower lower corner.
     * @param upper upper corner.
     * @throws MismatchedDimensionException if the two positions do not have the same dimension.
     */
    public GeometryEnvelope(final DirectPosition lower, final DirectPosition upper) throws MismatchedDimensionException {
        super(lower, upper);
    }

    /**
     * Constructs an envelope defined by two corners given as vectors.
     * The reference system is the one of the vectors.
     *
     * @param lower lower corner.
     * @param upper upper corner.
     * @throws MismatchedDimensionException if the two vectors do not have the same dimension.
     */
    public GeometryEnvelope(final ReadOnly.Vector<?> lower, final ReadOnly.Vector<?> upper) throws MismatchedDimensionException {
        super(asPosition(lower), asPosition(upper));
    }

    /**
     * Returns a view of the given vector as a direct position.
     */
    private static DirectPosition asPosition(final ReadOnly.Vector<?> vector) {
        return Vectors.asDirectPostion((vector instanceof Vector<?> v) ? v : vector.copy());
    }

    /**
     * Adds a position given as a vector to this envelope.
     *
     * @param position the position to add.
     * @throws MismatchedDimensionException if the vector does not have the dimension of this envelope.
     *
     * @see #add(DirectPosition)
     */
    public void add(final ReadOnly.Vector<?> position) throws MismatchedDimensionException {
        add(asPosition(position));
    }

    /**
     * Tests whether a position given as a vector is inside this envelope or on its border.
     *
     * @param position the position to test.
     * @return whether the position is inside this envelope or on its border.
     * @throws MismatchedDimensionException if the vector does not have the dimension of this envelope.
     *
     * @see #contains(DirectPosition)
     */
    public boolean contains(final ReadOnly.Vector<?> position) throws MismatchedDimensionException {
        return contains(asPosition(position));
    }

    /**
     * Returns a new vector of the dimension and reference system of this envelope.
     */
    private Vector<?> createVector() {
        final CoordinateReferenceSystem crs = getCoordinateReferenceSystem();
        return (crs != null) ? Vectors.create(crs, DataType.DOUBLE) : Vectors.createDouble(getDimension());
    }

    /**
     * Returns the lower corner of this envelope as a vector.
     * Changes to the returned vector do not change this envelope.
     *
     * @return a copy of the lower corner.
     *
     * @see #getLowerCorner()
     */
    public Vector<?> getLower() {
        final Vector<?> corner = createVector();
        for (int i = 0, n = getDimension(); i < n; i++) {
            corner.set(i, getLower(i));
        }
        return corner;
    }

    /**
     * Returns the upper corner of this envelope as a vector.
     * Changes to the returned vector do not change this envelope.
     *
     * @return a copy of the upper corner.
     *
     * @see #getUpperCorner()
     */
    public Vector<?> getUpper() {
        final Vector<?> corner = createVector();
        for (int i = 0, n = getDimension(); i < n; i++) {
            corner.set(i, getUpper(i));
        }
        return corner;
    }

    /**
     * Returns the median of this envelope as a vector.
     * Changes to the returned vector do not change this envelope.
     *
     * @return the median position.
     *
     * @see #getMedian()
     */
    public Vector<?> getCenter() {
        final Vector<?> center = createVector();
        for (int i = 0, n = getDimension(); i < n; i++) {
            center.set(i, getMedian(i));
        }
        return center;
    }

    /**
     * Returns a copy of this envelope.
     *
     * @return a copy of this envelope.
     */
    @Override
    public GeometryEnvelope clone() {
        return (GeometryEnvelope) super.clone();
    }
}
