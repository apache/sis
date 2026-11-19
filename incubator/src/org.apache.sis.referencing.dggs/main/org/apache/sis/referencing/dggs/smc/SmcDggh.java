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

import org.apache.sis.referencing.dggs.internal.shared.AbstractDiscreteGlobalGridHierarchy;


/**
 * Hierarchy of SMC grids, from level 0 to the finest level whose identifiers fit in 64 bits.
 * <p>
 * Zone identifiers have two forms:
 * <ul>
 *   <li>64-bit integers: bits 58 to 62 hold the level, followed by the row and then the column, both sized for
 *       the finest level. The sign bit is always 0, so identifiers sort by level, then row, then column.</li>
 *   <li>Texts of the form {@code "level-i-j"}, where <var>i</var> is the column and <var>j</var> the row
 *       of the cell south-west corner in units of the level. Example: {@code "7-1020-412"}.</li>
 * </ul>
 * Methods accepting an identifier of unknown form accept both, and SMC zones.
 *
 * @author Johann Sorel (Geomatys)
 * @see "Li 2023, SMC grid guide, section 3.1"
 */
final class SmcDggh extends AbstractDiscreteGlobalGridHierarchy<SmcDggrs> {

    /**
     * Number of bits available for row and column indices in long identifiers.
     */
    private static final int INDEX_BITS = 58;

    /**
     * Highest level which can be stored in the 5 remaining bits of long identifiers.
     */
    private static final int LEVEL_LIMIT = 31;

    private static final char SEPARATOR = '-';

    private final SmcDgg[] grids;

    /**
     * Number of bits holding the column index in long identifiers.
     */
    private final int columnBits;

    /**
     * Creates the grids of all levels whose identifiers fit in 64 bits.
     *
     * @throws IllegalArgumentException if the indices of level 0 do not fit in identifiers.
     */
    SmcDggh(final SmcDggrs dggrs) {
        super(dggrs);
        final int levelZeroBits = bitLength(dggrs.columnCount) + bitLength(dggrs.rowCount);
        if (levelZeroBits > INDEX_BITS) {
            throw new IllegalArgumentException("Level 0 indices need " + levelZeroBits
                    + " bits, but identifiers have only " + INDEX_BITS + '.');
        }
        grids = new SmcDgg[Math.min(LEVEL_LIMIT, (INDEX_BITS - levelZeroBits) / 2) + 1];
        for (int i = 0; i < grids.length; i++) {
            grids[i] = new SmcDgg(this, i);
        }
        columnBits = bitLength(grids[grids.length - 1].columnCount);
    }

    /**
     * Returns the number of bits needed for the indices from 0 inclusive to the given count exclusive.
     */
    private static int bitLength(final long count) {
        return Long.SIZE - Long.numberOfLeadingZeros(count - 1);
    }

    @Override
    public SmcDgg get(final int level) {
        return grids[level];
    }

    @Override
    public int size() {
        return grids.length;
    }

    @Override
    public boolean supportLongIdentifiers() {
        return true;
    }

    @Override
    public String toTextIdentifier(final Object zoneId) throws IllegalArgumentException {
        return format(getZone(zoneId));
    }

    @Override
    public long toLongIdentifier(final Object zoneId) throws IllegalArgumentException {
        return encode(getZone(zoneId));
    }

    /**
     * Returns the zone designated by the given identifier.
     *
     * @param  identifier  a {@link Long}, a {@link CharSequence} or an SMC zone.
     * @throws IllegalArgumentException if the identifier type is unsupported or designates no cell.
     */
    @Override
    public SmcZone getZone(final Object identifier) throws IllegalArgumentException {
        if (identifier instanceof SmcZone zone) {
            return zone;
        } else if (identifier instanceof CharSequence text) {
            return parse(text);
        } else if (identifier instanceof Long id) {
            return decode(id);
        }
        throw new IllegalArgumentException("Unsupported SMC zone identifier: " + identifier);
    }

    long encode(final SmcZone zone) {
        return (((long) zone.grid.getRefinementLevel()) << INDEX_BITS) | (zone.row << columnBits) | zone.column;
    }

    /**
     * Returns the zone of the given 64-bit identifier.
     *
     * @throws IllegalArgumentException if the identifier designates no cell.
     */
    SmcZone decode(final long identifier) {
        if (identifier < 0) {
            throw new IllegalArgumentException("Negative SMC zone identifier: " + identifier);
        }
        final long indices = identifier & ((1L << INDEX_BITS) - 1);
        return grid((int) (identifier >>> INDEX_BITS))
                .zone(indices >>> columnBits, indices & ((1L << columnBits) - 1));
    }

    String format(final SmcZone zone) {
        return Integer.toString(zone.grid.getRefinementLevel()) + SEPARATOR + zone.column + SEPARATOR + zone.row;
    }

    /**
     * Returns the zone of the given {@code "level-i-j"} identifier.
     *
     * @throws IllegalArgumentException if the identifier is malformed or designates no cell.
     */
    SmcZone parse(final CharSequence identifier) {
        final String text = identifier.toString().trim();
        final int first  = text.indexOf(SEPARATOR);
        final int second = text.indexOf(SEPARATOR, first + 1);
        if (first <= 0 || second <= first + 1 || second == text.length() - 1
                || text.indexOf(SEPARATOR, second + 1) >= 0)
        {
            throw new IllegalArgumentException("SMC zone identifier must have the \"level-i-j\" form: " + text);
        }
        try {
            final int  level  = Integer.parseInt(text, 0, first, 10);
            final long column = Long.parseLong(text, first + 1, second, 10);
            final long row    = Long.parseLong(text, second + 1, text.length(), 10);
            return grid(level).zone(row, column);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SMC zone identifier must have the \"level-i-j\" form: " + text, e);
        }
    }

    /**
     * Returns the grid of the given level.
     *
     * @throws IllegalArgumentException if the level does not exist.
     */
    private SmcDgg grid(final int level) {
        if (level < 0 || level >= grids.length) {
            throw new IllegalArgumentException("Level " + level + " is outside the 0 … " + (grids.length - 1) + " range.");
        }
        return grids[level];
    }
}
