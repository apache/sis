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

import java.util.Objects;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;


/**
 * Common abstraction for implementations that manage the display and user manipulation
 * of {@link MapLayer} instances. This base class makes no assumption about the geometry
 * of the display device (e.g. flat video monitor using Cartesian coordinate system, or
 * planetarium dome using spherical coordinate system).
 *
 * <p>A newly constructed {@code Canvas} is initial empty. To make something appears, at least one
 * {@link MapLayer} must be added. The visual content depends on the {@link MapLayer} data and associated style.
 * The contents are usually symbols, features or images, but some implementations can also manage non-geographic
 * elements like a map scale.</p>
 *
 * <p>There is three {@linkplain CoordinateReferenceSystem coordinate reference systems}
 * involved (at least conceptually) in rendering of geospatial data:</p>
 *
 * <ol class="verbose">
 *   <li>The <cite>data CRS</cite> is specific to the data to be displayed.
 *       It may be anything convertible to the <cite>objective CRS</cite>.
 *       Different {@link MapItem} instances may use different data CRS,
 *       potentially with a different number of dimensions.</li>
 *   <li>The {@linkplain #getObjectiveCRS objective CRS} is the common CRS in which all data
 *       are converted before to be displayed. If the objective CRS involves a map projection,
 *       it determines the deformation of shapes that user will see on the display device.
 *       The objective CRS should have the same number of dimensions than the display device
 *       (often 2). Its domain of validity should be wide enough for encompassing all data.
 *       The {@link org.apache.sis.referencing.CRS#suggestCommonTarget CRS.suggestCommonTarget(…)}
 *       method may be helpful for choosing an objective CRS from a set of data CRS.</li>
 *   <li>The <cite>device CRS</cite> is the coordinate system of the display device.
 *       The conversion from <cite>objective CRS</cite> to <cite>display CRS</cite> should
 *       be an affine transform with a scale, a translation and optionally a rotation.</li>
 * </ol>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class Canvas extends Observable {
    /**
     * The {@value} property name, used for notifications about changes in objective CRS.
     * Associated values are instances of {@link CoordinateReferenceSystem}.
     *
     * @see #getObjectiveCRS()
     * @see #setObjectiveCRS(CoordinateReferenceSystem)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public static final String OBJECTIVE_CRS_PROPERTY = "objectiveCRS";

    /**
     * The coordinate reference system to display.
     * If {@code null}, no transformation is applied on data CRS.
     *
     * @see #getObjectiveCRS()
     */
    private CoordinateReferenceSystem objectiveCRS;

    /**
     * The conversion (usually affine) from objective CRS to display coordinate system.
     * The number of source dimensions shall be the number of dimensions of {@link #objectiveCRS}.
     * The number of target dimensions shall be the number of dimensions of the display device (usually 2).
     * This transform shall never be {@code null}.
     *
     * @see #getObjectiveToDisplay()
     */
    private LinearTransform objectiveToDisplay;

    /**
     * Creates a new canvas for an output device having the specified number of dimensions.
     *
     * @param  dimension  the number of dimensions of the objective CRS.
     */
    protected Canvas(final int dimension) {
        ArgumentChecks.ensureStrictlyPositive("dimension", dimension);
        objectiveToDisplay = MathTransforms.identity(dimension);
    }

    /**
     * Returns the Coordinate Reference System in which all data are transformed before displaying.
     * The coordinate system of that CRS (Cartesian or spherical) should be related to the display
     * device coordinate system with only a final scale, a translation and optionally a rotation
     * to add.
     *
     * @return the Coordinate Reference System in which to transform all data before displaying.
     *
     * @see #OBJECTIVE_CRS_PROPERTY
     */
    public CoordinateReferenceSystem getObjectiveCRS() {
        return objectiveCRS;
    }

    /**
     * Sets the Coordinate Reference System in which all data are transformed before displaying.
     * If the given value is different than the previous value, then a change event is sent to
     * all listeners registered for the {@value #OBJECTIVE_CRS_PROPERTY} property.
     *
     * @param  newValue  the new Coordinate Reference System in which to transform all data before displaying.
     * @throws NullPointerException if the given CRS is null.
     * @throws MismatchedDimensionException if the given CRS does not have the number of dimensions of the display device.
     * @throws RenderException if the objective CRS can not be set to the given value for another reason.
     */
    public void setObjectiveCRS(final CoordinateReferenceSystem newValue) throws RenderException {
        ArgumentChecks.ensureNonNull(OBJECTIVE_CRS_PROPERTY, newValue);
        ArgumentChecks.ensureDimensionMatches(OBJECTIVE_CRS_PROPERTY, objectiveToDisplay.getSourceDimensions(), newValue);
        final CoordinateReferenceSystem oldValue = objectiveCRS;
        if (!Objects.equals(oldValue, newValue)) {
            objectiveCRS = newValue;
            firePropertyChange(OBJECTIVE_CRS_PROPERTY, oldValue, newValue);
        }
    }

    /**
     * Returns the conversion from objective CRS to display coordinate system, usually as an affine transform.
     * The number of source and target dimensions is the number of dimensions of the display device (usually 2).
     * This conversion will change every time that the user applies a zoom or a translation on the viewed data.
     *
     * @return conversion (usually affine) from objective CRS to display coordinate system.
     */
    public LinearTransform getObjectiveToDisplay() {
        return objectiveToDisplay;
    }
}
