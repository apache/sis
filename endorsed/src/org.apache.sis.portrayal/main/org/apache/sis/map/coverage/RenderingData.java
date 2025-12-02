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
package org.apache.sis.map.coverage;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;
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
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.PixelTranslation;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.image.ImageLayout;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.ErrorHandler;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.internal.shared.ColorModelType;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.coverage.internal.shared.SampleDimensions;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.WraparoundApplicator;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.CloneAccess;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.io.TableAppender;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.portrayal.PlanarCanvas;       // For javadoc.


/**
 * The {@code RenderedImage} to draw in a {@link PlanarCanvas} together with transforms from pixel coordinates
 * to display coordinates. This is a helper class for implementations of stateful renderer.
 * All grid geometries and transforms managed by this class are two-dimensional.
 * If the source data have more dimensions, a two-dimensional slice will be taken.
 *
 * <h2>Note on Java2D optimizations</h2>
 * {@link Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)} implementation
 * has the following optimizations:
 *
 * <ul class="verbose">
 *   <li>If the image is an instance of {@link BufferedImage},
 *       then the {@link AffineTransform} can be anything. Java2D applies interpolations efficiently.</li>
 *   <li>Otherwise if the {@link AffineTransform} scale factors are 1 and the translations are integers,
 *       then Java2D invokes {@link RenderedImage#getTile(int, int)}. It makes possible for us to create
 *       a very large image covering the whole data but with tiles computed only when first requested.</li>
 *   <li>Otherwise Java2D invokes {@link RenderedImage#getData(Rectangle)}, which is more costly.
 *       We try to avoid that situation.</li>
 * </ul>
 *
 * Consequently, our strategy is to prepare a resampled image for the whole data when the zoom level changed
 * and rely on tiling for reducing actual computations to required tiles. Since pan gestures are expressed
 * in pixel coordinates, the translation terms in {@code resampledToDisplay} transform should stay integers.
 *
 * <p>Current version of this class does not perform a special case for {@link BufferedImage}.
 * It may not be desirable because interpolations would not be applied in the same way, except
 * when SIS {@link ImageProcessor} would have interpolated RGB color values anyway like Java2D.
 * We wait to see if this class works well in the general case before doing special cases.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class RenderingData implements CloneAccess {
    /**
     * The logger for portrayal.
     */
    private static final Logger LOGGER = Logger.getLogger(Modules.PORTRAYAL);

    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     *
     * @see #xyDimensions
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * Whether to allow the creation of {@link java.awt.image.IndexColorModel}. This flag may be temporarily set
     * to {@code false} for testing or debugging. If {@code false}, images may be only grayscale and may be much
     * slower to render, but should still be visible.
     */
    @Debug
    private static final boolean CREATE_INDEX_COLOR_MODEL = true;

    /**
     * The image layout used for rendering. We allow creating images larger then necessary
     * if it can improve subdivisions in tiles.
     */
    private static final ImageLayout IMAGE_LAYOUT = ImageLayout.DEFAULT.allowImageBoundsAdjustments(true);

    /**
     * Loader for reading and caching coverages at various resolutions.
     * Required if no image has been explicitly assigned to {@link #data}.
     * The same instance may be shared by many {@link RenderingData} objects.
     */
    public MultiResolutionCoverageLoader coverageLoader;

    /**
     * The pyramid level of {@linkplain #data} loaded by the {@linkplain #coverageLoader}.
     * Value 0 is finest resolution.
     */
    private int currentPyramidLevel;

    /**
     * The slice extent which has been used for rendering the {@linkplain #data}.
     * May be {@code null} if the grid coverage has only two dimensions with a size greater than 1 cell.
     */
    private GridExtent currentSlice;

    /**
     * The dimensions to select in the grid coverage for producing an image.
     * This is an array of length {@value #BIDIMENSIONAL} almost always equal to {0,1}.
     * The values are inferred from {@link #currentSlice}.
     */
    private int[] xyDimensions;

    /**
     * The data fetched from {@link GridCoverage#render(GridExtent)} for {@link #currentSlice}.
     * This rendered image may be tiled and fetching those tiles may require computations to be performed
     * in background threads. Pixels in this {@code data} image are mapped to pixels in the display
     * {@link PlanarCanvas} by the following chain of operations:
     *
     * <ol>
     *   <li><code>{@linkplain #dataGeometry}.getGridGeometry(CELL_CENTER)</code></li>
     *   <li><code>{@linkplain #changeOfCRS}.getMathTransform()</code></li>
     *   <li>{@link PlanarCanvas#getObjectiveToDisplay()}</li>
     * </ol>
     *
     * This field is initially {@code null}.
     *
     * @see #dataGeometry
     * @see #dataRanges
     * @see #ensureImageLoaded(GridCoverage, GridExtent, boolean)
     * @see #getSourceImage()
     */
    private RenderedImage data;

    /**
     * Conversion from {@link #data} pixel coordinates to the coverage CRS, together with geospatial area.
     * It contains the {@link GridGeometry#getGridToCRS(PixelInCell)} value of {@link GridCoverage} reduced
     * to two dimensions and with a translation added for taking in account the requested {@code sliceExtent}.
     * The coverage CRS is initially the same as the {@linkplain PlanarCanvas#getObjectiveCRS() objective CRS},
     * but may become different later if user selects a different objective CRS.
     *
     * @see #data
     * @see #dataRanges
     * @see #setImageSpace(GridGeometry, List, int[])
     */
    private GridGeometry dataGeometry;

    /**
     * Ranges of sample values in each band of {@link #data}. This is used for determining on which sample values
     * to apply colors when user asked to apply a color ramp. May be {@code null}.
     *
     * @see #setImageSpace(GridGeometry, List, int[])
     * @see #statistics()
     */
    private List<SampleDimension> dataRanges;

    /**
     * Conversion or transformation from {@linkplain #data} CRS to {@linkplain PlanarCanvas#getObjectiveCRS()
     * objective CRS}, or {@code null} if not yet computed. This is an identity operation if the user did not
     * selected a different CRS after the coverage has been shown.
     */
    private CoordinateOperation changeOfCRS;

    /**
     * Conversion from {@link #data} pixel coordinates to {@linkplain PlanarCanvas#getObjectiveCRS() objective CRS}.
     * This is value of {@link GridGeometry#getGridToCRS(PixelInCell)} invoked on {@link #dataGeometry}, concatenated
     * with {@link #changeOfCRS} and potentially completed by a wraparound operation.
     * May be {@code null} if not yet computed.
     */
    private MathTransform cornerToObjective;

    /**
     * Conversion from {@linkplain PlanarCanvas#getObjectiveCRS() objective CRS} to {@link #data} pixel coordinates.
     * This is the inverse of {@link #changeOfCRS} (potentially with a wraparound operation) concatenated with inverse
     * of {@link GridGeometry#getGridToCRS(PixelInCell)} on {@link #dataGeometry}.
     * May be {@code null} if not yet computed.
     */
    private MathTransform objectiveToCenter;

    /**
     * The inverse of the {@linkplain PlanarCanvas#objectiveToDisplay objective to display} transform which was
     * active at the time resampled images have been computed. The concatenation of this transform with the actual
     * "objective to display" transform at the time the rendered image is drawn should be a translation.
     * May be {@code null} if not yet computed.
     *
     * @see #getTransform(LinearTransform)
     */
    private AffineTransform displayToObjective;

    /**
     * Statistics on pixel values of current {@link #data}, or {@code null} if not yet computed.
     * This is the cached value of {@link #statistics()}.
     *
     * @see #statistics()
     */
    private Statistics[] statistics;

    /**
     * The processor that we use for resampling image and recoloring the image.
     */
    public final ImageProcessor processor;

    /**
     * Creates a new instance initialized to no image.
     *
     * @param  errorHandler  where to report errors during tile computations.
     */
    public RenderingData(final ErrorHandler errorHandler) {
        processor = new ImageProcessor();
        processor.setErrorHandler(errorHandler);
        processor.setImageLayout(IMAGE_LAYOUT);
    }

    /**
     * Clears this renderer. This method should be invoked when the source of data (resource or coverage) changed.
     * The {@link #displayToObjective} transform will be recomputed from scratch when first needed.
     */
    public final void clear() {
        clearCRS();
        coverageLoader     = null;
        displayToObjective = null;
        statistics         = null;
        data               = null;
        dataRanges         = null;
        dataGeometry       = null;
        xyDimensions       = null;
        currentSlice       = null;
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
     * Verifies if this {@code RenderingData} contains an image for the given objective CRS.
     * If this is not the case, the cached resampled images will need to be discarded.
     *
     * @param  objectiveCRS  the coordinate reference system to use for rendering.
     * @return whether the data are valid for the given objective CRS.
     */
    public final boolean validateCRS(final CoordinateReferenceSystem objectiveCRS) {
        if (changeOfCRS != null && !CRS.equivalent(objectiveCRS, changeOfCRS.getTargetCRS())) {
            clearCRS();
            return false;
        }
        return true;
    }

    /**
     * Sets the input space (domain) and output space (ranges) of the image to be rendered.
     * Those values can be initially provided by {@link org.apache.sis.storage.GridCoverageResource}
     * and replaced later by the actual {@link GridCoverage} values after coverage loading is completed.
     * It is caller's responsibility to reduce <var>n</var>-dimensional domain to two dimensions.
     *
     * @param  domain  the two-dimensional grid geometry, or {@code null} if there is no data.
     * @param  ranges  descriptions of bands, or {@code null} if there is no data.
     * @param  xyDims  the dimensions to select in the grid coverage for producing an image.
     *                 This is an array of length {@value #BIDIMENSIONAL} almost always equal to {0,1}.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public final void setImageSpace(GridGeometry domain, final List<SampleDimension> ranges, final int[] xyDims) {
        /*
         * If the grid geometry does not define a "grid to CRS" transform, set it to an identity transform.
         * We do that because this class needs a complete `GridGeometry` as much as possible.
         */
        if (domain != null && !domain.isDefined(GridGeometry.GRID_TO_CRS)
                           &&  domain.isDefined(GridGeometry.EXTENT))
        {
            CoordinateReferenceSystem crs = null;
            if (domain.isDefined(GridGeometry.CRS)) {
                crs = domain.getCoordinateReferenceSystem();
            }
            final GridExtent extent = domain.getExtent();
            domain = new GridGeometry(extent, PixelInCell.CELL_CENTER,
                    MathTransforms.identity(extent.getDimension()), crs);
        }
        dataGeometry = domain;
        dataRanges   = ranges;
        xyDimensions = xyDims;
        processor.setFillValues(SampleDimensions.backgrounds(dataRanges));
    }

    /**
     * Returns {@code true} if the {@link #dataGeometry} properties specified by the argument are set.
     *
     * @param  bitmask  any combination of {@link GridGeometry#CRS}, {@link GridGeometry#ENVELOPE} or other bits.
     * @return {@code true} if all the specified properties are defined by {@link #dataGeometry}.
     */
    private boolean isDefined(final int bitmask) {
        return (dataGeometry != null) && dataGeometry.isDefined(bitmask);
    }

    /**
     * Loads a new grid coverage if {@linkplain #data} is null or if the pyramid level changed.
     * It is caller's responsibility to ensure that {@link #coverageLoader} has a non-null value
     * and is using the right resource before to invoke this method.
     *
     * <p>Caller should invoke {@link #ensureImageLoaded(GridCoverage, GridExtent, boolean)}
     * after this method (this is not done automatically).</p>
     *
     * @param  objectiveToDisplay  transform used for rendering the coverage on screen.
     * @param  objectivePOI        point where to compute resolution, in coordinates of objective CRS.
     * @return the loaded grid coverage, or {@code null} if no loading has been done
     *         (which means that the coverage is unchanged, not that it does not exist).
     * @throws TransformException if an error occurred while computing resolution from given transforms.
     * @throws DataStoreException if an error occurred while loading the coverage.
     *
     * @see #setImageSpace(GridGeometry, List, int[])
     */
    public final GridCoverage ensureCoverageLoaded(final LinearTransform objectiveToDisplay, final DirectPosition objectivePOI)
            throws TransformException, DataStoreException
    {
        final MathTransform dataToObjective = (changeOfCRS != null) ? changeOfCRS.getMathTransform() : null;
        final MultiResolutionCoverageLoader loader = coverageLoader;
        final int level = loader.findPyramidLevel(dataToObjective, objectiveToDisplay, objectivePOI);
        if (data != null && level == currentPyramidLevel) {
            return null;
        }
        data = null;
        currentPyramidLevel = level;
        return loader.getOrLoad(level);
    }

    /**
     * Fetches the rendered image if {@linkplain #data} is null or is for a different slice.
     * This method needs to be invoked at least once after {@link #setImageSpace(GridGeometry, List, int[])}.
     * The {@code coverage} given in argument should be the value returned by a previous call to
     * {@link #ensureCoverageLoaded(LinearTransform, DirectPosition)}, except that it shall not be null.
     *
     * @param  coverage     the coverage from which to read data. Shall not be null.
     * @param  sliceExtent  a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *                      May be {@code null} if this grid coverage has only two dimensions with a size greater than 1 cell.
     * @param  force        whether to force data loading. Should be {@code true} if {@code coverage} changed since last call.
     * @return whether the {@linkplain #data} changed.
     * @throws FactoryException if the CRS changed but the transform from old to new CRS cannot be determined.
     * @throws TransformException if an error occurred while transforming coordinates from old to new CRS.
     */
    public final boolean ensureImageLoaded(GridCoverage coverage, final GridExtent sliceExtent, final boolean force)
            throws FactoryException, TransformException
    {
        if (!force && data != null && Objects.equals(currentSlice, sliceExtent)) {
            return false;
        }
        coverage = coverage.forConvertedValues(true);
        final GridGeometry old = dataGeometry;
        final List<SampleDimension> ranges = coverage.getSampleDimensions();
        final RenderedImage image = coverage.render(sliceExtent);
        final GridGeometry domain;
        final int[] xyDims;
        {   // For local scope of image properties.
            Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
            if (value instanceof GridGeometry) {
                domain = (GridGeometry) value;
                value  = image.getProperty(PlanarImage.XY_DIMENSIONS_KEY);
                xyDims = (value instanceof int[]) ? (int[]) value
                        : (sliceExtent != null) ? sliceExtent.getSubspaceDimensions(BIDIMENSIONAL)
                        : ArraysExt.range(0, BIDIMENSIONAL);
            } else {
                var r = new ImageRenderer(coverage, sliceExtent);
                domain = r.getImageGeometry(BIDIMENSIONAL);
                xyDims = r.getXYDimensions();
            }
        }
        setImageSpace(domain, ranges, xyDims);      // Implies `dataGeometry = domain`.
        currentSlice = sliceExtent;
        data = image;
        /*
         * Update the transforms in a way that preserve the current zoom level, translation, etc.
         * We compute the change in the "data grid to objective CRS" transforms caused by the change
         * in data grid geometry, then we concatenate that change to the existing transforms.
         * That way, the objective CRS is kept unchanged.
         */
        if (old != null && cornerToObjective != null && objectiveToCenter != null) {
            MathTransform toNew = null, toOld = null;
            if (old.isDefined(GridGeometry.CRS) && isDefined(GridGeometry.CRS)) {
                final CoordinateReferenceSystem oldCRS = old.getCoordinateReferenceSystem();
                final CoordinateReferenceSystem newCRS = dataGeometry.getCoordinateReferenceSystem();
                if (newCRS != oldCRS) {             // Quick check for the vast majority of cases.
                    /*
                     * Transform computed below should always be the identity transform,
                     * but we check anyway as a safety. A non-identity transform would be
                     * a pyramid where the CRS changes according the pyramid level.
                     */
                    final GeographicBoundingBox areaOfInterest = Extents.union(
                            dataGeometry.getGeographicExtent().orElse(null),
                            old.getGeographicExtent().orElse(null));
                    toNew = CRS.findOperation(oldCRS, newCRS, areaOfInterest).getMathTransform();
                    toOld = toNew.inverse();
                }
            }
            /*
             * `inverse` is the transform from new grid coordinates to old grid coordinates.
             * `forward` is the converse, with the addition of half-pixel translation terms.
             */
            final MathTransform inverse = concatenate(PixelInCell.CELL_CORNER, dataGeometry, old, toOld);
            final MathTransform forward = concatenate(PixelInCell.CELL_CENTER, old, dataGeometry, toNew);
            cornerToObjective = MathTransforms.concatenate(inverse, cornerToObjective);
            objectiveToCenter = MathTransforms.concatenate(objectiveToCenter, forward);
        }
        return true;
        /*
         * Note: the `forward` transform above is of particular interest and may be returned in a future version.
         * It is the transform from new pixel coordinates to old pixel coordinates of the data before resampling
         * (i.e. ignoring changes caused by user's zoom or pan gestures on the map). Typical values are:
         *
         * • An identity transform, meaning that the data changed but the new data uses the same pixel coordinates
         *   than the previous data. For example the user may have selected a new slice in a three-dimensional cube.
         * • An affine transform represented by a diagonal matrix, i.e. with only scale factors and no translation.
         *   It happens when there is a change of resolution between the previous data and the new one, for example
         *   because a zoom change caused a change of pyramid level in `MultiResolutionCoverageLoader`.
         *   In such case the scale factors are typically 0.5 (after zoom-in) or 2 (after zoom out).
         *
         * That transform has already been applied to `RenderingData` internal state,
         * but maybe some caller will need to apply that change to its own data.
         * We wait to see if such need happens.
         */
    }

    /**
     * Computes the transform that represent a change of "data grid to objective" transform
     *
     * @param  anchor       the cell part to map (center or corner).
     * @param  toCRS        the grid geometry for which to use the "grid to CRS" transform.
     * @param  toGrid       the grid geometry for which to use the "CRS to grid" transform.
     * @param  changeOfCRS  transform from CRS of {@code toCRS} to CRS of {@code toGrid}.
     */
    private static MathTransform concatenate(final PixelInCell anchor, final GridGeometry toCRS,
            final GridGeometry toGrid, final MathTransform changeOfCRS) throws TransformException
    {
        final MathTransform forward = toCRS .getGridToCRS(anchor);
        final MathTransform inverse = toGrid.getGridToCRS(anchor).inverse();
        if (changeOfCRS != null) {
            return MathTransforms.concatenate(forward, changeOfCRS, inverse);
        } else {
            return MathTransforms.concatenate(forward, inverse);
        }
    }

    /**
     * Returns the image which will be used as the source for rendering operations.
     *
     * @return the image loaded be {@link #ensureImageLoaded(GridCoverage, GridExtent, boolean)}.
     */
    public final RenderedImage getSourceImage() {
        return data;
    }

    /**
     * Returns the position at the center of source data, or {@code null} if none.
     * The coordinates are expressed in the CRS of the source coverage.
     */
    private DirectPosition getSourceMedian() {
        if (isDefined(GridGeometry.ENVELOPE)) {
            return AbstractEnvelope.castOrCopy(dataGeometry.getEnvelope()).getMedian();
        }
        return null;
    }

    /**
     * Returns statistics on the source image (computed when first requested, then cached).
     * There is one {@link Statistics} instance per band. This is an information for dynamic
     * stretching of image color ramp. Such recoloring operation should use statistics on the
     * source image instead of statistics on the shown image in order to have stable colors
     * during pans or zooms.
     *
     * <p>The returned map is suitable for use with {@link ImageProcessor#stretchColorRamp(RenderedImage, Map)}.
     * The map content is:</p>
     * <ul>
     *   <li>{@code "statistics"}: the statistics as a {@code Statistics[]} array.</li>
     *   <li>{@code "sampleDimensions"}: band descriptions as a {@code List<SampleDimension>}.</li>
     * </ul>
     *
     * This operation may be costly since it causes the loading of full image.
     * If {@link #coverageLoader} is non-null, statistics will be computed on the
     * image with coarsest resolution.
     *
     * @return statistics on sample values for each band, in a modifiable map.
     * @throws DataStoreException if an error occurred while reading the image at coarsest resolution.
     */
    protected final Map<String,Object> statistics() throws DataStoreException {
        if (statistics == null) {
            RenderedImage image = data;
            final MultiResolutionCoverageLoader loader = coverageLoader;
            if (loader != null) {
                final int level = loader.getLastLevel();
                if (level != currentPyramidLevel) {
                    /*
                     * If coarser data are available, we will compute statistics on those data instead of on the
                     * current pyramid level. We need to adjust the slice extent to the coordinates of coarser data.
                     */
                    final GridCoverage coarse = loader.getOrLoad(level).forConvertedValues(true);
                    GridExtent sliceExtent = currentSlice;
                    if (sliceExtent != null) {
                        if (sliceExtent.getDimension() <= BIDIMENSIONAL) {
                            sliceExtent = null;
                        } else {
                            final GridExtent ce = coarse.getGridGeometry().getExtent();
                            for (final int i : xyDimensions) {
                                sliceExtent = sliceExtent.withRange(i, ce.getLow(i), ce.getHigh(i));
                            }
                        }
                    }
                    image = coarse.render(sliceExtent);
                }
            }
            statistics = processor.valueOfStatistics(image, null, SampleDimensions.toSampleFilters(dataRanges));
        }
        final var modifiers = new HashMap<String,Object>(8);
        modifiers.put("statistics", statistics);
        modifiers.put("sampleDimensions", dataRanges);
        return modifiers;
    }

    /**
     * Sets the coordinate reference system of the display. This method does nothing if the CRS was already set.
     * <em>It does not verify if CRS is the same</em>, it is caller responsibility to clear {@link #changeOfCRS}
     * before to invoke this method for forcing a change of CRS.
     *
     * <p>This method updates the following fields only:</p>
     * <ul>
     *   <li>{@link #changeOfCRS}</li>
     *   <li>{@link #processor} positional accuracy hint</li>
     * </ul>
     *
     * @param  objectiveCRS  value of {@link PlanarCanvas#getObjectiveCRS()}.
     * @throws TransformException if an error occurred while transforming coordinates from grid to new CRS.
     */
    public final void setObjectiveCRS(final CoordinateReferenceSystem objectiveCRS) throws TransformException {
        if (changeOfCRS == null && objectiveCRS != null && isDefined(GridGeometry.CRS)) try {
            changeOfCRS = CRS.findOperation(dataGeometry.getCoordinateReferenceSystem(), objectiveCRS,
                                            dataGeometry.getGeographicExtent().orElse(null));
            final double accuracy = CRS.getLinearAccuracy(changeOfCRS);
            processor.setPositionalAccuracyHints(
//                  TODO: uncomment after https://issues.apache.org/jira/browse/SIS-497 is fixed.
//                  Quantities.create(0.25, Units.PIXEL),
                    (accuracy > 0) ? Quantities.create(accuracy, Units.METRE) : null);
        } catch (FactoryException e) {
            recoverableException(e);
            // Leave `changeOfCRS` to null.
        }
    }

    /**
     * Creates the resampled image, then optionally applies an index color model.
     * This method will compute the {@link MathTransform} steps from image coordinate system
     * to display coordinate system if those steps have not already been computed.
     *
     * @param  recoloredImage      {@link #data} or a derived (typically recolored) image.
     * @param  objectiveToDisplay  value of {@link PlanarCanvas#getObjectiveToDisplay()}.
     * @param  objectivePOI        value of {@link PlanarCanvas#getPointOfInterest(boolean)} in objective CRS.
     * @return image with operation applied and color ramp stretched.
     * @throws TransformException if an error occurred in the use of "grid to CRS" transforms.
     */
    public final RenderedImage resampleAndConvert(final RenderedImage   recoloredImage,
                                                  final LinearTransform objectiveToDisplay,
                                                  final DirectPosition  objectivePOI)
            throws TransformException
    {
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
         * the result to 32 bit integer range). This is okay because only visible tiles will be created.
         *
         * NOTE: if user pans image close to integer range limit, a new resampled image will need to be computed
         *       for shifting away from integer overflow risk situation. This check is done by the caller.
         */
        final LinearTransform inverse = objectiveToDisplay.inverse();
        displayToObjective = AffineTransforms2D.castOrCopy(inverse);
        MathTransform cornerToDisplay = MathTransforms.concatenate(cornerToObjective, objectiveToDisplay);
        MathTransform displayToCenter = MathTransforms.concatenate(inverse, objectiveToCenter);
        /*
         * If the source image is world-wide and if the transform involves a projection that cannot represent
         * the whole world, then we need to clip the image to a domain supported by the map projection.
         */
        final Rectangle bounds = ImageUtilities.getBounds(recoloredImage);
        MathTransforms.getDomain(cornerToDisplay).ifPresent((domain) -> {
            Shapes2D.intersect(bounds, domain, 0, 1);
        });
        /*
         * For computing the bounds of the resampled image, we need to round to a smaller rectangle.
         * Otherwise, interpolation will require coordinates slightly outside the source image bounds,
         * which produce NaN values (often rendered as black borders) in that target image.
         */
        final Rectangle2D resampled = Shapes2D.transform(MathTransforms.bidimensional(cornerToDisplay), bounds, null);
        bounds.x      = (int)  Math.ceil (resampled.getMinX() - Numerics.COMPARISON_THRESHOLD);
        bounds.y      = (int)  Math.ceil (resampled.getMinY() - Numerics.COMPARISON_THRESHOLD);
        bounds.width  = (int) (Math.floor(resampled.getMaxX() + Numerics.COMPARISON_THRESHOLD) - bounds.x);
        bounds.height = (int) (Math.floor(resampled.getMaxY() + Numerics.COMPARISON_THRESHOLD) - bounds.y);
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
         * that we can use with `IndexColorModel`. The two operations (resampling and conversions) are
         * combined in a single "visualization" operation of efficiency.
         *
         * TODO: if `colors` is null, instead of defaulting to `ColorScaleBuilder.GRAYSCALE` we should get the colors
         *       from the current ColorModel. This work should be done in `ColorScaleBuilder` by converting the ranges
         *       of sample values in source image to ranges of sample values in destination image, then query
         *       ColorModel.getRGB(Object) for increasing integer values in that range.
         */
        if (CREATE_INDEX_COLOR_MODEL) {
            final ColorModelType ct = ColorModelType.find(recoloredImage.getColorModel());
            if (ct.isSlow || ct.useColorRamp) try {
                SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.set(dataRanges);
                return processor.visualize(recoloredImage, bounds, displayToCenter);
            } finally {
                SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.remove();
            }
        }
        return processor.resample(recoloredImage, bounds, displayToCenter);
    }

    /**
     * Conversion or transformation from {@linkplain PlanarCanvas#getObjectiveCRS() objective CRS} to
     * {@linkplain #data} CRS. This transform will include {@code WraparoundTransform} steps if needed.
     *
     * @param  transform     the transform to concatenate with a "wraparound" operation.
     * @param  sourceMedian  point of interest in the <em>source</em> CRS of given transform.
     * @param  targetMedian  point of interest after wraparound.
     * @param  targetCRS     the target CRS of the given transform.
     */
    private static MathTransform applyWraparound(final MathTransform transform, DirectPosition sourceMedian,
            final DirectPosition targetMedian, final CoordinateReferenceSystem targetCRS) throws TransformException
    {
        if (targetMedian == null) {
            return transform;
        }
        /*
         * This method is invoked with `sourceMedian` expressed in the transform source CRS.
         * But by contract, `WraparoundApplicator` needs that point in the transform target CRS.
         */
        if (sourceMedian != null && !transform.isIdentity()) {
            sourceMedian = transform.transform(sourceMedian, null);
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
    public final RenderedImage prefetch(final RenderedImage resampledImage, final AffineTransform resampledToDisplay,
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
     * Gets the transform to use for painting the resampled image. If the image to draw is an instance of
     * {@link BufferedImage}, then it is okay to have any transform. However for other kinds of image,
     * it is important that the transform has scale factors of 1 and integer translations because Java2D
     * has an optimization which avoid to copy the whole data only for that case.
     *
     * @param  objectiveToDisplay  the transform from objective CRS to canvas coordinates.
     * @return transform from resampled image to canvas (display) coordinates.
     */
    public final AffineTransform getTransform(final LinearTransform objectiveToDisplay) {
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
    public final float getDataPixelSize(final DirectPosition objectivePOI) {
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
     * Returns the conversion from {@link #data} pixel coordinates to
     * {@linkplain PlanarCanvas#getObjectiveCRS() objective CRS}.
     *
     * @param  anchor  whether the conversion should start from pixel corner or pixel center.
     * @return conversion from data pixel coordinates to objective CRS.
     */
    public final MathTransform getDataToObjective(final PixelInCell anchor) {
        return PixelTranslation.translate(cornerToObjective, PixelInCell.CELL_CORNER, anchor);
    }

    /**
     * Converts the given bounds from objective coordinates to pixel coordinates in the source coverage.
     *
     * @param  bounds  objective coordinates.
     * @return data coverage cell coordinates (in pixels), or {@code null} if unknown.
     * @throws TransformException if the bounds cannot be transformed.
     */
    public final Rectangle objectiveToData(final Rectangle2D bounds) throws TransformException {
        if (objectiveToCenter == null) return null;
        return (Rectangle) Shapes2D.transform(MathTransforms.bidimensional(objectiveToCenter), bounds, new Rectangle());
    }

    /**
     * Returns whether {@link #dataGeometry} or {@link #objectiveToCenter} changed since a previous rendering.
     * This is used for information purposes only.
     *
     * @param  previous  previous instance of {@code RenderingData}.
     * @return whether this {@code RenderingData} does a different rendering than previous {@code RenderingData}.
     */
    public final boolean hasChanged(final RenderingData previous) {
        /*
         * Really !=, not Object.equals(Object), because we rely on new instances to be created
         * (even if equal) as a way to detect that cached values have not been reused.
         */
        return (previous.dataGeometry != dataGeometry) || (previous.objectiveToCenter != objectiveToCenter);
    }

    /**
     * Invoked when an exception occurred while computing a transform but the painting process can continue.
     * This method pretends that the warning come from {@link PlanarCanvas} class since it is the public API.
     */
    private static void recoverableException(final Exception e) {
        Logging.recoverableException(LOGGER, PlanarCanvas.class, "render", e);
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
     * @return a string representation for debugging purposes.
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
