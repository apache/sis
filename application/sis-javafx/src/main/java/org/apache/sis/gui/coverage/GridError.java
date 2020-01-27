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
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Controls to put in the middle of a tile if an error occurred while loading that tile.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridError extends VBox {
    /**
     * The area where to write the error message.
     */
    private final Label message;

    /**
     * If we failed to fetch a tile, the tile in error. If more than one tile has an error,
     * then the tile having the largest intersection with the view area. This visible error
     * may change during scrolling.
     */
    private GridTile.Error visibleError;

    /**
     * The last error reported. This is usually equal to {@link #visibleError}, except that
     * the visible error may be {@code null} while {@code lastError} is never reset to null.
     */
    private GridTile.Error lastError;

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
        super(9);
        message = new Label();
        message.setTextFill(Color.RED);
        final Button   details = new Button(Vocabulary.format(Vocabulary.Keys.Details));
        final Button   retry   = new Button(Vocabulary.format(Vocabulary.Keys.Retry));
        final TilePane buttons = new TilePane(12, 0, details, retry);
        message.setLabelFor(buttons);
        buttons.setPrefRows(1);
        buttons.setPrefColumns(2);
        buttons.setAlignment(Pos.CENTER);
        details.setMaxWidth(100);           // Arbitrary limit, width enough for allowing TilePane to resize.
        retry  .setMaxWidth(100);
        getChildren().addAll(message, buttons);
        setAlignment(Pos.CENTER);
        details.setOnAction(this::showDetails);
    }

    /**
     * Invoked by {@link GridViewSkin#layoutChildren(double, double, double, double)} when a new layout is beginning.
     *
     * @param  area  zero-based row and columns indices of the area currently shown in {@link GridView}.
     */
    final void initialize(final Rectangle area) {
        viewArea     = area;
        visibleError = null;
        if (++updateCount == 0) {
            updateCount = 1;        // Paranoiac safety in case we did a cycle over all integer values.
        }
    }

    /**
     * Updates this error control with the given status. If this control is already showing an error message,
     * it will be updated only if the given status cover a larger view area. If this control has been updated,
     * then this method returns the zero-based row and column indices of the region in error in the view area.
     * Otherwise (if this method did nothing), this method returns {@code null}.
     *
     * @param  status  the candidate error status.
     * @return new indices of visible area, or {@code null} if no change.
     *         This is a direct reference to internal field; do not modify.
     */
    final Rectangle update(final GridTile.Error status) {
        if (status != visibleError && status.updateAndCompare(updateCount, viewArea, visibleError)) {
            visibleError = lastError = status;
            String text = status.message;
            final Throwable exception = status.exception;
            String more = exception.getLocalizedMessage();
            if (more != null) {
                text = text + System.lineSeparator() + more;
            }
            message.setText(text);
            return status.visibleArea;
        }
        return null;
    }

    /**
     * Invoked when the user click on the "details" button.
     */
    private void showDetails(final ActionEvent event) {
        if (lastError != null) {
            ExceptionReporter.show(Resources.format(Resources.Keys.ErrorDataAccess), lastError.message, lastError.exception);
        }
    }
}
