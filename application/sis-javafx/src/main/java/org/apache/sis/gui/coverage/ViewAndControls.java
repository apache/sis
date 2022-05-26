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
import javafx.scene.control.Toggle;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Accordion;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.ObservableList;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * A {@link GridView} or {@link CoverageCanvas} together with the controls to show in a {@link CoverageExplorer}.
 * When the image or coverage is updated in a view, the {@link #coverageChanged(Resource, GridCoverage)} method
 * is invoked, which will in turn update the {@link CoverageExplorer} properties. Coverage changes are applied
 * on the view then propagated to {@code CoverageExplorer} rather than the opposite direction because loading
 * mechanisms are implemented in the view (different views may load a different amount of data).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
abstract class ViewAndControls {
    /**
     * Margin to keep around captions on top of tables or lists.
     */
    private static final Insets CAPTION_MARGIN = new Insets(12, 0, 6, 0);

    /**
     * Margin to keep around captions after the first one.
     */
    private static final Insets NEXT_CAPTION_MARGIN = new Insets(30, 0, 6, 0);

    /**
     * Same indentation as {@link Styles#FORM_INSETS}, but without the space on other sides.
     * This is used when the node is outside a group created by {@link Styles#createControlGrid(int, Label...)}.
     */
    static final Insets CONTENT_MARGIN = new Insets(0, 0, 0, Styles.FORM_INSETS.getLeft());

    /**
     * Index of {@link #sliceSelector} in the list of children of {@link #viewAndNavigation}.
     */
    private static final int SLICE_SELECTOR_INDEX = 3;

    /**
     * The toolbar button for selecting this view.
     * This is initialized after construction and only if a button bar exists.
     */
    Toggle selector;

    /**
     * The main component which is showing coverage data or image together with status bar and {@link #sliceSelector}.
     * This is the component to show on the right (largest) part of the split pane.
     */
    final VBox viewAndNavigation;

    /**
     * The panes for controlling the view, set by subclass constructors and unmodified after construction.
     * Those panes are the components to show on the left (smaller) part of the split pane.
     * Callers will typically put those components in an {@link Accordion}.
     */
    TitledPane[] controlPanes;

    /**
     * The {@link #controlPanes} put together in an accordion. Built only if requested
     * (may never be requested if the caller creates its own accordion with additional panes,
     * as {@link org.apache.sis.gui.dataset.ResourceExplorer} does).
     *
     * @see #controls()
     */
    private Accordion controls;

    /**
     * The control for selecting a slice in a <var>n</var>-dimensional data cube.
     */
    protected final GridSliceSelector sliceSelector;

    /**
     * The widget which contain this view. This is the widget to inform when the coverage changed.
     *
     * @see #notifyDataChanged(GridCoverageResource, GridCoverage)
     */
    private final CoverageExplorer owner;

    /**
     * Creates a new view-control pair.
     *
     * @param  owner  the widget which creates this view. Can not be null.
     */
    protected ViewAndControls(final CoverageExplorer owner) {
        this.owner = owner;
        sliceSelector = new GridSliceSelector(owner.getLocale());
        viewAndNavigation = new VBox();
        sliceSelector.selectedExtentProperty().addListener((p,o,n) -> {
            final GridCoverage coverage = ViewAndControls.this.owner.getCoverage();
            if (coverage != null) {
                load(new ImageRequest(coverage, n));    // Show a new slice of data.
            }
        });
    }

    /**
     * Invoked by subclass constructors for declaring the main visual component.
     * The given component will be added to the {@link #viewAndNavigation} node.
     */
    final void setView(final Region view, final StatusBar status) {
        final Region bar = status.getView();
        final Region nav = sliceSelector.getView();
        VBox.setVgrow(view, Priority.ALWAYS);
        VBox.setVgrow(bar,  Priority.NEVER);
        VBox.setVgrow(nav,  Priority.NEVER);
        final Separator sep = new Separator();
        viewAndNavigation.getChildren().setAll(view, sep, bar);     // `nav` will be added only when non-empty.
        SplitPane.setResizableWithParent(viewAndNavigation, Boolean.TRUE);
        sliceSelector.status = status;
    }

    /**
     * Returns the controls for controlling the view.
     * This is the component to show on the left (smaller) part of the split pane.
     */
    final Accordion controls() {
        if (controls == null) {
            final TitledPane[] panes = controlPanes;
            controls = new Accordion(panes);
            controls.setExpandedPane(panes[0]);
            SplitPane.setResizableWithParent(controls, Boolean.FALSE);
        }
        return controls;
    }

    /**
     * Sets the view content to the given resource, coverage or image.
     * This method is invoked when a new source of data (either a resource or a coverage) is specified,
     * or when a previously hidden view is made visible. Implementations may start a background thread.
     *
     * @param  request  the resource, coverage or image to set, or {@code null} for clearing the view.
     */
    abstract void load(ImageRequest request);

    /**
     * Notifies all controls that a new coverage has been loaded.
     * Subclasses shall invoke this method in the JavaFX thread after loading completed.
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     */
    void notifyDataChanged(final GridCoverageResource resource, final GridCoverage coverage) {
        sliceSelector.gridGeometry.set(coverage != null ? coverage.getGridGeometry() : null);
        final ObservableList<Node> components = viewAndNavigation.getChildren();
        final int count = components.size();
        if (sliceSelector.isEmpty()) {
            if (count > SLICE_SELECTOR_INDEX) {
                components.remove(SLICE_SELECTOR_INDEX);
            }
        } else {
            if (count <= SLICE_SELECTOR_INDEX) {
                components.add(sliceSelector.getView());
            }
        }
        owner.notifyDataChanged(resource, coverage);
    }




    // ════════ Helper methods for subclass constructors ════════════════════════════════════════════════════════

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
    private static Font fontOfGroup() {
        return Font.font(null, FontWeight.BOLD, -1);
    }
}
