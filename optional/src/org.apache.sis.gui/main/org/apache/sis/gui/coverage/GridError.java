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
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Controls to put in the middle of a tile if an error occurred while loading that tile.
 * This class contains the reason why a tile request failed, together with some information
 * that depends on the viewing context. In particular {@link #getVisibleRegion(Rectangle)}
 * needs to be recomputed every time that the visible area in the {@link GridView} changed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridError extends VBox {
    /**
     * The background for error boxes.
     */
    private static final Background BACKGROUND = new Background(new BackgroundFill(Color.FLORALWHITE, null, null));

    /**
     * The tile in error.
     */
    private final GridTile tile;

    /**
     * The reason for the failure to load a tile.
     */
    private final Throwable exception;

    /**
     * The first line of {@link #message}, also used as header text in the "details" dialog box.
     */
    private final String header;

    /**
     * The area where to write the error message. The text said "cannot fetch tile (x, y)"
     * with tile indices, followed by the exception message (if any) on next line.
     */
    private final Label message;

    /**
     * The row and column indices of the tile.
     * This is computed by {@link GridView#getTileBounds(int, int)} and should be constant.
     */
    private final Rectangle region;

    /**
     * Creates a new error control for the specified exception.
     */
    GridError(final GridView view, final GridTile tile, final Throwable exception) {
        super(9);
        this.tile      = tile;
        this.exception = exception;
        this.region    = view.getTileBounds(tile.tileX, tile.tileY);
        this.header    = Resources.format(Resources.Keys.CanNotFetchTile_2, tile.tileX, tile.tileY);

        final var details = new Button(Vocabulary.format(Vocabulary.Keys.Details));
        final var retry   = new Button(Vocabulary.format(Vocabulary.Keys.Retry));
        final var buttons = new TilePane(12, 0, details, retry);
        buttons.setPrefRows(1);
        buttons.setPrefColumns(2);
        buttons.setAlignment(Pos.CENTER);
        details.setMaxWidth(100);               // Arbitrary limit, width enough for allowing TilePane to resize.
        retry  .setMaxWidth(100);
        details.setFocusTraversable(false);     // For avoiding confusing behavior (would be in random order anyway).
        retry  .setFocusTraversable(false);

        final String t = exception.getLocalizedMessage();
        message = new Label((t == null) ? header : header + System.lineSeparator() + t);
        message.setTextFill(Color.RED);
        message.setLabelFor(buttons);

        getChildren().addAll(message, buttons);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12, 18, 24, 18));
        details.setOnAction((e) -> showDetails());
        retry  .setOnAction((e) -> retry());
        setBackground(BACKGROUND);
    }

    /**
     * Returns the bounds of the error controls which is currently visible in the {@link GridView}.
     * This is the intersection of {@link #region} with the area currently shown in the grid view.
     * May vary during scrolling and is empty if the tile in error is outside the visible area.
     *
     * @param  viewArea  zero-based row and columns indices of the area currently shown in {@link GridView}.
     */
    final Rectangle getVisibleRegion(final Rectangle viewArea) {
        return viewArea.intersection(region);
    }

    /**
     * Invoked when the user click on the "details" button.
     */
    private void showDetails() {
        ExceptionReporter.show(this, Resources.format(Resources.Keys.ErrorDataAccess), header, exception);
    }

    /**
     * Invoked when the user asked to retry a tile computation.
     */
    private void retry() {
        final var view = (GridView) getParent();
        ((GridViewSkin) view.getSkin()).removeError(this);
        tile.clear();
        view.requestLayout();
    }
}
