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

import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.measure.Quantity;
import com.google.common.geometry.S2Polygon;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.dggs.internal.shared.AbstractDiscreteGlobalGrid;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;


/**
 * All SMC cells of one refinement level.
 * <p>
 * Rows and columns are counted from the south-west corner of the domain, in units of this level.
 * A cell is identified by its row and its first column, which is a multiple of the row merge factor.
 * Coordinates are lattice coordinates in degrees.
 *
 * @author Johann Sorel (Geomatys)
 * @see "Li 2023, SMC grid guide, sections 3.1 and 3.4"
 */
final class SmcDgg extends AbstractDiscreteGlobalGrid<SmcDggh> {

    final SmcDggrs dggrs;

    /**
     * Cell width (before merging) and height in degrees.
     */
    final double longitudeIncrement, latitudeIncrement;

    /**
     * Number of columns (before merging) and of rows.
     */
    final long columnCount, rowCount;

    /**
     * Highest merge exponent, such that each row holds a whole number of cells,
     * and at least 2 cells when the lattice wraps around the globe.
     */
    private final int mergeCapExponent;

    /**
     * Number of zones, computed when first needed.
     */
    private volatile long zoneCount = -1;

    SmcDgg(final SmcDggh dggh, final int level) {
        super(dggh, level);
        dggrs              = dggh.dggrs;
        longitudeIncrement = Math.scalb(dggrs.longitudeIncrement, -level);
        latitudeIncrement  = Math.scalb(dggrs.latitudeIncrement,  -level);
        columnCount        = ((long) dggrs.columnCount) << level;
        rowCount           = ((long) dggrs.rowCount)    << level;
        final int twos     = Long.numberOfTrailingZeros(columnCount);
        mergeCapExponent   = (dggrs.wraps && Long.bitCount(columnCount) == 1) ? twos - 1 : twos;
    }

    SmcDggh hierarchy() {
        return hierarchy;
    }

    /**
     * Returns whether the given row is made of a single round polar cell.
     *
     * @see "Li 2023, SMC grid guide, section 3.4"
     */
    boolean isPolarRow(final long row) {
        return (dggrs.southPolar && row == 0) || (dggrs.northPolar && row == rowCount - 1);
    }

    /**
     * Returns the number of columns merged in each cell of the given row.
     * <p>
     * It is 2<sup><var>p</var></sup>, where <var>p</var> is the number of size-changing parallels
     * arccos(2<sup>−<var>p</var></sup>) (60°, 75.5°, 82.8°, 86.4°, …) strictly crossed by the poleward edge of the row,
     * which is the largest <var>p</var> with cos φ &lt; 2<sup>−<var>p</var></sup>. A row ending exactly on a parallel,
     * such as 60°, is not merged. The exponent is capped by {@link #mergeCapExponent}.
     * Polar rows return the column count.
     *
     * @see "Li 2023, SMC grid guide, section 3.1"
     */
    long mergeFactor(final long row) {
        if (isPolarRow(row)) {
            return columnCount;
        }
        final double poleward = Math.max(Math.abs(latitude(row)), Math.abs(latitude(row + 1))) - SmcDggrs.TOLERANCE;
        final double log2 = -Math.log(Math.cos(Math.toRadians(poleward))) / Math.log(2);
        return 1L << (int) Math.max(0, Math.min(Math.ceil(log2) - 1, mergeCapExponent));
    }

    /**
     * Returns the latitude in degrees of the south edge of the given row.
     * The row after the last one gives the domain north edge. Domain edges on poles are exactly ±90°.
     */
    double latitude(final long row) {
        if (row == 0 && dggrs.southPolar) return -90;
        if (row == rowCount && dggrs.northPolar) return 90;
        return dggrs.southLatitude + row * latitudeIncrement;
    }

    /**
     * Returns the longitude in degrees of the west edge of the given column.
     */
    double longitude(final long column) {
        return dggrs.westLongitude + column * longitudeIncrement;
    }

    /**
     * Returns the zone of the given row which contains the given column.
     */
    SmcZone zoneCovering(final long row, final long column) {
        return new SmcZone(this, row, column - Math.floorMod(column, mergeFactor(row)));
    }

    /**
     * Adds the zones of the given row which overlap the given range of columns.
     *
     * @param  target  where to add the zones.
     * @param  row     row of the zones.
     * @param  from    first column, inclusive.
     * @param  to      last column, exclusive.
     */
    void addZones(final Collection<? super SmcZone> target, final long row, final long from, final long to) {
        final long step = mergeFactor(row);
        for (long column = from - Math.floorMod(from, step); column < to; column += step) {
            target.add(new SmcZone(this, row, column));
        }
    }

    /**
     * Returns the zone at the given indices.
     *
     * @throws IllegalArgumentException if the indices do not designate a cell of this level.
     */
    SmcZone zone(final long row, final long column) {
        if (row < 0 || row >= rowCount || column < 0 || column >= columnCount) {
            throw new IllegalArgumentException("No row " + row + " and column " + column + " at level " + level + '.');
        }
        final SmcZone zone = zoneCovering(row, column);
        if (zone.column != column) {
            throw new IllegalArgumentException("Column " + column + " of row " + row + " at level " + level
                    + " is not the first column of a cell. The cell starts at column " + zone.column + '.');
        }
        return zone;
    }

    /**
     * Returns the zone containing the given lattice coordinates.
     *
     * @return the zone, or {@code null} if the coordinates are outside the domain or not finite.
     */
    SmcZone zoneAt(final double longitude, final double latitude) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || latitude < latitude(0) - SmcDggrs.TOLERANCE
                || latitude > latitude(rowCount) + SmcDggrs.TOLERANCE)
        {
            return null;
        }
        final long row = Math.clamp((long) Math.floor((latitude - dggrs.southLatitude) / latitudeIncrement), 0, rowCount - 1);
        double offset = longitude - dggrs.westLongitude;
        offset -= 360 * Math.floor(offset / 360);
        if (offset >= 360 - SmcDggrs.TOLERANCE) {
            offset = 0;
        }
        if (!dggrs.wraps && offset > columnCount * longitudeIncrement + SmcDggrs.TOLERANCE) {
            return null;
        }
        return zoneCovering(row, Math.clamp((long) Math.floor(offset / longitudeIncrement), 0, columnCount - 1));
    }

    /**
     * Returns the zone containing the given lattice coordinates, or throws an exception if outside the domain.
     */
    private SmcZone zoneAtOrFail(final double longitude, final double latitude) throws TransformException {
        final SmcZone zone = zoneAt(longitude, latitude);
        if (zone == null) {
            throw new TransformException("Lattice position (" + longitude + ", " + latitude
                    + ") is outside the SMC lattice domain.");
        }
        return zone;
    }

    /**
     * Returns the cell height as a length on the sphere, which is the resolution quoted for SMC grids
     * (for example 25 km for level 7 of the global lattice).
     */
    @Override
    public Quantity<?> getPrecision() {
        return Quantities.create(Math.toRadians(latitudeIncrement) * dggrs.dggs.radius(), Units.METRE);
    }

    /**
     * Returns the zone containing the given position.
     *
     * @throws TransformException if the position cannot be converted or is outside the lattice domain.
     */
    @Override
    public SmcZone getZone(final DirectPosition position) throws TransformException {
        final double[] coordinates = dggrs.dggs.toLattice(position);
        return zoneAtOrFail(coordinates[0], coordinates[1]);
    }

    /**
     * Returns the number of zones. Runs of rows having the same merge factor are found by binary search
     * on each side of the equator, where merge factors are monotonic.
     */
    @Override
    public long getZoneCount() {
        long count = zoneCount;
        if (count < 0) {
            long low  = 0;
            long high = rowCount;
            while (low < high) {
                // Search the first row whose centre is not south of the equator.
                final long mid = (low + high) >>> 1;
                if (latitude(mid) + latitude(mid + 1) < 0) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            zoneCount = count = countZones(0, low) + countZones(low, rowCount);
        }
        return count;
    }

    /**
     * Counts the zones of rows where merge factors are monotonic.
     */
    private long countZones(final long start, final long end) {
        long total = 0;
        long row = start;
        while (row < end) {
            final long merge = mergeFactor(row);
            long low  = row;
            long high = end - 1;
            while (low < high) {
                final long mid = (low + high + 1) >>> 1;
                if (mergeFactor(mid) == merge) {
                    low = mid;
                } else {
                    high = mid - 1;
                }
            }
            total += (low - row + 1) * (columnCount / merge);
            row = low + 1;
        }
        return total;
    }

    /**
     * Returns the zones intersecting the given extent, or all zones if the extent is null.
     * All zones are streamed lazily, row by row from south to north.
     */
    @Override
    public Stream<Zone> getZones(final GeographicExtent extent) throws TransformException {
        if (extent == null) {
            return LongStream.range(0, rowCount).mapToObj(this::rowZones).flatMap((zones) -> zones);
        }
        final S2Polygon geometry = DiscreteGlobalGridSystems.toS2Polygon(extent);
        final List<Zone> roots;
        try (Stream<Zone> zones = hierarchy.get(0).getZones()) {
            roots = zones.toList();
        }
        return DiscreteGlobalGridSystems.spatialSearch(roots, level, geometry);
    }

    private Stream<Zone> rowZones(final long row) {
        final long step = mergeFactor(row);
        return LongStream.iterate(0, (column) -> column < columnCount, (column) -> column + step)
                .mapToObj((column) -> new SmcZone(this, row, column));
    }

    @Override
    public Stream<Zone> getZones(final Zone parent) throws TransformException {
        final int parentLevel = parent.getLocationType().getRefinementLevel();
        if (parentLevel > level) {
            throw new IllegalArgumentException("Parent zone is at a finer level than this grid.");
        }
        return parent.getChildrenAtRelativeDepth(level - parentLevel);
    }

    @Override
    protected long getZoneLongIdentifier(final double[] source, final int offset) throws TransformException {
        final double[] coordinates = new double[2];
        dggrs.dggs.toLattice(source, offset, coordinates, 0);
        return hierarchy.encode(zoneAtOrFail(coordinates[0], coordinates[1]));
    }

    @Override
    protected void getZonePosition(final long zoneId, final double[] target, final int offset) throws TransformException {
        final SmcZone zone = hierarchy.decode(zoneId);
        target[offset]     = zone.centerLongitude();
        target[offset + 1] = zone.centerLatitude();
        dggrs.dggs.toBase(target, offset, 1);
    }
}
