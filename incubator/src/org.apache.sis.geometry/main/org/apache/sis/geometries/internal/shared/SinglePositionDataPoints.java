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

import java.util.function.Supplier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;


/**
 * The single position a geometry is placed by, seen as a sequence of one position. This is the
 * sequence of a shape which is defined by one position and by parameters rather than by a list
 * of coordinates: the center of a sphere, the center of an oriented box.
 *
 * <p>This class holds no position of its own. The position is read from the geometry each time
 * it is asked for, so this view follows a geometry which replaces its position, and writing
 * through it moves the geometry.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SinglePositionDataPoints implements DataPoints {

    /**
     * How to reach the position of the geometry this view is built on. It is read on each call
     * rather than kept, so that replacing the position of the geometry is seen through this view.
     */
    private final Supplier<Vector<?>> position;

    /**
     * Creates a view over the position the given supplier reads.
     *
     * @param  position  how to read the position of the geometry, not null.
     */
    public SinglePositionDataPoints(final Supplier<Vector<?>> position) {
        this.position = position;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return position.get().getSampleSystem().getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
        throw new UnsupportedOperationException("The reference system shall be set on the geometry itself.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>The type is derived from the position, which carries its own sample system and data
     * type, so that this sequence describes the single attribute it holds.</p>
     */
    @Override
    public DataPointsType getType() {
        final Vector<?> p = position.get();
        final SampleSystem ss = p.getSampleSystem();
        final DataType dt = p.getDataType();
        final DataPointsType.Template type = new DataPointsType.Template();
        type.addOrReplaceAttribute(DataPointsType.ATT_POSITION, ss, dt);
        return type;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Point getPoint(int index) {
        ensureValid(index);
        return new IndexedPoint(this, index);
    }

    @Override
    public Vector<?> getPosition(int index) {
        ensureValid(index);
        return position.get();
    }

    @Override
    public void setPosition(int index, Vector<?> value) {
        ensureValid(index);
        position.get().set(value);
    }

    @Override
    public Vector<?> getAttribute(int index, String name) {
        ensureValid(index);
        return DataPointsType.ATT_POSITION.equals(name) ? position.get() : null;
    }

    @Override
    public void setAttribute(int index, String name, Vector<?> value) {
        ensureValid(index);
        if (!DataPointsType.ATT_POSITION.equals(name)) {
            throw new IllegalArgumentException("This sequence holds no \"" + name + "\" attribute.");
        }
        position.get().set(value);
    }

    /**
     * Verifies that the given index is the only one this sequence has.
     */
    private static void ensureValid(final int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("This sequence holds a single position, at index 0.");
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
