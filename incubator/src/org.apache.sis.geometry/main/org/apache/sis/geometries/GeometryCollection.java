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

import javax.measure.Quantity;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.curve.ProductCurve;
import org.apache.sis.geometries.internal.shared.ConcatenatedDataPoints;
import org.apache.sis.geometries.internal.shared.DefaultGeometryCollection;
import org.apache.sis.geometries.internal.shared.DefaultMultiPoint;
import org.apache.sis.geometries.internal.shared.EmptyDataPoints;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;


/**
 * A typed collection of geometric objects which behaves as the set union of its elements.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the elements are in the same spatial reference system, which is also the reference
 *       system of the collection.</li>
 *   <li>Every element is an instance of one of the {@linkplain #getElementType() allowed types},
 *       or of a subtype of one of them.</li>
 *   <li>The topological dimension of the collection is the largest topological dimension among
 *       its elements.</li>
 *   <li>No other constraint is placed on the elements, but subtypes may restrict membership based
 *       on dimension and may constrain the degree of spatial overlap between elements.</li>
 * </ul>
 *
 * <p>Note: because a collection behaves as the union of its elements and not as a set of sets, an
 * implementation which needs a genuine set of sets should use a plain array of geometries instead
 * of this interface.</p>
 *
 * Deviation from OGC-SFA :<br>
 * <ul>
 *   <li>OGC-SFA defines several sub types of GeometryCollection, like MultiCurve, MultiSurface, MultiLineString, MultiPolygon, MultiPoint.
 *   The subtypes defined : getLength, isClosed(), getArea().
 *   Since ISO-19107:2019 covers a much larger scope of geometry, creating a subtype for each would duplicate the number of interfaces.
 *   ISO-19107:2019 decided instead to have a single GeometryCollection and add a getElementType restricting the possible content
 *   of the collection. The methods from OGC SFA are then moved on GeometryCollection but will raise an error if the collection element type
 *   is not applicable.
 *   </li>
 * </ul>
 * Deviation from ISO-19107 :<br>
 * <ul>
 *   <li>The returned type of getElementType() is expected to be a Set in ISO 19107, to make it more natural in Java and the Generics
 *   this Set has been reduced to a single element, which must be the first common parent.
 *   No use cases has arised to require this limitation to be redefined as a Set.
 *   </li>
 * </ul>
 *
 * @param  <T>  type of the elements of this collection.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see OGC Simple Feature Access 1.2.1 - 6.1.3
 * @see ISO 19107:2019 - 6.4.31
 */
@UML(identifier="Collection", specification=ISO_19107)
public sealed interface GeometryCollection<T extends Geometry> extends Geometry
        permits ProductCurve,
                DefaultMultiPoint,
                DefaultGeometryCollection,
                MultiMeshPrimitive,
                MeshPrimitive.Abs.Points,
                MeshPrimitive.Abs.Lines
{

    /**
     * Well-known text keyword of this geometry type.
     */
    static final String TYPE = "GEOMETRYCOLLECTION";
    static final String TYPE_MULTICURVE = "MULTICURVE";
    static final String TYPE_MULTILINESTRING = "MULTILINESTRING";
    static final String TYPE_MULTISURFACE = "MULTISURFACE";
    static final String TYPE_MULTIPOLYGON = "MULTIPOLYGON";
    static final String TYPE_MULTIPOINT = "MULTIPOINT";
    static final String TYPE_MULTIPOLYHEDRON = "MULTIPOLYHEDRON";

    /**
     * Returns {@link GeometryType#COLLECTION}.
     *
     * @see ISO 19107:2019 - 6.4.4.23
     */
    @Override
    default GeometryType getGeometryType() {
        return GeometryType.COLLECTION;
    }

    /**
     * Geometry types allowed in this collection, usually fixed at construction time
     * according to the purpose of this particular collection.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Every element of this collection is an instance of one of the returned types,
     *       or of a subtype of one of them.</li>
     *   <li>An implementation may make this set read-only when the purpose of the collection
     *       restricts the allowed types.</li>
     * </ul>
     *
     * Deviation from ISO-19107 :<br>
     * <ul>
     *   <li>The returned type is expected to be a Set in ISO 19107, to make it more natural in Java and the Generics
     *   this Set has been reduced to a single element, which must be the first common parent.
     *   </li>
     * </ul>
     *
     * @return geometry type allowed in this collection.
     *
     * @see ISO 19107:2019 - 6.4.31.2
     */
    @UML(identifier="elementType", specification=ISO_19107)
    GeometryType getElementType();

    /**
     * Returns whether this collection has no element.
     *
     * @see ISO 19107:2019 - 6.4.4.2
     */
    @Override
    default boolean isEmpty() {
        return getNumGeometries() == 0;
    }

    /**
     * Returns the largest topological dimension among the elements of this collection which contain
     * the given position, or −1 if none of them contains it. If the given position is null, returns
     * the {@linkplain #getTopologicDimension() largest dimension} among all elements.
     *
     * <p>Containment is tested with {@link Geometry#intersects(Geometry)}, which may be unsupported
     * for some element types.</p>
     *
     * @param  point  position where to evaluate the dimension, or null for the whole collection.
     * @return topological dimension at the given position.
     *
     * @see ISO 19107:2019 - 6.4.4.25
     */
    @Override
    default int getDimension(DirectPosition point) {
        if (point == null) {
            return getTopologicDimension();
        }
        Point position = null;
        int dimension = -1;
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            final Geometry element = getGeometryN(i);
            if (element.getTopologicDimension() <= dimension) {
                continue;       // Cannot increase the result, no need to test containment.
            }
            if (element instanceof GeometryCollection<?> aggregate) {
                dimension = Math.max(dimension, aggregate.getDimension(point));
            } else {
                if (position == null) {
                    position = GeometryFactory.DEFAULT.createPoint(getCoordinateReferenceSystem(), point.getCoordinates());
                }
                if (element.intersects(position)) {
                    dimension = element.getTopologicDimension();
                }
            }
        }
        return dimension;
    }

    /**
     * Returns the largest topological dimension among the elements of this collection,
     * or −1 if this collection is empty.
     *
     * @see ISO 19107:2019 - 6.4.4.22
     */
    @Override
    default int getTopologicDimension() {
        int dimension = -1;
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            dimension = Math.max(dimension, getGeometryN(i).getTopologicDimension());
        }
        return dimension;
    }

    /**
     * Returns the number of geometries in this GeometryCollection.
     * It can only change by adding or removing elements.
     *
     * @return the number of geometries in this GeometryCollection.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.3.2
     * @see ISO 19107:2019 - 6.4.31.3
     */
    @UML(identifier="numElement", specification=ISO_19107)
    int getNumGeometries();

    /**
     * Returns the Nth geometry in this GeometryCollection.
     *
     * <p>Difference with ISO 19107:</p>
     * <ul>
     *   <li>for {@code add} as defined by ISO 19107, use the union operator of
     *       {@link org.apache.sis.geometries.operation.GeometryProcessor};</li>
     *   <li>for {@code remove} as defined by ISO 19107, use the difference operator of
     *       {@link org.apache.sis.geometries.operation.GeometryProcessor}.</li>
     * </ul>
     *
     * @param n geometry index.
     * @return the Nth geometr in this GeometryCollection.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.3.2
     * @see ISO 19107:2019 - 6.4.32, 6.4.32.2, 6.4.32.3
     */
    @UML(identifier="element", specification=ISO_19107)
    T getGeometryN(int n);

    @Override
    default Envelope getEnvelope() {
        GeneralEnvelope e = null;
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            T sn = getGeometryN(i);
            Envelope envelope = sn.getEnvelope();
            if (envelope != null) {
                if (e == null) {
                    e = new GeneralEnvelope(envelope);
                } else {
                    e.add(envelope);
                }
            }
        }
        if (e == null) {
            e = new GeneralEnvelope(getCoordinateReferenceSystem());
            e.setToNaN();
        }
        return e;
    }

    @Override
    default DataPointsType getDataPointsType() {
        if (getNumGeometries() == 0) return null;
        return getGeometryN(0).getDataPointsType();
    }

    /**
     * Returns the positions of the elements of this collection, in the order this collection
     * holds them. A position held by two elements appears as many times as it is held.
     */
    @Override
    default DataPoints getDataPoints() {
        final int n = getNumGeometries();
        if (n == 0) {
            return new EmptyDataPoints(getCoordinateReferenceSystem());
        }
        final Geometry[] elements = new Geometry[n];
        for (int i = 0; i < n; i++) {
            elements[i] = getGeometryN(i);
        }
        return ConcatenatedDataPoints.of(elements);
    }

    /**
     * Returns TRUE if this MultiCurve is closed [StartPoint () = EndPoint () for each Curve in this MultiCurve].
     *
     * Can be called only if collection element type is made of Curve.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.8.2
     * @return true if multicurve is closed.
     */
    default boolean isClosed() {
        if (!GeometryType.CURVE.isAssignableFrom(getElementType())) {
            throw new IllegalArgumentException("Collection must be composed of " + GeometryType.CURVE + " to call this method.");
        }
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            if (!((Curve) getGeometryN(i)).isClosed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The Length of this MultiCurve which is equal to the sum of the lengths of the element Curves.
     * The unit of measurement is the one of the first element,
     * or dimensionless if this MultiCurve has no element.
     *
     * Can be called only if collection element type is made of Curve.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.8.2
     * @return length of the multicurve.
     */
    default Quantity<?> getLength() {
        if (!GeometryType.CURVE.isAssignableFrom(getElementType())) {
            throw new IllegalArgumentException("Collection must be composed of " + GeometryType.CURVE + " to call this method.");
        }
        final int n = getNumGeometries();
        if (n == 0) {
            return Quantities.create(0, Units.UNITY);
        }
        Quantity<?> length = ((Curve) getGeometryN(0)).getLength();
        for (int i = 1; i < n; i++) {
            length = Geometries.add(length, ((Curve)getGeometryN(i)).getLength());
        }
        return length;
    }

    /**
     * The area of this MultiSurface, as measured in the spatial reference system of this MultiSurface.
     * The unit of measurement is the one of the first element,
     * or dimensionless if this MultiSurface has no element.
     *
     * Can be called only if collection element type is made of Surface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.13.2
     * @return area of the surface.
     */
    default Quantity<?> getArea() {
        if (!GeometryType.SURFACE.isAssignableFrom(getElementType())) {
            throw new IllegalArgumentException("Collection must be composed of " + GeometryType.SURFACE + " to call this method.");
        }
        final int n = getNumGeometries();
        if (n == 0) {
            return Quantities.create(0, Units.UNITY);
        }
        Quantity<?> area = ((Surface)getGeometryN(0)).getArea();
        for (int i = 1; i < n; i++) {
            area = Geometries.add(area,  ((Surface)getGeometryN(i)).getArea());
        }
        return area;
    }

    /**
     * A Point guaranteed to be on this MultiSurface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.13.2
     * @return point guaranteed to be on this MultiSurface.
     */
    default Point getPointOnSurface() {
        return getRepresentativePoint();
    }

}
