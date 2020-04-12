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

import javafx.scene.control.SplitPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Toggle;
import javafx.scene.layout.Region;
import javafx.event.ActionEvent;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.ToolbarButton;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.Widget;


/**
 * A view of {@link GridCoverage} numerical values together with controls for band selection
 * and other operations. This class manages a {@link GridView} for showing the numerical values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CoverageExplorer extends Widget {
    /**
     * Index in the {@link #views} array for the {@link GridView} (tabular data)
     * or the {@link CoverageView} (image).
     */
    private static final int TABLE_VIEW = 0, IMAGE_VIEW = 1;

    /**
     * The coverage shown in this view. Note that setting this property to a non-null value may not
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
        coverageProperty.addListener(this::onCoverageSpecified);
        /*
         * The coverage property may be shown in various ways (tabular data, image).
         * Each visualization way is an entry in the `views` array.
         */
        final Resources  localized  = Resources.forLocale(null);
        final Vocabulary vocabulary = Vocabulary.getResources(localized.getLocale());
        views = new Controls[2];
        views[TABLE_VIEW] = new GridControls(vocabulary);
        views[IMAGE_VIEW] = new CoverageControls(vocabulary, coverageProperty);
        for (final Controls c : views) {
            SplitPane.setResizableWithParent(c.controls(), Boolean.FALSE);
            SplitPane.setResizableWithParent(c.view(),     Boolean.TRUE);
        }
        final Controls c = views[TABLE_VIEW];
        content = new SplitPane(c.controls(), c.view());
        content.setDividerPosition(0, Styles.INITIAL_SPLIT);
        /*
         * Prepare buttons to add on the toolbar. Those buttons are not managed by this class;
         * they are managed by org.apache.sis.gui.dataset.DataWindow. We only declare here the
         * text and action for each button.
         */
        final ToggleGroup group = new ToggleGroup();
        ToolbarButton.insert(content,
            new Separator(),
            new Selector(IMAGE_VIEW).createButton(group, "\uD83D\uDDFA\uFE0F", localized, Resources.Keys.Visualize),    // 🗺 — World map.
            new Selector(TABLE_VIEW).createButton(group, "\uD83D\uDD22\uFE0F", localized, Resources.Keys.TabularData)   // 🔢 — Input symbol for numbers.
        );
        final ObservableList<Toggle> toggles = group.getToggles();
        group.selectToggle(toggles.get(toggles.size() - 1));
    }

    /**
     * The action to execute when the user selects a view.
     */
    private final class Selector extends ToolbarButton {
        /** {@link #TABLE_VIEW} or {@link #IMAGE_VIEW}. */
        private final int index;

        /** Creates a new action which will show the view at the given index. */
        Selector(final int index) {
            this.index = index;
        }

        /** Invoked when the user selects another view to show (tabular data or the image). */
        @Override public void handle(final ActionEvent event) {
            final Toggle button = (Toggle) event.getSource();
            if (button.isSelected()) {
                final Controls c = views[index];
                content.getItems().setAll(c.controls(), c.view());
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
        final GridView main = (GridView) views[TABLE_VIEW].view();
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
}
