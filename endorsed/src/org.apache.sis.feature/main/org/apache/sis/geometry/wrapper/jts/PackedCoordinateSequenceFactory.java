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
package org.apache.sis.geometry.wrapper.jts;

import java.io.Serializable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;


/**
 * A factory of JTS coordinate sequence storing coordinates in a single {@code float[]} or {@code double[]} array.
 * This class serves the same purpose as {@link org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory}
 * but without caching the {@code Coordinate[]} array.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PackedCoordinateSequenceFactory implements CoordinateSequenceFactory, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8160800176640629524L;

    /**
     * Whether to use double-precision floating point numbers.
     */
    protected final boolean doublePrecision;

    /**
     * Creates a new factory.
     *
     * @param  doublePrecision  whether to use double-precision floating point numbers.
     */
    PackedCoordinateSequenceFactory(final boolean doublePrecision) {
        this.doublePrecision = doublePrecision;
    }

    /**
     * Creates a new sequence with the given coordinates. All values are copied.
     * The number of dimensions of the sequence is the minimal number of dimensions found in all coordinates.
     * We use the minimal number because requesting a inexistent dimension or measure in {@link Coordinate}
     * may cause an exception to be thrown.
     *
     * @param  coordinates  the coordinate values, or {@code null} for an empty sequence.
     * @return a newly created coordinate sequence with the given coordinate values.
     */
    @Override
    public CoordinateSequence create(final Coordinate[] coordinates) {
        int size = 0;
        boolean noZ = true;
        boolean noM = true;
        if (coordinates != null) {
            size = coordinates.length;
            for (final Coordinate c : coordinates) {
                if (noZ) noZ = Double.isNaN(c.getZ());
                if (noM) noM = Double.isNaN(c.getM());
                if (noZ & noM) break;   // Shortcut.
            }
        }
        int measures  = noM ? 0 : 1;
        int dimension = noZ ? Factory.BIDIMENSIONAL : Factory.TRIDIMENSIONAL;
        final PackedCoordinateSequence cs = create(size, dimension + measures, measures);
        if (size != 0) {
            cs.setCoordinates(coordinates);
        }
        return cs;
    }

    /**
     * Creates a new sequence as a copy of the given sequence.
     *
     * @param  original  the sequence to copy, or {@code null} for an empty sequence.
     * @return a newly created coordinate sequence with the values of the given sequence.
     */
    @Override
    public CoordinateSequence create(final CoordinateSequence original) {
        if (original instanceof PackedCoordinateSequence) {
            return original.copy();
        }
        final int dimension, measures, size;
        if (original != null) {
            dimension = original.getDimension();
            measures  = original.getMeasures();
            size      = original.size();
        } else {
            dimension = Factory.BIDIMENSIONAL;
            measures  = 0;
            size      = 0;
        }
        final PackedCoordinateSequence cs = create(size, dimension, measures);
        if (size != 0) {
            cs.setCoordinates(original);
        }
        return cs;
    }

    /**
     * Creates a new coordinate sequence for the given number of dimensions.
     *
     * @param  size       number of coordinate tuples.
     * @param  dimension  number of dimensions, {@value Factory#BIDIMENSIONAL} or {@value Factory#TRIDIMENSIONAL}.
     * @return a newly created coordinate sequence with all values initialized to zero.
     */
    @Override
    public CoordinateSequence create(final int size, final int dimension) {
        return create(size, dimension, 0);
    }

    /**
     * Creates a new coordinate sequence for the given number of dimensions and measures.
     *
     * @param  size       number of coordinate tuples.
     * @param  dimension  number of dimensions, including the number of measures.
     * @param  measures   number of <var>M</var> coordinates.
     * @return a newly created coordinate sequence with all values initialized to zero.
     */
    @Override
    public PackedCoordinateSequence create(final int size, final int dimension, final int measures) {
        if (doublePrecision) {
            return new PackedCoordinateSequence.Double(size, dimension, measures);
        } else {
            return new PackedCoordinateSequence.Float(size, dimension, measures);
        }
    }
}
