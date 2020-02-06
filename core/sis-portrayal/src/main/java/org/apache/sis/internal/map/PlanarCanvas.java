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
package org.apache.sis.internal.map;

import java.util.Map;
import java.awt.geom.AffineTransform;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;


/**
 * A canvas for two-dimensional display device using a Cartesian coordinate system.
 * Data are reduced to a two-dimensional slice before to be displayed.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class PlanarCanvas extends Canvas {
    /**
     * The display Coordinate Reference System used by all {@code PlanarCanvas} instances.
     */
    private static final DefaultEngineeringCRS DISPLAY_CRS;
    static {
        Map<String,?> property = singletonMap(NAME_KEY, "Display on two-dimensional Cartesian coordinate system");
        DefaultCartesianCS cs = new DefaultCartesianCS(property,
                new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "Column"), "x", AxisDirection.DISPLAY_RIGHT, Units.PIXEL),
                new DefaultCoordinateSystemAxis(singletonMap(NAME_KEY, "Row"),    "y", AxisDirection.DISPLAY_DOWN,  Units.PIXEL));
        property = singletonMap(NAME_KEY, cs.getName());        // Reuse the same Identifier instance.
        DISPLAY_CRS = new DefaultEngineeringCRS(property, new DefaultEngineeringDatum(property), cs);
    }

    /**
     * The conversion from {@linkplain #getObjectiveCRS() objective CRS} to the display coordinate system.
     * This transform will be modified in-place when user applies zoom, translation or rotation on the view area.
     */
    private final AffineTransform objectiveToDisplay;

    /**
     * An immutable snapshot of {@link #objectiveToDisplay}, created when needed.
     * This field is reset to {@code null} when {@link #objectiveToDisplay} is modified.
     *
     * @see #getObjectiveToDisplay()
     */
    private AffineTransform2D conversionSnapshot;

    /**
     * Creates a new two-dimensional canvas.
     */
    protected PlanarCanvas() {
        super(DISPLAY_CRS);
        objectiveToDisplay = new AffineTransform();
    }

    /**
     * Returns the conversion from objective CRS to display coordinate system.
     * The number of source and target dimensions is always 2.
     * That conversion will change every time that the user zooms or scrolls on viewed data.
     *
     * @return conversion from objective CRS to display coordinate system.
     */
    @Override
    public LinearTransform getObjectiveToDisplay() {
        if (conversionSnapshot == null) {
            conversionSnapshot = new AffineTransform2D(objectiveToDisplay);
        }
        return conversionSnapshot;
    }
}
