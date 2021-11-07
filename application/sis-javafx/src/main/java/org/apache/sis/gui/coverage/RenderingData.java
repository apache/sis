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
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.DoubleUnaryOperator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelTranslation;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.image.ErrorHandler;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.internal.coverage.j2d.ColorModelType;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.referencing.WraparoundApplicator;
import org.apache.sis.internal.processing.image.Isolines;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Debug;
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
 * @version 1.2
 * @since   1.1
 * @module
 */
final class RenderingData implements Cloneable {
    /**
     * Whether to allow the creation of {@link java.awt.image.IndexColorModel}. This flag may be temporarily set
     * to {@code false} for testing or debugging. If {@code false}, images may be only grayscale and may be much
     * slower to render, but should still be visible.
     */
    @Debug
    private static final boolean CREATE_INDEX_COLOR_MODEL = true;

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
     *
     * @see #dataGeometry
     * @see #dataRanges
     * @see #setImage(RenderedImage, GridGeometry, List)
     */
    private RenderedImage data;

    /**
     * Conversion from {@link #data} pixel coordinates to the coverage CRS, together with geospatial area.
     * It contains the {@link GridGeometry#getGridToCRS(PixelInCell)} value of {@link GridCoverage} reduced
     * to two dimensions and with a translation added for taking in account the requested {@code sliceExtent}.
     * The coverage CRS is initially the same as the {@linkplain CoverageCanvas#getObjectiveCRS() objective CRS},
     * but may become different later if user selects a different objective CRS.
     *
     * @see #data
     * @see #dataRanges
     * @see #setImage(RenderedImage, GridGeometry, List)
     */
    private GridGeometry dataGeometry;

    /**
     * Ranges of sample values in each band of {@link #data}. This is used for determining on which sample values
     * to apply colors when user asked to apply a color ramp. May be {@code null}.
     *
     * @see #data
     * @see #dataGeometry
     * @see #setImage(RenderedImage, GridGeometry, List)
     */
    private List<SampleDimension> dataRanges;

    /**
     * Conversion or transformation from {@linkplain #data} CRS to {@linkplain CoverageCanvas#getObjectiveCRS()
     * objective CRS}, or {@code null} if not yet computed. This is an identity operation if the user did not
     * selected a different CRS after the coverage has been shown.
     */
    private CoordinateOperation changeOfCRS;

    /**
     * Conversion from {@link #data} pixel coordinates to {@linkplain CoverageCanvas#getObjectiveCRS() objective CRS}.
     * This is value of {@link GridGeometry#getGridToCRS(PixelInCell)} invoked on {@link #dataGeometry}, concatenated
     * with {@link #changeOfCRS} and potentially completed by a wraparound operation.
     * May be {@code null} if not yet computed.
     */
    private MathTransform cornerToObjective;

    /**
     * Conversion from {@linkplain CoverageCanvas#getObjectiveCRS() objective CRS} to {@link #data} pixel coordinates.
     * This is the inverse of {@link #changeOfCRS} (potentially with a wraparound operation) concatenated with inverse
     * of {@link GridGeometry#getGridToCRS(PixelInCell)} on {@link #dataGeometry}.
     * May be {@code null} if not yet computed.
     */
    private MathTransform objectiveToCenter;

    /**
     * The inverse of the {@linkplain CoverageCanvas#objectiveToDisplay objective to display} transform which was
     * active at the time resampled images have been computed. The concatenation of this transform with the actual
     * "objective to display" transform at the time the rendered image is drawn should be a translation.
     * May be {@code null} if not yet computed.
     *
     * @see #getTransform(LinearTransform)
     */
    private AffineTransform displayToObjective;

    /**
     * Key of the currently selected alternative in {@link CoverageCanvas#derivedImages} map.
     *
     * @see #recolor()
     */
    Stretching selectedDerivative;

    /**
     * Statistics on pixel values of current {@link #data}, or {@code null} if none or not yet computed.
     * There is one {@link Statistics} instance per band.
     *
     * @see #recolor()
     */
    private Statistics[] statistics;

    /**
     * The processor that we use for resampling image and stretching their color ramps.
     */
    final ImageProcessor processor;

    /**
     * Creates a new instance initialized to no image.
     *
     * @param  errorHandler  where to report errors during tile computations.
     */
    RenderingData(final ErrorHandler errorHandler) {
        selectedDerivative = Stretching.NONE;
        processor = new ImageProcessor();
        processor.setErrorHandler(errorHandler);
        processor.setImageResizingPolicy(ImageProcessor.Resizing.EXPAND);
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
        objectiveToCenter = null;
    }

    /**
     * Sets the data to given image, which can be {@code null}.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    final void setImage(final RenderedImage data, final GridGeometry domain, final List<SampleDimension> ranges) {
        clearCRS();
        displayToObjective = null;
        statistics         = null;
        this.data          = data;
        this.dataGeometry  = domain;
        this.dataRanges    = ranges;        // Not cloned because already an unmodifiable list.
        if (domain != null && !domain.isDefined(GridGeometry.GRID_TO_CRS)
                           &&  domain.isDefined(GridGeometry.EXTENT))
        {
            CoordinateReferenceSystem crs = null;
            if (domain.isDefined(GridGeometry.CRS)) {
                crs = domain.getCoordinateReferenceSystem();
            }
            final GridExtent extent = domain.getExtent();
            dataGeometry = new GridGeometry(extent, PixelInCell.CELL_CENTER,
                    MathTransforms.identity(extent.getDimension()), crs);
        }
    }

    /**
     * Returns the position at the center of source data, or {@code null} if none.
     */
    private DirectPosition getSourceMedian() {
        if (dataGeometry.isDefined(GridGeometry.ENVELOPE)) {
            return AbstractEnvelope.castOrCopy(dataGeometry.getEnvelope()).getMedian();
        }
        return null;
    }

    /**
     * Stretches the color ramp of source image according the current value of {@link #selectedDerivative}.
     * This method uses the original image as the source of statistics. It saves computation time
     * (no need to recompute the statistics when the projection is changed) and provides more stable
     * visual output when standard deviations are used for configuring the color ramp.
     *
     * @return the given image with {@link #selectedDerivative} applied.
     */
    final RenderedImage recolor() {
        RenderedImage image = data;
        if (selectedDerivative != Stretching.NONE) {
            final Map<String,Object> modifiers = new HashMap<>(4);
            if (statistics == null) {
                statistics = processor.valueOfStatistics(image, null, (DoubleUnaryOperator[]) null);
            }
            modifiers.put("statistics", statistics);
            if (selectedDerivative == Stretching.AUTOMATIC) {
                modifiers.put("multStdDev", 3);
            }
            modifiers.put("sampleDimensions", dataRanges);
            image = processor.stretchColorRamp(image, modifiers);
        }
        return image;
    }

    /**
     * Creates the resampled image, then optionally applies an index color model.
     * This method will compute the {@link MathTransform} steps from image coordinate system
     * to display coordinate system if those steps have not already been computed.
     *
     * @param  recoloredImage      the image computed by {@link #recolor()}.
     * @param  objectiveCRS        value of {@link CoverageCanvas#getObjectiveCRS()}.
     * @param  objectiveToDisplay  value of {@link CoverageCanvas#getObjectiveToDisplay()}.
     * @param  objectivePOI        value of {@link CoverageCanvas#getPointOfInterest(boolean)} in objective CRS.
     * @return image with operation applied and color ramp stretched.
     */
    final RenderedImage resampleAndConvert(final RenderedImage             recoloredImage,
                                           final CoordinateReferenceSystem objectiveCRS,
                                           final LinearTransform           objectiveToDisplay,
                                           final DirectPosition            objectivePOI)
            throws TransformException
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
                final double accuracy = CRS.getLinearAccuracy(changeOfCRS);
                processor.setPositionalAccuracyHints(
//                      TODO: uncomment after https://issues.apache.org/jira/browse/SIS-497 is fixed.
//                      Quantities.create(0.25, Units.PIXEL),
                        (accuracy > 0) ? Quantities.create(accuracy, Units.METRE) : null);
            } catch (FactoryException e) {
                recoverableException(e);
                // Leave `changeOfCRS` to null.
            }
        }
        /*
         * Following transforms are computed when first needed after the new data have been specified,
         * or after the objective CRS changed. If non-null, `objToCenterNoWrap` is the same transform
         * than `objectiveToCenter` but without wraparound steps. A non-null value means that we need
         * to check if wraparound step is really needed and replace `objectiveToCenter` if it appears
         * to be unnecessary.
         */
        MathTransform objToCenterNoWrap = null;
        if (cornerToObjective == null || objectiveToCenter == null) {
            cornerToObjective = dataGeometry.getGridToCRS(PixelInCell.CELL_CORNER);
            objectiveToCenter = dataGeometry.getGridToCRS(PixelInCell.CELL_CENTER).inverse();
            if (changeOfCRS != null) {
                DirectPosition median = getSourceMedian();
                MathTransform forward = changeOfCRS.getMathTransform();
                MathTransform inverse = forward.inverse();
                MathTransform nowrap  = inverse;
                try {
                    forward = applyWraparound(forward, median, objectivePOI, changeOfCRS.getTargetCRS());
                    inverse = applyWraparound(inverse, objectivePOI, median, changeOfCRS.getSourceCRS());
                } catch (TransformException e) {
                    recoverableException(e);
                }
                if (inverse != nowrap) {
                    objToCenterNoWrap = MathTransforms.concatenate(nowrap, objectiveToCenter);
                }
                cornerToObjective = MathTransforms.concatenate(cornerToObjective, forward);
                objectiveToCenter = MathTransforms.concatenate(inverse, objectiveToCenter);
            }
        }
        /*
         * Create a resampled image for current zoom level. If the image is zoomed, the resampled image bounds
         * will be very large, potentially larger than 32 bit integer capacity (calculation done below clamps
         * the result to 32 bit integer range). This is okay since only visible tiles will be created.
         *
         * NOTE: if user pans image close to integer range limit, a new resampled image will need to be computed
         *       for shifting away from integer overflow risk situation. This check is done by the caller.
         */
        final LinearTransform inverse = objectiveToDisplay.inverse();
        displayToObjective = AffineTransforms2D.castOrCopy(inverse);
        MathTransform cornerToDisplay = MathTransforms.concatenate(cornerToObjective, objectiveToDisplay);
        MathTransform displayToCenter = MathTransforms.concatenate(inverse, objectiveToCenter);
        final Rectangle bounds = (Rectangle) Shapes2D.transform(
                MathTransforms.bidimensional(cornerToDisplay),
                ImageUtilities.getBounds(recoloredImage), new Rectangle());
        /*
         * Verify if wraparound is really necessary. We do this check because the `displayToCenter` transform
         * may be used for every pixels, so it is worth to make that transform more efficient if possible.
         */
        if (objToCenterNoWrap != null) {
            final MathTransform nowrap = MathTransforms.concatenate(inverse, objToCenterNoWrap);
            if (!isWraparoundNeeded(bounds, displayToCenter, nowrap)) {
                objectiveToCenter = objToCenterNoWrap;
                displayToCenter = nowrap;
            }
        }
        /*
         * Apply a map projection on the image, then convert the floating point results to integer values
         * that we can use with IndexColorModel.
         *
         * TODO: if `colors` is null, instead of defaulting to `Colorizer.GRAYSCALE` we should get the colors
         *       from the current ColorModel. This work should be done in Colorizer by converting the ranges of
         *       sample values in source image to ranges of sample values in destination image, then query
         *       ColorModel.getRGB(Object) for increasing integer values in that range.
         */
        if (CREATE_INDEX_COLOR_MODEL) {
            final ColorModelType ct = ColorModelType.find(recoloredImage.getColorModel());
            if (ct.isSlow || (processor.getCategoryColors() != null && ct.useColorRamp)) {
                return processor.visualize(recoloredImage, bounds, displayToCenter, dataRanges);
            }
        }
        return processor.resample(recoloredImage, bounds, displayToCenter);
    }

    /**
     * Conversion or transformation from {@linkplain CoverageCanvas#getObjectiveCRS() objective CRS} to
     * {@linkplain #data} CRS. This transform will include {@code WraparoundTransform} steps if needed.
     */
    private static MathTransform applyWraparound(final MathTransform transform, final DirectPosition sourceMedian,
            final DirectPosition targetMedian, final CoordinateReferenceSystem targetCRS) throws TransformException
    {
        if (targetMedian == null) {
            return transform;
        }
        return new WraparoundApplicator(sourceMedian, targetMedian, targetCRS.getCoordinateSystem()).forDomainOfUse(transform);
    }

    /**
     * Tests whether wraparound step seems necessary. This method transforms all corners and all centers
     * of the given rectangle using the two specified transform. If the results differ by one pixel or more,
     * the wraparound step is considered necessary.
     *
     * @param  bounds     rectangular coordinates of the display device, in pixels.
     * @param  reference  transform from display coordinates to {@link #dataGeometry} cell coordinates.
     * @param  nowrap     same as {@code reference} but with a wraparound step. Used as a reference.
     * @return {@code true} if at least one coordinate is distant from the reference coordinate by at least one pixel.
     *
     * @see org.apache.sis.coverage.grid.CoordinateOperationFinder#isWraparoundNeeded
     */
    private static boolean isWraparoundNeeded(final Rectangle bounds,
            final MathTransform reference, final MathTransform nowrap) throws TransformException
    {
        final int      numPts = 9;
        final int      srcDim = nowrap.getSourceDimensions();       // Should always be 2, but we are paranoiac.
        final int      tgtDim = nowrap.getTargetDimensions();       // Idem.
        final double[] source = new double[srcDim * numPts];
        final double[] target = new double[tgtDim * numPts];
        for (int pi=0; pi<numPts; pi++) {
            final double x, y;
            switch (pi % 3) {
                case 0:  x = bounds.getMinX();    break;
                case 1:  x = bounds.getMaxX();    break;
                default: x = bounds.getCenterX(); break;
            }
            switch (pi / 3) {
                case 0:  y = bounds.getMinY();    break;
                case 1:  y = bounds.getMaxY();    break;
                default: y = bounds.getCenterY(); break;
            }
            final int i = pi * srcDim;
            source[i  ] = x;
            source[i+1] = y;
        }
        nowrap   .transform(source, 0, target, 0, numPts);
        reference.transform(source, 0, source, 0, numPts);
        for (int i=0; i<target.length; i++) {
            final double r = source[i];
            if (!(Math.abs(target[i] - r) < 1) && Double.isFinite(r)) {     // Use `!` for catching NaN.
                return true;
            }
        }
        return false;
    }

    /**
     * Computes immediately, possibly using many threads, the tiles that are going to be displayed.
     * The returned instance should be used only for current rendering event; it should not be cached.
     *
     * @param  resampledImage      the image computed by {@link #resampleAndConvert resampleAndConvert(…)}.
     * @param  resampledToDisplay  the transform computed by {@link #getTransform(LinearTransform)}.
     * @param  displayBounds       size and location of the display device (plus margin), in pixel units.
     * @return a temporary image with tiles intersecting the display region already computed.
     */
    final RenderedImage prefetch(final RenderedImage resampledImage, final AffineTransform resampledToDisplay,
                                 final Envelope2D displayBounds)
    {
        final Rectangle areaOfInterest;
        try {
            areaOfInterest = (Rectangle) AffineTransforms2D.transform(resampledToDisplay.createInverse(), displayBounds, new Rectangle());
        } catch (NoninvertibleTransformException e) {
            recoverableException(e);
            return resampledImage;
        }
        return processor.prefetch(resampledImage, areaOfInterest);
    }

    /**
     * Gets the transform to use for painting the stretched image. If the image to draw is an instance of
     * {@link BufferedImage}, then it is okay to have any transform. However for other kinds of image,
     * it is important that the transform has scale factors of 1 and integer translations because Java2D
     * has an optimization which avoid to copy the whole data only for that case.
     */
    final AffineTransform getTransform(final LinearTransform objectiveToDisplay) {
        if (displayToObjective == null) {
            return new AffineTransform();
        }
        AffineTransform resampledToDisplay = AffineTransforms2D.castOrCopy(objectiveToDisplay);
        if (resampledToDisplay == objectiveToDisplay) {
            resampledToDisplay = new AffineTransform(resampledToDisplay);
        }
        resampledToDisplay.concatenate(displayToObjective);
        ImageUtilities.roundIfAlmostInteger(resampledToDisplay);
        return resampledToDisplay;
    }

    /**
     * Returns an estimation of the size of data pixels, in objective CRS.
     *
     * @param  objectivePOI  point of interest in objective CRS.
     * @return an estimation of the source pixel size at the given location.
     */
    final float getDataPixelSize(final DirectPosition objectivePOI) {
        if (objectiveToCenter != null) try {
            final Matrix d = objectiveToCenter.derivative(objectivePOI);
            double sum = 0;
            for (int j=d.getNumRow(); --j >= 0;) {
                for (int i=d.getNumCol(); --i >= 0;) {
                    final double v = d.getElement(j, i);
                    sum += v*v;
                }
            }
            final float r = (float) (1 / Math.sqrt(sum));
            if (r > 0 && r != Float.POSITIVE_INFINITY) {
                return r;
            }
        } catch (TransformException e) {
            recoverableException(e);
        }
        return 0;
    }

    /**
     * Converts the given bounds from objective coordinates to pixel coordinates in the source coverage.
     *
     * @param  bounds  objective coordinates.
     * @return data coverage cell coordinates (in pixels), or {@code null} if unknown.
     * @throws TransformException if the bounds can not be transformed.
     */
    final Rectangle objectiveToData(final Rectangle2D bounds) throws TransformException {
        if (objectiveToCenter == null) return null;
        return (Rectangle) Shapes2D.transform(MathTransforms.bidimensional(objectiveToCenter), bounds, new Rectangle());
    }

    /**
     * Returns whether {@link #dataGeometry} or {@link #objectiveToCenter} changed since a previous rendering.
     * This is used for information purposes only.
     */
    final boolean hasChanged(final RenderingData previous) {
        /*
         * Really !=, not Object.equals(Object), because we rely on new instances to be created
         * (even if equal) as a way to detect that cached values have not been reused.
         */
        return (previous.dataGeometry != dataGeometry) || (previous.objectiveToCenter != objectiveToCenter);
    }

    /**
     * Invoked when an exception occurred while computing a transform but the painting process can continue.
     * This method pretends that the warning come from {@link CoverageCanvas} class since it is the public API.
     */
    private static void recoverableException(final Exception e) {
        Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), CoverageCanvas.class, "render", e);
    }

    /**
     * Prepares isolines by computing the the Java2D shapes that were not already computed in a previous rendering.
     * This method shall be invoked in a background thread after image rendering has been completed (because this
     * method uses some image computation results).
     *
     * @param  isolines  value of {@link IsolineRenderer#prepare()}, or {@code null} if none.
     * @return result of isolines generation, or {@code null} if there is no isoline to compute.
     * @throws TransformException if an interpolated point can not be transformed using the given transform.
     */
    final Future<Isolines[]> generate(final IsolineRenderer.Snapshot[] isolines) throws TransformException {
        if (isolines == null) return null;
        final MathTransform centerToObjective = PixelTranslation.translate(
                cornerToObjective, PixelInCell.CELL_CORNER, PixelInCell.CELL_CENTER);
        return IsolineRenderer.generate(isolines, data, centerToObjective);
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

    /**
     * Returns a string representation for debugging purposes.
     * The string content may change in any future version.
     *
     * @see CoverageCanvas#toString()
     */
    @Override
    public String toString() {
        final String lineSeparator = System.lineSeparator();
        final StringBuilder buffer = new StringBuilder(6000);
        final TableAppender table  = new TableAppender(buffer);
        table.setMultiLinesCells(true);
        try {
            table.nextLine('═');
            table.append("Geometry of source coverage:").append(lineSeparator)
                 .append(String.valueOf(dataGeometry))
                 .appendHorizontalSeparator();
            table.append("Pixel corners to objective CRS:").append(lineSeparator)
                 .append(String.valueOf(cornerToObjective))
                 .appendHorizontalSeparator();
            table.append("Median in data CRS:").append(lineSeparator)
                 .append(String.valueOf(getSourceMedian()))
                 .nextLine();
            table.nextLine('═');
            table.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writing to `StringBuilder`.
        }
        return buffer.toString();
    }
}
