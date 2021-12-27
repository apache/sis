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

import java.io.Serializable;
import java.util.Arrays;
import org.apache.sis.util.ArgumentChecks;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;


/**
 * A JTS coordinate sequence which stores coordinates in a single {@code float[]} or {@code double[]} array.
 * This class serves the same purpose than {@link org.locationtech.jts.geom.impl.PackedCoordinateSequence}
 * but without caching the {@code Coordinate[]} array.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
abstract class PackedCoordinateSequence implements CoordinateSequence, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6323915437380051705L;

    /**
     * Number of dimensions for this coordinate sequence.
     *
     * @see #getDimension()
     */
    protected final int dimension;

    /**
     * Whether this coordinate sequence has <var>z</var> and/or <var>M</var> coordinate values.
     * This is a combination of {@link #Z_MASK} and {@link #M_MASK} bit masks.
     *
     * @see #hasZ()
     * @see #hasM()
     */
    private final int hasZM;

    /**
     * Bit to set to 1 in the {@link #hasZM} mask if this coordinate sequence
     * has <var>z</var> and/or <var>M</var> coordinate values.
     */
    private static final int Z_MASK = 1, M_MASK = 2;      // Z_MASK must be 1 for bit twiddling reason.

    /**
     * Creates a new sequence initialized to a copy of the given sequence.
     * This is for constructors implementing the {@link #copy()} method.
     */
    PackedCoordinateSequence(final PackedCoordinateSequence original) {
        dimension = original.dimension;
        hasZM     = original.hasZM;
    }

    /**
     * Creates a new coordinate sequence for the given number of dimensions.
     *
     * @param  dimension  number of dimensions, including the number of measures.
     * @param  measures   number of <var>M</var> coordinates.
     */
    PackedCoordinateSequence(final int dimension, final int measures) {
        ArgumentChecks.ensurePositive("measures", measures);
        ArgumentChecks.ensureBetween("dimension", Factory.BIDIMENSIONAL + measures,
                       Math.addExact(Factory.TRIDIMENSIONAL, measures), dimension);
        this.dimension = dimension;
        int hasZM = (measures == 0) ? 0 : M_MASK;
        if ((dimension - measures) >= Factory.TRIDIMENSIONAL) {
            hasZM |= Z_MASK;
        }
        this.hasZM = hasZM;
    }

    /**
     * Returns the number of spatial dimensions,
     * which is {@value Factory#BIDIMENSIONAL} or {@value Factory#TRIDIMENSIONAL}.
     */
    private static int getSpatialDimension(final int hasZM) {
        return Factory.BIDIMENSIONAL | (hasZM & Z_MASK);
    }

    /**
     * Returns the number of dimensions for all coordinates in this sequence,
     * including {@linkplain #getMeasures() measures}.
     */
    @Override
    public final int getDimension() {
        return dimension;
    }

    /**
     * Returns the number of <var>M</var> coordinates.
     */
    @Override
    public final int getMeasures() {
        return dimension - getSpatialDimension(hasZM);
    }

    /**
     * Returns whether this coordinate sequence has <var>z</var> coordinate values.
     */
    @Override
    public final boolean hasZ() {
        return (hasZM & Z_MASK) != 0;
    }

    /**
     * Returns whether this coordinate sequence has <var>M</var> coordinate values.
     */
    @Override
    public final boolean hasM() {
        return (hasZM & M_MASK) != 0;
    }

    /**
     * Returns the <var>x</var> coordinate value for the tuple at the given index.
     */
    @Override
    public final double getX(final int index) {
        return coordinate(index * dimension + X);
    }

    /**
     * Returns the <var>y</var> coordinate value for the tuple at the given index.
     */
    @Override
    public final double getY(final int index) {
        return coordinate(index * dimension + Y);
    }

    /**
     * Returns the <var>z</var> coordinate value for the tuple at the given index,
     * or {@link java.lang.Double.NaN} if this sequence has no <var>z</var> coordinates.
     */
    @Override
    public final double getZ(final int index) {
        return (hasZM & Z_MASK) != 0 ? coordinate(index * dimension + Z) : java.lang.Double.NaN;
    }

    /**
     * Returns the first <var>M</var> coordinate value for the tuple at the given index,
     * or {@link java.lang.Double.NaN} if this sequence has no <var>M</var> coordinates.
     */
    @Override
    public final double getM(final int index) {
        switch (hasZM) {
            default:              return java.lang.Double.NaN;
            case M_MASK:          return coordinate(index * dimension + Z);
            case M_MASK | Z_MASK: return coordinate(index * dimension + M);
        }
    }

    /**
     * Returns the coordinate tuple at the given index.
     *
     * @param  index  index of the coordinate tuple.
     * @return coordinate tuple at the given index.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public final Coordinate getCoordinate(int index) {
        index *= dimension;
        final double x = coordinate(  index);
        final double y = coordinate(++index);
        switch (hasZM) {
            default:              return new Coordinate    (x,y);
            case 0:               return new CoordinateXY  (x,y);
            case Z_MASK:          return new Coordinate    (x,y, coordinate(++index));
            case M_MASK:          return new CoordinateXYM (x,y, coordinate(++index));
            case Z_MASK | M_MASK: return new CoordinateXYZM(x,y, coordinate(++index), coordinate(++index));
        }
    }

    /**
     * Copies the coordinate tuple at the given index into the specified target.
     *
     * @param  index     index of the coordinate tuple.
     * @param  dest  where to copy the coordinates.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public final void getCoordinate(int index, final Coordinate dest) {
        index *= dimension;
        dest.x = coordinate(  index);
        dest.y = coordinate(++index);
        switch (hasZM) {
            case Z_MASK:          dest.setZ(coordinate(++index)); break;
            case Z_MASK | M_MASK: dest.setZ(coordinate(++index)); // Fall through
            case M_MASK:          dest.setM(coordinate(++index)); break;
        }
    }

    /**
     * Returns the coordinate tuple at given index.
     *
     * @param  index  index of the coordinate tuple.
     * @return coordinate tuple at the given index.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public final Coordinate getCoordinateCopy(int index) {
        return getCoordinate(index);
    }

    /**
     * Returns a coordinate value from the coordinate tuple at the given index.
     * For performance reasons, this method does not check {@code dim} validity.
     *
     * @param  index  index of the coordinate tuple.
     * @param  dim    index of the coordinate value in the tuple.
     * @return value of the specified value in the coordinate tuple.
     */
    @Override
    public final double getOrdinate(final int index, final int dim) {
        return coordinate(index * dimension + dim);
    }

    /**
     * Returns the coordinate value at the given index in the packed array.
     *
     * @param  index  index in the packed array.
     * @return coordinate value at the given index.
     */
    abstract double coordinate(int index);

    /**
     * Sets all coordinates in this sequence. The length of the given array
     * shall be equal to {@link #size()} (this is not verified).
     */
    abstract void setCoordinates(Coordinate[] values);

    /**
     * Sets all coordinates in this sequence. The size of the given sequence
     * shall be equal to {@link #size()} (this is not verified).
     */
    void setCoordinates(CoordinateSequence values) {
        setCoordinates(values.toCoordinateArray());
    }

    /**
     * Coordinate sequence storing values in a packed {@code double[]} array.
     */
    static final class Double extends PackedCoordinateSequence {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 1940132733783453171L;

        /** The packed coordinates. */
        private final double[] coordinates;

        /** Creates a new sequence initialized to a copy of the given sequence. */
        private Double(final Double original) {
            super(original);
            coordinates = original.coordinates.clone();
        }

        /** Creates a new coordinate sequence with given values. */
        Double(final double[] array, final int length) {
            super(Factory.BIDIMENSIONAL, 0);
            coordinates = Arrays.copyOf(array, length);
        }

        /** Creates a new coordinate sequence for the given number of tuples. */
        Double(final int size, final int dimension, final int measures) {
            super(dimension, measures);
            coordinates = new double[Math.multiplyExact(size, dimension)];
        }

        /** Returns the number of coordinate tuples in this sequence. */
        @Override public int size() {
            return coordinates.length / dimension;
        }

        /** Returns the coordinate value at the given index in the packed array. */
        @Override double coordinate(int index) {
            return coordinates[index];
        }

        /** Sets a coordinate value for the coordinate tuple at the given index. */
        @Override public void setOrdinate(int index, int dim, double value) {
            coordinates[index * dimension + dim] = value;
        }

        /** Sets all coordinates in this sequence. */
        @Override void setCoordinates(final Coordinate[] values) {
            int t = 0;
            for (final Coordinate c : values) {
                for (int i=0; i<dimension; i++) {
                    coordinates[t++] = c.getOrdinate(i);
                }
            }
            assert t == coordinates.length;
        }

        /** Sets all coordinates in this sequence. */
        @Override void setCoordinates(final CoordinateSequence values) {
            if (values instanceof org.locationtech.jts.geom.impl.PackedCoordinateSequence.Double) {
                System.arraycopy(((org.locationtech.jts.geom.impl.PackedCoordinateSequence.Double) values).getRawCoordinates(), 0, coordinates, 0, coordinates.length);
            } else {
                super.setCoordinates(values);
            }
        }

        /** Expands the given envelope to include the (x,y) coordinates of this sequence. */
        @Override public Envelope expandEnvelope(final Envelope envelope) {
            for (int i=0; i < coordinates.length; i += dimension) {
                envelope.expandToInclude(coordinates[i], coordinates[i+1]);
            }
            return envelope;
        }

        /** Returns a copy of this sequence. */
        @Override public CoordinateSequence copy() {
            return new Double(this);
        }

        /** Returns a hash code value for this sequence. */
        @Override public int hashCode() {
            return Arrays.hashCode(coordinates) + super.hashCode();
        }

        /** Compares the given object with this sequence for equality. */
        @Override public boolean equals(final Object obj) {
            return super.equals(obj) && Arrays.equals(((Double) obj).coordinates, coordinates);
        }
    }

    /**
     * Coordinate sequence storing values in a packed {@code float[]} array.
     */
    static final class Float extends PackedCoordinateSequence {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2625498691139718968L;

        /** The packed coordinates. */
        private final float[] coordinates;

        /** Creates a new sequence initialized to a copy of the given sequence. */
        private Float(final Float original) {
            super(original);
            coordinates = original.coordinates.clone();
        }

        /** Creates a new coordinate sequence with given values. */
        Float(final float[] array, final int length) {
            super(Factory.BIDIMENSIONAL, 0);
            coordinates = Arrays.copyOf(array, length);
        }

        /** Creates a new coordinate sequence for the given number of tuples. */
        Float(final int size, final int dimension, final int measures) {
            super(dimension, measures);
            coordinates = new float[Math.multiplyExact(size, dimension)];
        }

        /** Returns the number of coordinate tuples in this sequence. */
        @Override public int size() {
            return coordinates.length / dimension;
        }

        /** Returns the coordinate value at the given index in the packed array. */
        @Override double coordinate(int index) {
            return coordinates[index];
        }

        /** Sets a coordinate value for the coordinate tuple at the given index. */
        @Override public void setOrdinate(int index, int dim, double value) {
            coordinates[index * dimension + dim] = (float) value;
        }

        /** Sets all coordinates in this sequence. */
        @Override void setCoordinates(final Coordinate[] values) {
            int t = 0;
            for (final Coordinate c : values) {
                for (int i=0; i<dimension; i++) {
                    coordinates[t++] = (float) c.getOrdinate(i);
                }
            }
            assert t == coordinates.length;
        }

        /** Sets all coordinates in this sequence. */
        @Override void setCoordinates(final CoordinateSequence values) {
            if (values instanceof org.locationtech.jts.geom.impl.PackedCoordinateSequence.Float) {
                System.arraycopy(((org.locationtech.jts.geom.impl.PackedCoordinateSequence.Float) values).getRawCoordinates(), 0, coordinates, 0, coordinates.length);
            } else {
                super.setCoordinates(values);
            }
        }

        /** Expands the given envelope to include the (x,y) coordinates of this sequence. */
        @Override public Envelope expandEnvelope(final Envelope envelope) {
            for (int i=0; i < coordinates.length; i += dimension) {
                envelope.expandToInclude(coordinates[i], coordinates[i+1]);
            }
            return envelope;
        }

        /** Returns a copy of this sequence. */
        @Override public CoordinateSequence copy() {
            return new Float(this);
        }

        /** Returns a hash code value for this sequence. */
        @Override public int hashCode() {
            return Arrays.hashCode(coordinates) + super.hashCode();
        }

        /** Compares the given object with this sequence for equality. */
        @Override public boolean equals(final Object obj) {
            return super.equals(obj) && Arrays.equals(((Float) obj).coordinates, coordinates);
        }
    }

    /**
     * Returns a copy of all coordinates in this sequence.
     */
    @Override
    public final Coordinate[] toCoordinateArray() {
        final Coordinate[] coordinates = new Coordinate[size()];
        for (int i=0; i < coordinates.length; i++) {
            coordinates[i] = getCoordinate(i);
        }
        return coordinates;
    }

    /**
     * Returns a string representation of this coordinate sequence.
     */
    public final String toString() {
        return CoordinateSequences.toString(this);
    }

    /**
     * Returns a hash code value for this sequence.
     */
    @Override
    public int hashCode() {
        return (37 * dimension) ^ hasZM;
    }

    /**
     * Compares the given object with this sequence for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final PackedCoordinateSequence other = (PackedCoordinateSequence) obj;
            return other.dimension == dimension && other.hasZM == hasZM;
        }
        return false;
    }

    /**
     * Returns a copy of this sequence.
     *
     * @deprecated Inherits the deprecation status from JTS.
     */
    @Deprecated
    public final Object clone() {
        return copy();
    }
}
