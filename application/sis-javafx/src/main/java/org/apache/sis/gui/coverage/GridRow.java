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

import java.awt.image.RenderedImage;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;


/**
 * A row in a {@link RenderedImage}. This is only a pointer to a row of pixels in an image,
 * not a storage for pixel values. The row to be shown is identified by {@link #getIndex()},
 * which is a zero-based index. Note that <var>y</var> coordinates in a {@link RenderedImage}
 * do not necessarily starts at 0, so a constant offset may exist between {@link #getIndex()}
 * values and image <var>y</var> coordinates.
 *
 * <p>{@link GridRow} instances are created by JavaFX {@link VirtualFlow}, which is responsible
 * for reusing cells. A relatively small amount of {@code GridRow} instances should be created
 * even if the image contains millions of rows.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridRow extends IndexedCell<Void> {
    /**
     * The {@link VirtualFlow} which is managing this row. This is the value given to the constructor,
     * casted to the type used by {@link GridView}. There is two main properties that we want to access:
     *
     * <ul>
     *   <li>{@link GridViewSkin.Flow#getHorizontalPosition()} for the position of the horizontal scroll bar.</li>
     *   <li>{@link GridViewSkin.Flow#getWidth()} for the width of the visible region.
     * </ul>
     *
     * Those two properties are used for creating the minimal amount of {@link GridCell} needed
     * for rendering this row.
     */
    final GridViewSkin.Flow flow;

    /**
     * The grid view where this row will be shown.
     * This is {@code flow.getParent()} but fetched once for efficiency.
     */
    final GridView view;

    /**
     * The arbitrary-based <var>y</var> coordinate in the image. This is not necessarily the {@code row}
     * index in the table since {@link RenderedImage} coordinate system do not necessarily starts at zero.
     * This value may be outside image bounds, in which case this {@code GridRow} should be rendered as empty.
     */
    private int y;

    /**
     * The zero-based <var>y</var> coordinate of the tile. This is not necessarily the
     * {@code tileY} index in the image, since image tile index may not start at zero.
     * This value is computed from {@link #y} value and cached for efficiency.
     */
    private int tileRow;

    /**
     * Invoked by {@link VirtualFlow} when a new cell is needed.
     * This constructor is referenced by lambda-function in {@link GridViewSkin}.
     */
    GridRow(final VirtualFlow<GridRow> owner) {
        flow = (GridViewSkin.Flow) owner;
        view = (GridView) owner.getParent();
        setPrefWidth(view.getContentWidth());
        setFont(Font.font(null, FontWeight.BOLD, -1));      // Apply only to the header column.
    }

    /**
     * Invoked when this {@code GridRow} is used for showing a new image row.
     * We override this method as an alternative to registering a listener to
     * {@link #indexProperty()} (for reducing the number of object allocations).
     *
     * @param  row  index of the new row.
     */
    @Override
    public void updateIndex(final int row) {
        super.updateIndex(row);
        y = view.toImageY(row);
        tileRow = view.toTileRow(row);
        final Skin<?> skin = getSkin();
        if (skin != null) {
            ((GridRowSkin) skin).setRowIndex(row);
        }
    }

    /**
     * Returns the sample value in the given column of this row. If the tile is not available at the time
     * this method is invoked, then the tile will loaded in a background thread and the grid view will be
     * refreshed when the tile become available.
     *
     * @param  column  zero-based <var>x</var> coordinate of sample to get (may differ from image coordinate).
     * @return the sample value in the specified column, or {@code null} if not yet available.
     */
    final String getSampleValue(final int column) {
        return view.getSampleValue(y, tileRow, column);
    }

    /**
     * Creates a new instance of the skin responsible for rendering this grid row.
     * From the perspective of {@link IndexedCell}, the {@link Skin} is a black box.
     * It listens and responds to changes in state of this grid row.
     *
     * @return the renderer of this grid row.
     */
    @Override
    protected Skin<GridRow> createDefaultSkin() {
        return new GridRowSkin(this);
    }
}
