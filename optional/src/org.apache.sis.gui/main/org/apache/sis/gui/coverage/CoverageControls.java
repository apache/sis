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

import java.util.List;
import java.util.Locale;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.collections.ObservableList;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.gui.dataset.WindowHandler;
import org.apache.sis.gui.map.MapMenu;
import org.apache.sis.image.Interpolation;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.controls.ValueColorMapper;
import org.apache.sis.gui.controls.SyncWindowList;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link CoverageCanvas} with associated controls to show in a {@link CoverageExplorer}.
 * This class installs bidirectional bindings between {@link CoverageCanvas} and the controls.
 * The controls are updated when the coverage shown in {@link CoverageCanvas} is changed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CoverageControls extends ViewAndControls {
    /**
     * The component for showing sample values.
     *
     * @see CoverageExplorer#getCanvas()
     */
    final CoverageCanvas view;

    /**
     * The control showing categories and their colors for the current coverage.
     */
    private final TableView<Category> categoryTable;

    /**
     * The control used for selecting a color ramp for a given category.
     */
    private final CoverageStyling styling;

    /**
     * The control used for selecting a color stretching mode.
     */
    private final ChoiceBox<Stretching> stretching;

    /**
     * The control used for selecting the interpolation method.
     */
    private final ChoiceBox<Interpolation> interpolation;

    /**
     * The renderer of isolines.
     */
    private final IsolineRenderer isolines;

    /**
     * Creates a new set of coverage controls.
     *
     * @param  owner   the widget which creates this view. Cannot be null.
     * @param  window  the handler of the window which will show the coverage explorer.
     */
    CoverageControls(final CoverageExplorer owner, final WindowHandler window) {
        super(owner);
        final Locale     locale     = owner.getLocale();
        final Resources  resources  = Resources.forLocale(locale);
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);

        view = new CoverageCanvas(this, locale);
        status.track(view);
        final MapMenu menu = new MapMenu(view);
        menu.addReferenceSystems(owner.referenceSystems);
        menu.addCopyOptions(status);
        /*
         * "Display" section with the following controls:
         *    - Current CRS
         *    - Interpolation
         *    - Color stretching
         *    - Colors for each category
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final Label crsControl = new Label();
            crsControl.setPadding(CONTENT_MARGIN);
            crsControl.setTooltip(new Tooltip(resources.getString(Resources.Keys.SelectCrsByContextMenu)));
            menu.selectedReferenceSystem().ifPresent((text) -> crsControl.textProperty().bind(text));
            /*
             * Creates a "Values" sub-section with the following controls:
             *   - Interpolation
             *   - Color stretching
             */
            interpolation = InterpolationConverter.button(view);
            stretching = Stretching.createButton((p,o,n) -> view.setStretching(n));
            final GridPane valuesControl = Styles.createControlGrid(0,
                label(vocabulary, Vocabulary.Keys.Interpolation, interpolation),
                label(vocabulary, Vocabulary.Keys.Stretching, stretching));
            /*
             * Creates a "Categories" section with the category table.
             */
            styling = new CoverageStyling(view);
            categoryTable = styling.createCategoryTable(resources, vocabulary);
            VBox.setVgrow(categoryTable, Priority.ALWAYS);
            /*
             * All sections put together.
             */
            displayPane = new VBox(
                    labelOfGroup(vocabulary, Vocabulary.Keys.ReferenceSystem, crsControl,    true),  crsControl,
                    labelOfGroup(vocabulary, Vocabulary.Keys.Values,          valuesControl, false), valuesControl,
                    labelOfGroup(vocabulary, Vocabulary.Keys.Categories,      categoryTable, false), categoryTable);
        }
        /*
         * "Isolines" section with the following controls:
         *    - Colors for each isoline levels
         */
        final VBox isolinesPane;
        {   // Block for making variables locale to this scope.
            final ValueColorMapper mapper = new ValueColorMapper(resources, vocabulary);
            isolines = new IsolineRenderer(view);
            isolines.setIsolineTables(List.of(mapper.getSteps()));
            final Region style = mapper.getView();
            VBox.setVgrow(style, Priority.ALWAYS);
            isolinesPane = new VBox(style);                         // TODO: add band selector
        }
        /*
         * Synchronized windows. A synchronized windows is a window which can reproduce the same gestures
         * (zoom, pan, rotation) than the window containing this view. The maps displayed in different
         * windows do not need to use the same map projection; translations will be adjusted as needed.
         */
        final SyncWindowList windows = new SyncWindowList(window, resources, vocabulary);
        /*
         * Put all sections together and have the first one expanded by default.
         * The "Properties" section will be built by `PropertyPaneCreator` only if requested.
         */
        final TitledPane deferred;                  // Control to be built only if requested.
        controlPanes = new TitledPane[] {
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Display),  displayPane),
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Isolines), isolinesPane),
            new TitledPane(resources.getString(Resources.Keys.Windows), windows.getView()),
            deferred = new TitledPane(vocabulary.getString(Vocabulary.Keys.Properties), null)
        };
        /*
         * Set listeners: changes on `CoverageCanvas` properties are propagated to the corresponding
         * `CoverageExplorer` properties. This constructor does not install listeners in the opposite
         * direction; instead `CoverageExplorer` will invoke `load(ImageRequest)`.
         */
        view.resourceProperty.addListener((p,o,n) -> notifyDataChanged(n, null));
        view.coverageProperty.addListener((p,o,n) -> notifyDataChanged(view.getResourceIfAdjusting(), n));
        deferred.expandedProperty().addListener(new PropertyPaneCreator(view, deferred));
        setView(view.getView());
    }

    /**
     * Invoked in JavaFX thread after {@link CoverageCanvas} resource or coverage property value changed.
     * This method updates the controls GUI with new information available and update the corresponding
     * {@link CoverageExplorer} properties.
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     */
    private void notifyDataChanged(final GridCoverageResource resource, final GridCoverage coverage) {
        if (isAdjustingSlice) {
            return;
        }
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
        view.setImage(request);
    }

    /**
     * Copies the styling configuration from the given controls.
     * This is invoked when the user click on "New window" button.
     */
    final void copyStyling(final CoverageControls c) {
        styling.copyStyling(c.styling);
        GUIUtilities.copySelection(c.stretching, stretching);
        GUIUtilities.copySelection(c.interpolation, interpolation);
    }
}
