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

import javafx.scene.control.Control;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Toggle;
import javafx.scene.layout.Region;
import javafx.event.ActionEvent;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.ToolbarButton;
import org.apache.sis.internal.gui.NonNullObjectProperty;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.Widget;


/**
 * An image or tabular view of {@link GridCoverage} together with controls for band selection and other operations.
 * This class manages a {@link CoverageCanvas} and a {@link GridView} for showing the visual and the numerical values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see CoverageCanvas
 * @see GridView
 *
 * @since 1.1
 * @module
 */
public class CoverageExplorer extends Widget {
    /**
     * Type of view shown in the explorer.
     * It may be either an image or a table of numerical values.
     */
    public enum View {
        /**
         * Shows the coverage numerical value in a table. This view uses {@link GridView}.
         * This is the default value of newly constructed {@link CoverageExplorer}.
         */
        TABLE("\uD83D\uDD22\uFE0F", Resources.Keys.Visualize),      // 🔢 — Input symbol for numbers.

        /**
         * Shows the coverage visual as an image. This view uses {@link CoverageCanvas}.
         */
        IMAGE("\uD83D\uDDFA\uFE0F", Resources.Keys.TabularData);    // 🗺 — World map.

        /**
         * The Unicode characters to use as icon.
         */
        final String icon;

        /**
         * Key from {@link Resources} bundle for the localized text to use as tooltip.
         */
        final short tooltip;

        /**
         * Creates a new enumeration value.
         */
        private View(final String icon, short tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
        }
    }

    /**
     * The coverage shown in this explorer. Note that setting this property to a non-null value may not
     * modify the view content immediately. Instead, a background process will request the tiles.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.</p>
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

    /**
     * The type of view (image or tabular data) shown in this explorer.
     * The default value is {@link View#TABLE}.
     *
     * <div class="note"><b>API note:</b>
     * the reason for setting default value to tabular data is because it requires loading much less data with
     * {@link java.awt.image.RenderedImage}s supporting deferred tile loading. By contrast {@link View#IMAGE}
     * may require loading the full image.</div>
     *
     * @see #getViewType()
     * @see #setViewType(View)
     */
    public final ObjectProperty<View> viewTypeProperty;

    /**
     * Whether the {@link #coverageProperty} is in process of being set, in which case some
     * listeners should not react.
     */
    private boolean isCoverageAdjusting;

    /**
     * The control that put everything together.
     * The type of control may change in any future SIS version.
     *
     * @see #getView()
     */
    private final SplitPane content;

    /**
     * The different views we can provide on {@link #coverageProperty},
     * together with associated controls.
     */
    private final Controls[] views;

    /**
     * Creates an initially empty explorer.
     */
    public CoverageExplorer() {
        coverageProperty = new SimpleObjectProperty<>(this, "coverage");
        viewTypeProperty = new NonNullObjectProperty<>(this, "viewType", View.TABLE);
        coverageProperty.addListener(this::onCoverageSpecified);
        viewTypeProperty.addListener(this::onViewTypeSpecified);
        /*
         * Prepare buttons to add on the toolbar. Those buttons are not managed by this class;
         * they are managed by org.apache.sis.gui.dataset.DataWindow. We only declare here the
         * text and action for each button.
         */
        final View[]  viewTypes = View.values();
        final ToggleGroup group = new ToggleGroup();
        final Control[] buttons = new Control[viewTypes.length + 1];
        buttons[0] = new Separator();
        /*
         * The coverage property may be shown in various ways (tabular data, image).
         * Each visualization way is an entry in the `views` array.
         */
        final Resources  localized  = Resources.forLocale(null);
        final Vocabulary vocabulary = Vocabulary.getResources(localized.getLocale());
        views = new Controls[viewTypes.length];
        for (final View type : viewTypes) {
            final Controls c;
            switch (type) {
                case TABLE: c = new GridControls(vocabulary); break;
                case IMAGE: c = new CoverageControls(vocabulary, coverageProperty); break;
                default: throw new AssertionError(type);
            }
            SplitPane.setResizableWithParent(c.controls(), Boolean.FALSE);
            SplitPane.setResizableWithParent(c.view(),     Boolean.TRUE);
            c.selector = new Selector(type).createButton(group, type.icon, localized, type.tooltip);
            buttons[buttons.length - type.ordinal() - 1] = c.selector;  // Buttons in reverse order.
            views[type.ordinal()] = c;
        }
        final Controls c = views[0];                            // First View enumeration is default value.
        group.selectToggle(group.getToggles().get(0));
        content = new SplitPane(c.controls(), c.view());
        content.setDividerPosition(0, Styles.INITIAL_SPLIT);
        ToolbarButton.insert(content, buttons);
    }

    /**
     * The action to execute when the user selects a view.
     */
    private final class Selector extends ToolbarButton {
        /** The view to select when the button is pressed. */
        private final View view;

        /** Creates a new action which will show the view at the given index. */
        Selector(final View view) {
            this.view = view;
        }

        /** Invoked when the user selects another view to show (tabular data or the image). */
        @Override public void handle(final ActionEvent event) {
            final Toggle button = (Toggle) event.getSource();
            if (button.isSelected()) {
                setViewType(view);
            } else {
                button.setSelected(true);       // Prevent situation where all buttons are unselected.
            }
        }
    }

    /**
     * Returns the region containing the grid view, band selector and any other control managed
     * by this {@code CoverageExplorer}. The subclass is implementation dependent and may change
     * in any future version.
     *
     * @return the region to show.
     */
    @Override
    public final Region getView() {
        return content;
    }

    /**
     * Returns the source of sample values for this explorer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     *
     * @return the coverage shown in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final GridCoverage getCoverage() {
        return coverageProperty.get();
    }

    /**
     * Sets the coverage to show in this view.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and will appear after an
     * undetermined amount of time.
     *
     * @param  coverage  the data to show in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     */
    public final void setCoverage(final GridCoverage coverage) {
        coverageProperty.set(coverage);
    }

    /**
     * Loads coverage in a background thread from the given source.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The grid content may appear unmodified after this method returns;
     * the modifications will appear after an undetermined amount of time.
     *
     * @param  source  the coverage or resource to load, or {@code null} if none.
     */
    public final void setCoverage(final ImageRequest source) {
        if (source == null) {
            setCoverage((GridCoverage) null);
        } else {
            source.listener = this;
            startLoading(source);
        }
    }

    /**
     * Invoked when a new coverage has been specified.
     *
     * @param  property  the {@link #coverageProperty} (ignored).
     * @param  previous  ignored.
     * @param  coverage  the new coverage.
     */
    private void onCoverageSpecified(final ObservableValue<? extends GridCoverage> property,
                                     final GridCoverage previous, final GridCoverage coverage)
    {
        if (!isCoverageAdjusting) {
            startLoading(null);                                         // Clear data.
            updateBandTable(coverage);
            if (coverage != null) {
                startLoading(new ImageRequest(coverage, null));         // Start a background thread.
            }
        }
    }

    /**
     * Invoked in JavaFX thread by {@link ImageLoader} when the coverage has been read.
     * This method does not set the image because it will be set by {@link ImageLoader}.
     * This method is invoked only as a step during the loading process, which is continuing
     * after this method invocation.
     *
     * @param  coverage  the new coverage, or {@code null} if loading failed.
     */
    final void onCoverageLoaded(final GridCoverage coverage) {
        updateBandTable(coverage);
        isCoverageAdjusting = true;
        try {
            setCoverage(coverage);
        } finally {
            isCoverageAdjusting = false;
        }
    }

    /**
     * Invoked by {@link #setCoverage(ImageRequest)} for starting data loading in a background thread.
     * This method is invoked in JavaFX thread.
     *
     * @param  source  the coverage or resource to load, or {@code null} if none.
     */
    private void startLoading(final ImageRequest source) {
        final GridView main = (GridView) views[View.TABLE.ordinal()].view();
        main.setImage(source);
    }

    /**
     * Invoked after {@link #setCoverage(ImageRequest)} for updating the table of sample dimensions
     * with information become available. This method is invoked in JavaFX thread.
     *
     * @param  data  the new coverage, or {@code null} if none.
     */
    private void updateBandTable(final GridCoverage data) {
        for (final Controls c : views) {
            c.updateBandTable(data);
        }
    }

    /**
     * Returns the type of view (image or tabular data) shown in this explorer.
     * The default value is {@link View#TABLE}.
     *
     * @return the type of view shown in this explorer.
     *
     * @see #viewTypeProperty
     */
    public final View getViewType() {
        return viewTypeProperty.get();
    }

    /**
     * Sets the type of view to show in this explorer.
     *
     * @param  coverage  the type of view to show in this explorer.
     *
     * @see #viewTypeProperty
     */
    public final void setViewType(final View coverage) {
        viewTypeProperty.set(coverage);
    }

    /**
     * Invoked when a new view type has been specified.
     *
     * @param  property  the {@link #viewTypeProperty} (ignored).
     * @param  previous  ignored.
     * @param  view      the new view type.
     */
    private void onViewTypeSpecified(final ObservableValue<? extends View> property,
                                     final View previous, final View view)
    {
        final Controls c = views[view.ordinal()];
        content.getItems().setAll(c.controls(), c.view());
        ((Toggle) c.selector).setSelected(true);
    }
}
