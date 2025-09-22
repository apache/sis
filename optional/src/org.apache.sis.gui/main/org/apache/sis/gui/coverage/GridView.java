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

import java.util.Optional;
import java.text.NumberFormat;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.LogHandler;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.image.DataType;


/**
 * A view of numerical values in a {@link RenderedImage}. The rendered image is typically a two dimensional slice
 * of a {@link GridCoverage}. The number of rows is the image height and the number of columns is the image width.
 * The view shows one band at a time, but the band to show can be changed (thus providing a navigation in a third
 * dimension).
 *
 * <p>This class is designed for large images, with tiles loaded in a background thread only when first needed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see CoverageExplorer
 *
 * @since 1.1
 */
@DefaultProperty("image")
public class GridView extends Control {
    /**
     * If a loading is in progress, the loading process. Otherwise {@code null}.
     */
    private ImageLoader loader;

    /**
     * The data shown in this table. Note that setting this property to a non-null value may not
     * modify the grid content immediately. Instead, a background process will request the tiles.
     *
     * @see #getImage()
     * @see #setImage(RenderedImage)
     * @see #setImage(ImageRequest)
     */
    public final ObjectProperty<RenderedImage> imageProperty;

    /**
     * Information copied from {@link #imageProperty} for performance.
     * Those values are used only for checking if a coordinate is inside image bounds.
     * The maximum coordinates are exclusive.
     */
    private int minX, minY, maxX, maxY;

    /**
     * Information copied from {@link #imageProperty} for performance.
     * Must be always greater than zero for avoiding division by zero.
     */
    private int tileWidth, tileHeight;

    /**
     * Information copied from {@link #imageProperty} for performance.
     */
    private int tileGridXOffset, tileGridYOffset;

    /**
     * A cache of most recently used {@link #imageProperty} tiles.
     * We use a simple caching mechanism, keeping the most recently used tiles up to some maximal amount of memory.
     * No need for something more advanced because the real cache is done by {@link org.apache.sis.image.ComputedImage}.
     * The purpose of this cache is to remember that a tile is immediately available and that we do not need to start
     * a background thread.
     */
    private final GridTileCache tiles;

    /**
     * The most recently used tile.
     * Cached separately because it will be the desired tile in the vast majority of cases.
     */
    private GridTile lastTile;

    /**
     * The image band to show in the table.
     * It should be a number between 0 inclusive and {@link SampleModel#getNumBands()} exclusive.
     *
     * @see #getBand()
     * @see #setBand(int)
     */
    public final IntegerProperty bandProperty;

    /**
     * Width of header cells to be shown in the first column.
     * The first (header) column contains the row indices, or sometimes the coordinate values.
     * This size includes the {@linkplain #cellSpacing cell spacing}.
     * It shall be a number strictly greater than zero.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property, use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final DoubleProperty headerWidth;

    /**
     * Width of data cell to be shown in other columns than the header column.
     * This size includes the {@linkplain #cellSpacing cell spacing}.
     * It shall be a number strictly greater than zero.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property, use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final DoubleProperty cellWidth;

    /**
     * Height of all rows in the grid.
     * It shall be a number strictly greater than zero.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property, use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final DoubleProperty cellHeight;

    /**
     * Horizontal space between cells, as a number equals or greater than zero.
     * There is no property for vertical cell spacing because increasing the
     * {@linkplain #cellHeight cell height} should be sufficient.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property, use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final DoubleProperty cellSpacing;

    /**
     * The background color of row and column headers.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property, use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final ObjectProperty<Paint> headerBackground;

    /**
     * The formatter to use for writing header values (row and column numbers).
     */
    private final NumberFormat headerFormat;

    /**
     * The formatter to use for writing sample values. This is also the property for the localized format pattern.
     * Note that this pattern depends on current locale. It is provided for user interactions (i.e. in a GUI control)
     * instead of programmatic action.
     *
     * @see #cellFormatPattern()
     * @see java.text.DecimalFormat#toLocalizedPattern()
     */
    final CellFormat cellFormat;

    /**
     * If this grid view is associated with controls, these controls. Otherwise {@code null}.
     * This is used only for notifications. A future version may use a more generic listener.
     * We use this specific mechanism because there is no {@code coverageProperty} in this class.
     *
     * @see GridControls#notifyDataChanged(GridCoverageResource, GridCoverage)
     */
    final GridControls controls;

    /**
     * Creates an initially empty grid view. The content can be set after
     * construction by a call to {@link #setImage(RenderedImage)}.
     */
    public GridView() {
        this(null);
    }

    /**
     * Creates an initially empty grid view. The content can be set after
     * construction by a call to {@link #setImage(RenderedImage)}.
     *
     * @param  controls  the controls of this grid view, or {@code null} if none.
     */
    @SuppressWarnings("this-escape")        // `this` appears in a cyclic graph.
    GridView(final GridControls controls) {
        this.controls    = controls;
        bandProperty     = new BandProperty();
        imageProperty    = new SimpleObjectProperty<>(this, "image");
        headerWidth      = new SimpleDoubleProperty  (this, "headerWidth", 80);
        cellWidth        = new SimpleDoubleProperty  (this, "cellWidth",   60);
        cellHeight       = new SimpleDoubleProperty  (this, "cellHeight",  20);
        cellSpacing      = new SimpleDoubleProperty  (this, "cellSpacing",  4);
        headerBackground = new SimpleObjectProperty<>(this, "headerBackground", Color.GAINSBORO);
        headerFormat     = NumberFormat.getIntegerInstance();
        cellFormat       = new CellFormat(this);
        tiles            = new GridTileCache();
        tileWidth        = 1;
        tileHeight       = 1;       // For avoiding division by zero.

        setMinSize(120, 40);        // 2 cells on each dimension.
        imageProperty.addListener((p,o,n) -> onImageSpecified(n));
        // Other listeners registered by GridViewSkin.Flow.
    }

    /**
     * The property for selecting the band to show. This property verifies
     * the validity of given band argument before to modify the value.
     * The expected value is a zero-based band index.
     *
     * @see #getBand()
     * @see #setBand(int)
     */
    private final class BandProperty extends IntegerPropertyBase {
        @Override public Object getBean() {return GridView.this;}
        @Override public String getName() {return "band";}

        /** Invoked when a new band is selected. */
        @Override public void set(final int band) {
            final RenderedImage image = getImage();
            final SampleModel sm;
            if (image != null && (sm = image.getSampleModel()) != null) {
                ArgumentChecks.ensureBetween("band", 0, sm.getNumBands() - 1, band);
                cellFormat.configure(image, band);
            } else {
                ArgumentChecks.ensurePositive("band", band);
            }
            super.set(band);
            updateCellValues();
        }

        /** Sets the band without performing checks, except ensuring that value is positive. */
        final void setNoCheck(final int bands) {
            super.set(Math.max(bands, 0));
        }
    }

    /**
     * Returns the source of sample values for this table.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     *
     * @return the image shown in this table, or {@code null} if none.
     *
     * @see #imageProperty
     */
    public final RenderedImage getImage() {
        return imageProperty.get();
    }

    /**
     * Sets the image to show in this table.
     * This method shall be invoked from the JavaFX thread.
     * This method returns quickly, it does not attempt to fetch any tile.
     * Calls to {@link RenderedImage#getTile(int, int)} will be done in a background thread when first needed.
     *
     * @param  image  the image to show in this table, or {@code null} if none.
     *
     * @see #imageProperty
     */
    public final void setImage(final RenderedImage image) {
        imageProperty.set(image);
        // Above call will cause an invocation of `onImageSpecified(image)`.
    }

    /**
     * Loads image in a background thread from the given source.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The grid content may appear unmodified after this method returns.
     * The modifications will appear after an undetermined amount of time.
     *
     * @param  source  the coverage or resource to load, or {@code null} if none.
     *
     * @see CoverageExplorer#setCoverage(ImageRequest)
     */
    public void setImage(final ImageRequest source) {
        if (source == null) {
            setImage((RenderedImage) null);
            if (controls != null) {
                controls.notifyDataChanged(null, null);
            }
        } else {
            cancelLoader();
            loader = new ImageLoader(source);
            BackgroundThreads.execute(loader);
        }
    }

    /**
     * Invoked after the image has been loaded or after failure.
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     * @param  image     the loaded image, or {@code null} on failure.
     */
    private void setLoadedImage(GridCoverageResource resource, GridCoverage coverage, RenderedImage image) {
        loader = null;          // Must be first for preventing cancellation.
        setImage(image);
        if (controls != null && !controls.isAdjustingSlice) {
            controls.notifyDataChanged(resource, coverage);
        }
    }

    /**
     * A task for loading {@link GridCoverage} from a resource in a background thread, then fetching an image from it.
     *
     * @see #setImage(ImageRequest)
     */
    private final class ImageLoader extends Task<RenderedImage> {
        /**
         * The image source together with optional parameters for reading only a subset.
         */
        private final ImageRequest request;

        /**
         * The coverage that has been read.
         * It may either be specified explicitly in the {@link #request}, or read from the resource.
         */
        private GridCoverage coverage;

        /**
         * Creates a new task for loading an image from the specified coverage resource.
         *
         * @param  request  source of the image to load.
         */
        ImageLoader(final ImageRequest request) {
            this.request = request;
        }

        /**
         * Invoked in a background thread for loading the coverage and rendering the image.
         * The slice selector and the status bar will be updated as soon as the coverage is available.
         *
         * @return the image loaded from the source given at construction time.
         * @throws DataStoreException if an error occurred while loading the grid coverage.
         */
        @Override
        protected RenderedImage call() throws Exception {
            final Long id = LogHandler.loadingStart(request.resource);
            try {
                coverage = request.load().forConvertedValues(true);
                if (isCancelled()) {
                    return null;
                }
                GridExtent slice = request.slice;
                final GridControls c = controls;
                if (c != null) {
                    final GridGeometry gg = coverage.getGridGeometry();
                    slice = BackgroundThreads.runAndWait(() -> {
                        final GridExtent s = c.configureSliceSelector(gg);
                        final int[] xydims = c.sliceSelector.getXYDimensions();
                        c.status.applyCanvasGeometry(gg, s, xydims[0], xydims[1]);
                        return s;
                    });
                }
                return coverage.render(slice);
            } finally {
                LogHandler.loadingStop(id);
            }
        }

        /**
         * Invoked in JavaFX thread after {@link GridView#loader} completed its task successfully.
         * This method updates the image shown in this {@link GridView} and configures the status bar.
         */
        @Override
        protected void succeeded() {
            setLoadedImage(request.resource, coverage, getValue());
        }

        /**
         * Invoked in JavaFX thread on cancellation. This method clears all controls.
         */
        @Override
        protected void cancelled() {
            setLoadedImage(null, null, null);
        }

        /**
         * Invoked in JavaFX thread on failure.
         * Current implementation popups a dialog box for reporting the error.
         */
        @Override
        protected void failed() {
            cancelled();
            ExceptionReporter.canNotReadFile(GridView.this, request.resource, getException());
        }
    }

    /**
     * Returns the index of the band shown in this grid view.
     *
     * @return index of the currently visible band number.
     *
     * @see #bandProperty
     */
    public final int getBand() {
        return bandProperty.get();
    }

    /**
     * Sets the number of the band to show in this grid view.
     * This value should be from 0 (inclusive) to the number of bands in the image (exclusive).
     *
     * @param  index  the band to make visible.
     * @throws IllegalArgumentException if the given band index is out of bounds.
     */
    public final void setBand(final int index) {
        bandProperty.set(index);
    }

    /**
     * If an image is loaded in a background thread, cancel the loading process.
     * This method is invoked when a new image is specified.
     */
    private void cancelLoader() {
        final ImageLoader previous = loader;
        if (previous != null) {
            loader = null;
            previous.cancel(BackgroundThreads.NO_INTERRUPT_DURING_IO);
        }
    }

    /**
     * Invoked (indirectly) when the user sets a new {@link RenderedImage}.
     * See {@link #setImage(RenderedImage)} for more description.
     *
     * @param  image  the new image to show. May be {@code null}.
     * @throws ArithmeticException if the "max x/y" property is too large.
     */
    private void onImageSpecified(final RenderedImage image) {
        cancelLoader();
        tiles.clear();              // Let the garbage collector disposes the rasters.
        lastTile = null;
        maxX = Integer.MIN_VALUE;   // A way to make sure that all coordinates are considered out of bounds.
        maxY = Integer.MIN_VALUE;
        if (image != null) {
            tileWidth       = Math.max(1, image.getTileWidth());
            tileHeight      = Math.max(1, image.getTileHeight());
            tileGridXOffset = image.getTileGridXOffset();
            tileGridYOffset = image.getTileGridYOffset();
            cellFormat.dataTypeIsInteger = false;           // To be kept consistent with `cellFormat` pattern.
            final SampleModel sm = image.getSampleModel();
            if (sm != null) {                               // Should never be null, but we are paranoiac.
                final int numBands = sm.getNumBands();
                if (getBand() >= numBands) {
                    ((BandProperty) bandProperty).setNoCheck(numBands - 1);
                }
                cellFormat.dataTypeIsInteger = DataType.isInteger(sm);
            }
            cellFormat.configure(image, getBand());
            // Set image bounds only after everything else succeeded.
            minX = image.getMinX();
            maxX = Math.addExact(minX, image.getWidth());
            minY = image.getMinY();
            maxY = Math.addExact(minY, image.getHeight());
        }
        final Skin<?> skin = getSkin();             // May be null if the view is not yet shown.
        if (skin instanceof GridViewSkin) {         // Could be a user instance (not recommended).
            ((GridViewSkin) skin).clear();
        }
        requestLayout();
    }

    /**
     * Rewrites the cell values. This method can be invoked when the band to show has changed,
     * or when a change is detected in a writable image. This method assumes that the image size
     * has not changed.
     */
    final void updateCellValues() {
        final Skin<?> skin = getSkin();             // May be null if the view is not yet shown.
        if (skin instanceof GridViewSkin) {         // Could be a user instance (not recommended).
            ((GridViewSkin) skin).updateCellValues();
        }
    }

    /**
     * Configures the given scroll bar.
     *
     * @param  bar       the scroll bar to configure.
     * @param  numCells  number of cells created by the caller (one more than the number of visible cells).
     * @param  vertical  {@code true} if the scroll bar is vertical, or {@code false} if horizontal.
     */
    final void scaleScrollBar(final ScrollBar bar, int numCells, final boolean vertical) {
        int min, max;
        if (vertical) {
            min = minY;
            max = maxY;
        } else {
            min = minX;
            max = maxX;
        }
        if (max > min) {
            numCells = Math.max(1, Math.min(numCells - 2, max));
            max -= numCells;
            bar.setMin(min);
            bar.setMax(max);
            bar.setVisibleAmount(numCells);
            double value = bar.getValue();
            if (value < min) {
                value = min;
            } else if (value > max) {
                value = max;
            } else {
                return;
            }
            bar.setValue(value);
        }
    }

    /**
     * Returns the bounds of a single tile in the image. This method is invoked only
     * if an error occurred during {@link RenderedImage#getTile(int, int)} invocation.
     *
     * <h4>Design note</h4>
     * We use <abbr>AWT</abbr> rectangle instead of JavaFX rectangle
     * because generally we use AWT for everything related to {@link RenderedImage}.
     *
     * @param  tileX  <var>x</var> coordinates of the tile for which to get the bounds.
     * @param  tileY  <var>y</var> coordinates of the tile for which to get the bounds.
     * @return the zero-based bounds of the specified tile in the image.
     */
    final Rectangle getTileBounds(final int tileX, final int tileY) {
        return new Rectangle(Numerics.clamp(tileGridXOffset + Math.multiplyFull(tileX, tileWidth)),
                             Numerics.clamp(tileGridYOffset + Math.multiplyFull(tileY, tileHeight)),
                             tileWidth, tileHeight);
    }

    /**
     * Formats the sample value at the image coordinates. If the tile is not available at the time
     * that this method is invoked, then the tile will be loaded in a background thread and the grid
     * view will be refreshed when the tile become available.
     *
     * @param  x  image <var>x</var> coordinate of the sample value to get.
     * @param  y  image <var>y</var> coordinate of the sample value to get.
     * @return the sample value at the specified coordinates, or {@code null} if not available.
     *
     * @see #formatCoordinateValue(long)
     *
     * @since 1.5
     */
    public final String formatSampleValue(final long x, final long y) {
        if (x < minX || x >= maxX || y < minY || y >= maxY) {
            return null;
        }
        /*
         * Fetch the tile where is located the (x,y) image coordinate of the pixel to get.
         * If that tile has never been requested before, or has been discarded by the cache,
         * start a background thread for fetching the tile and return null immediately. This
         * method will be invoked again with the same coordinates after the tile become ready.
         */
        final int tileX = Math.toIntExact(Math.floorDiv(x - tileGridXOffset, tileWidth));
        final int tileY = Math.toIntExact(Math.floorDiv(y - tileGridYOffset, tileHeight));
        GridTile cache = lastTile;
        if (cache == null || cache.tileX != tileX || cache.tileY != tileY) {
            final var key = new GridTile(tileX, tileY);
            cache = tiles.putIfAbsent(key, key);
            if (cache == null) cache = key;
            lastTile = cache;
        }
        Raster tile = cache.tile();
        if (tile == null) {
            cache.load(this);
            return null;
        }
        // The casts are sure to be valid because of the range check at the beginning of this method.
        return cellFormat.format(tile, (int) x, (int) y, getBand());
    }

    /**
     * Formats a <var>x</var> or <var>y</var> pixel coordinate values.
     * They are the values to write in the header row or header column.
     *
     * @param  index  the pixel coordinate to format.
     * @return string representation of the given pixel coordinate.
     *
     * @since 1.5
     */
    public final String formatCoordinateValue(final long index) {
        return cellFormat.format(headerFormat, index);
    }

    /**
     * The property for the pattern of values in cells. Note that this pattern depends on current locale.
     * It is provided for user interactions (i.e. in a GUI control) instead of programmatic action.
     *
     * @return the <em>localized</em> format pattern property, or an empty value if the {@link NumberFormat}
     *         used for writing cell values is not an instance of {@link java.text.DecimalFormat}.
     *
     * @see java.text.DecimalFormat#toLocalizedPattern()
     */
    public final Optional<StringProperty> cellFormatPattern() {
        return cellFormat.hasPattern() ? Optional.of(cellFormat) : Optional.empty();
    }

    /**
     * Hides coordinates in the status bar.
     */
    final void hideCoordinates() {
        if (controls != null) {
            controls.status.handle(null);
        }
    }

    /**
     * Creates a new instance of the skin responsible for rendering this grid view.
     * From the perspective of this {@link Control}, the {@link Skin} is a black box.
     * It listens and responds to changes in state of this grid view. This method is
     * called if no skin is provided via <abbr>CSS</abbr> or {@link #setSkin(Skin)}.
     *
     * @return the renderer of this grid view.
     */
    @Override
    protected final Skin<GridView> createDefaultSkin() {
        return new GridViewSkin(this);
    }
}
