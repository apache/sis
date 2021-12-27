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
package org.apache.sis.internal.feature.jts;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


/**
 * A geometry transformer which uses a {@link MathTransform} for changing coordinate values.
 * This class does not change the number of points.
 * This class is not thread-safe.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.0
 * @module
 */
final class GeometryCoordinateTransform extends GeometryTransform {
    /**
     * The transform to apply on coordinate values.
     */
    private final MathTransform transform;

    /**
     * A temporary buffer holding coordinates to transform.
     * Created when first needed in order to have an estimation of size needed.
     */
    private double[] coordinates;

    /**
     * Creates a new geometry transformer using the given coordinate transform.
     * It is caller's responsibility to ensure that the number of source and target dimensions
     * of the given transform are equal to the number of dimensions of the geometries to transform.
     *
     * @param  transform  the transform to apply on coordinate values.
     * @param  factory    the factory to use for creating geometries. Shall not be null.
     */
    GeometryCoordinateTransform(final MathTransform transform, final GeometryFactory factory) {
        super(factory);
        this.transform = transform;
    }

    /**
     * Transforms the given sequence of coordinate tuples, producing a new sequence of tuples.
     * This method tries to transform coordinates in batches, in order to reduce the amount of
     * calls to {@link MathTransform#transform(double[], int, double[], int, int)}.
     *
     * @param  sequence   sequence of coordinate tuples to transform.
     * @param  minPoints  minimum number of points to preserve.
     * @return the transformed sequence of coordinate tuples.
     * @throws TransformException if an error occurred while transforming a tuple.
     */
    @Override
    protected CoordinateSequence transform(final CoordinateSequence sequence, final int minPoints) throws TransformException {
        final int srcDim   = transform.getSourceDimensions();
        final int tgtDim   = transform.getTargetDimensions();
        final int maxDim   = Math.max(srcDim, tgtDim);
        final int count    = sequence.size();
        final int capacity = Math.max(4, Math.min(100, count));
        final CoordinateSequence out = coordinateFactory.create(count, tgtDim);
        if (coordinates == null || coordinates.length / maxDim < capacity) {
            coordinates = new double[capacity * maxDim];
        }
        for (int base=0, n; (n = Math.min(count - base, capacity)) > 0; base += n) {
            int batch = n * srcDim;
            for (int i=0; i<batch; i++) {
                coordinates[i] = sequence.getOrdinate(base + i/srcDim, i % srcDim);
            }
            transform.transform(coordinates, 0, coordinates, 0, n);
            batch = n * tgtDim;
            for (int i=0; i<batch; i++) {
                out.setOrdinate(base + i/tgtDim, i % tgtDim, coordinates[i]);
            }
        }
        return out;
    }
}
