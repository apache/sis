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
package org.apache.sis.referencing.dggs.smc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.BoundingPolygon;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.wrapper.s2.S2;
import org.apache.sis.referencing.dggs.RefinementLevel;
import org.apache.sis.referencing.dggs.internal.shared.AbstractZone;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Cell of an SMC grid, identified by its level, its row and its first column.
 * <p>
 * The column is the index of the cell south-west corner in units of the level, as the <var>i</var> index
 * of SMC cell arrays. It is therefore a multiple of the row merge factor, and 0 for polar cells.
 * Bounds are in lattice coordinates.
 *
 * @author Johann Sorel (Geomatys)
 * @see "Li 2023, SMC grid guide, section 3.1"
 */
final class SmcZone extends AbstractZone<SmcDggrs> {

    /**
     * Shape of all cells except polar ones.
     */
    static final String QUADRILATERAL = "quadrilateral";

    /**
     * Shape of the round cells centred on lattice poles.
     */
    static final String POLAR_CAP = "polar cap";

    final SmcDgg grid;
    final long row;
    final long column;

    SmcZone(final SmcDgg grid, final long row, final long column) {
        super(grid.dggrs);
        this.grid   = grid;
        this.row    = row;
        this.column = column;
    }

    /**
     * Returns whether this cell is a round cell centred on a lattice pole.
     *
     * @see "Li 2023, SMC grid guide, section 3.4"
     */
    boolean isPolar() {
        return grid.isPolarRow(row);
    }

    /**
     * Returns whether this cell is the polar cell of the lattice north pole.
     */
    boolean isNorthPolar() {
        return isPolar() && north() == 90;
    }

    long mergeFactor() {
        return grid.mergeFactor(row);
    }

    double west() {
        return grid.longitude(column);
    }

    double east() {
        return grid.longitude(column + mergeFactor());
    }

    double south() {
        return grid.latitude(row);
    }

    double north() {
        return grid.latitude(row + 1);
    }

    /**
     * Returns the longitude of the representative position. Polar cells use the domain west edge.
     */
    double centerLongitude() {
        return isPolar() ? west() : (west() + east()) / 2;
    }

    /**
     * Returns the latitude of the representative position. Polar cells use their pole.
     */
    double centerLatitude() {
        if (isPolar()) {
            return isNorthPolar() ? 90 : -90;
        }
        return (south() + north()) / 2;
    }

    /**
     * Returns the 64-bit identifier as a {@link Long}.
     */
    @Override
    public Object getIdentifier() {
        return getLongIdentifier();
    }

    @Override
    public long getLongIdentifier() {
        return grid.hierarchy().encode(this);
    }

    /**
     * Returns the {@code "level-i-j"} identifier.
     */
    @Override
    public CharSequence getTextIdentifier() {
        return grid.hierarchy().format(this);
    }

    @Override
    public String getShapeType() {
        return isPolar() ? POLAR_CAP : QUADRILATERAL;
    }

    /**
     * Returns the cell area on the sphere having the celestial body surface.
     */
    @Override
    public Double getAreaMetersSquare() {
        final double radius = dggrs.dggs.radius();
        final double width  = Math.toRadians(east() - west());
        return radius * radius * width * Math.abs(Math.sin(Math.toRadians(north())) - Math.sin(Math.toRadians(south())));
    }

    @Override
    public RefinementLevel getLocationType() {
        return new RefinementLevel(dggrs, grid.getRefinementLevel());
    }

    /**
     * Returns the cell of the previous level which contains this cell.
     *
     * @return the parent, or {@code null} at level 0.
     */
    @Override
    public SmcZone getFirstParent() {
        final int level = grid.getRefinementLevel();
        return (level > 0) ? getFirstParent(level - 1) : null;
    }

    /**
     * Returns the cell of the given level which contains this cell.
     * Since cells are nested, it is the cell covering the south-west corner of this cell.
     *
     * @throws IllegalArgumentException if the level is negative or finer than the level of this zone.
     */
    @Override
    public SmcZone getFirstParent(final int refinementLevel) {
        final int depth = grid.getRefinementLevel() - refinementLevel;
        if (refinementLevel < 0 || depth < 0) {
            throw new IllegalArgumentException("Level " + refinementLevel + " is not an ancestor level of " + this);
        }
        return grid.hierarchy().get(refinementLevel).zoneCovering(row >> depth, column >> depth);
    }

    @Override
    public List<SmcZone> getParents() {
        final SmcZone parent = getFirstParent();
        return (parent != null) ? List.of(parent) : List.of();
    }

    /**
     * Returns the cells of the next level which exactly cover this cell, row by row from south to north
     * and from west to east.
     * <p>
     * There are 4 children when the merge factor does not change. A child row merging fewer columns than
     * the row of this cell holds more children, and the children of a polar cell are the next polar cell
     * and its whole ring row.
     *
     * @return the children, or an empty list at the finest level.
     */
    @Override
    public List<SmcZone> getChildren() {
        final SmcDggh hierarchy = grid.hierarchy();
        final int level = grid.getRefinementLevel() + 1;
        if (level >= hierarchy.size()) {
            return List.of();
        }
        final SmcDgg finer = hierarchy.get(level);
        final List<SmcZone> children = new ArrayList<>();
        final long first = column << 1;
        final long end   = (column + mergeFactor()) << 1;
        finer.addZones(children, row << 1, first, end);
        finer.addZones(children, (row << 1) + 1, first, end);
        return children;
    }

    /**
     * Returns the cells of the same level which share an edge, or part of an edge, with this cell.
     * <p>
     * The neighbors of a polar cell are all cells of its ring row.
     * Cells outside a regional domain are omitted.
     *
     * @return the neighbors, without duplicates and without this cell.
     */
    @Override
    public Set<SmcZone> getNeighbors() {
        final Set<SmcZone> neighbors = new LinkedHashSet<>();
        final long end = column + mergeFactor();
        if (!isPolar()) {
            for (final long adjacent : new long[] {column - 1, end}) {
                final long c = dggrs.wraps ? Math.floorMod(adjacent, grid.columnCount) : adjacent;
                if (c >= 0 && c < grid.columnCount) {
                    neighbors.add(grid.zoneCovering(row, c));
                }
            }
        }
        if (row > 0) {
            grid.addZones(neighbors, row - 1, column, end);
        }
        if (row + 1 < grid.rowCount) {
            grid.addZones(neighbors, row + 1, column, end);
        }
        neighbors.remove(this);
        return neighbors;
    }

    @Override
    public BoundingPolygon getGeographicExtent() {
        return getGeographicExtent(1);
    }

    /**
     * Returns the cell outline with parallels densified by the given subdivision.
     */
    @Override
    public BoundingPolygon getGeographicExtent(final int subdivision) {
        try {
            return S2.toGeographicExtent(new CellOutline(this).toPolygon(subdivision));
        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns the cell envelope in the base CRS. Polar cells extend to their pole.
     */
    @Override
    public Envelope getEnvelope() {
        try {
            return new CellOutline(this).envelope();
        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns the cell centre, or the pole for polar cells, in the base CRS.
     */
    @Override
    public DirectPosition getPosition() {
        try {
            return dggrs.dggs.toPosition(centerLongitude(), centerLatitude());
        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SmcZone)) return false;
        final SmcZone other = (SmcZone) obj;
        return row == other.row && column == other.column
            && grid.getRefinementLevel() == other.grid.getRefinementLevel()
            && (dggrs == other.dggrs || dggrs.equals(other.dggrs));
    }

    @Override
    public int hashCode() {
        return (31 * Long.hashCode(row) + Long.hashCode(column)) * 37 + grid.getRefinementLevel();
    }

    @Override
    public String toString() {
        return "SmcZone[" + getTextIdentifier() + ']';
    }
}
