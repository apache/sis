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

import javafx.scene.Node;
import javafx.scene.control.skin.CellSkinBase;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;


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
    }

    /**
     * Invoked during the layout pass to position the cells to be rendered by this row.
     * This method also sets the content of the cell.
     *
     * <div class="note"><b>Note:</b> I'm not sure it is a good practice to add/remove children
     * and to modify text values here, but I have not identified another place yet. However the
     * JavaFX implementation of table skin seems to do the same, so I presume it is okay.</div>
     *
     * @param  x       the <var>x</var> position of this row, usually 0.
     * @param  y       the <var>y</var> position of this row, usually 0 (this is a relative position).
     * @param  width   width of the region where to render this row (for example 400).
     * @param  height  height of the region where to render this row (for example 400).
     */
    @Override
    protected void layoutChildren(final double x, final double y, final double width, final double height) {
        /*
         * Do not invoke super.layoutChildren(…) since we are doing a different layout.
         * The first child is a javafx.scene.text.Text instance, which we use for row header.
         */
        final GridRow row = getSkinnable();
        final ObservableList<Node> children = getChildren();
        ((Text) children.get(0)).setText(String.valueOf(row.getIndex()));
        /*
         * All children starting at index 1 (i.e. children at indices `column + 1`)
         * shall be GridCell instances created in this method.
         */
        int column = 0;
        double xc = GridView.cellWidth;
        while (xc < width) {
            final GridCell child;
            if (++column < children.size()) {
                child = (GridCell) children.get(column);
            } else {
                child = new GridCell();
                children.add(child);
            }
            final Number value = row.getSampleValue(column);
            child.updateItem(value, value == null);     // TODO: difference between "empty" and "still loading".
            child.resizeRelocate(xc + GridView.horizontalCellSpacing, 0, GridView.cellWidth, height);
            xc += GridView.cellWidth + 2*GridView.horizontalCellSpacing;
        }
    }
}
