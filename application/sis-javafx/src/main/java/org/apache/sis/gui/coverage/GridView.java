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

import java.text.NumberFormat;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.DataBuffer;
import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.gui.BackgroundThreads;


/**
 * A view of numerical values in a {@link RenderedImage}. The rendered image is typically a two dimensional slice
 * of a {@link GridCoverage}. The number of rows is the image height and the number of columns is the image width.
 * The view shows one band at a time, but the band to show can be changed (thus providing a navigation in a third
 * dimension).
 *
 * <p>This class is designed for large images, with tiles loaded in a background thread only when first needed.
 * This is not a general purpose grid viewer; for matrices of relatively small size (e.g. less than 100 columns),
 * consider using the standard JavaFX {@link javafx.scene.control.TableView} instead.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 *
 * @todo Allow users to specify a {@link NumberFormat} pattern for writing sample values.
 */
@DefaultProperty("image")
public class GridView extends Control {
    /**
     * Minimum cell width and height. Must be greater than zero, otherwise infinite loops may happen.
     *
     * @see #getSizeValue(DoubleProperty)
     */
    static final int MIN_CELL_SIZE = 1;

    /**
     * The string value for sample values that are out of image bounds.
     */
    static final String OUT_OF_BOUNDS = "";

    /**
     * If a loading is in progress, the loading process. Otherwise {@code null}.
     *
     * @see #coverageDefined(WorkerStateEvent)
     */
    private ImageLoader loader;

    /**
     * The data shown in this table. Note that setting this property to a non-null value may not
     * modify the grid content immediately. Instead, a background process will request the tiles.
     *
     * @see #getImage()
     * @see #setImage(RenderedImage)
     */
    public final ObjectProperty<RenderedImage> imageProperty;

    /**
     * Information copied from {@link #imageProperty} for performance.
     */
    private int width, height, minX, minY, numXTiles;

    /**
     * Information copied from {@link #imageProperty} for performance.
     * Must be always greater than zero for avoiding division by zero.
     */
    private int tileWidth, tileHeight;

    /**
     * Information copied and adjusted from {@link #imageProperty} for performance. Values are adjusted for using
     * zero-based indices as expected by JavaFX tables (by contrast, pixel indices in a {@link RenderedImage} may
     * start at a non-zero value).
     */
    private int tileGridXOffset, tileGridYOffset;

    /**
     * A cache of most recently used {@link #imageProperty} tiles. We use a very simple caching mechanism here,
     * keeping the most recently used tiles up to 10 Mb of memory. We do not need more sophisticated mechanism
     * since "real" caching is done by {@link org.apache.sis.image.ComputedImage}. The purpose of this cache is
     * to remember that a tile is immediately available and that we do not need to start a background thread.
     */
    private final GridTileCache tiles = new GridTileCache();

    /**
     * The most recently used tile. Cached separately because it will be the desired tile in the vast majority
     * of cases.
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
     * The first (header) column contains the row indices, or sometime the coordinate values.
     * This size includes the {@linkplain #cellSpacing cell spacing}.
     * It shall be a number strictly greater than zero.
     *
     * <p>We do not define getter/setter for this property; use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</p>
     */
    public final DoubleProperty headerWidth;

    /**
     * Width of data cell to be shown in other columns than the header column.
     * This size includes the {@linkplain #cellSpacing cell spacing}.
     * It shall be a number strictly greater than zero.
     *
     * <p>We do not define getter/setter for this property; use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</p>
     */
    public final DoubleProperty cellWidth;

    /**
     * Height of all rows in the grid.
     * It shall be a number strictly greater than zero.
     *
     * <p>We do not define getter/setter for this property; use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</p>
     */
    public final DoubleProperty cellHeight;

    /**
     * Horizontal space between cells, as a number equals or greater than zero.
     * There is no property for vertical cell spacing because increasing the
     * {@linkplain #cellHeight cell height} should be sufficient.
     *
     * <p>We do not define getter/setter for this property; use {@link DoubleProperty#set(double)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</p>
     */
    public final DoubleProperty cellSpacing;

    /**
     * The background color of row and column headers.
     *
     * <p>We do not define getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</p>
     */
    public final ObjectProperty<Paint> headerBackground;

    /**
     * The formatter to use for writing header values (row and column numbers).
     */
    private final NumberFormat headerFormat;

    /**
     * The formatter to use for writing sample values.
     */
    final CellFormat cellFormat;

    /**
     * Creates an initially empty grid view. The content can be set after
     * construction by a call to {@link #setImage(RenderedImage)}.
     */
    public GridView() {
        imageProperty    = new SimpleObjectProperty<>(this, "image");
        bandProperty     = new SimpleIntegerProperty (this, "band");
        headerWidth      = new SimpleDoubleProperty  (this, "headerWidth", 60);
        cellWidth        = new SimpleDoubleProperty  (this, "cellWidth",   60);
        cellHeight       = new SimpleDoubleProperty  (this, "cellHeight",  20);
        cellSpacing      = new SimpleDoubleProperty  (this, "cellSpacing",  4);
        headerBackground = new SimpleObjectProperty<>(this, "headerBackground", Color.GAINSBORO);
        headerFormat     = NumberFormat.getIntegerInstance();
        cellFormat       = new CellFormat();
        tileWidth        = 1;
        tileHeight       = 1;       // For avoiding division by zero.

        setMinSize(120, 40);        // 2 cells on each dimension.
        imageProperty.addListener(this::onImageSpecified);
        bandProperty .addListener(this::onBandSpecified);
        // Other listeners registered by GridViewSkin.Flow.
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
     * This method shall be invoked from JavaFX thread and returns quickly; it does not attempt to fetch any tile.
     * Calls to {@link RenderedImage#getTile(int, int)} will be done in a background thread when first needed.
     *
     * @param  image  the image to show in this table, or {@code null} if none.
     *
     * @see #imageProperty
     */
    public final void setImage(final RenderedImage image) {
        imageProperty.set(image);
    }

    /**
     * Loads image in a background thread from the given source.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The grid content may appear unmodified after this method returns;
     * the modifications will appear after an undetermined amount of time.
     *
     * @param  source  the coverage or resource to load, or {@code null} if none.
     */
    public void setImage(final ImageRequest source) {
        if (source == null) {
            setImage((RenderedImage) null);
        } else {
            if (loader != null) {
                loader.cancel();
            }
            loader = new ImageLoader(source, true);
            loader.setOnSucceeded(this::coverageDefined);
            BackgroundThreads.execute(loader);
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
        final RenderedImage image = getImage();
        final SampleModel sm;
        if (image != null && (sm = image.getSampleModel()) != null) {
            ArgumentChecks.ensureBetween("band", 0, sm.getNumBands() - 1, index);
        } else {
            ArgumentChecks.ensurePositive("band", index);
        }
        bandProperty.set(index);
    }

    /**
     * Invoked in JavaFX thread after {@link #loader} completed its task successfully.
     */
    private void coverageDefined(final WorkerStateEvent event) {
        final ImageLoader result = loader;
        loader = null;
        setImage(result.getValue());
    }

    /**
     * Invoked (indirectly) when the user sets a new {@link RenderedImage}.
     * See {@link #setImage(RenderedImage)} for more description.
     *
     * @param  property  the {@link #imageProperty} (ignored).
     * @param  previous  the previous image (ignored).
     * @param  image     the new image to show. May be {@code null}.
     * @throws ArithmeticException if the "tile grid x/y offset" property is too large.
     */
    private void onImageSpecified(final ObservableValue<? extends RenderedImage> property,
                                  final RenderedImage previous, final RenderedImage image)
    {
        if (loader != null) {
            loader.cancel();
            loader = null;
        }
        tiles.clear();          // Let garbage collector dispose the rasters.
        lastTile = null;
        width    = 0;
        height   = 0;
        if (image != null) {
            minX              = image.getMinX();
            minY              = image.getMinY();
            width             = image.getWidth();
            height            = image.getHeight();
            numXTiles         = image.getNumXTiles();
            tileWidth         = Math.max(1, image.getTileWidth());
            tileHeight        = Math.max(1, image.getTileHeight());
            tileGridXOffset   = Math.subtractExact(image.getTileGridXOffset(), minX);
            tileGridYOffset   = Math.subtractExact(image.getTileGridYOffset(), minY);
            cellFormat.dataTypeisInteger = false;           // To be kept consistent with `cellFormat` pattern.
            final SampleModel sm = image.getSampleModel();
            if (sm != null) {                               // Should never be null, but we are paranoiac.
                final int numBands = sm.getNumBands();
                if (bandProperty.get() >= numBands) {
                    bandProperty.set(numBands - 1);
                }
                final int dataType = sm.getDataType();
                cellFormat.dataTypeisInteger = (dataType >= DataBuffer.TYPE_BYTE && dataType <= DataBuffer.TYPE_INT);
            }
            cellFormat.configure(image, bandProperty.getValue());
        }
        contentChanged(true);
    }

    /**
     * Invoked (indirectly) when the user selects a new band.
     * See {@link #setBand(int)} for more description.
     *
     * @param  property  the {@link #bandProperty} (ignored).
     * @param  previous  the previous band index (ignored).
     * @param  band      index of the new band to show.
     */
    private void onBandSpecified(final ObservableValue<? extends Number> property,
                                 final Number previous, final Number band)
    {
        cellFormat.configure(getImage(), band.intValue());
        contentChanged(false);
    }

    /**
     * Invoked when the content may have changed. If {@code all} is {@code true}, then everything
     * may have changed including the number of rows and columns. If {@code all} is {@code false}
     * then the number of rows and columns is assumed the same.
     */
    final void contentChanged(final boolean all) {
        final Skin<?> skin = getSkin();             // May be null if the view is not yet shown.
        if (skin instanceof GridViewSkin) {         // Could be a user instance (not recommended).
            ((GridViewSkin) skin).contentChanged(all);
        }
    }

    /**
     * Returns the width that this view would have if it was fully shown (without horizontal scroll bar).
     * This value depends on the number of columns in the image and the size of each cell.
     * This method does not take in account the space occupied by the vertical scroll bar.
     */
    final double getContentWidth() {
        /*
         * Add one more column for avoiding offsets caused by the rounding of scroll bar position
         * to integer multiple of column size. The 20 minimal value used below is arbitrary;
         * we take a value close to the vertical scrollbar width as a safety.
         */
        final double w = getSizeValue(cellWidth);
        return width * w + getSizeValue(headerWidth) + Math.max(w, 20);
    }

    /**
     * Returns the number of rows in the image. This is also the number of rows in the
     * {@link GridViewSkin} virtual flow, which is using a vertical primary direction.
     *
     * @see javafx.scene.control.skin.VirtualContainerBase#getItemCount()
     */
    final int getImageHeight() {
        return height;
    }

    /**
     * Returns the bounds of a single tile in the image. This method is invoked only
     * if an error occurred during {@link RenderedImage#getTile(int, int)} invocation.
     * The returned bounds are zero-based (may not be the bounds in image coordinates).
     *
     * <div class="note"><b>Note:</b> we use AWT rectangle instead than JavaFX rectangle
     * because generally we use AWT for everything related to {@link RenderedImage}.</div>
     *
     * @param  tileX  <var>x</var> coordinates of the tile for which to get the bounds.
     * @param  tileY  <var>y</var> coordinates of the tile for which to get the bounds.
     * @return the zero-based bounds of the specified tile in the image.
     */
    final Rectangle getTileBounds(final int tileX, final int tileY) {
        return new Rectangle(tileX * tileWidth  + tileGridXOffset,
                             tileY * tileHeight + tileGridYOffset,
                             tileWidth, tileHeight);
    }

    /**
     * Converts a grid row index to tile index. Note that those {@link RenderedImage}
     * tile coordinates do not necessarily start at 0; negative values may be valid.
     *
     * @see GridRow#tileY
     */
    final int toTileY(final int row) {
        return Math.floorDiv(Math.subtractExact(row, tileGridYOffset), tileHeight);
    }

    /**
     * Returns the sample value in the given column of the given row. If the tile is not available at the
     * time this method is invoked, then the tile will loaded in a background thread and the grid view will
     * be refreshed when the tile become available.
     *
     * <p>The {@code tileY} parameter is computed by {@link #toTileY(int)} and stored in {@link GridRow}.</p>
     *
     * @param  tileY    arbitrary-based <var>y</var> coordinate of the tile.
     * @param  row      zero-based <var>y</var> coordinate of sample to get (may differ from image coordinate Y).
     * @param  column   zero-based <var>x</var> coordinate of sample to get (may differ from image coordinate X).
     * @return the sample value in the specified column, or {@code null} if unknown (because the loading process
     *         is still under progress), or the empty string ({@code ""}) if out of bounds.
     * @throws ArithmeticException if an index is too large for the 32 bits integer capacity.
     *
     * @see GridRow#getSampleValue(int)
     */
    final String getSampleValue(final int tileY, final int row, final int column) {
        if (row < 0 || row >= height || column < 0 || column >= width) {
            return OUT_OF_BOUNDS;
        }
        /*
         * Fetch the tile where is located the (x,y) image coordinate of the pixel to get.
         * If that tile has never been requested before, or has been discarded by the cache,
         * start a background thread for fetching the tile and return null immediately; this
         * method will be invoked again with the same coordinates after the tile become ready.
         */
        final int tileX = Math.floorDiv(Math.subtractExact(column, tileGridXOffset), tileWidth);
        GridTile cache = lastTile;
        if (cache == null || cache.tileX != tileX || cache.tileY != tileY) {
            final GridTile key = new GridTile(tileX, tileY, numXTiles);
            cache = tiles.putIfAbsent(key, key);
            if (cache == null) cache = key;
            lastTile = cache;
        }
        Raster tile = cache.tile();
        if (tile == null) {
            cache.load(this);
            return null;
        }
        /*
         * At this point we have the tile. Get the desired number and format its string representation.
         * As a slight optimization, we reuse the previous string representation if the number is the same.
         * It may happen in particular with fill values.
         */
        return cellFormat.format(tile,
                Math.addExact(column, minX),
                Math.addExact(row,    minY),
                getBand());
    }

    /**
     * Formats a row index or column index.
     *
     * @param  index     the row or column index to format.
     * @param  vertical  {@code true} if formatting row index, or {@code false} if formatting column index.
     */
    final String formatHeaderValue(final int index, final boolean vertical) {
        if (index >= 0 && index < (vertical ? height : width)) {
            return cellFormat.format(headerFormat, index + (long) (vertical ? minY : minX));
        }
        return OUT_OF_BOUNDS;
    }

    /**
     * Creates a new instance of the skin responsible for rendering this grid view.
     * From the perspective of this {@link Control}, the {@link Skin} is a black box.
     * It listens and responds to changes in state of this grid view. This method is
     * called if no skin is provided via CSS or {@link #setSkin(Skin)}.
     *
     * @return the renderer of this grid view.
     */
    @Override
    protected final Skin<GridView> createDefaultSkin() {
        return new GridViewSkin(this);
    }

    /**
     * Returns the value of the given property as a real number not smaller than {@value #MIN_CELL_SIZE}.
     * We use this method instead of {@link Math#max(double, double)} because we want {@link Double#NaN}
     * values to be replaced by {@value #MIN_CELL_SIZE}.
     */
    static double getSizeValue(final DoubleProperty property) {
        final double value = property.get();
        return (value >= MIN_CELL_SIZE) ? value : MIN_CELL_SIZE;
    }
}
