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
import java.util.List;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.Arc;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDRange;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vector2D;


/**
 * A chain of circular arcs, each of them centred on a control point and joining two consecutive
 * data points.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultArc extends DefaultConic implements Arc {

    /**
     * Radius vectors giving, together with the two end points, the plane and the rotation direction
     * of each arc.
     */
    protected final List<Vector> radius;

    /**
     * Creates a chain of circular arcs.
     *
     * @param  points         start and end points of the arcs, two consecutive arcs sharing a point.
     *                        There is one more point than there are arcs.
     * @param  controlPoints  centres of the circles carrying the arcs, one per arc.
     * @param  radius         radius vectors of the arcs, possibly empty.
     * @param  cycle          whether this chain closes on itself.
     */
    public DefaultArc(final DataPoints points, final Array controlPoints,
            final List<Vector> radius, final boolean cycle)
    {
        super(points, controlPoints, cycle);
        this.radius = (radius == null) ? List.of() : List.copyOf(radius);
    }

    /**
     * Returns the number of arcs in this chain, which is one less than the number of data points.
     */
    @Override
    public int getNumArc() {
        final int size = points.size();
        return (size == 0) ? 0 : size - 1;
    }

    @Override
    public List<Vector> getRadius() {
        return radius;
    }

    /**
     * Returns the smallest envelope containing this chain of arcs. That envelope contains the data points,
     * plus each point where an arc reaches an extreme along an axis.
     *
     * @throws IllegalStateException if there is not one control point per arc, or if the arc is a half turn
     *         with no radius vector in more than two dimensions.
     */
    @Override
    public Envelope getEnvelope() {
        if (points.isEmpty()) {
            final var envelope = new NDRange(getCoordinateReferenceSystem());
            envelope.setToNaN();
            return envelope;
        }
        final var envelope = new NDRange(points.getEnvelope());
        for (final Vector<?>[] arc : circularArcs()) {
            Curves.addArc(envelope, arc[0], arc[1], arc[2]);
        }
        return envelope;
    }

    /**
     * Returns a polyline whose points are on this chain of arcs, including all its data points.
     *
     * @throws IllegalStateException for the same reasons as {@link #getEnvelope()}.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        final var pieces = new ArrayList<CurveLinearizer.Piece>();
        for (final Vector<?>[] arc : circularArcs()) {
            pieces.add(CurveLinearizer.circularArc(arc[0], arc[1], arc[2], false));
        }
        if (pieces.isEmpty()) {
            return GeometryFactory.DEFAULT.createLineString(points);
        }
        return CurveLinearizer.linearize(this, spacing, offset, pieces);
    }

    /**
     * Returns the start point, a middle point and the end point of each arc. The middle point is at the
     * radius vector from the centre if there is one. Otherwise it is the middle of the shorter arc,
     * since an arc is less than a half turn.
     *
     * @throws IllegalStateException if there is not one control point per arc, or if the arc is a half turn
     *         with no radius vector in more than two dimensions.
     */
    private List<Vector<?>[]> circularArcs() {
        final int count = getNumArc();
        if (count == 0) {
            return List.of();
        }
        if (controlPoints == null || controlPoints.getLength() < count) {
            throw new IllegalStateException("A chain of " + count + " arcs needs one centre per arc as control points.");
        }
        final var arcs = new ArrayList<Vector<?>[]>(count);
        for (int i = 0; i < count; i++) {
            final Vector<?> start  = points.getPosition(i).copy(DataType.DOUBLE);
            final Vector<?> end    = points.getPosition(i + 1).copy(DataType.DOUBLE);
            final Vector<?> centre = controlPoints.get(i).copy(DataType.DOUBLE);
            final Vector<?> middle;
            if (i < radius.size()) {
                middle = centre.add(radius.get(i));
            } else {
                final double r = start.subtract(centre).length();
                final Vector<?> bisector = start.subtract(centre).add(end).subtract(centre);
                final double length = bisector.length();
                if (length > r * 1E-12) {
                    middle = centre.add(bisector.scale(r / length));
                } else if (centre.getDimension() == 2) {
                    // Half turn: counterclockwise by default.
                    middle = new Vector2D.Double(centre.get(0) - (start.get(1) - centre.get(1)),
                                                 centre.get(1) + (start.get(0) - centre.get(0)));
                } else {
                    throw new IllegalStateException("Arc " + i + " is a half turn, which plane is not determined"
                            + " without a radius vector in " + centre.getDimension() + " dimensions.");
                }
            }
            arcs.add(new Vector<?>[] {start, middle, end});
        }
        return arcs;
    }

}
