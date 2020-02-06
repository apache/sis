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

import java.awt.geom.AffineTransform;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.operation.transform.LinearTransform;


/**
 * A canvas in which data are reduced to a two-dimensional slice before to be displayed.
 * This canvas assumes that the display device uses a Cartesian coordinate system
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Canvas2D extends Canvas {
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
    protected Canvas2D() {
        super(null);    // TODO
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
