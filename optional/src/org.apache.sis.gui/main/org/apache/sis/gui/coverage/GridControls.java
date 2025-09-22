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

import javafx.beans.property.DoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.internal.Styles;


/**
 * A {@link GridView} with associated controls to show in a {@link CoverageExplorer}.
 * This class installs bidirectional bindings between {@link GridView} and the controls.
 * The controls are updated when the image shown in {@link GridView} is changed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridControls extends ViewAndControls {
    /**
     * The component for showing sample values.
     */
    private final GridView view;

    /**
     * The control showing sample dimensions for the current coverage.
     */
    private final TableView<SampleDimension> sampleDimensions;

    /**
     * Creates a new set of grid controls.
     *
     * @param  owner  the widget which creates this view. Cannot be null.
     */
    GridControls(final CoverageExplorer owner) {
        super(owner);
        view = new GridView(this);
        final Vocabulary vocabulary = Vocabulary.forLocale(owner.getLocale());
        sampleDimensions = new BandRangeTable(view.cellFormat).create(vocabulary);
        BandSelectionListener.bind(view.bandProperty, sampleDimensions.getSelectionModel());
        /*
         * "Display" section with the following controls:
         *    - Coverage domain as a list of CRS dimensions with two of them selected (TODO).
         *    - Coverage range as a list of sample dimensions with at least one selected.
         *    - Number format as a localized pattern.
         *    - Cell width as a slider.
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final GridPane gp = Styles.createControlGrid(0,
                label(vocabulary, Vocabulary.Keys.Width,  createSlider(view.cellWidth)),
                label(vocabulary, Vocabulary.Keys.Height, createSlider(view.cellHeight)),
                label(vocabulary, Vocabulary.Keys.Format, view.cellFormat.createEditor()));

            Styles.setAllRowToSameHeight(gp);
            displayPane = new VBox(
                    labelOfGroup(vocabulary, Vocabulary.Keys.SampleDimensions, sampleDimensions, true), sampleDimensions,
                    labelOfGroup(vocabulary, Vocabulary.Keys.Cells, gp, false), gp);
        }
        /*
         * All sections put together.
         */
        controlPanes = new TitledPane[] {
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Display), displayPane)
            // TODO: more controls to be added in a future version.
        };
        setView(view);
    }

    /**
     * Creates a new slider for the given range of values and bound to the specified properties.
     * This is used for creating the sliders to show in the "Display" pane.
     */
    private static Slider createSlider(final DoubleProperty property) {
        final var slider = new Slider(20, 120, property.getValue());
        property.bind(slider.valueProperty());
        slider.setShowTickMarks(false);
        return slider;
    }

    /**
     * Invoked after {@link GridView#setImage(ImageRequest)} for updating the table of sample
     * dimensions when information become available. This method is invoked in JavaFX thread.
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     */
    final void notifyDataChanged(final GridCoverageResource resource, final GridCoverage coverage) {
        final ObservableList<SampleDimension> items = sampleDimensions.getItems();
        if (coverage != null) {
            items.setAll(coverage.getSampleDimensions());
            sampleDimensions.getSelectionModel().clearAndSelect(view.getBand());
        } else {
            items.clear();
        }
        owner.notifyDataChanged(resource, coverage);
    }

    /**
     * Sets the view content to the given image.
     * This method is invoked when a new source of data (either a resource or a coverage) is specified,
     * or when a previously hidden view is made visible. This implementation starts a background thread.
     *
     * @param  request  the image to set, or {@code null} for clearing the view.
     */
    @Override
    final void load(final ImageRequest request) {
        view.setImage(request);
    }
}
