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
import javafx.scene.control.TreeItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableView;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.sis.gui.map.style.MapItem;
import org.apache.sis.gui.map.style.ItemController;
import org.apache.sis.gui.map.style.MapLayer;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.image.Interpolation;
import org.apache.sis.coverage.Category;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.tiling.TiledGridCoverageResource;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A node shown in a tree of map items for controlling the rendering of a single grid coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StyleController extends ItemController {
    /**
     * The canvas for which to control the rendering.
     */
    private final CoverageCanvas canvas;

    /**
     * The controls for configuring the appearance of the grid coverage.
     * Created when first needed.
     */
    private final Region configurationPanel;

    /**
     * The control showing categories and their colors for the current coverage.
     */
    final TableView<Category> categoryTable;

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
     * Whether to show a visual indication of which tiles are read.
     * Creates when first needed.
     */
    private ItemController showTileReads;

    /**
     * Creates a controller for a map layer.
     * The controller is initially selected.
     *
     * @param  canvas  where the coverage will be rendered.
     */
    StyleController(final CoverageCanvas canvas) {
        this.canvas = canvas;
        setSelected(true);
        setIndependent(true);
        selectedProperty().addListener((p,o,n) -> canvas.setCoverageHidden(!n));
        /*
         * "Display" section with the following controls:
         *    - Current CRS
         *    - Interpolation
         *    - Color stretching
         *    - Colors for each category
         */
        final Locale     locale     = canvas.getLocale();
        final Resources  resources  = Resources.forLocale(locale);
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        /*
         * Creates a "Values" sub-section with the following controls:
         *   - Interpolation
         *   - Color stretching
         */
        interpolation = InterpolationConverter.button(canvas);
        stretching = Stretching.createButton((p,o,n) -> canvas.setStretching(n));
        final GridPane valuesControl = Styles.createControlGrid(0,
                CoverageControls.label(vocabulary, Vocabulary.Keys.Interpolation, interpolation),
                CoverageControls.label(vocabulary, Vocabulary.Keys.Stretching, stretching));
        /*
         * Creates a "Categories" section with the category table.
         */
        styling = new CoverageStyling(canvas);
        categoryTable = styling.createCategoryTable(resources, vocabulary);
        VBox.setVgrow(categoryTable, Priority.ALWAYS);
        /*
         * All sections put together.
         */
        configurationPanel = new VBox(
                CoverageControls.labelOfGroup(vocabulary, Vocabulary.Keys.Values,     valuesControl, true),  valuesControl,
                CoverageControls.labelOfGroup(vocabulary, Vocabulary.Keys.Categories, categoryTable, false), categoryTable);
    }

    /**
     * Returns a panel of JavaFX controls for configuring the appearance of the grid coverage.
     *
     * @return the configuration panel for the grid coverage.
     */
    @Override
    protected Region getConfigurationPanel() {
        return configurationPanel;
    }

    /**
     * Copies the styling configuration from the given controls.
     * This is invoked when the user click on "New window" button.
     */
    final void copyStyling(final StyleController c) {
        styling.copyStyling(c.styling);
        GUIUtilities.copySelection(c.stretching, stretching);
        GUIUtilities.copySelection(c.interpolation, interpolation);
    }

    /**
     * Sets the item to show.
     *
     * @param  item  the new item, or {@code null} if none.
     */
    final void setData(final MapLayer<GridCoverageResource> item) {
        setValue(item);
        final boolean isTiled = (item.resource instanceof TiledGridCoverageResource);
        final ObservableList<TreeItem<MapItem>> children = getChildren();
        final int last = children.size() - 1;
        if (last >= 0 && children.get(last) == showTileReads) {
            if (!isTiled) {
                children.remove(last);
            }
        } else if (isTiled) {
            if (showTileReads == null) {
                final Resources resources = Resources.forLocale(canvas.getLocale());
                showTileReads = new ItemController(new MapItem(resources.getString(Resources.Keys.ShowTileReadEvents)));
                showTileReads.selectedProperty().addListener((p,o,n) -> canvas.showTileReads(n));
                showTileReads.setIndependent(true);
            }
            children.add(showTileReads);
        }
    }
}
