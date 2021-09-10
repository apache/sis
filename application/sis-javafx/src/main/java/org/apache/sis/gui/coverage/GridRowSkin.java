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

import java.util.List;
import java.util.ArrayList;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.control.skin.CellSkinBase;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;


/**
 * The renderer of {@link GridRow} instances. On construction, this object contains only one child.
 * That child is an instance of {@link javafx.scene.text.Text} and is used for the row header. All
 * other children will be instances of {@link GridCell} created and removed as needed during the
 * layout pass.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridRowSkin extends CellSkinBase<GridRow> {
    /**
     * Invoked by {@link GridRow#createDefaultSkin()}.
     */
    GridRowSkin(final GridRow owner) {
        super(owner);
        setRowIndex(owner.getIndex());
    }

    /**
     * Invoked when the index to show in the header column changed.
     */
    final void setRowIndex(final int index) {
        final Text header = (Text) getChildren().get(0);
        header.setText(getSkinnable().view.formatHeaderValue(index, true));
    }

    /**
     * Invoked during the layout pass to position the cells to be rendered by this row.
     * This method also sets the content of the cell.
     *
     * <div class="note"><b>Note:</b> I'm not sure it is a good practice to add/remove children
     * and to modify text values here, but I have not identified another place yet. However the
     * JavaFX implementation of table skin seems to do the same, so I presume it is okay.</div>
     *
     * The {@code width} argument can be a large number (for example 24000) because it includes
     * the area outside the view. In order to avoid creating a large amount of {@link GridCell}
     * instances, this method have to find the current view port area and render only the cells
     * in that area. We do not have to do that vertically because the vertical virtualization
     * is done by {@link GridViewSkin} parent class.
     *
     * @param  x       the <var>x</var> position of this row, usually 0.
     * @param  y       the <var>y</var> position of this row, usually 0 (this is a relative position).
     * @param  width   width of the row, including the area currently hidden because out of view.
     * @param  height  height of the region where to render this row (for example 16).
     */
    @Override
    protected void layoutChildren(final double x, final double y, final double width, final double height) {
        /*
         * Do not invoke super.layoutChildren(…) since we are doing a different layout.
         * The first child is a `javafx.scene.text.Text`, which we use for row header.
         */
        final ObservableList<Node> children = getChildren();
        final GridRow row = getSkinnable();
        final GridViewSkin layout = (GridViewSkin) row.view.getSkin();
        /*
         * Set the position of the header cell, but not its content. The content has been set by
         * `setRowIndex(int)` and does not need to be recomputed even during horizontal scroll.
         */
        double pos = layout.leftPosition;               // Horizontal position in the virtual view.
        ((Text) children.get(0)).resizeRelocate(pos, y, layout.headerWidth, height);
        pos += layout.headerWidth;
        /*
         * Get the beginning (pos) and end (limit) of the region to render. We create only the amount
         * of GridCell instances needed for rendering this region. We should not create cells for the
         * whole row since it would be too many cells (can be millions). Instead we recycle the cells
         * in a list of children that we try to keep small. All children starting at index 1 shall be
         * GridCell instances created in this method.
         */
        final double cellWidth  = layout.cellWidth;             // Includes the cell spacing.
        final double available  = layout.cellInnerWidth;
        final double limit      = layout.rightPosition;         // Horizontal position where to stop.
        int          column     = layout.firstVisibleColumn;    // Zero-based column index in image.
        int          childIndex = 0;
        List<GridCell> newChildren = null;
        final int count = children.size();
        while (pos < limit) {
            /*
             * For sample value, we need to recompute both the values and the position. Note that even if
             * the cells appear at the same positions visually (with different content), they moved in the
             * virtual flow if some scrolling occurred.
             */
            final GridCell cell;
            if (++childIndex < count) {
                cell = (GridCell) children.get(childIndex);
            } else {
                cell = new GridCell();
                cell.setAlignment(Pos.CENTER_RIGHT);
                if (newChildren == null) {
                    newChildren = new ArrayList<>(1 + (int) ((limit - pos) / cellWidth));
                }
                newChildren.add(cell);
            }
            final String value = row.getSampleValue(column++);
            cell.updateItem(value, value == GridView.OUT_OF_BOUNDS);            // Identity comparison is okay here.
            cell.resizeRelocate(pos, 0, available, height);
            pos += cellWidth;
        }
        /*
         * Add or remove fields only at the end of this method in order to fire only one change event.
         * It is important to remove unused fields not only for saving memory, but also for preventing
         * those fields to appear at random positions in the rendered region.
         */
        if (newChildren != null) {
            children.addAll(newChildren);
        } else if (++childIndex < count) {
            children.remove(childIndex, count);
        }
    }
}
