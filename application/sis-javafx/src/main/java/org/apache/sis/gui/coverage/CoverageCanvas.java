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

import java.util.Locale;
import java.util.EnumMap;
import java.util.Objects;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.gui.ImageRenderings;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.gui.map.MapCanvasAWT;


/**
 * A canvas for {@link RenderedImage} provided by a {@link GridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see CoverageExplorer
 *
 * @since 1.1
 * @module
 */
public class CoverageCanvas extends MapCanvasAWT {
    /**
     * The data shown in this canvas. Note that setting this property to a non-null value may not
     * modify the canvas content immediately. Instead, a background process will request the tiles.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.</p>
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

    /**
     * A subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     * May be {@code null} if the grid coverage has only two dimensions with a size greater than 1 cell.
     *
     * @see #getSliceExtent()
     * @see #setSliceExtent(GridExtent)
     * @see GridCoverage#render(GridExtent)
     */
    public final ObjectProperty<GridExtent> sliceExtentProperty;

    /**
     * Different ways to represent the data. The {@link #data} field shall be one value from this map.
     *
     * @see #setImage(Stretching, RenderedImage)
     */
    private final EnumMap<Stretching,RenderedImage> dataAlternatives;

    /**
     * Key of the currently selected alternative in {@link #dataAlternatives} map.
     *
     * @see #setImage(Stretching, RenderedImage)
     */
    private Stretching currentDataAlternative;

    /**
     * The data to show, or {@code null} if not yet specified. This image may be tiled,
     * and fetching tiles may require computations to be performed in background thread.
     * The size of this {@code RenderedImage} is not necessarily the {@link #image} size.
     * In particular {@code data} way cover a larger area.
     */
    private RenderedImage data;

    /**
     * The {@link GridGeometry#getGridToCRS(PixelInCell)} conversion of rendered {@linkplain #data}
     * as an affine transform. This is often an immutable instance.
     */
    private AffineTransform gridToCRS;

    /**
     * Creates a new two-dimensional canvas for {@link RenderedImage}.
     */
    public CoverageCanvas() {
        super(Locale.getDefault());
        coverageProperty       = new SimpleObjectProperty<>(this, "coverage");
        sliceExtentProperty    = new SimpleObjectProperty<>(this, "sliceExtent");
        dataAlternatives       = new EnumMap<>(Stretching.class);
        currentDataAlternative = Stretching.NONE;
        coverageProperty   .addListener((p,o,n) -> onImageSpecified());
        sliceExtentProperty.addListener((p,o,n) -> onImageSpecified());
    }

    /**
     * Returns the data which are the source of all alternative images that may be stored in the
     * {@link #dataAlternatives} map. All alternative images are computed from this source.
     */
    private RenderedImage getSourceData() {
        return dataAlternatives.get(Stretching.NONE);
    }

    /**
     * Returns the region containing the image view.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    final Region getView() {
        return fixedPane;
    }

    /**
     * Returns the source of image for this viewer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     *
     * @return the coverage shown in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final GridCoverage getCoverage() {
        return coverageProperty.get();
    }

    /**
     * Sets the coverage to show in this viewer.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and will appear after an
     * undetermined amount of time.
     *
     * @param  coverage  the data to show in this viewer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final void setCoverage(final GridCoverage coverage) {
        coverageProperty.set(coverage);
    }

    /**
     * Returns a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @return subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final GridExtent getSliceExtent() {
        return sliceExtentProperty.get();
    }

    /**
     * Sets a subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @param  sliceExtent  subspace of the grid coverage extent where all dimensions except two have a size of 1 cell.
     *
     * @see #sliceExtentProperty
     * @see GridCoverage#render(GridExtent)
     */
    public final void setSliceExtent(final GridExtent sliceExtent) {
        sliceExtentProperty.set(sliceExtent);
    }

    /**
     * Sets the background, as a color for now but more patterns may be allowed in a future version.
     */
    final void setBackground(final Color color) {
        fixedPane.setBackground(new Background(new BackgroundFill(color, null, null)));
    }

    /**
     * Invoked when a new coverage has been specified or when the slice extent changed.
     * This method starts loading in a background thread.
     */
    private void onImageSpecified() {
        data = null;
        dataAlternatives.clear();
        final GridCoverage coverage = getCoverage();
        if (coverage == null) {
            clear();
        } else {
            final GridExtent sliceExtent = getSliceExtent();
            execute(new Task<RenderedImage>() {
                /** Invoked in background thread for fetching the image. */
                @Override protected RenderedImage call() {
                    return coverage.render(sliceExtent);
                }

                /** Invoked in JavaFX thread on success. */
                @Override protected void succeeded() {
                    if (coverage.equals(getCoverage()) && Objects.equals(sliceExtent, getSliceExtent())) {
                        setImage(getValue(), coverage.getGridGeometry(), sliceExtent);
                    }
                }
            });
        }
    }

    /**
     * Invoked when the user selected a new color stretching mode. Also invoked {@linkplain #onImageSpecified after
     * loading a new image or a new slice} for switching the new image to the same type of range as previously selected.
     * If the image for the specified type is not already available, then this method computes the image in a background
     * thread and refreshes the view after the computation completed.
     */
    final void setStretching(final Stretching type) {
        currentDataAlternative = type;
        final RenderedImage alt = dataAlternatives.get(type);
        if (alt != null) {
            setImage(type, alt);
        } else {
            final RenderedImage source = getSourceData();
            if (source != null) {
                execute(new Task<RenderedImage>() {
                    /** Invoked in background thread for fetching the image. */
                    @Override protected RenderedImage call() {
                        switch (type) {
                            case VALUE_RANGE: return ImageRenderings.valueRangeStretching(source);
                            case AUTOMATIC:   return ImageRenderings. automaticStretching(source);
                            default:          return source;
                        }
                    }

                    /** Invoked in JavaFX thread on success. */
                    @Override protected void succeeded() {
                        if (source.equals(getSourceData())) {
                            setImage(type, getValue());
                        }
                    }
                });
            }
        }
    }

    /**
     * Invoked in JavaFX thread for setting the image to show. The given image should be a slice
     * produced by current value of {@link #coverageProperty} (should be verified by the caller).
     *
     * @param  type  the type of range used for scaling the color ramp of given image.
     * @param  alt   the image or alternative image to show (can be {@code null}).
     */
    private void setImage(final Stretching type, RenderedImage alt) {
        /*
         * Store the result but do not necessarily show it because maybe the user changed the
         * `Stretching` during the time the background thread was working. If the user did not
         * changed the type, then the `alt` variable below will stay unchanged.
         */
        dataAlternatives.put(type, alt);
        alt = dataAlternatives.get(currentDataAlternative);
        if (!Objects.equals(alt, data)) {
            data = alt;
            requestRepaint();
        }
    }

    /**
     * Invoked when a new image has been successfully loaded.
     *
     * @todo Needs to handle non-affine transform.
     *
     * @param  image        the image to load.
     * @param  geometry     the grid geometry of the coverage that produced the image.
     * @param  sliceExtent  the extent that was requested.
     */
    private void setImage(final RenderedImage image, final GridGeometry geometry, final GridExtent sliceExtent) {
        setImage(Stretching.NONE, image);
        setStretching(currentDataAlternative);
        try {
            gridToCRS = AffineTransforms2D.castOrCopy(geometry.getGridToCRS(PixelInCell.CELL_CENTER));
        } catch (RuntimeException e) {                      // Conversion not defined or not affine.
            gridToCRS = null;
            errorOccurred(e);
        }
        Envelope visibleArea = null;
        if (gridToCRS != null && geometry.isDefined(GridGeometry.ENVELOPE)) {
            visibleArea = geometry.getEnvelope();
        } else if (geometry.isDefined(GridGeometry.EXTENT)) try {
            final GridExtent extent = geometry.getExtent();
            visibleArea = extent.toEnvelope(MathTransforms.identity(extent.getDimension()));
        } catch (TransformException e) {
            // Should never happen because we asked for an identity transform.
            errorOccurred(e);
        }
        setObjectiveBounds(visibleArea);
    }

    /**
     * Invoked in JavaFX thread for creating a renderer to be executed in a background thread.
     * This method prepares the information needed but does not start the rendering itself.
     * The rendering will be done later by a call to {@link Renderer#paint(Graphics2D)}.
     */
    @Override
    protected Renderer createRenderer() {
        final RenderedImage data = this.data;       // Need to copy this reference here before background task.
        if (data == null) {
            return null;
        }
        /*
         * At each rendering operation, compute the transform from `data` cell coordinates to pixel coordinates
         * of the image shown in this view. We do this computation every times because `objectiveToDisplay` may
         * vary at any time, and also because we need a new `AffineTransform` instance anyway (we can not reuse
         * an existing instance, because it needs to be stable for use by the background thread).
         */
        final AffineTransform gridToDisplay = new AffineTransform(objectiveToDisplay);
        if (gridToCRS != null) {
            gridToDisplay.concatenate(gridToCRS);
        }
        return new Renderer() {
            @Override protected void paint(final Graphics2D gr) {
                gr.drawRenderedImage(data, gridToDisplay);
            }
        };
    }
}
