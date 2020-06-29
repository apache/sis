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
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * A {@link GridView} or {@link CoverageCanvas} together with the controls
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
    private static final Insets CAPTION_MARGIN = new Insets(12, 0, 6, 0);

    /**
     * Margin to keep around captions after the first one.
     */
    private static final Insets NEXT_CAPTION_MARGIN = new Insets(30, 0, 6, 0);

    /**
     * Margin for adding an indentation to a node when the node is inside a group
     * created by {@link Styles#createControlGrid(int, Label...)}.
     */
    static final Insets INDENT = new Insets(0, 0, 0, 15);

    /**
     * Margin for adding an indentation to a node when the node is outside a group
     * created by {@link Styles#createControlGrid(int, Label...)}.
     */
    static final Insets INDENT_OUTSIDE = new Insets(0, 0, 0, 15 + Styles.FORM_INSETS.getLeft());

    /**
     * The toolbar button for selecting this view.
     * This is initialized after construction.
     */
    ButtonBase selector;

    /**
     * Creates a new control.
     */
    Controls() {
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
    static Label label(final IndexedResourceBundle vocabulary, final short key, final Control control) {
        if (control == null) {
            return null;
        }
        control.setMaxWidth(Double.POSITIVE_INFINITY);
        final Label label = new Label(vocabulary.getLabel(key));
        label.setLabelFor(control);
        return label;
    }

    /**
     * Creates a label with the specified text associated to the given group of controls.
     *
     * @param  vocabulary  the resources from which to get the text.
     * @param  key         {@code vocabulary} key of the text to put in the label.
     * @param  group       the group of controls to associate to the label.
     * @param  isFirst     whether the given group is the first group in the pane.
     * @return label associated to the given group of controls.
     */
    static Label labelOfGroup(final IndexedResourceBundle vocabulary, final short key, final Region group, final boolean isFirst) {
        final Label label = new Label(vocabulary.getString(key));
        label.setPadding(isFirst ? CAPTION_MARGIN : NEXT_CAPTION_MARGIN);
        label.setLabelFor(group);
        label.setFont(fontOfGroup());
        return label;
    }

    /**
     * Returns the font to assign to the label of a group of control.
     */
    static Font fontOfGroup() {
        return Font.font(null, FontWeight.BOLD, -1);
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
     * Invoked in JavaFX thread after {@link CoverageExplorer#setCoverage(ImageRequest)} completed.
     * Implementation should update the GUI with new information available, in particular
     * the coordinate reference system and the list of sample dimensions.
     *
     * @param  data  the new coverage, or {@code null} if none.
     */
    abstract void coverageChanged(GridCoverage data);
}
