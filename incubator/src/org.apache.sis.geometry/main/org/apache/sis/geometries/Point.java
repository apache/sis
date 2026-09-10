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

import java.util.List;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.internal.shared.DefaultPoint;
import org.apache.sis.geometries.internal.shared.IndexedPoint;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.Tuple;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A 0-dimensional geometric primitive representing a single location in coordinate space.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The topological dimension is 0.</li>
 *   <li>The boundary of a point is always the {@link Empty} geometry,
 *       therefore a point is always a {@linkplain Geometry#isCycle() cycle}.</li>
 *   <li>A point has no {@linkplain Primitive#getSegments() segment}.</li>
 * </ul>
 *
 * <p>Note: OGC Simple Feature Access describes a point as having an x-coordinate value and a
 * y-coordinate value.</p>
 *
 * <p>Difference with ISO 19107: a point differs from a {@link org.opengis.geometry.DirectPosition}
 * in that it is an object with a system-provided identity, whereas a direct position is a data type
 * whose only identity is its own value. This interface exposes the location as a {@link Tuple}
 * instead of a direct position, in order to accommodate additional attributes like in GLTF or
 * GPU models.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.4
 * @see ISO 19107:2019 - 6.4.13
 */
@UML(identifier="Point", specification=ISO_19107)
public sealed interface Point extends Primitive
        permits DefaultPoint,
                IndexedPoint,
                ArrayDataPoints.Indexed,
                MeshPrimitive.Vertex,
                Triangle.InterpolatedPoint
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "POINT";

    /**
     * Location of this point in its reference system.
     *
     * @return point coordinate
     *
     * @see ISO 19107:2019 - 6.4.13.2
     */
    @UML(identifier="position", specification=ISO_19107)
    Tuple<?> getPosition();

    /**
     * Returns tuple for given name.
     *
     * @param name seached attribute name
     * @return attribute or null.
     */
    Tuple<?> getAttribute(String name);

    /**
     * Sets the value of the attribute of the given name.
     *
     * @param name  name of the attribute to set.
     * @param tuple new attribute value.
     */
    void setAttribute(String name, Tuple<?> tuple);

    /**
     * View this point as a single point sequence
     *
     * @return this point as a sequence of one data point.
     */
    default DataPoints asDataPoint() {
        return new DataPoints() {
            @Override
            public CoordinateReferenceSystem getCoordinateReferenceSystem() {
                return Point.this.getCoordinateReferenceSystem();
            }

            @Override
            public void setCoordinateReferenceSystem(CoordinateReferenceSystem cs) throws IllegalArgumentException {
                Point.this.setCoordinateReferenceSystem(cs);
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public Point getPoint(int index) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return Point.this;
            }

            @Override
            public Tuple getPosition(int index) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return Point.this.getPosition();
            }

            @Override
            public void setPosition(int index, Tuple<?> value) {
                if (index != 0) throw new IndexOutOfBoundsException();
                Point.this.getPosition().set(value);
            }

            @Override
            public Tuple getAttribute(int index, String name) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return Point.this.getAttribute(name);
            }

            @Override
            public void setAttribute(int index, String name, Tuple<?> value) {
                if (index != 0) throw new IndexOutOfBoundsException();
                Point.this.setAttribute(name, value);
            }

            @Override
            public AttributesType getAttributesType() {
                return Point.this.getAttributesType();
            }
        };
    }

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns 0: a point is a single location.
     *
     * @see ISO 19107:2019 - 6.4.4.22
     */
    @Override
    default int getTopologicDimension() {
        return 0;
    }

    /**
     * Returns an empty list: a point cannot be decomposed.
     *
     * @see ISO 19107:2019 - 6.4.11.2
     */
    @Override
    default List<Primitive> getSegments() {
        return List.of();
    }

    /**
     * Returns {@code true}: the boundary of a point is empty, therefore a point closes on itself.
     *
     * @see ISO 19107:2019 - 6.4.4.14
     */
    @UML(identifier="isCycle", specification=ISO_19107)
    @Override
    default boolean isCycle() {
        return true;
    }

    /**
     * Returns {@code true}: a single location can neither self-intersect nor self-tangent.
     *
     * @see ISO 19107:2019 - 6.4.4.15
     */
    @UML(identifier="isSimple", specification=ISO_19107)
    @Override
    default boolean isSimple() {
        return true;
    }

    /**
     * Returns {@code this}: a point is its own centroid.
     *
     * @see ISO 19107:2019 - 6.4.4.8
     */
    @UML(identifier="centroid", specification=ISO_19107)
    @Override
    default Point getCentroid() {
        return this;
    }

    /**
     * Returns {@code this}: a point is interior to itself.
     *
     * @see ISO 19107:2019 - 6.4.4.19
     */
    @UML(identifier="representativePoint", specification=ISO_19107)
    @Override
    default Point getRepresentativePoint() {
        return this;
    }

    @Override
    default Envelope getEnvelope() {
        final Tuple<?> first = getPosition();
        final BBox env = new BBox(first, first);
        env.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
        return env;
    }

    @Override
    default String asText() {
        final Tuple crd = getPosition();
        final StringBuilder sb = new StringBuilder("POINT (");
        AbstractGeometry.toText(sb, crd);
        sb.append(')');
        return sb.toString();
    }

    /**
     * Returns the vector, in the tangent space at this point, whose direction determines the
     * geodesic curve reaching the given position and whose length is the distance to it.
     *
     * @param  toPoint  position to reach from this point.
     * @return vector from this point to the given position.
     *
     * @see ISO 19107:2019 - 6.4.13.4
     */
    @UML(identifier="vectorToPoint", specification=ISO_19107)
    default Vector<?> vectorToPoint(DirectPosition toPoint) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the direction from this point toward the given position, without the distance.
     * This is {@link #vectorToPoint(DirectPosition)} reduced to its direction.
     *
     * @param  toPoint  position to reach from this point.
     * @return bearing from this point to the given position.
     *
     * @see ISO 19107:2019 - 6.4.13.5
     */
    @UML(identifier="bearing", specification=ISO_19107)
    default Bearing bearing(DirectPosition toPoint) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the position reached from this point by following the geodesic curve in the direction
     * of the given vector, over a distance equal to the length of that vector.
     * This solves the first geodesic problem.
     *
     * @param  bearing  vector in the tangent space at this point, giving both a direction and a distance.
     * @return position at the given bearing and distance from this point.
     *
     * @see ISO 19107:2019 - 6.4.13.6
     */
    @UML(identifier="pointAtDistance", specification=ISO_19107)
    default DirectPosition pointAtDistance(Vector<?> bearing){
        //TODO
        throw new UnsupportedOperationException();
    }

}
