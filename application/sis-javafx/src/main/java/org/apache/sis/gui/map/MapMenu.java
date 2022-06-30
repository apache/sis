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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.referencing.PositionableProjection;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.Resources;
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
     * A handler for controlling the contextual menu.
     * Created when first needed.
     */
    private MapCanvas.MenuHandler menuHandler;

    /**
     * Groups of menu items that have been added. Bits in this mask are set when {@link #addCopyOptions(StatusBar)}
     * {@link #addReferenceSystems(RecentReferenceSystems)} or similar methods are invoked. Each {@code addFoo(…)}
     * method can be invoked only once.
     */
    private int defined;

    /**
     * Bit in {@link #defined} mask for tracking which {@code addFoo(…)} methods have been invoked.
     */
    private static final int CRS = 1, COPY = 2;

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
     * Invoked before an {@code addFoo(…)} method starts creating new menu items.
     * First, this method ensures that the specified group of menus has not yet been added.
     * Then the group of menus is marked as added. Next the {@link #menuHandler} instance
     * is created if needed, then returned.
     *
     * @param  mask  one of {@link #CRS}, {@link #COPY}, <i>etc.</i> constants.
     * @return the {@link #menuHandler} instance, created when first needed.
     * @throws IllegalStateException if the specified group has already been added.
     */
    private MapCanvas.MenuHandler startNewMenuItems(final int mask) {
        if ((defined & mask) != 0) {
            throw new IllegalStateException();
        }
        defined |= mask;
        if (menuHandler == null) {
            menuHandler = canvas.new MenuHandler(this);
        }
        return menuHandler;
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
     *
     * @see #selectedReferenceSystem()
     */
    public void addReferenceSystems(final RecentReferenceSystems preferences) {
        ArgumentChecks.ensureNonNull("preferences", preferences);
        final MapCanvas.MenuHandler handler = startNewMenuItems(CRS);
        final Menu systemChoices = preferences.createMenuItems(true, handler);
        handler.selectedCrsProperty = RecentReferenceSystems.getSelectedProperty(systemChoices);
        handler.positionables       = new ToggleGroup();

        final Resources resources = Resources.forLocale(canvas.getLocale());
        final Menu localSystems = new Menu(resources.getString(Resources.Keys.CenteredProjection));
        for (final PositionableProjection projection : PositionableProjection.values()) {
            final RadioMenuItem item = new RadioMenuItem(projection.toString());
            item.setToggleGroup(handler.positionables);
            item.setOnAction((e) -> handler.createProjectedCRS(projection));
            localSystems.getItems().add(item);
        }
        getItems().addAll(systemChoices, localSystems);
        canvas.addPropertyChangeListener(MapCanvas.OBJECTIVE_CRS_PROPERTY, handler);
    }

    /**
     * Adds a menu item for copying coordinates at the mouse position where right click occurred.
     * The coordinate reference system is determined by the status bar; it is not necessarily the
     * coordinate reference system of the map.
     *
     * @param  format  status bar determining the CRS and format to use for coordinate values.
     */
    public void addCopyOptions(final StatusBar format) {
        ArgumentChecks.ensureNonNull("format", format);
        final MapCanvas.MenuHandler handler = startNewMenuItems(COPY);
        final Resources resources = Resources.forLocale(canvas.getLocale());
        final MenuItem coordinates = resources.menu(Resources.Keys.CopyCoordinates, (event) -> {
            try {
                final String text = format.formatTabSeparatedCoordinates(handler.x, handler.y);
                final ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
            } catch (TransformException | RuntimeException e) {
                ExceptionReporter.show(getOwnerWindow(), ((MenuItem) event.getSource()).getText(), null, e);
            }
        });
        getItems().add(coordinates);
    }


    /**
     * Returns an observable value for showing the currently selected CRS as a text.
     * The value is absent if {@link #addReferenceSystems(RecentReferenceSystems)} has never been invoked.
     *
     * @return the currently selected CRS as a text.
     *
     * @see #addReferenceSystems(RecentReferenceSystems)
     */
    public Optional<ObservableObjectValue<String>> selectedReferenceSystem() {
        if (menuHandler != null) {
            final ObjectProperty<ReferenceSystem> selectedCrsProperty = menuHandler.selectedCrsProperty;
            if (selectedCrsProperty != null) {
                return Optional.of(new SelectedCRS(selectedCrsProperty, canvas.getLocale()));
            }
        }
        return Optional.empty();
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
