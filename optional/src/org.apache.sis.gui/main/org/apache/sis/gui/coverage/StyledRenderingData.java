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
package org.apache.sis.gui.coverage;

import java.util.Map;
import java.util.concurrent.Future;
import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.ErrorHandler;
import org.apache.sis.image.processing.isoline.Isolines;
import org.apache.sis.map.coverage.RenderingData;


/**
 * The {@code RenderedImage} to draw in a {@link CoverageCanvas} together with transform
 * from pixel coordinates to display coordinates.
 *
 * This class extends the base {@code RenderingData} with additional visual components
 * that are specific to {@link CoverageCanvas}. It may be replaced by a more generic
 * renderer in this future. So this class is maybe temporary.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StyledRenderingData extends RenderingData {
    /**
     * Key of the currently selected alternative in {@link CoverageCanvas#derivedImages} map.
     *
     * @see #recolor()
     */
    Stretching selectedDerivative;

    /**
     * Creates a new instance initialized to no image.
     *
     * @param  errorHandler  where to report errors during tile computations.
     */
    StyledRenderingData(final ErrorHandler errorHandler) {
        super(errorHandler);
        selectedDerivative = Stretching.NONE;
    }

    /**
     * Stretches the color ramp of source image according the current value of {@link #selectedDerivative}.
     * This method uses the original image as the source of statistics. It saves computation time
     * (no need to recompute the statistics when the projection is changed) and provides more stable
     * visual output when standard deviations are used for configuring the color ramp.
     *
     * @return the source image with {@link #selectedDerivative} applied.
     */
    final RenderedImage recolor() throws DataStoreException {
        RenderedImage image = getSourceImage();
        if (selectedDerivative != Stretching.NONE) {
            final Map<String, Object> modifiers = statistics();
            if (selectedDerivative == Stretching.AUTOMATIC) {
                modifiers.put("multStdDev", 3);
            }
            image = processor.stretchColorRamp(image, modifiers);
        }
        return image;
    }

    /**
     * Prepares isolines by computing the Java2D shapes that were not already computed in a previous rendering.
     * This method shall be invoked in a background thread after image rendering has been completed (because this
     * method uses some image computation results).
     *
     * @param  isolines  value of {@link IsolineRenderer#prepare()}, or {@code null} if none.
     * @return result of isolines generation, or {@code null} if there are no isolines to compute.
     * @throws TransformException if an interpolated point cannot be transformed using the given transform.
     */
    final Future<Isolines[]> generate(final IsolineRenderer.Snapshot[] isolines) throws TransformException {
        if (isolines == null) return null;
        final MathTransform centerToObjective = getDataToObjective(PixelInCell.CELL_CENTER);
        return IsolineRenderer.generate(isolines, getSourceImage(), centerToObjective);
    }

    /**
     * Creates new rendering data initialized to a copy of this instance.
     */
    @Override
    public StyledRenderingData clone() {
        return (StyledRenderingData) super.clone();
    }
}
