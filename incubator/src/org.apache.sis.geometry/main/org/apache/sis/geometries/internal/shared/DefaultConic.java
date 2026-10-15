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
import java.util.Objects;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.Conic;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;


/**
 * A chain of conic section arcs, each of them determined by five data points and drawn in the
 * tangent plane at one control point.
 *
 * @author Johann Sorel (Geomatys)
 */
public non-sealed class DefaultConic extends AbstractGeometry implements Conic {

    /**
     * Points lying on the conic, four of them being consumed by each arc plus the one shared with
     * the previous arc.
     */
    protected final DataPoints points;

    /**
     * Centers of the exponential maps in which the arcs are constructed, one per arc.
     * May be {@code null} if this conic declares no control point.
     */
    protected final Array controlPoints;

    /**
     * Whether this conic closes on itself.
     */
    protected final boolean cycle;

    /**
     * Creates a conic from the given data points and control points.
     *
     * @param  points         points lying on the conic. At least five of them, the first one also
     *                        acting as the last one when {@code cycle} is {@code true}.
     * @param  controlPoints  centers of the exponential maps of the arcs, or {@code null} if none.
     * @param  cycle          whether this conic closes on itself.
     */
    public DefaultConic(final DataPoints points, final Array controlPoints, final boolean cycle) {
        this.points        = Objects.requireNonNull(points);
        this.controlPoints = controlPoints;
        this.cycle         = cycle;
    }

    @Override
    public DataPoints getDataPoints() {
        return points;
    }

    @Override
    public Array getControlPoints() {
        return controlPoints;
    }

    @Override
    public boolean isCycle() {
        return cycle;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return points.getCoordinateReferenceSystem();
    }

    @Override
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        points.setCoordinateReferenceSystem(crs);
    }

    @Override
    public DataPointsType getDataPointsType() {
        return points.getType();
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    /**
     * Returns the smallest envelope containing this conic. That envelope contains the data points,
     * plus each point where an arc reaches an extreme along an axis. Each arc is computed in the plane
     * of its five data points, with the coordinates handled as Cartesian ones: the exponential map at
     * the control points is the identity.
     *
     * @throws IllegalStateException if the data points cannot be split in arcs of five points,
     *         or if the points of an arc are not coplanar, do not determine a single non-degenerate conic,
     *         are not in their order along the conic, or are joined through a point at infinity.
     */
    @Override
    public Envelope getEnvelope() {
        if (points.isEmpty()) {
            final var envelope = new GeometryEnvelope(getCoordinateReferenceSystem());
            envelope.setToNaN();
            return envelope;
        }
        final var envelope = new GeometryEnvelope(points.getEnvelope());
        for (final DefaultConicArc arc : arcs()) {
            arc.addExtremes(envelope);
        }
        return envelope;
    }

    /**
     * Returns a polyline whose points are on this conic, including all its data points.
     * Each arc is computed in the plane of its five data points, as in {@link #getEnvelope()}.
     *
     * @throws IllegalStateException for the same reasons as {@link #getEnvelope()}.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        final var pieces = new ArrayList<CurveLinearizer.Piece>();
        for (final DefaultConicArc arc : arcs()) {
            pieces.add(arc.piece());
        }
        if (pieces.isEmpty()) {
            return GeometryFactory.DEFAULT.createLineString(points);
        }
        return CurveLinearizer.linearize(this, spacing, offset, pieces);
    }

    /**
     * Returns the number of data points of each arc: 5 for a general conic.
     */
    protected int pointsPerArc() {
        return 5;
    }

    /**
     * Creates the arc going through the given data points.
     *
     * @param  p  the {@link #pointsPerArc()} data points of the arc.
     * @return the arc going through the given points.
     * @throws IllegalStateException if the points do not define a valid arc.
     */
    protected DefaultConicArc arc(final Vector<?>[] p) {
        return new DefaultConicArc(p, false);
    }

    /**
     * Returns the arcs of this conic, made of {@link #pointsPerArc()} data points each, consecutive arcs sharing a point.
     *
     * @throws IllegalStateException if the data points cannot be split in arcs, or do not define valid arcs.
     */
    private List<DefaultConicArc> arcs() {
        if (points.isEmpty()) {
            return List.of();
        }
        final var positions = new ArrayList<Vector<?>>();
        for (int i = 0; i < points.size(); i++) {
            positions.add(points.getPosition(i).copy(DataType.DOUBLE));
        }
        final int size = pointsPerArc(), step = size - 1;
        if (cycle && positions.size() % step == 0) {
            positions.add(positions.get(0));        // The first data point closes the curve.
        }
        final int n = positions.size();
        if (n < size || (n - 1) % step != 0) {
            throw new IllegalStateException("This curve is made of arcs of " + size + " data points, consecutive arcs"
                    + " sharing a point, so it needs " + step + "k+1 data points, but got " + n + '.');
        }
        final var arcs = new ArrayList<DefaultConicArc>();
        for (int i = 0; i + step < n; i += step) {
            arcs.add(arc(positions.subList(i, i + size).toArray(Vector<?>[]::new)));
        }
        return arcs;
    }
}
