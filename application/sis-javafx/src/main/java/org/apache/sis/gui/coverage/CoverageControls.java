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
import java.util.Objects;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javafx.scene.control.Accordion;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ChoiceBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.image.Interpolation;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A {@link CoverageCanvas} with associated controls to show in a {@link CoverageExplorer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CoverageControls extends Controls implements PropertyChangeListener {
    /**
     * The component for showing sample values.
     */
    private final CoverageCanvas view;

    /**
     * The controls for changing {@link #view}.
     */
    private final Accordion controls;

    /**
     * The image together with the status bar.
     */
    private final BorderPane imageAndStatus;

    /**
     * The coordinate reference system selected in the {@link ChoiceBox}.
     */
    private final ObjectProperty<ReferenceSystem> referenceSystem;

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
        final Color background = Color.BLACK;
        view = new CoverageCanvas();
        view.setBackground(background);
        final StatusBar statusBar = new StatusBar(referenceSystems);
        statusBar.canvas.set(view);
        imageAndStatus = new BorderPane(view.getView());
        imageAndStatus.setBottom(statusBar.getView());
        /*
         * "Display" section with the following controls:
         *    - Coordinate reference system
         *    - Interpolation
         *    - Color stretching
         *    - Background color
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final GridPane referencing = createControlGrid(1,
                label(vocabulary, Vocabulary.Keys.Interpolation, createInterpolationButton(vocabulary.getLocale()))
            );
            final ChoiceBox<ReferenceSystem> systemChoices = referenceSystems.createChoiceBox((p,o,n) -> {
                if (n instanceof CoordinateReferenceSystem) {
                    view.setObjectiveCRS((CoordinateReferenceSystem) n, p);
                }
            });
            systemChoices.setMaxWidth(Double.POSITIVE_INFINITY);
            GridPane.setConstraints(systemChoices, 0, 0, 2, 1);     // First row and column, span 2 columns.
            referencing.getChildren().add(systemChoices);
            referenceSystem = systemChoices.valueProperty();

            final GridPane colors = createControlGrid(0,
                label(vocabulary, Vocabulary.Keys.Stretching, Stretching.createButton((p,o,n) -> view.setStretching(n))),
                label(vocabulary, Vocabulary.Keys.Background, createBackgroundButton(background))
            );
            displayPane = new VBox(
                labelOfGroup(vocabulary, Vocabulary.Keys.ReferenceSystem, referencing, true), referencing,
                labelOfGroup(vocabulary, Vocabulary.Keys.Colors,          colors,     false), colors);
        }
        /*
         * Put all sections together and have the first one expanded by default.
         */
        controls = new Accordion(
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Display), displayPane)
            // TODO: more controls to be added in a future version.
        );
        controls.setExpandedPane(controls.getPanes().get(0));
        view.coverageProperty.bind(coverage);
        view.addPropertyChangeListener(CoverageCanvas.OBJECTIVE_CRS_PROPERTY, this);
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
     * Creates the button for selecting a background color.
     */
    private ColorPicker createBackgroundButton(final Color background) {
        final ColorPicker b = new ColorPicker(background);
        b.setOnAction((e) -> {
            view.setBackground(((ColorPicker) e.getSource()).getValue());
        });
        return b;
    }

    /**
     * Invoked in JavaFX thread after {@link CoverageExplorer#setCoverage(ImageRequest)} completed.
     * This method updates the GUI with new information available.
     *
     * @param  data  the new coverage, or {@code null} if none.
     */
    @Override
    final void coverageChanged(final GridCoverage data) {
        // TODO
    }

    /**
     * Invoked when a canvas property changed.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        final Object value = event.getNewValue();
        if (value instanceof CoordinateReferenceSystem) {
            referenceSystem.set((CoordinateReferenceSystem) value);
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
