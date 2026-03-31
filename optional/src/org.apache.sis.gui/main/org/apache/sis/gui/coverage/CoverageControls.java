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
import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TitledPane;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.ObservableList;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.gui.dataset.WindowHandler;
import org.apache.sis.gui.map.MapMenu;
import org.apache.sis.gui.map.style.MapLayer;
import org.apache.sis.gui.map.style.MapContextView;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.DataStoreOpener;
import org.apache.sis.gui.internal.BackgroundThreads;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;
import org.apache.sis.gui.controls.SyncWindowList;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.Logging;


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
     * Provides widget for controlling the rendering of the coverage.
     * This is the root of the {@link MapContextView} tree.
     */
    private final StyleController style;

    /**
     * Tool tip for the reference system shown in the status bar.
     */
    private final Tooltip referenceSystemTooltip;

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
        final ObservableObjectValue<String> mapCRS = menu.selectedReferenceSystem().orElse(null);
        if (mapCRS == null) {
            referenceSystemTooltip = null;
        } else {
            referenceSystemTooltip = new Tooltip(resources.getString(Resources.Keys.SelectCrsByContextMenu));
            mapCRS.addListener((p,o,n) -> notifyReferenceSystemChanged(n, status.getReferenceSystemName().get(), true));
            status.getReferenceSystemName().addListener((p,o,n) -> notifyReferenceSystemChanged(mapCRS.get(), n, false));
        }
        /*
         * "Layers" section with the following controls:
         *    - Tree of layers associated to the coverage (styling, isolines, visual indication of loaded tiles).
         */
        final var layers = new MapContextView(resources);
        style = new StyleController(view);
        layers.setRootItem(style);
        /*
         * "Isolines" section with the following controls:
         *    - Colors for each isoline levels
         */
        if (view.isolines == null) {
            final var isolines = new IsolineController(view, vocabulary.getString(Vocabulary.Keys.Isolines));
            style.getChildren().add(isolines);
            view.isolines = isolines;
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
            new TitledPane(vocabulary.getString(Vocabulary.Keys.Layers), layers.getView()),
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
     * Invoked in JavaFX thread after the reference system of the coverage or of the status bar changed.
     * The {@code interim} argument is {@code true} if the change was in the <abbr>CRS</abbr> of the map,
     * and the <abbr>CRS</abbr> shown in the status bar is expected to be updated to the same value soon.
     * In the latter case, we avoid distracting the user with a message saying that the reference systems
     * are not consistent.
     *
     * @param  coverage     the name of the reference system of the rendered coverage.
     * @param  coordinates  the name of the reference system of coordinates shown in the status bar.
     * @param  interim      {@code true} if this event is expected to be followed soon by another event.
     */
    private void notifyReferenceSystemChanged(String coverage, final String coordinates, final boolean interim) {
        if (coverage != null && coordinates != null && !coverage.equals(coordinates)) {
            if (interim) {
                status.setDefaultMessage(null, referenceSystemTooltip);
                return;
            }
            coverage = Resources.forLocale(owner.getLocale()).getString(Resources.Keys.MismatchedRS);
        }
        status.setDefaultMessage(coverage, coverage == null ? null : referenceSystemTooltip);
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
        BackgroundThreads.execute(() -> {
            final Locale locale = owner.getLocale();
            String name;
            try {
                name = DataStoreOpener.findLabel(resource, locale, true);
            } catch (DataStoreException | RuntimeException e) {
                // Declare `setResource` as the public method invoking (indirectly) this method.
                Logging.recoverableException(LOGGER, CoverageExplorer.class, "setResource", e);
                name = DataStoreOpener.fallbackLabel(resource, locale);
            }
            final var layer = new MapLayer<>(resource, name);
            Platform.runLater(() -> {
                if (owner.getResource() == resource) {      // Verify that the resource did not changed concurrently.
                    style.setData(layer);
                }
            });
        });
        final ObservableList<Category> items = style.categoryTable.getItems();
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
        style.copyStyling(c.style);
    }
}
