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
package org.apache.sis.geometries;

import java.util.HashMap;
import java.util.Map;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.internal.shared.IndexedPoint;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.ReadOnly;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vectors;
import org.apache.sis.maths.Vector;


/**
 * A BBOx geometry defined by lower and upper corners.
 * This is not an axis oriented bounding box, see OBBox for the oriented counterpart.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BBox extends GeneralEnvelope implements Geometry {

    private Map<String,Object> properties;

    /**
     * @param dimension number of dimensions of the bbox, must be positive.
     */
    public BBox(int dimension) {
        super(dimension);
    }

    /**
     * @param dimension number of dimensions of the bbox, must be positive.
     * @param corners lower corner and higher corner, in crs axis order
     */
    public BBox(int dimension, double ... corners) {
        super(dimension);
        setEnvelope(corners);
    }

    /**
     * @param lower lower corner
     * @param upper upper corner
     */
    public BBox(Vector lower, Vector upper) {
        super(Vectors.asDirectPostion(lower), Vectors.asDirectPostion(upper));
    }

    /**
     * @param crs sphere coordinate system, not null.
     */
    public BBox(CoordinateReferenceSystem crs) {
        super(crs);
    }

    /**
     * @param crs sphere coordinate system, not null.
     * @param corners lower corner and higher corner, in crs axis order
     */
    public BBox(CoordinateReferenceSystem crs, double ... corners) {
        super(crs);
        setEnvelope(corners);
    }

    /**
     * @param env Envelope to copy crs and coordinates from.
     */
    public BBox(Envelope env) {
        super(env);
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.BBOX;
    }

    public void add(Vector<?> position) throws MismatchedDimensionException {
        add(Vectors.asDirectPostion(position));
    }

    /**
     * {@inheritDoc }
     */
    public Vector<?> getLower() {
        return Vectors.castOrWrap(super.getLowerCorner());
    }

    /**
     * {@inheritDoc }
     */
    public Vector<?> getUpper() {
        return Vectors.castOrWrap(super.getUpperCorner());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Envelope getEnvelope() {
        return this.clone();
    }

    @Override
    public Geometry getBoundary() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized Map<String, Object> userProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public DataPointsType getDataPointsType() {
        return getDataPoints().getType();
    }

    /**
     * Returns the two corners of this box, the lower one first, as a sequence of two positions.
     * Those corners are what this box is made of, so writing a position through the returned
     * sequence moves the corresponding corner.
     */
    @Override
    public DataPoints getDataPoints() {
        return new Corners();
    }

    /**
     * The two corners of the enclosing box, seen as a sequence of two positions.
     * This class holds no coordinate of its own: it reads and writes the box.
     */
    private final class Corners implements DataPoints {

        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return BBox.this.getCoordinateReferenceSystem();
        }

        @Override
        public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
            BBox.this.setCoordinateReferenceSystem(cs);
        }

        @Override
        public DataPointsType getType() {
            final DataPointsType.Template type = new DataPointsType.Template();
            type.addOrReplaceAttribute(DataPointsType.ATT_POSITION,
                    SampleSystem.of(BBox.this.getCoordinateReferenceSystem()), DataType.DOUBLE);
            return type;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public Point getPoint(int index) {
            ensureValid(index);
            return new IndexedPoint(this, index);
        }

        @Override
        public Vector<?> getPosition(int index) {
            ensureValid(index);
            final int dim = getDimension();
            final Vector<?> position = Vectors.create(BBox.this.getCoordinateReferenceSystem(), DataType.DOUBLE);
            for (int i = 0; i < dim; i++) {
                position.set(i, (index == 0) ? getMinimum(i) : getMaximum(i));
            }
            return position;
        }

        @Override
        public void setPosition(int index, ReadOnly.Vector<?> value) {
            ensureValid(index);
            final int dim = getDimension();
            for (int i = 0; i < dim; i++) {
                if (index == 0) {
                    setRange(i, value.get(i), getMaximum(i));
                } else {
                    setRange(i, getMinimum(i), value.get(i));
                }
            }
        }

        @Override
        public Vector<?> getAttribute(int index, String name) {
            return DataPointsType.ATT_POSITION.equals(name) ? getPosition(index) : null;
        }

        @Override
        public void setAttribute(int index, String name, ReadOnly.Vector<?> value) {
            if (!DataPointsType.ATT_POSITION.equals(name)) {
                throw new IllegalArgumentException("A box holds no \"" + name + "\" attribute.");
            }
            setPosition(index, value);
        }

        /**
         * Verifies that the given index is one of the two corners.
         */
        private void ensureValid(final int index) {
            if (index != 0 && index != 1) {
                throw new IndexOutOfBoundsException("A box has two corners, at index 0 and 1.");
            }
        }

        @Override
        public int hashCode() {
            return DataPoints.hashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return DataPoints.equals(this, obj);
        }
    }

}
