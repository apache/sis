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
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;


/**
 * Builds the coordinate reference system of a {@link GridExtent}.
 * This is used only in the rare case where we need to represent an extent as an envelope.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class GridExtentCRS {
    /**
     * The datum for grid.
     */
    private static final EngineeringDatum DATUM = new DefaultEngineeringDatum(properties("Grid"));

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
     * Builds a coordinate reference system of the given number of dimensions for the given axis types.
     */
    static EngineeringCRS build(final int dimension, final DimensionNameType[] types, final Locale locale)
            throws FactoryException
    {
        final CSFactory csFactory = DefaultFactories.forBuildin(CSFactory.class);
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[dimension];
        if (types != null) {
skip:       for (int i=0; i<dimension; i++) {
                final DimensionNameType type = types[i];
                if (type != null) {
                    final String abbreviation;
                    final AxisDirection direction;
                    if (type == DimensionNameType.ROW || type == DimensionNameType.LINE) {
                        abbreviation = "y"; direction = AxisDirection.ROW_POSITIVE;
                    } else if (type == DimensionNameType.COLUMN || type == DimensionNameType.SAMPLE) {
                        abbreviation = "x"; direction = AxisDirection.COLUMN_POSITIVE;
                    } else if (type == DimensionNameType.VERTICAL) {
                        abbreviation = "z"; direction = AxisDirection.UP;
                    } else if (type == DimensionNameType.TIME) {
                        abbreviation = "t"; direction = AxisDirection.FUTURE;
                    } else {
                        abbreviation = "d" + dimension;
                        direction = AxisDirection.OTHER;
                    }
                    // Verify that no other axis has the same direction.
                    for (int j=i; --j >= 0;) {
                        final CoordinateSystemAxis previous = axes[j];
                        if (previous != null && direction.equals(previous.getDirection())) {
                            continue skip;
                        }
                    }
                    final String name = Types.toString(Types.getCodeTitle(type), locale);
                    axes[i] = axis(csFactory, name, abbreviation, direction);
                }
            }
        }
        for (int i=0; i<dimension; i++) {
            if (axes[i] == null) {
                final String name = Vocabulary.getResources(locale).getString(Vocabulary.Keys.Dimension_1, i);
                final String abbreviation = "d" + dimension;
                axes[i] = axis(csFactory, name, abbreviation, AxisDirection.OTHER);
            }
        }
        final Map<String,?> properties = properties("Grid extent");
        final CoordinateSystem cs;
        switch (dimension) {
            case 2:  cs = csFactory.createAffineCS(properties, axes[0], axes[1]); break;
            case 3:  cs = csFactory.createAffineCS(properties, axes[0], axes[1], axes[2]); break;
            default: cs = new AbstractCS(properties, axes); break;
        }
        return DefaultFactories.forBuildin(CRSFactory.class).createEngineeringCRS(properties(cs.getName()), DATUM, cs);
    }
}
