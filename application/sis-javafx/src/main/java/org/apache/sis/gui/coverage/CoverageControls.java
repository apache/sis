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
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.gui.map.MapMenu;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.internal.gui.control.ValueColorMapper;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link CoverageCanvas} with associated controls to show in a {@link CoverageExplorer}.
 * This class installs bidirectional bindings between {@link CoverageCanvas} and the controls.
 * The controls are updated when the coverage shown in {@link CoverageCanvas} is changed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class CoverageControls extends ViewAndControls {
    /**
     * The component for showing sample values.
     */
    private final CoverageCanvas view;

    /**
     * The control showing categories and their colors for the current coverage.
     */
    private final TableView<Category> categoryTable;

    /**
     * The renderer of isolines.
     */
    private final IsolineRenderer isolines;

    /**
     * The controls for changing {@link #view}.
     */
    private final Accordion controls;

    /**
     * The image together with the status bar.
     */
    private final BorderPane imageAndStatus;

    /**
     * Creates a new set of coverage controls.
     *
     * @param  owner  the widget which create this view. Can not be null.
     */
    CoverageControls(final CoverageExplorer owner) {
        super(owner);
        final Locale     locale     = owner.getLocale();
        final Resources  resources  = Resources.forLocale(locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);

        view = new CoverageCanvas(locale);
        view.setBackground(Color.BLACK);
        view.statusBar = new StatusBar(owner.referenceSystems, view);
        imageAndStatus = new BorderPane(view.getView());
        imageAndStatus.setBottom(view.statusBar.getView());
        final MapMenu menu = new MapMenu(view);
        menu.addReferenceSystems(owner.referenceSystems);
        menu.addCopyOptions(view.statusBar);
        /*
         * "Display" section with the following controls:
         *    - Current CRS
         *    - Interpolation
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final Label crsControl = new Label();
            final Label crsHeader  = labelOfGroup(vocabulary, Vocabulary.Keys.ReferenceSystem, crsControl, true);
            crsControl.setPadding(CONTENT_MARGIN);
            crsControl.setTooltip(new Tooltip(resources.getString(Resources.Keys.SelectCrsByContextMenu)));
            menu.selectedReferenceSystem().ifPresent((text) -> crsControl.textProperty().bind(text));
            /*
             * Creates a "Values" sub-section with the following controls:
             *   - Interpolation
             */
            final GridPane valuesControl = Styles.createControlGrid(0,
                label(vocabulary, Vocabulary.Keys.Interpolation, InterpolationConverter.button(view)));
            final Label valuesHeader = labelOfGroup(vocabulary, Vocabulary.Keys.Values, valuesControl, false);
            /*
             * All sections put together.
             */
            displayPane = new VBox(crsHeader, crsControl, valuesHeader, valuesControl);
        }
        /*
         * "Colors" section with the following controls:
         *    - Colors for each category
         *    - Color stretching
         */
        final VBox colorsPane;
        {   // Block for making variables locale to this scope.
            final CoverageStyling styling = new CoverageStyling(view);
            categoryTable = styling.createCategoryTable(vocabulary);
            final GridPane gp = Styles.createControlGrid(0,
                label(vocabulary, Vocabulary.Keys.Stretching, Stretching.createButton((p,o,n) -> view.setStyling(n))));

            colorsPane = new VBox(
                    labelOfGroup(vocabulary, Vocabulary.Keys.Categories, categoryTable, true), categoryTable, gp);
        }
        /*
         * "Isolines" section with the following controls:
         *    - Colors for each isoline levels
         */
        final VBox isolinesPane;
        {   // Block for making variables locale to this scope.
            final ValueColorMapper mapper = new ValueColorMapper(resources, vocabulary);
            isolines = new IsolineRenderer(view);
            isolines.setIsolineTables(java.util.Collections.singletonList(mapper.getSteps()));
            isolinesPane = new VBox(mapper.getView());              // TODO: add band selector
        }
        /*
         * Put all sections together and have the first one expanded by default.
         * The "Properties" section will be built by `PropertyPaneCreator` only if requested.
         */
        final TitledPane p1 = new TitledPane(vocabulary.getString(Vocabulary.Keys.SpatialRepresentation), displayPane);
        final TitledPane p2 = new TitledPane(vocabulary.getString(Vocabulary.Keys.Colors), colorsPane);
        final TitledPane p3 = new TitledPane(vocabulary.getString(Vocabulary.Keys.Isolines), isolinesPane);
        final TitledPane p4 = new TitledPane(vocabulary.getString(Vocabulary.Keys.Properties), null);
        controls = new Accordion(p1, p2, p3, p4);
        controls.setExpandedPane(p1);
        /*
         * Set listeners: changes on `CoverageCanvas` properties are propagated to the corresponding
         * `CoverageExplorer` properties. This constructor does not install listeners in the opposite
         * direction; instead `CoverageExplorer` will invoke `load(ImageRequest)`.
         */
        view.resourceProperty.addListener((p,o,n) -> onPropertySet(n, null));
        view.coverageProperty.addListener((p,o,n) -> onPropertySet(null, n));
        p4.expandedProperty().addListener(new PropertyPaneCreator(view, p4));
    }

    /**
     * Invoked in JavaFX thread after {@link CoverageCanvas} resource or coverage property value changed.
     * This method updates the controls GUI with new information available and update the corresponding
     * {@link CoverageExplorer} properties.
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     */
    private void onPropertySet(final GridCoverageResource resource, final GridCoverage coverage) {
        final ObservableList<Category> items = categoryTable.getItems();
        if (coverage == null) {
            items.clear();
        } else {
            final int visibleBand = 0;          // TODO: provide a selector for the band to show.
            items.setAll(coverage.getSampleDimensions().get(visibleBand).getCategories());
        }
        owner.notifyDataChanged(resource, coverage);
    }

    /**
     * Sets the view content to the given coverage.
     * This method is invoked when a new source of data (either a resource or a coverage) is specified,
     * or when a previously hidden view is made visible. This implementation starts a background thread.
     *
     * @param  request  the resource or coverage to set, or {@code null} for clearing the view.
     */
    @Override
    final void load(final ImageRequest request) {
        final GridCoverageResource resource;
        final GridCoverage coverage;
        if (request != null) {
            resource = request.resource;
            coverage = request.getCoverage().orElse(null);
        } else {
            resource = null;
            coverage = null;
        }
        view.setCoverage(resource, coverage);
    }

    /**
     * Returns the main component, which is showing coverage tabular data.
     */
    @Override
    final Region view() {
        return imageAndStatus;
    }

    /**
     * Returns the controls for controlling the view of tabular data.
     */
    @Override
    final Control controls() {
        return controls;
    }
}
