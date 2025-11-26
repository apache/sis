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
import java.util.Arrays;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.apache.sis.util.ArgumentChecks;


/**
 * A JTS coordinate sequence which stores coordinates in a single {@code float[]} or {@code double[]} array.
 * This class serves the same purpose as {@link org.locationtech.jts.geom.impl.PackedCoordinateSequence}
 * but without caching the {@code Coordinate[]} array.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class PackedCoordinateSequence implements CoordinateSequence, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3374522920399590093L;

    /**
     * Number of dimensions for this coordinate sequence.
     * Values are restricted to 2, 3 or 4.
     *
     * @see #getDimension()
     */
    final byte dimension;

    /**
     * Whether this coordinate sequence has <var>z</var> coordinate values.
     * If {@code true}, exactly one <var>z</var> value is present per coordinate tuple.
     */
    final boolean hasZ;

    /**
     * Whether this coordinate sequence has <var>M</var> coordinate values.
     * If {@code true}, one or more <var>M</var> values are present per coordinate tuple.
     */
    final boolean hasM;

    /**
     * Creates a new sequence initialized to a copy of the given sequence.
     * This is for constructors implementing the {@link #copy()} method.
     */
    PackedCoordinateSequence(final PackedCoordinateSequence original) {
        dimension = original.dimension;
        hasZ      = original.hasZ;
        hasM      = original.hasM;
    }

    /**
     * Creates a new coordinate sequence for the given number of dimensions.
     *
     * @param  dimension  number of dimensions, including the number of measures.
     * @param  measures   number of <var>M</var> coordinates.
     */
    PackedCoordinateSequence(final int dimension, final int measures) {
        ArgumentChecks.ensureBetween("measures", 0, 100, measures);     // Arbitrary upper limit.
        ArgumentChecks.ensureBetween("dimension",
                Factory.BIDIMENSIONAL  + measures,
                Factory.TRIDIMENSIONAL + measures,
                dimension);
        this.dimension = (byte) dimension;
        this.hasM = (measures != 0);
        this.hasZ = (dimension - measures) >= Factory.TRIDIMENSIONAL;
    }

    /**
     * Returns the number of dimensions for all coordinates in this sequence.
     * This value includes the number of {@linkplain #getMeasures() measures}.
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
        return dimension - (hasZ ? Factory.TRIDIMENSIONAL : Factory.BIDIMENSIONAL);
    }

    /**
     * Returns whether this coordinate sequence has <var>z</var> coordinate values.
     * If {@code true}, exactly one <var>z</var> value is present per coordinate tuple.
     */
    @Override
    public final boolean hasZ() {
        return hasZ;
    }

    /**
     * Returns whether this coordinate sequence has <var>M</var> coordinate values.
     * If {@code true}, one or more <var>M</var> values are present per coordinate tuple.
     */
    @Override
    public final boolean hasM() {
        return hasM;
    }

    /**
     * Returns the <var>x</var> coordinate value for the tuple at the given index.
     * For performance reasons, this method does not check the validity of the {@code index} argument.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final double getX(final int index) {
        return coordinate(index * dimension + X);
    }

    /**
     * Returns the <var>y</var> coordinate value for the tuple at the given index.
     * For performance reasons, this method does not check the validity of the {@code index} argument.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final double getY(final int index) {
        return coordinate(index * dimension + Y);
    }

    /**
     * Returns the <var>z</var> coordinate value for the tuple at the given index.
     * If this sequence has no <var>z</var> coordinates, returns {@link java.lang.Double.NaN}.
     * For performance reasons, this method does not check the validity of the {@code index} argument.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final double getZ(final int index) {
        return hasZ ? coordinate(index * dimension + Z) : java.lang.Double.NaN;
    }

    /**
     * Returns the first <var>M</var> coordinate value for the tuple at the given index.
     * If this sequence has no <var>M</var> coordinates, returns {@link java.lang.Double.NaN}.
     * For performance reasons, this method does not check the validity of the {@code index} argument.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final double getM(final int index) {
        return hasM ? coordinate(index * dimension + (hasZ ? M : Z)) : java.lang.Double.NaN;
    }

    /**
     * Returns the coordinate tuple at the given index.
     * For performance reasons, this method does not check the validity of the {@code index} argument.
     *
     * @param  index  index of the coordinate tuple.
     * @return coordinate tuple at the given index.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final Coordinate getCoordinate(int index) {
        index *= dimension;
        final double x = coordinate(  index);
        final double y = coordinate(++index);
        if (!(hasZ | hasM)) {
            return new CoordinateXY(x, y);   // Most common case.
        }
        final double z = coordinate(++index);
        if (!hasM) return new Coordinate   (x, y, z);
        if (!hasZ) return new CoordinateXYM(x, y, z);
        return new CoordinateXYZM(x, y, z, coordinate(++index));
    }

    /**
     * Copies the coordinate tuple at the given index into the specified target.
     *
     * @param  index  index of the coordinate tuple.
     * @param  dest   where to copy the coordinates.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final void getCoordinate(int index, final Coordinate dest) {
        index *= dimension;
        dest.x = coordinate(  index);
        dest.y = coordinate(++index);
        if (hasZ) dest.setZ(coordinate(++index));
        if (hasM) dest.setM(coordinate(++index));
    }

    /**
     * Returns the coordinate tuple at given index.
     *
     * @param  index  index of the coordinate tuple.
     * @return coordinate tuple at the given index.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
     */
    @Override
    public final Coordinate getCoordinateCopy(int index) {
        return getCoordinate(index);
    }

    /**
     * Returns a coordinate value from the coordinate tuple at the given index.
     * For performance reasons, this method does not check arguments validity.
     *
     * @param  index  index of the coordinate tuple.
     * @param  dim    index of the coordinate value in the tuple.
     * @return value of the specified value in the coordinate tuple.
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds (not always detected).
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
    @SuppressWarnings("CloneableImplementsClone")
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
            int skip = getMeasures();
            if (hasM) skip--;
            for (final Coordinate c : values) {
                /*always*/coordinates[t++] = c.getX();
                /*always*/coordinates[t++] = c.getY();
                if (hasZ) coordinates[t++] = c.getZ();
                if (hasM) coordinates[t++] = c.getM();
                t += skip;
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
    @SuppressWarnings("CloneableImplementsClone")
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
            int skip = getMeasures();
            if (hasM) skip--;
            for (final Coordinate c : values) {
                /*always*/coordinates[t++] = (float) c.getX();
                /*always*/coordinates[t++] = (float) c.getY();
                if (hasZ) coordinates[t++] = (float) c.getZ();
                if (hasM) coordinates[t++] = (float) c.getM();
                t += skip;
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
        final var coordinates = new Coordinate[size()];
        for (int i=0; i < coordinates.length; i++) {
            coordinates[i] = getCoordinate(i);
        }
        return coordinates;
    }

    /**
     * Returns a string representation of this coordinate sequence.
     */
    @Override
    public final String toString() {
        return CoordinateSequences.toString(this);
    }

    /**
     * Returns a hash code value for this sequence.
     */
    @Override
    public int hashCode() {
        return (37 * dimension) ^ (hasZ ? 3 : 7) ^ (hasM ? 11 : 17);
    }

    /**
     * Compares the given object with this sequence for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final var other = (PackedCoordinateSequence) obj;
            return other.dimension == dimension && other.hasZ == hasZ && other.hasM == hasM;
        }
        return false;
    }

    /**
     * Returns a copy of this sequence.
     *
     * @deprecated Inherits the deprecation status from JTS.
     */
    @Override
    @Deprecated
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final Object clone() {
        return copy();
    }
}
