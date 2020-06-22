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
import java.util.HashMap;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.PreferredSize;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.math.Statistics;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;


/**
 * The {@code RenderedImage} to draw in a {@link CoverageCanvas} together with transform
 * from pixel coordinates to display coordinates.
 *
 * <h2>Note on Java2D optimizations</h2>
 * {@link Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)} implementation
 * has the following optimizations:
 *
 * <ul class="verbose">
 *   <li>If the image is an instance of {@link BufferedImage},
 *       then the {@link AffineTransform} can be anything. Java2D applies interpolations efficiently.</li>
 *   <li>Otherwise if the {@link AffineTransform} scale factors are 1 and the translations are integers,
 *       then Java2D invokes {@link RenderedImage#getTile(int, int)}. It make possible for us to create
 *       a very large image covering the whole data but with tiles computed only when first requested.</li>
 *   <li>Otherwise Java2D invokes {@link RenderedImage#getData(Rectangle)}, which is more costly.
 *       We try to avoid that situation.</li>
 * </ul>
 *
 * Consequently our strategy is to prepare a resampled image for the whole data when the zoom level changed
 * and rely on tiling for reducing actual computations to required tiles. Since pan gestures are expressed
 * in pixel coordinates, the translation terms in {@code resampledToDisplay} transform should stay integers.
 *
 * @todo This class does not perform a special case for {@link BufferedImage}. We wait to see if this class
 *       works well in the general case before doing special cases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class RenderingData implements Cloneable {
    /**
     * The data fetched from {@link GridCoverage#render(GridExtent)} for current {@code sliceExtent}.
     * This rendered image may be tiled and fetching those tiles may require computations to be performed
     * in background threads. Pixels in this {@code data} image are mapped to pixels in the display
     * {@link CoverageCanvas#image} by the following chain of operations:
     *
     * <ol>
     *   <li><code>{@linkplain #dataGeometry}.getGridGeometry(CELL_CENTER)</code></li>
     *   <li><code>{@linkplain #changeOfCRS}.getMathTransform()</code></li>
     *   <li>{@link CoverageCanvas#getObjectiveToDisplay()}</li>
     * </ol>
     */
    private RenderedImage data;

    /**
     * Conversion from {@link #data} pixel coordinates to the coverage CRS, together with geospatial area.
     * It contains the {@link GridGeometry#getGridToCRS(PixelInCell)} value of {@link GridCoverage} reduced
     * to two dimensions and with a translation added for taking in account the requested {@code sliceExtent}.
     * The coverage CRS is initially the same as the {@linkplain CoverageCanvas#getObjectiveCRS() objective CRS},
     * but may become different later if user selects a different objective CRS.
     */
    private GridGeometry dataGeometry;

    /**
     * Conversion or transformation from {@linkplain #data} CRS to {@linkplain CoverageCanvas#getObjectiveCRS()
     * objective CRS}, or {@code null} if not yet computed. This is an identity operation if the user did not
     * selected a different CRS after the coverage has been shown.
     */
    private CoordinateOperation changeOfCRS;

    /**
     * The conversion from {@link #data} pixel coordinates to {@linkplain CoverageCanvas#getObjectiveCRS()
     * objective CRS}. This is {@link GridGeometry#getGridToCRS(PixelInCell)} on {@link #dataGeometry}
     * concatenated with {@link #changeOfCRS}. May be {@code null} if not yet computed.
     */
    private MathTransform cornerToObjective, centerToObjective;

    /**
     * The inverse of the {@linkplain CoverageCanvas#objectiveToDisplay objective to display} transform which was
     * active at the time resampled images have been computed. The concatenation of this transform with the actual
     * "objective to display" transform at the time the rendered image is drawn should be a translation.
     */
    private AffineTransform displayToObjective;

    /**
     * Key of the currently selected alternative in {@link CoverageCanvas#resampledImages} map.
     */
    Stretching selectedDerivative;

    /**
     * Statistics on pixel values of current {@link #data}, or {@code null} if none or not yet computed.
     * There is one {@link Statistics} instance per band.
     */
    private Statistics[] statistics;

    /**
     * The processor that we use for resampling image and stretching their color ramps.
     */
    private ImageProcessor processor;

    /**
     * Creates a new instance initialized to no image.
     *
     * @todo Listen to logging messages. We need to create a logging panel first.
     */
    RenderingData() {
        selectedDerivative = Stretching.NONE;
        processor = new ImageProcessor();
        processor.setErrorAction(ImageProcessor.ErrorAction.LOG);
    }

    /**
     * Returns {@code true} if this object has no data.
     */
    final boolean isEmpty() {
        return data == null;
    }

    /**
     * Verifies if this {@code RenderingData} contains an image for the given objective CRS.
     * If this is not the case, the cached resampled images will be discarded.
     *
     * @param  objectiveCRS  the coordinate reference system to use for rendering.
     * @return whether the data are valid for the given objective CRS.
     */
    final boolean validateCRS(final CoordinateReferenceSystem objectiveCRS) {
        if (changeOfCRS != null && !Utilities.equalsIgnoreMetadata(objectiveCRS, changeOfCRS.getTargetCRS())) {
            clearCRS();
            return false;
        }
        return true;
    }

    /**
     * Clears the cache of transforms that depend on the CRS.
     */
    private void clearCRS() {
        changeOfCRS       = null;
        cornerToObjective = null;
        centerToObjective = null;
    }

    /**
     * Sets the data to given image, which can be {@code null}.
     */
    final void setImage(final RenderedImage data, final GridGeometry dataGeometry) {
        clearCRS();
        displayToObjective = null;
        statistics         = null;
        this.data          = data;
        this.dataGeometry  = dataGeometry;
    }

    /**
     * Gets the interpolation method to use during resample operations.
     *
     * @see CoverageCanvas#getInterpolation()
     */
    final Interpolation getInterpolation() {
        return processor.getInterpolation();
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * @see CoverageCanvas#setInterpolation(Interpolation)
     */
    final void setInterpolation(final Interpolation newValue) {
        processor = processor.clone();          // Previous processor may be in use by background thread.
        processor.setInterpolation(newValue);
    }

    /**
     * Creates the resampled image. This method will compute the {@link MathTransform} steps from image
     * coordinate system to display coordinate system if those steps have not already been computed.
     */
    final RenderedImage resample(final CoordinateReferenceSystem objectiveCRS,
            final LinearTransform objectiveToDisplay) throws TransformException
    {
        if (changeOfCRS == null && objectiveCRS != null && dataGeometry.isDefined(GridGeometry.CRS)) {
            DefaultGeographicBoundingBox areaOfInterest = null;
            if (dataGeometry.isDefined(GridGeometry.ENVELOPE)) try {
                areaOfInterest = new DefaultGeographicBoundingBox();
                areaOfInterest.setBounds(dataGeometry.getEnvelope());
            } catch (TransformException e) {
                recoverableException(e);
                // Leave `areaOfInterest` to null.
            }
            try {
                changeOfCRS = CRS.findOperation(dataGeometry.getCoordinateReferenceSystem(), objectiveCRS, areaOfInterest);
            } catch (FactoryException e) {
                recoverableException(e);
                // Leave `changeOfCRS` to null.
            }
        }
        if (cornerToObjective == null || centerToObjective == null) {
            cornerToObjective = dataGeometry.getGridToCRS(PixelInCell.CELL_CORNER);
            centerToObjective = dataGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
            if (changeOfCRS != null) {
                final MathTransform tr = changeOfCRS.getMathTransform();
                cornerToObjective = MathTransforms.concatenate(cornerToObjective, tr);
                centerToObjective = MathTransforms.concatenate(centerToObjective, tr);
            }
        }
        /*
         * Create a resampled image for current zoom level. If the image is zoomed, the resampled image bounds
         * will be very large, potentially larger than 32 bit integer capacity (calculation done below clamps
         * the result to 32 bit integer range). This is okay since only visible tiles will be created.
         *
         * TODO: if user pans the image close to integer range limit, we should create a new resampled image
         *       shifted to new location (i.e. clear `CoverageCanvas.resampledImages` for forcing this method
         *       to be invoked again). The intent is to move away from integer overflow situation.
         */
        final LinearTransform inverse = objectiveToDisplay.inverse();
        displayToObjective = AffineTransforms2D.castOrCopy(inverse);
        final MathTransform cornerToDisplay = MathTransforms.concatenate(cornerToObjective, objectiveToDisplay);
        final MathTransform displayToCenter = MathTransforms.concatenate(inverse, centerToObjective.inverse());
        final PreferredSize bounds = (PreferredSize) Shapes2D.transform(
                MathTransforms.bidimensional(cornerToDisplay),
                ImageUtilities.getBounds(data), new PreferredSize());
        return processor.resample(data, bounds, displayToCenter);
    }

    /**
     * Applies the image operation (if any) on the given resampled image, than stretches the color ramp.
     *
     * @param  resampledImage  the image computed by {@link #resample(CoordinateReferenceSystem, LinearTransform)}.
     * @param  displayBounds   size and location of the display device, in pixel units.
     * @return image with operation applied and color ramp stretched. May be the same instance than given image.
     */
    final RenderedImage filter(RenderedImage resampledImage, final Rectangle2D displayBounds) {
        if (selectedDerivative != Stretching.NONE) {
            final Map<String,Object> modifiers = new HashMap<>(4);
            /*
             * Select the original image as the source of statistics. It saves computation time (no need
             * to recompute the statistics when the projection is changed) and provides more stable visual
             * output (color ramp computed from same standard deviation in "automatic" mode).
             */
            if (statistics == null) {
                statistics = processor.getStatistics(data, null);
            }
            modifiers.put("statistics", statistics);
            if (selectedDerivative == Stretching.AUTOMATIC) {
                modifiers.put("MultStdDev", 3);
            }
            return processor.stretchColorRamp(resampledImage, modifiers);
        }
        return resampledImage;
    }

    /**
     * Computes immediately, possibly using many threads, the tiles that are going to be displayed.
     * The returned instance should be used only for current rendering event; it should not be cached.
     *
     * @param  filteredImage       the image computed by {@link #filter(RenderedImage, Rectangle2D)}.
     * @param  resampledToDisplay  the transform computed by {@link #getTransform(LinearTransform)}.
     * @param  displayBounds       size and location of the display device, in pixel units.
     * @return a temporary image with tiles intersecting the display region already computed.
     */
    final RenderedImage prefetch(final RenderedImage filteredImage, final AffineTransform resampledToDisplay,
                                 final Envelope2D displayBounds)
    {
        try {
            return processor.prefetch(filteredImage, (Rectangle) AffineTransforms2D.transform(
                        resampledToDisplay.createInverse(), displayBounds, new Rectangle()));
        } catch (NoninvertibleTransformException e) {
            recoverableException(e);
            return filteredImage;
        }
    }

    /**
     * Gets the transform to use for painting the stretched image. If the image to draw is an instance of
     * {@link BufferedImage}, then it is okay to have any transform. However for other kinds of image,
     * it is important that the transform has scale factors of 1 and integer translations because Java2D
     * has an optimization which avoid to copy the whole data only for that case.
     */
    final AffineTransform getTransform(final LinearTransform objectiveToDisplay) {
        AffineTransform resampledToDisplay = AffineTransforms2D.castOrCopy(objectiveToDisplay);
        if (resampledToDisplay == objectiveToDisplay) {
            resampledToDisplay = new AffineTransform(resampledToDisplay);
        }
        resampledToDisplay.concatenate(displayToObjective);
        ImageUtilities.roundIfAlmostInteger(resampledToDisplay);
        return resampledToDisplay;
    }

    /**
     * Invoked when an exception occurred while computing a transform but the painting process can continue.
     * This method pretends that the warning come from {@link CoverageCanvas} class since it is the public API.
     */
    private static void recoverableException(final Exception e) {
        Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), CoverageCanvas.class, "render", e);
    }

    /**
     * Creates new rendering data initialized to a copy of this instance.
     */
    @Override
    public RenderingData clone() {
        try {
            return (RenderingData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
