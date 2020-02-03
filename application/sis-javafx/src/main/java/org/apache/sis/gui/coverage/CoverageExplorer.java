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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
     * Margin to keep around captions on top of tables or lists.
     */
    private static final Insets CAPTION_MARGIN = new Insets(12, 0, 9, 0);

    /**
     * Space between a group of controls and the border encompassing the group.
     */
    private static final Insets GROUP_INSETS = new Insets(12);

    /**
     * The border to use for grouping some controls together.
     */
    private static final Border GROUP_BORDER = new Border(new BorderStroke(
            Styles.GROUP_BORDER, BorderStrokeStyle.SOLID, null, null));

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
    private final TableView<SampleDimension> sampleDimensions;

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
        final Vocabulary vocabulary = Vocabulary.getResources((Locale) null);
        gridView         = new GridView();
        sampleDimensions = new CategoryCellFactory(gridView.cellFormat).createSampleDimensionTable(vocabulary);
        /*
         * "Coverage" section with the following controls:
         *    - Coverage domain as a list of CRS dimensions with two of them selected (TODO).
         *    - Coverage range as a list of sample dimensions with at least one selected.
         */
        final VBox coveragePane;
        {   // Block for making variables locale to this scope.
            final Label label = new Label(vocabulary.getLabel(Vocabulary.Keys.SampleDimensions));
            label.setPadding(CAPTION_MARGIN);
            label.setLabelFor(sampleDimensions);
            coveragePane = new VBox(label, sampleDimensions);
        }
        /*
         * "Display" section with the following controls:
         *    - Number format as a localized pattern (TODO).
         *    - Cell width as a slider.
         */
        final VBox displayPane;
        {   // Block for making variables locale to this scope.
            final GridPane gp = new GridPane();
            final ColumnConstraints sliderColumn = new ColumnConstraints();
            sliderColumn.setHgrow(Priority.ALWAYS);
            gp.getColumnConstraints().setAll(new ColumnConstraints(), sliderColumn);
            gp.setPadding(GROUP_INSETS);
            gp.setBorder(GROUP_BORDER);
            gp.setVgap(9);
            gp.setHgap(9);
            int row = 0;
            do {
                final DoubleProperty property;
                final double min, max;
                final short key;
                if (row == 0) {key = Vocabulary.Keys.Width;   property = gridView.cellWidth;   min = 30; max = 200;}
                else          {key = Vocabulary.Keys.Height;  property = gridView.cellHeight;  min = 10; max =  50;}
                final Label  label  = new Label(vocabulary.getLabel(key));
                final Slider slider = new Slider(min, max, property.getValue());
                property.bind(slider.valueProperty());
                slider.setShowTickMarks(false);
                label.setLabelFor(slider);
                GridPane.setConstraints(label,  0, row);
                GridPane.setConstraints(slider, 1, row);
                gp.getChildren().addAll(label, slider);
            } while (++row <= 1);
            final Label label = new Label(vocabulary.getLabel(Vocabulary.Keys.Cells));
            label.setPadding(CAPTION_MARGIN);
            label.setLabelFor(gp);
            displayPane = new VBox(label, gp);
        }
        /*
         * Put all sections together and have the first one expanded by default.
         */
        final Accordion controls = new Accordion(
                new TitledPane(vocabulary.getString(Vocabulary.Keys.Coverage), coveragePane),
                new TitledPane(vocabulary.getString(Vocabulary.Keys.Display),  displayPane)
                // TODO: more controls to be added in a future version.
        );
        controls.setExpandedPane(controls.getPanes().get(0));
        content = new SplitPane(controls, gridView);
        SplitPane.setResizableWithParent(controls, Boolean.FALSE);
        SplitPane.setResizableWithParent(gridView, Boolean.TRUE);
        content.setDividerPosition(0, Styles.INITIAL_SPLIT);

        coverageProperty = new SimpleObjectProperty<>(this, "coverage");
        coverageProperty.addListener(this::onCoverageSpecified);
        gridView.bandProperty.addListener(this::onBandSpecified);
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
            source.listener = this;
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
        }
        onCoverageLoaded(coverage);
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
        final ObservableList<SampleDimension> items = sampleDimensions.getItems();
        if (coverage != null) {
            items.setAll(coverage.getSampleDimensions());
            sampleDimensions.getSelectionModel().clearAndSelect(gridView.getBand());
        } else {
            items.clear();
        }
    }

    /**
     * Invoked when the selected band changed. This method ensures that the selected row
     * in the sample dimension table matches the band which is shown in the grid view.
     */
    private void onBandSpecified(final ObservableValue<? extends Number> property,
                                 final Number previous, final Number band)
    {
        sampleDimensions.getSelectionModel().clearAndSelect(band.intValue());
    }
}
