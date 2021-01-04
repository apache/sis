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
import java.util.Objects;
import java.lang.ref.Reference;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.apache.sis.storage.Resource;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.map.MapMenu;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.image.Interpolation;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link CoverageCanvas} with associated controls to show in a {@link CoverageExplorer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageControls extends Controls {
    /**
     * The component for showing sample values.
     */
    private final CoverageCanvas view;

    /**
     * The control showing categories and their colors for the current coverage.
     */
    private final TableView<Category> categoryTable;

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
     * @param  vocabulary  localized set of words, provided in argument because often known by the caller.
     * @param  coverage    property containing the coverage to show.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    CoverageControls(final Vocabulary vocabulary, final ObjectProperty<GridCoverage> coverage,
                     final RecentReferenceSystems referenceSystems)
    {
        final Resources resources = Resources.forLocale(vocabulary.getLocale());
        view = new CoverageCanvas(vocabulary.getLocale());
        view.setBackground(Color.BLACK);
        final StatusBar statusBar = new StatusBar(referenceSystems, view);
        view.statusBar = statusBar;
        imageAndStatus = new BorderPane(view.getView());
        imageAndStatus.setBottom(statusBar.getView());
        final MapMenu menu = new MapMenu(view);
        menu.addReferenceSystems(referenceSystems);
        menu.addCopyOptions(statusBar);
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
                label(vocabulary, Vocabulary.Keys.Interpolation, createInterpolationButton(vocabulary.getLocale())));
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
         * Put all sections together and have the first one expanded by default.
         * The "Properties" section will be built by `PropertyPaneCreator` only if requested.
         */
        final TitledPane p1 = new TitledPane(vocabulary.getString(Vocabulary.Keys.SpatialRepresentation), displayPane);
        final TitledPane p2 = new TitledPane(vocabulary.getString(Vocabulary.Keys.Colors), colorsPane);
        final TitledPane p3 = new TitledPane(vocabulary.getString(Vocabulary.Keys.Properties), null);
        controls = new Accordion(p1, p2, p3);
        controls.setExpandedPane(p1);
        view.coverageProperty.bind(coverage);
        p3.expandedProperty().addListener(new PropertyPaneCreator(view, p3));
    }

    /**
     * Creates the controls for choosing an interpolation method.
     */
    private ChoiceBox<Interpolation> createInterpolationButton(final Locale locale) {
        final ChoiceBox<Interpolation> b = new ChoiceBox<>();
        b.setConverter(new InterpolationConverter(locale));
        b.getItems().setAll(InterpolationConverter.INTERPOLATIONS);
        b.getSelectionModel().select(view.getInterpolation());
        view.interpolationProperty.bind(b.getSelectionModel().selectedItemProperty());
        return b;
    }

    /**
     * Gives a localized {@link String} instance for a given {@link Interpolation} and conversely.
     */
    private static final class InterpolationConverter extends StringConverter<Interpolation> {
        /** The interpolation supported by this converter. */
        static final Interpolation[] INTERPOLATIONS = {
            Interpolation.NEAREST, Interpolation.BILINEAR, Interpolation.LANCZOS
        };

        /** Keys of localized names for each {@link #INTERPOLATIONS} element. */
        private static final short[] VOCABULARIES = {
            Vocabulary.Keys.NearestNeighbor, Vocabulary.Keys.Bilinear, 0
        };

        /** The locale to use for string representation. */
        private final Locale locale;

        /** Creates a new converter for the given locale. */
        InterpolationConverter(final Locale locale) {
            this.locale = locale;
        }

        /** Returns a string representation of the given item. */
        @Override public String toString(final Interpolation item) {
            for (int i=0; i<INTERPOLATIONS.length; i++) {
                if (INTERPOLATIONS[i].equals(item)) {
                    final short key = VOCABULARIES[i];
                    if (key != 0) {
                        return Vocabulary.getResources(locale).getString(key);
                    } else if (item == Interpolation.LANCZOS) {
                        return "Lanczos";
                    }
                }
            }
            return Objects.toString(item);
        }

        /** Returns the interpolation for the given text. */
        @Override public Interpolation fromString(final String text) {
            final Vocabulary vocabulary = Vocabulary.getResources(locale);
            for (int i=0; i<VOCABULARIES.length; i++) {
                final short key = VOCABULARIES[i];
                final Interpolation item = INTERPOLATIONS[i];
                if ((key != 0 && vocabulary.getString(key).equalsIgnoreCase(text))
                                        || item.toString().equalsIgnoreCase(text))
                {
                    return item;
                }
            }
            return null;
        }
    }

    /**
     * Invoked the first time that the "Properties" pane is opened for building the JavaFX visual components.
     * We deffer the creation of this pane because it is often not requested at all, since this is more for
     * developers than users.
     */
    private static final class PropertyPaneCreator implements ChangeListener<Boolean> {
        /** A copy of {@link CoverageControls#view} reference. */
        private final CoverageCanvas view;

        /** The pane where to set the content. */
        private final TitledPane pane;

        /** Creates a new {@link ImagePropertyExplorer} constructor. */
        PropertyPaneCreator(final CoverageCanvas view, final TitledPane pane) {
            this.view = view;
            this.pane = pane;
        }

        /** Creates the {@link ImagePropertyExplorer} when {@link TitledPane#expandedProperty()} changed. */
        @Override public void changed(ObservableValue<? extends Boolean> property, Boolean oldValue, Boolean newValue) {
            if (newValue) {
                pane.expandedProperty().removeListener(this);
                final ImagePropertyExplorer properties = view.createPropertyExplorer();
                properties.updateOnChange.bind(pane.expandedProperty());
                pane.setContent(properties.getView());
            }
        }
    }

    /**
     * Invoked in JavaFX thread after {@link CoverageExplorer#setCoverage(ImageRequest)} completed.
     * This method updates the GUI with new information available.
     *
     * @param  data        the new coverage, or {@code null} if none.
     * @param  originator  the resource from which the data has been read, or {@code null} if unknown.
     */
    @Override
    final void coverageChanged(final GridCoverage data, final Reference<Resource> originator) {
        view.setOriginator(originator);
        if (data != null) {
            final int visibleBand = 0;          // TODO: provide a selector for the band to show.
            final List<SampleDimension> bands = data.getSampleDimensions();
            categoryTable.getItems().setAll(bands.get(visibleBand).getCategories());
        } else {
            categoryTable.getItems().clear();
        }
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
