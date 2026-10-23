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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Prism;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;


/**
 * A base shape extruded from an optional lower limit to an upper limit.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultPrism extends AbstractGeometry implements Prism {

    /**
     * Base shape which is extruded.
     */
    protected final Geometry base;

    /**
     * Lower and upper limits of the extrusion.
     */
    protected final NumberRange<?> extrusionRange;

    /**
     * Coordinate reference system in which the extrusion range is expressed.
     */
    protected final SingleCRS extrusionCrs;

    /**
     * Creates a prism by extruding the given base shape.
     *
     * @param  base            base shape of the prism, for example a polygon or a circle.
     * @param  extrusionRange  lower and upper limits of the extrusion.
     * @param  extrusionCrs    coordinate reference system of the extrusion range.
     */
    public DefaultPrism(final Geometry base, final NumberRange<?> extrusionRange, final SingleCRS extrusionCrs) {
        this.base           = Objects.requireNonNull(base);
        this.extrusionRange = Objects.requireNonNull(extrusionRange);
        this.extrusionCrs   = extrusionCrs;
    }

    @Override
    public Geometry getBase() {
        return base;
    }

    @Override
    public NumberRange<?> getExtrusionRange() {
        return extrusionRange;
    }

    @Override
    public SingleCRS getExtrusionCrs() {
        return extrusionCrs;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return base.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        base.setCoordinateReferenceSystem(crs);
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    /**
     * Returns the envelope of the base extended by the extrusion range. If the reference system of the base
     * contains the extrusion reference system, the range extends the corresponding axis. Otherwise the
     * extrusion adds a dimension, and the envelope is in the compound of the two reference systems.
     */
    @Override
    public Envelope getEnvelope() {
        final Envelope baseEnvelope = base.getEnvelope();
        if (baseEnvelope == null) {
            return null;
        }
        final double min = extrusionRange.getMinDouble();
        final double max = extrusionRange.getMaxDouble();
        final CoordinateReferenceSystem baseCRS = base.getCoordinateReferenceSystem();
        if (extrusionCrs != null) {
            int offset = 0;
            for (final SingleCRS component : CRS.getSingleComponents(baseCRS)) {
                final int dimension = component.getCoordinateSystem().getDimension();
                if (Utilities.equalsIgnoreMetadata(component, extrusionCrs)) {
                    final var envelope = new GeneralEnvelope(baseEnvelope);
                    for (int i = offset; i < offset + dimension; i++) {
                        envelope.setRange(i, Math.min(envelope.getMinimum(i), min),
                                             Math.max(envelope.getMaximum(i), max));
                    }
                    return envelope;
                }
                offset += dimension;
            }
        }
        final int n = baseEnvelope.getDimension();
        final GeneralEnvelope envelope;
        if (extrusionCrs != null) {
            try {
                envelope = new GeneralEnvelope(CRS.compound(baseCRS, extrusionCrs));
            } catch (FactoryException e) {
                throw new IllegalStateException("Cannot combine the base and extrusion reference systems.", e);
            }
        } else {
            envelope = new GeneralEnvelope(n + 1);
        }
        for (int i = 0; i < n; i++) {
            envelope.setRange(i, baseEnvelope.getMinimum(i), baseEnvelope.getMaximum(i));
        }
        envelope.setRange(n, min, max);
        return envelope;
    }

    /**
     * Returns the geometry covered by the extrusion of the base.
     */
    @Override
    public Geometry toExtrusion() {
        return new Extrusion().extrude(base);
    }

    /**
     * Builds the extrusion of the base and of its elements. This class holds the reference system of the
     * extruded geometries and the axis on which the extrusion is done, which are the same for all elements.
     */
    private final class Extrusion {
        /**
         * Factory of the extruded geometries.
         */
        private final GeometryFactory factory = GeometryFactory.DEFAULT;

        /**
         * Reference system of the extruded geometries.
         */
        private final CoordinateReferenceSystem crs;

        /**
         * Index of the extrusion axis in {@link #crs}, which is the dimension of the base if the extrusion adds a dimension.
         */
        private final int axis;

        /**
         * Lower and upper limits of the extrusion.
         */
        private final double min, max;

        /**
         * Finds the reference system and the axis of the extrusion.
         * The extrusion is along an axis of the base if the extrusion reference system is a component
         * of the base one. Otherwise the extrusion adds a dimension after the dimensions of the base.
         */
        Extrusion() {
            if (extrusionCrs != null && extrusionCrs.getCoordinateSystem().getDimension() != 1) {
                throw new UnsupportedOperationException("The extrusion of a prism is computed along a single axis only.");
            }
            final CoordinateReferenceSystem baseCRS = base.getCoordinateReferenceSystem();
            final int baseDimension = baseCRS.getCoordinateSystem().getDimension();
            CoordinateReferenceSystem target = null;
            int index = baseDimension;
            if (extrusionCrs != null) {
                int offset = 0;
                for (final SingleCRS component : CRS.getSingleComponents(baseCRS)) {
                    if (Utilities.equalsIgnoreMetadata(component, extrusionCrs)) {
                        target = baseCRS;
                        index  = offset;
                        break;
                    }
                    offset += component.getCoordinateSystem().getDimension();
                }
            }
            if (target == null) {
                if (extrusionCrs == null) {
                    target = Geometries.getUndefinedCRS(baseDimension + 1);
                } else {
                    try {
                        target = CRS.compound(baseCRS, extrusionCrs);
                    } catch (FactoryException e) {
                        throw new IllegalStateException("Cannot combine the base and extrusion reference systems.", e);
                    }
                }
            }
            crs  = target;
            axis = index;
            min  = extrusionRange.getMinDouble();
            max  = extrusionRange.getMaxDouble();
        }

        /**
         * Returns the extrusion of the given geometry, which is the base or one of its elements.
         */
        Geometry extrude(final Geometry geometry) {
            if (geometry instanceof Point point) {
                return extrude(point);
            } else if (geometry instanceof LineString line) {
                return extrude(line);
            } else if (geometry instanceof Polygon polygon) {
                return extrude(polygon);
            } else if (geometry instanceof GeometryCollection<?> collection) {
                return extrude(collection);
            }
            throw new UnsupportedOperationException("Cannot extrude a " + geometry.getGeometryType() + '.');
        }

        /**
         * Returns the extrusion of a point, which is the line string from the lower limit to the upper limit.
         */
        private LineString extrude(final Point point) {
            return factory.createLineString(copy(point.getDataPoints(), new int[] {0, 0}, new double[] {min, max}));
        }

        /**
         * Returns the extrusion of a line string, which is a polyhedral surface
         * made of one quadrilateral for each segment of the line string.
         */
        private PolyhedralSurface<Polygon> extrude(final LineString line) {
            final DataPoints points = line.getDataPoints();
            final int[] order = new int[points.size()];
            Arrays.setAll(order, (i) -> i);
            final var faces = new ArrayList<Polygon>();
            walls(points, order, faces);
            return factory.createPolyhedralSurface(crs, faces.toArray(Polygon[]::new));
        }

        /**
         * Returns the extrusion of a polygon, which is a polyhedron whose exterior shell is made of the polygon
         * at the lower limit, the polygon at the upper limit and the walls of the extrusion of its rings.
         *
         * <p>The rings of the polygon are first ordered so that, seen from the upper side of the extrusion axis,
         * the exterior one is counterclockwise and the holes are clockwise. The upper face then keeps that order,
         * the lower face reverses it, and the walls are built as for line strings, which makes all the faces
         * oriented outward.</p>
         */
        private Polyhedron extrude(final Polygon polygon) {
            if (crs.getCoordinateSystem().getDimension() != 3) {
                throw new UnsupportedOperationException("The extrusion of a polygon is computed in 3 dimensions only.");
            }
            if (polygon.isEmpty()) {
                return factory.createPolyhedron(factory.createMultiPolygon(crs), List.of());
            }
            /*
             * The two other axes, in increasing order, followed by the extrusion axis form a right-handed
             * system if they are an even permutation of (0, 1, 2), which is the case unless the extrusion
             * axis is in the middle. In a left-handed system, the counterclockwise order is reversed.
             */
            final int h0 = (axis == 0) ? 1 : 0;
            final int h1 = (axis == 2) ? 1 : 2;
            final boolean rightHanded = (axis != 1);
            final var rings = new ArrayList<DataPoints>();
            rings.add(polygon.getExteriorRing().getDataPoints());
            for (final LinearRing hole : polygon.getInteriorRings()) {
                rings.add(hole.getDataPoints());
            }
            final var orders = new ArrayList<int[]>(rings.size());
            for (int k = 0; k < rings.size(); k++) {
                orders.add(order(rings.get(k), h0, h1, (k == 0) == rightHanded));
            }
            final var faces = new ArrayList<Polygon>();
            faces.add(cap(rings, orders, min, true));
            faces.add(cap(rings, orders, max, false));
            for (int k = 0; k < rings.size(); k++) {
                walls(rings.get(k), orders.get(k), faces);
            }
            return factory.createPolyhedron(factory.createMultiPolygon(crs, faces.toArray(Polygon[]::new)), List.of());
        }

        /**
         * Returns the collection of the extrusions of the elements of the given collection.
         */
        private GeometryCollection<Geometry> extrude(final GeometryCollection<?> collection) {
            final var elements = new Geometry[collection.getNumGeometries()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = extrude(collection.getGeometryN(i));
            }
            final GeometryType type = switch (collection.getElementType()) {
                case POINT                  -> GeometryType.LINESTRING;
                case LINESTRING, LINEARRING -> GeometryType.POLYHEDRALSURFACE;
                case POLYGON, TRIANGLE      -> GeometryType.POLYHEDRON;
                default                     -> GeometryType.GEOMETRY;
            };
            return factory.createGeometryCollection(type, crs, elements);
        }

        /**
         * Adds to the given list the walls of the extrusion of the given positions. The wall of the segment
         * going from <var>a</var> to <var>b</var> goes from the lower to the upper limit along <var>b</var>,
         * then back to the lower limit along <var>a</var>.
         *
         * @param  points  the positions to extrude.
         * @param  order   indices of the positions in the order of the segments.
         * @param  faces   where to add the walls.
         */
        private void walls(final DataPoints points, final int[] order, final List<Polygon> faces) {
            for (int i = 1; i < order.length; i++) {
                final int a = order[i-1];
                final int b = order[i];
                if (!isRepeated(points, a, b)) {
                    faces.add(factory.createPolygon(factory.createLinearRing(copy(points,
                            new int[]    {a,   b,   b,   a,   a},
                            new double[] {min, min, max, max, min})), null));
                }
            }
        }

        /**
         * Returns whether the two given positions have the same coordinates, the extrusion one excepted.
         * The extrusion of the segment joining such positions has no area.
         */
        private boolean isRepeated(final DataPoints points, final int a, final int b) {
            final double[] pa = points.getPosition(a).toArrayDouble();
            final double[] pb = points.getPosition(b).toArrayDouble();
            for (int d = 0; d < pa.length; d++) {
                if (d != axis && pa[d] != pb[d]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns the polygon at the given extrusion value, as a face of its extrusion.
         *
         * @param  rings    positions of the exterior ring of the polygon followed by those of its holes.
         * @param  orders   order of the positions of each ring for a face oriented toward the upper limit.
         * @param  value    the extrusion value of all positions of the face.
         * @param  reverse  whether to reverse the orders, for a face oriented toward the lower limit.
         */
        private Polygon cap(final List<DataPoints> rings, final List<int[]> orders, final double value, final boolean reverse) {
            final var faceRings = new ArrayList<LinearRing>(rings.size());
            for (int k = 0; k < rings.size(); k++) {
                final int[] order = orders.get(k);
                final int n = order.length;
                final int[] indices = new int[n];
                for (int i = 0; i < n; i++) {
                    indices[i] = order[reverse ? n - 1 - i : i];
                }
                final double[] values = new double[n];
                Arrays.fill(values, value);
                faceRings.add(factory.createLinearRing(copy(rings.get(k), indices, values)));
            }
            return factory.createPolygon(faceRings.get(0), faceRings.subList(1, faceRings.size()));
        }

        /**
         * Returns copies of the given positions of the source with the given extrusion values.
         * The position at index <var>i</var> of the returned {@code DataPoints} is a copy of the
         * position at index {@code indices[i]} of the source, with all its attributes, except
         * that its coordinate on the extrusion axis is {@code values[i]}.
         *
         * @param  source   positions to copy.
         * @param  indices  indices in the source of the positions to copy.
         * @param  values   extrusion value of each copied position.
         */
        private DataPoints copy(final DataPoints source, final int[] indices, final double[] values) {
            final DataPoints copy = source.subset(indices);
            final Map<String,Array> attributes = new LinkedHashMap<>();
            for (final String name : copy.getType().getAttributeNames()) {
                attributes.put(name, copy.getAttributeArray(name));
            }
            //the positions are in the reference system of the extrusion, which may have one more dimension.
            final Array positions = NDArrays.of(SampleSystem.of(crs),
                    copy.getType().getAttributeType(DataPointsType.ATT_POSITION), indices.length);
            for (int i = 0; i < indices.length; i++) {
                Vector<?> position = copy.getPosition(i);
                if (axis < position.getDimension()) {
                    final double[] coordinates = position.toArrayDouble();
                    coordinates[axis] = values[i];
                    position = Vectors.create(coordinates, position.getDataType());
                } else {
                    position = position.extend(values[i]);
                }
                positions.set(i, position);
            }
            attributes.put(DataPointsType.ATT_POSITION, positions);
            return factory.createDataPoints(attributes);
        }
    }

    /**
     * Returns the indices of the positions of the given closed ring, in their order or in reverse order,
     * so that the ring is counterclockwise or clockwise in the plane of the given axes.
     *
     * @param  ring              positions of the ring, the last one being equal to the first one.
     * @param  h0                index of the first axis of the plane.
     * @param  h1                index of the second axis of the plane.
     * @param  counterclockwise  whether the returned order shall be counterclockwise.
     */
    private static int[] order(final DataPoints ring, final int h0, final int h1, final boolean counterclockwise) {
        final int n = ring.size();
        double area = 0;
        Vector<?> p = ring.getPosition(n - 1);
        double px = p.get(h0);
        double py = p.get(h1);
        for (int i = 0; i < n; i++) {
            p = ring.getPosition(i);
            final double x = p.get(h0);
            final double y = p.get(h1);
            area += px * y - x * py;
            px = x;
            py = y;
        }
        final boolean reverse = (area > 0) != counterclockwise;
        final int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = reverse ? n - 1 - i : i;
        }
        return order;
    }

    @Override
    public Geometry getBoundary() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
