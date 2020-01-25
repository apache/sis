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

import java.awt.Rectangle;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import org.apache.sis.util.Classes;


/**
 * Controls to put in the middle of a tile if an error occurred while loading that tile.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridError extends Label {
    /**
     * If we failed to fetch a tile, the tile in error. If more than one tile has an error,
     * then the tile having the largest intersection with the view area. This visible error
     * may change during scrolling.
     */
    private GridTile.Error visibleError;

    /**
     * The zero-based row and columns indices of the area currently shown in {@link GridView}.
     * This is updated by {@link GridViewSkin#layoutChildren(double, double, double, double)}.
     */
    private Rectangle viewArea;

    /**
     * Incremented every time that a new layout is performed. This is used for detecting if a
     * {@link GridTile.Error} instance should recompute its visible area. It is not a problem
     * if this value overflows; we just check if values differ, not which one is greater.
     *
     * @see GridTile.Error#updateCount
     */
    private int updateCount;

    /**
     * Creates a new error control.
     */
    GridError() {
        setTextFill(Color.RED);
    }

    /**
     * Invoked by {@link GridViewSkin#layoutChildren(double, double, double, double)} when a new layout is beginning.
     *
     * @param  area  zero-based row and columns indices of the area currently shown in {@link GridView}.
     */
    final void initialize(final Rectangle area) {
        setVisible(false);
        viewArea     = area;
        visibleError = null;
        updateCount++;
    }

    /**
     * Updates this error control with the given status. If this control is already showing an error message,
     * it will be updated only if the given status cover a larger view area.
     *
     * @param  status  the candidate error status.
     */
    final boolean update(final GridTile.Error status) {
        if (status != visibleError && status.updateAndCompare(updateCount, viewArea, visibleError)) {
            visibleError = status;
            final Throwable exception = status.exception;
            String message = exception.getLocalizedMessage();
            if (message == null) {
                message = Classes.getShortClassName(exception);
            }
            setText(message);
            setVisible(true);
            return true;
        }
        return false;
    }

    /**
     * Returns the zero-based row and column indices of the region in error in the view area.
     * This method returns a direct reference to internal instance; do not modify.
     */
    final Rectangle getVisibleArea() {
        return visibleError.visibleArea;
    }
}
