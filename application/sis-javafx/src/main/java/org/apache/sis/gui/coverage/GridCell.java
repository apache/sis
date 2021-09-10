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

import javafx.scene.control.IndexedCell;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.CellSkinBase;


/**
 * A single cell in a {@link GridRow}. This cell contains one sample value of one pixel in an image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridCell extends IndexedCell<String> {
    /**
     * Creates a new cell.
     */
    GridCell() {
        /*
         * In unmanaged mode, the parent (GridRow) will ignore the cell preferred size computations and layout.
         * Changes in layout bounds will not trigger relayout above it. This is what we want since the parents
         * decide themselves when to layout in our implementation.
         */
        setManaged(false);
    }

    /**
     * Sets the sample value to show in this grid cell.
     * Note that the {@code value} may be null even if {@code empty} is false.
     * It may happen if the image is still loading in a background thread.
     *
     * @param  value  the sample value, or {@code null} if not available.
     * @param  empty  whether this cell is used for filling empty space
     *                (not to be confused with value not yet available).
     */
    @Override
    protected void updateItem(String value, final boolean empty) {
        super.updateItem(value, empty);
        if (value == null) value = "";
        setText(value);
    }

    /**
     * Creates a new instance of the skin responsible for rendering this grid cell.
     * From the perspective of {@link IndexedCell}, the {@link Skin} is a black box.
     * It listens and responds to changes in state of this grid cell.
     *
     * @return the renderer of this grid cell.
     */
    @Override
    protected Skin<GridCell> createDefaultSkin() {
        // We have nothing to add compared to the base implementation.
        return new CellSkinBase<>(this);
    }
}
