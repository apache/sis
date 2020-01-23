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

import java.util.Arrays;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.coverage.grid.GridCoverage;


/**
 * A view of numerical values in a {@link RenderedImage}. The rendered image is typically a two dimensional slice
 * of a {@link GridCoverage}. The number of rows is the image height and the number of columns is the image width.
 * The view shows one band at a time, but the band to show can be changed (thus providing a navigation in a third
 * dimension).
 *
 * <p>This class is designed for large images, with tiles loaded in a background thread only when first needed.
 * For matrices of relatively small size (e.g. less than 100 columns), consider using the standard JavaFX
 * {@link javafx.scene.control.TableView} instead.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DefaultProperty("image")
public class GridView extends Control {
    /**
     * Minimum cell width and height.
     */
    static final int MIN_CELL_SIZE = 1;

    /**
     * The string value for sample values that are out of image bounds.
     */
    static final String OUT_OF_BOUNDS = "";

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
    private int width, height, minX, minY, minTileX, minTileY, tileWidth, tileHeight, numXTiles;

    /**
     * Information copied and adjusted from {@link #imageProperty} for performance. Values are adjusted for using
     * zero-based indices as expected by JavaFX tables (by contrast, pixel indices in a {@link RenderedImage} may
     * start at a non-zero value). The results of those adjustments should be 0, but we nevertheless compute them
     * in case a {@link RenderedImage} defines unusual relationship between image and tile coordinate system.
     */
    private int tileGridXOffset, tileGridYOffset;

    /**
     * The {@link #imageProperty} tiles, fetched when first needed. All {@code Raster[]} array element and
     * {@code Raster} sub-array elements are initially {@code null} and created when first needed.
     * This field is null if and only if the image is null.
     */
    private Raster[][] tiles;

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
     * Creates an initially empty grid view. The content can be set after
     * construction by a call to {@link #setImage(RenderedImage)}.
     */
    public GridView() {
        imageProperty = new SimpleObjectProperty<>(this, "image");
        bandProperty  = new SimpleIntegerProperty (this, "band");
        headerWidth   = new SimpleDoubleProperty  (this, "headerWidth", 60);
        cellWidth     = new SimpleDoubleProperty  (this, "cellWidth",   60);
        cellHeight    = new SimpleDoubleProperty  (this, "cellHeight",  20);
        cellSpacing   = new SimpleDoubleProperty  (this, "cellSpacing",  2);

        imageProperty.addListener(this::startImageLoading);
        // Other listeners registered by GridViewSkin.Flow.

        tileWidth  = 1;
        tileHeight = 1;     // For avoiding division by zero.
    }

    /**
     * Returns the source of sample values for this table.
     *
     * @return the image shown in this table, or {@code null} if none.
     *
     * @see #imageProperty
     */
    public final RenderedImage getImage() {
        return imageProperty.get();
    }

    /**
     * Sets the image to show in this table. This method loads an arbitrary amount of tiles
     * in a background thread. It does not load all tiles if the image is large, unless the
     * user scroll over all tiles.
     *
     * <p><b>Note:</b> the table content may appear unmodified after this method returns.
     * The modifications will appear at an undetermined amount of time later.</p>
     *
     * @param  image  the image to show in this table, or {@code null} if none.
     *
     * @see #imageProperty
     */
    public final void setImage(final RenderedImage image) {
        imageProperty.set(image);
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
        if (image != null) {
            ArgumentChecks.ensureBetween("band", 0, image.getSampleModel().getNumBands() - 1, index);
        } else {
            ArgumentChecks.ensurePositive("band", index);
        }
        bandProperty.set(index);
    }

    /**
     * Invoked (indirectly) when the user sets a new {@link RenderedImage}.
     * See {@link #setImage(RenderedImage)} for method description.
     *
     * @param  property  the {@link #imageProperty} (ignored).
     * @param  previous  the previous image (ignored).
     * @param  image     the new image to show. May be {@code null}.
     * @throws ArithmeticException if the "tile grid x/y offset" property is too big. Should never happen
     *         since those properties should be zero after the adjustment mentioned in their javadoc.
     */
    private void startImageLoading(final ObservableValue<? extends RenderedImage> property,
                                   final RenderedImage previous, final RenderedImage image)
    {
        tiles  = null;       // Let garbage collector dispose the rasters.
        width  = 0;
        height = 0;
        if (image != null) {
            width           = image.getWidth();
            height          = image.getHeight();
            minX            = image.getMinX();
            minY            = image.getMinY();
            minTileX        = image.getMinTileX();
            minTileY        = image.getMinTileY();
            tileWidth       = image.getTileWidth();
            tileHeight      = image.getTileHeight();
            tileGridXOffset = Math.toIntExact(((long) image.getTileGridXOffset()) - minX + ((long) tileWidth)  * minTileX);
            tileGridYOffset = Math.toIntExact(((long) image.getTileGridYOffset()) - minY + ((long) tileHeight) * minTileY);
            numXTiles       = image.getNumXTiles();
            tiles           = new Raster[image.getNumYTiles()][];
            final int n     = image.getSampleModel().getNumBands();
            if (bandProperty.get() >= n) {
                bandProperty.set(n - 1);
            }
            final Skin<?> skin = getSkin();
            if (skin instanceof GridViewSkin) {
                ((GridViewSkin) skin).updateItemCount();
            }
        }
    }

    /**
     * Returns the width that this view would have if it was fully shown (without horizontal scroll bar).
     * This value depends on the number of columns in the image and the size of each cell.
     */
    final double getContentWidth() {
        return width * Math.max(MIN_CELL_SIZE, cellWidth.get()) + Math.max(MIN_CELL_SIZE, headerWidth.get());
    }

    /**
     * Returns the number of rows in the image. This is also the number of rows in the
     * {@link GridViewSkin} virtual flow, which is using a vertical primary direction.
     */
    final int getImageHeight() {
        return height;
    }

    /**
     * Converts a grid row index to image <var>y</var> coordinate. Those values may differ
     * because the image coordinate system does not necessarily starts at zero.
     *
     * @param  row  zero-based index of a row in this grid view.
     * @return image <var>y</var> coordinate (may be outside image bounds).
     * @throws ArithmeticException if image row for the given index is too large.
     */
    final int toImageY(final int row) {
        return Math.addExact(row, minY);
    }

    /**
     * Converts a grid row index to tile index. Note that the returned value may differ from
     * the {@link RenderedImage} tile <var>y</var> coordinates because the index returned by
     * this method is zero-based, while image tile index is arbitrary based.
     */
    final int toTileRow(final int row) {
        return Math.subtractExact(row, tileGridYOffset) / tileHeight;
    }

    /**
     * Returns the sample value in the given column of the given row. If the tile is not available at the
     * time this method is invoked, then the tile will loaded in a background thread and the grid view will
     * be refreshed when the tile become available.
     *
     * <p>The {@code y} parameter is computed by {@link #toImageY(int)} and the {@code tileRow} parameter
     * is computed by {@link #toTileRow(int)}. Those values are stored in {@link GridRow}.</p>
     *
     * @param  y        arbitrary-based <var>y</var> coordinate in the image (may differ from table {@code row}).
     * @param  tileRow  zero-based <var>y</var> coordinate of the tile (may differ from image tile Y).
     * @param  column   zero-based <var>x</var> coordinate of sample to get (may differ from image coordinate X).
     * @return the sample value in the specified column, or {@code null} if unknown (because the loading process
     *         is still under progress), or the empty string ({@code ""}) if out of bounds.
     * @throws ArithmeticException if an index is too large for the 32 bits integer capacity.
     *
     * @see GridRow#getSampleValue(int)
     */
    final String getSampleValue(final int y, final int tileRow, final int column) {
        if (y >= 0 && y < height && column >= 0 && column < width) {
            final int tx = Math.subtractExact(column, tileGridXOffset) / tileWidth;
            Raster[] row = tiles[tileRow];
            if (row == null) {
                tiles[tileRow] = row = new Raster[Math.min(16, numXTiles)];    // Arbitrary limit, expanded if needed.
            } else if (tx >= row.length && tx < numXTiles) {
                tiles[tileRow] = row = Arrays.copyOf(row, Math.min(tx*2, numXTiles));
            }
            Raster tile = row[tx];
            if (tile == null) {
                // TODO: load in background and return null for meaning "not yet available".
                tile = getImage().getTile(Math.addExact(tx, minTileX), Math.addExact(tileRow, minTileY));
                row[tx] = tile;
            }
            final int x = Math.addExact(column, minX);
            final int b = getBand();
            final Number value;
            // TODO: also return Float or Integer.
            value = tile.getSampleDouble(x, y, b);
            // TODO: format.
            return value.toString();
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
}
