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

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link GridView} or {@link CoverageView} together with the controls
 * to show in a {@link CoverageExplorer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class Controls {
    /**
     * Margin to keep around captions on top of tables or lists.
     */
    static final Insets CAPTION_MARGIN = new Insets(12, 0, 9, 0);

    /**
     * Space between a group of controls and the border encompassing the group.
     */
    private static final Insets GROUP_INSETS = new Insets(12);

    /**
     * The border to use for grouping some controls together.
     */
    private static final Border GROUP_BORDER = new Border(new BorderStroke(
            Styles.GROUP_BORDER, BorderStrokeStyle.SOLID, null, null));

    /**
     * Creates a new control.
     */
    Controls() {
    }

    /**
     * Creates a grid pane with one (label, control) per row and all rows with the same height.
     * The controls must be associated to the given labels by {@link Label#getLabelFor()}.
     * If a label is {@code null}, then no row is created for that label.
     *
     * @param  controls  (label, control) pairs to layout in rows.
     * @return a pane with each (label, control) pair on a row.
     */
    static GridPane createControlGrid(final Label... controls) {
        final GridPane gp = new GridPane();
        final ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().setAll(new ColumnConstraints(), controlColumn);
        gp.setPadding(GROUP_INSETS);
        gp.setBorder(GROUP_BORDER);
        gp.setVgap(9);
        gp.setHgap(9);
        int row = 0;
        for (final Label label : controls) {
            if (label != null) {
                final Node control = label.getLabelFor();
                GridPane.setConstraints(label,   0, row);
                GridPane.setConstraints(control, 1, row);
                gp.getChildren().addAll(label, control);
                row++;
            }
        }
        Styles.setAllRowToSameHeight(gp);
        return gp;
    }

    /**
     * Creates a label with the specified text (fetched from localized resources) associated to the given control.
     * If the given control is {@code null}, then this method returns {@code null} for skipping the row completely.
     *
     * @param  vocabulary  the resources from which to get the text.
     * @param  key         {@code vocabulary} key of the text to put in the label.
     * @param  control     the control to associate to the label, or {@code null} if none.
     * @return label associated to the given control, or {@code null} if the given control was null.
     */
    static Label label(final Vocabulary vocabulary, final short key, final Control control) {
        if (control == null) {
            return null;
        }
        control.setMaxWidth(Double.POSITIVE_INFINITY);
        final Label label = new Label(vocabulary.getLabel(key));
        label.setLabelFor(control);
        return label;
    }

    /**
     * Returns the main component, which is showing coverage data or image.
     * This is the component to shown on the right (largest) part of the split pane.
     */
    abstract Region view();

    /**
     * Returns the controls for controlling the view.
     * This is the component to shown on the left (smaller) part of the split pane.
     */
    abstract Control controls();

    /**
     * Invoked after {@link CoverageExplorer#setCoverage(ImageRequest)} for updating the table of
     * sample dimensions with information become available. This method is invoked in JavaFX thread.
     *
     * @param  data  the new coverage, or {@code null} if none.
     */
    abstract void updateBandTable(GridCoverage data);
}
