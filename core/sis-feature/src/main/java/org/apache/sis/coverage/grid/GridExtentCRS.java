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
package org.apache.sis.coverage.grid;

import java.util.Map;
import java.util.Collections;
import java.util.Locale;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Characters;


/**
 * Builds the engineering coordinate reference system of a {@link GridExtent}.
 * This is used only in the rare cases where we need to represent an extent as an envelope.
 * This class converts {@link DimensionNameType} codes into axis names, abbreviations and directions.
 * It is the converse of {@link GridExtent#typeFromAxes(CoordinateReferenceSystem, int)}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.2
 * @since   1.0
 * @module
 */
final class GridExtentCRS {
    /**
     * Do not allow instantiation of this class.
     */
    private GridExtentCRS() {
    }

    /**
     * Creates a properties map to give to CS, CRS or datum constructors.
     */
    private static Map<String,?> properties(final Object name) {
        return Collections.singletonMap(CoordinateSystemAxis.NAME_KEY, name);
    }

    /**
     * Creates a coordinate system axis of the given name.
     */
    private static CoordinateSystemAxis axis(final CSFactory csFactory, final String name,
            final String abbreviation, final AxisDirection direction) throws FactoryException
    {
        return csFactory.createCoordinateSystemAxis(properties(name), abbreviation, direction, Units.UNITY);
    }

    /**
     * Returns a default axis abbreviation for the given dimension.
     */
    private static String abbreviation(final int dimension) {
        final StringBuilder b = new StringBuilder(4).append('x').append(dimension);
        for (int i=b.length(); --i >= 1;) {
            b.setCharAt(i, Characters.toSuperScript(b.charAt(i)));
        }
        return b.toString();
    }

    /**
     * Builds a coordinate reference system for the given axis types. The CRS type is always engineering.
     * We can not create temporal CRS because we do not know the temporal datum origin.
     *
     * @param  gridToCRS  matrix of the transform used for converting grid cell indices to envelope coordinates.
     *         It does not matter whether it maps pixel center or corner (translation coefficients are ignored).
     * @param  types   the value of {@link GridExtent#types} or a default value (shall not be {@code null}).
     * @param  locale  locale to use for axis names, or {@code null} for default.
     * @return CRS for the grid, or {@code null}.
     *
     * @see GridExtent#typeFromAxes(CoordinateReferenceSystem, int)
     */
    static EngineeringCRS build(final Matrix gridToCRS, final DimensionNameType[] types, final Locale locale)
            throws FactoryException
    {
        final int tgtDim = gridToCRS.getNumRow() - 1;
        final int srcDim = Math.min(gridToCRS.getNumCol() - 1, types.length);
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[tgtDim];
        final CSFactory csFactory = DefaultFactories.forBuildin(CSFactory.class);
        boolean hasVertical = false;
        boolean hasTime     = false;
        boolean hasOther    = false;
        for (int i=0; i<srcDim; i++) {
            final DimensionNameType type = types[i];
            if (type != null) {
                /*
                 * Try to locate the CRS dimension corresponding to grid dimension j.
                 * We expect a one-to-one matching; if it is not the case, return null.
                 * Current version does not accept scale factors, but we could revisit
                 * in a future version if there is a need for it.
                 */
                int target = -1;
                double scale = 0;
                for (int j=0; j<tgtDim; j++) {
                    final double m = gridToCRS.getElement(j, i);
                    if (m != 0) {
                        if (target >= 0 || axes[j] != null || Math.abs(m) != 1) {
                            return null;
                        }
                        target = j;
                        scale  = m;
                    }
                }
                if (target < 0) {
                    return null;
                }
                /*
                 * This hard-coded set of axis directions is the converse of
                 * GridExtent.AXIS_DIRECTIONS map.
                 */
                String abbreviation;
                AxisDirection direction;
                if (type == DimensionNameType.COLUMN || type == DimensionNameType.SAMPLE) {
                    abbreviation = "x"; direction = AxisDirection.COLUMN_POSITIVE;
                } else if (type == DimensionNameType.ROW || type == DimensionNameType.LINE) {
                    abbreviation = "y"; direction = AxisDirection.ROW_POSITIVE;
                } else if (type == DimensionNameType.VERTICAL) {
                    abbreviation = "z"; direction = AxisDirection.UP; hasVertical = true;
                } else if (type == DimensionNameType.TIME) {
                    abbreviation = "t"; direction = AxisDirection.FUTURE; hasTime = true;
                } else {
                    abbreviation = abbreviation(target);
                    direction = AxisDirection.OTHER;
                    hasOther = true;
                }
                /*
                 * Verify that no other axis has the same direction and abbreviation. If duplicated
                 * values are found, keep only the first occurrence in grid axis order (may not be
                 * the CRS axis order).
                 */
                for (int k = tgtDim; --k >= 0;) {
                    final CoordinateSystemAxis previous = axes[k];
                    if (previous != null) {
                        if (direction.equals(AxisDirections.absolute(previous.getDirection()))) {
                            direction = AxisDirection.OTHER;
                            hasOther = true;
                        }
                        if (abbreviation.equals(previous.getAbbreviation())) {
                            abbreviation = abbreviation(target);
                        }
                    }
                }
                if (scale < 0) {
                    direction = AxisDirections.opposite(direction);
                }
                final String name = Types.toString(Types.getCodeTitle(type), locale);
                axes[target] = axis(csFactory, name, abbreviation, direction);
            }
        }
        /*
         * Search for axes that have not been created in above loop.
         * It happens when some axes have no associated `DimensionNameType` code.
         */
        for (int j=0; j<tgtDim; j++) {
            if (axes[j] == null) {
                final String name = Vocabulary.getResources(locale).getString(Vocabulary.Keys.Dimension_1, j);
                final String abbreviation = abbreviation(j);
                axes[j] = axis(csFactory, name, abbreviation, AxisDirection.OTHER);
            }
        }
        /*
         * Create a coordinate system of affine type if all axes seem spatial.
         * If no specialized type seems to fit, use an unspecified ("abstract")
         * coordinate system type in last resort.
         */
        final Map<String,?> properties = properties("Grid extent");
        final CoordinateSystem cs;
        if (hasOther || (tgtDim > (hasTime ? 1 : 3))) {
            cs = new AbstractCS(properties, axes);
        } else switch (tgtDim) {
            case 1:  {
                final CoordinateSystemAxis axis = axes[0];
                if (hasVertical) {
                    cs = csFactory.createVerticalCS(properties, axis);
                } else if (hasTime) {
                    cs = csFactory.createTimeCS(properties, axis);
                } else {
                    cs = csFactory.createLinearCS(properties, axis);
                }
                break;
            }
            case 2:  cs = csFactory.createAffineCS(properties, axes[0], axes[1]); break;
            case 3:  cs = csFactory.createAffineCS(properties, axes[0], axes[1], axes[2]); break;
            default: return null;
        }
        return DefaultFactories.forBuildin(CRSFactory.class).createEngineeringCRS(
                properties(cs.getName()), CommonCRS.Engineering.GRID.datum(), cs);
    }
}
