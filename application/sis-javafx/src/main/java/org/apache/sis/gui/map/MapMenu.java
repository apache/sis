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
package org.apache.sis.gui.map;

import java.util.Locale;
import java.util.Optional;
import javafx.scene.control.ContextMenu;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.referencing.PositionableProjection;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.ArgumentChecks;


/**
 * A {@link ContextMenu} that can be shown in a {@link MapCanvas}.
 * On construction, this menu is initially empty.
 * Items can be added by the following method calls:
 *
 * <ul>
 *   <li>{@link #addReferenceSystems(RecentReferenceSystems)}:<ul>
 *     <li><cite>Reference system</cite> with some items from EPSG database.</li>
 *     <li><cite>Centered projection</cite> with the list of {@link PositionableProjection} items.</li>
 *   </ul></li>
 * </ul>
 *
 * More choices may be added in a future versions.
 * In current implementation, there is no mechanism for removing menu items.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class MapMenu extends ContextMenu {
    /**
     * The canvas where this menu can be shown.
     */
    private final MapCanvas canvas;

    /**
     * The property for the selected coordinate reference system, created when first needed.
     */
    private ObjectProperty<ReferenceSystem> selectedSystem;

    /**
     * Whether {@link #addReferenceSystems(RecentReferenceSystems)} has been invoked.
     */
    private boolean hasCRS;

    /**
     * Creates an initially empty menu for the given canvas.
     *
     * @param  canvas  the canvas for which to create menus.
     */
    public MapMenu(final MapCanvas canvas) {
        ArgumentChecks.ensureNonNull("canvas", canvas);
        this.canvas = canvas;
    }

    /**
     * Adds menu items for CRS selection. The menu items are in two groups:
     *
     * <ul>
     *   <li><cite>Reference system</cite> with some items from EPSG database.</li>
     *   <li><cite>Centered projection</cite> with the list of {@link PositionableProjection} items.</li>
     * </ul>
     *
     * This method can be invoked at most once.
     *
     * @param  preferences  handler of menu items for selecting a CRS from a list of EPSG codes.
     *         Often {@linkplain RecentReferenceSystems#addUserPreferences() built from user preferences}.
     * @throws IllegalStateException if this method has already been invoked.
     */
    public void addReferenceSystems(final RecentReferenceSystems preferences) {
        ArgumentChecks.ensureNonNull("preferences", preferences);
        if (hasCRS) {
            throw new IllegalStateException();
        }
        hasCRS = true;
        selectedSystem = canvas.createContextMenu(this, preferences);
    }

    /**
     * Returns an observable value for showing the currently selected CRS as a text.
     * The value is absent if {@link #addReferenceSystems(RecentReferenceSystems)} has never been invoked.
     *
     * @return the currently selected CRS as a text.
     */
    public Optional<ObservableObjectValue<String>> selectedReferenceSystem() {
        if (selectedSystem != null) {
            return Optional.of(new SelectedCRS(selectedSystem, canvas.getLocale()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Implementation of the value returned by {@link #selectedReferenceSystem()}.
     * Implemented as a static class for reducing the number of direct references to {@link MapMenu}.
     */
    private static final class SelectedCRS extends ObjectBinding<String> {
        /** The property for the selected coordinate reference system. */
        private final ObjectProperty<ReferenceSystem> selectedSystem;

        /** The locale to use for fetching CRS name. */
        private final Locale locale;

        /** Creates a new binding. */
        SelectedCRS(final ObjectProperty<ReferenceSystem> selectedSystem, final Locale locale) {
            this.selectedSystem = selectedSystem;
            this.locale = locale;
            bind(selectedSystem);
        }

        /** Invoked when the reference system changed. */
        @Override protected String computeValue() {
            return IdentifiedObjects.getDisplayName(selectedSystem.get(), locale);
        }
    }
}
