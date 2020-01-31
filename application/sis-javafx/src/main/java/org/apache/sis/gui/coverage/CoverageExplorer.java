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

import java.util.Locale;
import java.awt.image.RenderedImage;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A view of {@link GridCoverage} numerical values together with controls for band selection
 * and other operations. This class manages a {@link GridView} for showing the numerical values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CoverageExplorer {
    /**
     * The data shown in this table. Note that setting this property to a non-null value may not
     * modify the grid content immediately. Instead, a background process will request the tiles.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.</p>
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

    /**
     * The component for showing sample values.
     */
    private final GridView gridView;

    /**
     * The control showing sample dimensions for the current coverage.
     */
    private final ListView<SampleDimension> sampleDimensions;

    /**
     * The control that put everything together.
     * The type of control may change in any future SIS version.
     *
     * @see #getView()
     */
    private final SplitPane content;

    /**
     * Creates an initially empty explorer.
     */
    public CoverageExplorer() {
        gridView         = new GridView();
        sampleDimensions = new ListView<>();
        sampleDimensions.setCellFactory(SampleDimensionCell::new);
        final Vocabulary vocabulary = Vocabulary.getResources((Locale) null);
        /*
         * Create the "Coverage" pane with the following controls:
         *    - Coverage domain as a list of CRS dimensions with two of them selected (TODO).
         *    - Coverage range as a list of sample dimensions with at least one selected.
         */
        final VBox coveragePane;
        {   // Block for making variables locale to this scope.
            final Label label = new Label(vocabulary.getString(Vocabulary.Keys.SampleDimensions));
            label.setLabelFor(sampleDimensions);
            coveragePane = new VBox(label, sampleDimensions);
        }

        final Accordion controls = new Accordion(
                new TitledPane(vocabulary.getString(Vocabulary.Keys.Coverage), coveragePane)
                // TODO: more controls to be added in a future version.
        );
        content = new SplitPane(controls, gridView);
        SplitPane.setResizableWithParent(controls, Boolean.FALSE);
        SplitPane.setResizableWithParent(gridView, Boolean.TRUE);
        content.setDividerPosition(0, Styles.INITIAL_SPLIT);

        coverageProperty = new SimpleObjectProperty<>(this, "coverage");
        coverageProperty.addListener(this::onCoverageSpecified);
    }

    /**
     * Returns the region containing the grid view, band selector and any other control managed
     * by this {@code CoverageExplorer}. The subclass is implementation dependent and may change
     * in any future version.
     *
     * @return the region to show.
     */
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
     * Sets the coverage to show in this table.
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
            source.addListener(this);
            gridView.setImage(source);
        }
    }

    /**
     * Invoked when a new coverage has been specified
     *
     * @param  property  the {@link #coverageProperty} (ignored).
     * @param  previous  ignored.
     * @param  coverage  the new coverage.
     */
    private void onCoverageSpecified(final ObservableValue<? extends GridCoverage> property,
                                     final GridCoverage previous, final GridCoverage coverage)
    {
        gridView.setImage((RenderedImage) null);
        if (coverage != null) {
            gridView.setImage(new ImageRequest(coverage, null));        // Start a background thread.
            sampleDimensions.getItems().setAll(coverage.getSampleDimensions());
        } else {
            sampleDimensions.getItems().clear();
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
    final void onLoadStep(final GridCoverage coverage) {
        final ObservableList<SampleDimension> items = sampleDimensions.getItems();
        if (coverage != null) {
            items.setAll(coverage.getSampleDimensions());
        } else {
            items.clear();
        }
    }

    /**
     * A row in the list of sample dimensions.
     */
    private static final class SampleDimensionCell extends ListCell<SampleDimension> {
        /** Invoked by lambda function (needs this exact signature). */
        SampleDimensionCell(final ListView<SampleDimension> list) {
        }

        /** Invoked when a new sample dimension needs to be shown. */
        @Override public void updateItem(final SampleDimension item, final boolean empty) {
            super.updateItem(item, empty);
            setText(empty ? "" : item.getName().toString());
        }
    }
}
