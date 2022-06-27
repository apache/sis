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
package org.apache.sis.gui.referencing;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.gazetteer.GazetteerFactory;
import org.apache.sis.referencing.gazetteer.GazetteerException;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;


/**
 * Keep a list of menu items up to date with an {@code ObservableList<ReferenceSystem>}.
 * The selected {@link MenuItem} is given by {@link ToggleGroup#selectedToggleProperty()}
 * but for the purpose of {@link RecentReferenceSystems} we rather need a property giving
 * the selected reference system directly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class MenuSync extends SimpleObjectProperty<ReferenceSystem> implements EventHandler<ActionEvent> {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * Keys where to store the reference system in {@link MenuItem} properties.
     */
    private static final String REFERENCE_SYSTEM_KEY = "ReferenceSystem";

    /**
     * Sentinel value associated to {@link #REFERENCE_SYSTEM_KEY} for requesting the {@link CRSChooser}.
     */
    private static final String CHOOSER = "CHOOSER";

    /**
     * The list of reference systems to show as menu items.
     */
    private final ObservableList<? extends ReferenceSystem> systems;

    /**
     * The list of menu items to keep up-to-date with an {@code ObservableList<ReferenceSystem>}.
     */
    private final ObservableList<MenuItem> menus;

    /**
     * The group of menus.
     */
    private final ToggleGroup group;

    /**
     * The action to execute when a reference system is selected. This is not directly the user-specified action,
     * but rather a {@link org.apache.sis.gui.referencing.RecentReferenceSystems.SelectionListener} instance wrapping
     * that action. This listener is invoked explicitly instead of using {@link SimpleObjectProperty} listeners because
     * we do not invoke it in all cases.
     */
    private final RecentReferenceSystems.SelectionListener action;

    /**
     * Creates a new synchronization for the given list of menu items.
     *
     * @param  systems  the reference systems for which to build menu items.
     * @param  bean     the menu to keep synchronized with the list of reference systems.
     * @param  action   the user-specified action to execute when a reference system is selected.
     */
    MenuSync(final ObservableList<ReferenceSystem> systems, final Menu bean, final RecentReferenceSystems.SelectionListener action) {
        super(bean, "value");
        this.systems = systems;
        this.menus   = bean.getItems();
        this.group   = new ToggleGroup();
        this.action  = action;
        /*
         * We do not register listener for `systems` list.
         * Instead `notifyChanges()` will be invoked directly by RecentReferenceSystems.
         */
        final MenuItem[] items = new MenuItem[systems.size()];
        final Locale locale = action.owner().locale;
        for (int i=0; i<items.length; i++) {
            items[i] = createItem(systems.get(i), locale);
        }
        menus.setAll(items);
        initialize();
    }

    /**
     * Sets the initial value to the first two-dimensional item in the {@link #systems} list, if any.
     * This method is invoked in JavaFX thread at construction time or, if it didn't work,
     * at some later time when the systems list may contain an element.
     * This method should not be invoked anymore after initialization succeeded.
     */
    private void initialize() {
        for (final ReferenceSystem system : systems) {
            if (system instanceof CoordinateReferenceSystem) {
                if (ReferencingUtilities.getDimension((CoordinateReferenceSystem) system) == BIDIMENSIONAL) {
                    set(system);
                    break;
                }
            }
        }
    }

    /**
     * Creates a new menu item for the given reference system.
     */
    private MenuItem createItem(final ReferenceSystem system, final Locale locale) {
        if (system == RecentReferenceSystems.OTHER) {
            final MenuItem item = new MenuItem(ObjectStringConverter.other(locale));
            item.getProperties().put(REFERENCE_SYSTEM_KEY, CHOOSER);
            item.setOnAction(this);
            return item;
        } else {
            final RadioMenuItem item = new RadioMenuItem(IdentifiedObjects.getDisplayName(system, locale));
            item.getProperties().put(REFERENCE_SYSTEM_KEY, system);
            item.setToggleGroup(group);
            item.setOnAction(this);
            return item;
        }
    }

    /**
     * Creates new menu items for references system by identifiers, offered in a separated sub-menu.
     * This list of reference system is fixed; items are not added or removed following user's selection.
     */
    final void addReferencingByIdentifiers() {
        final Locale locale = action.owner().locale;
        final GazetteerFactory factory = new GazetteerFactory();
        final Resources resources = Resources.forLocale(locale);
        final Menu menu = new Menu(resources.getString(Resources.Keys.ReferenceByIdentifiers));
        for (final String name : factory.getSupportedNames()) try {
            final ReferenceSystem system = factory.forName(name);
            final MenuItem item = new MenuItem(IdentifiedObjects.getDisplayName(system, locale));
            item.getProperties().put(REFERENCE_SYSTEM_KEY, system);
            item.setOnAction(this);
            menu.getItems().add(item);
        } catch (GazetteerException e) {
            RecentReferenceSystems.errorOccurred("createMenuItems", e);
        }
        menus.add(menu);
    }

    /**
     * Must be invoked after removing a menu item for avoiding memory leak.
     */
    private static void dispose(final MenuItem item) {
        item.setOnAction(null);
        if (item instanceof RadioMenuItem) {
            ((RadioMenuItem) item).setToggleGroup(null);
        }
    }

    /**
     * Invoked when the list of reference systems changed. While it would be possible to trace the permutations,
     * additions, removals and replacements done on the list, it is easier to recreate the menu items list from
     * scratch (with recycling of existing items) and inspect the differences.
     *
     * @see RecentReferenceSystems#notifyChanges()
     */
    final void notifyChanges() {
        /*
         * Build a map of current menu items. Key are CRS objects.
         */
        final var subMenus = new ArrayList<Menu>();
        final Map<Object,MenuItem> mapping = new IdentityHashMap<>();
        for (final Iterator<MenuItem> it = menus.iterator(); it.hasNext();) {
            final MenuItem item = it.next();
            if (item instanceof Menu) {
                subMenus.add((Menu) item);
            } else if (mapping.putIfAbsent(item.getProperties().get(REFERENCE_SYSTEM_KEY), item) != null) {
                it.remove();    // Remove duplicated item. Should never happen, but we are paranoiac.
                dispose(item);
            }
        }
        /*
         * Prepare a list of menu items and assign a value to all elements where the menu item can be reused as-is.
         * Other menu items are left to null for now; those null values may appear anywhere in the array. After this
         * loop, the map will contain only menu items for CRS that are no longer in the list of CRS to offer.
         */
        final int newCount = systems.size();
        final MenuItem[] items = new MenuItem[newCount + subMenus.size()];
        for (int i=0; i<newCount; i++) {
            Object key = systems.get(i);
            if (key == RecentReferenceSystems.OTHER) key = CHOOSER;
            items[i] = mapping.remove(key);
        }
        /*
         * Previous loop took all items that could be reused as-is. Now search for all items that are new.
         * For each new item to create, recycle an arbitrary `mapping` element (in any order) if some exist.
         * When creating new items, it may happen that one of those items represent the currently selected CRS.
         */
        ReferenceSystem selected = get();
        final Iterator<MenuItem> recycle = mapping.values().iterator();
        final Locale locale = action.owner().locale;
        for (int i=0; i<newCount; i++) {
            if (items[i] == null) {
                MenuItem item = null;
                final ReferenceSystem system = systems.get(i);
                if (system != RecentReferenceSystems.OTHER && recycle.hasNext()) {
                    item = recycle.next();
                    recycle.remove();
                    if (item instanceof RadioMenuItem) {
                        item.setText(IdentifiedObjects.getDisplayName(system, locale));
                        item.getProperties().put(REFERENCE_SYSTEM_KEY, system);
                    }
                }
                if (item == null) {
                    item = createItem(system, locale);
                }
                if (selected != null && system == selected) {
                    ((RadioMenuItem) item).setSelected(true);       // ClassCastException should never occur here.
                    selected = null;
                }
                items[i] = item;
            }
        }
        /*
         * If there is any item left, we must remove them from the ToggleGroup for avoiding memory leak.
         * The sub-menus (if any) are appended last with no change.
         */
        while (recycle.hasNext()) {
            dispose(recycle.next());
        }
        for (int i=newCount; i<items.length; i++) {
            items[i] = subMenus.get(i - newCount);
        }
        GUIUtilities.copyAsDiff(Arrays.asList(items), menus);
        /*
         * If we had no previously selected item, selects it now.
         */
        if (get() == null) {
            initialize();
        }
    }

    /**
     * Invoked when user selects a menu item. This method gets the old and new values and sends them to
     * {@link org.apache.sis.gui.referencing.RecentReferenceSystems.SelectionListener} as a change event.
     * That {@code SelectionListener} will update the list of reference systems, which may result
     * in a callback to {@link #notifyChanges()}. If the selected menu item is the "Other…" choice,
     * then {@code SelectionListener} will popup {@link CRSChooser} and callback {@link #set(ReferenceSystem)}
     * for storing the result. Otherwise we need to invoke {@link #set(ReferenceSystem)} ourselves.
     */
    @Override
    public void handle(final ActionEvent event) {
        // ClassCastException should not happen because this listener is registered only on MenuItem.
        final Object value = ((MenuItem) event.getSource()).getProperties().get(REFERENCE_SYSTEM_KEY);
        if (value == CHOOSER) {
            action.changed(this, get(), RecentReferenceSystems.OTHER);
        } else {
            set((ReferenceSystem) value);
        }
    }

    /**
     * Selects the specified reference system. This method is invoked by {@link RecentReferenceSystems} when the
     * selected CRS changed, either programmatically or by user action. User-specified {@link #action} is invoked,
     * which will typically start a background thread for transforming data. This method does nothing if the given
     * reference system is same as current one; this is important both for avoiding infinite loop and for avoiding
     * to invoke the potentially costly {@link #action}.
     */
    @Override
    public void set(ReferenceSystem system) {
        final ReferenceSystem old = get();
        if (old != system) {
            final ComparisonMode mode = action.owner().duplicationCriterion.get();
            for (final MenuItem item : menus) {
                if (item instanceof RadioMenuItem) {
                    final Object current = item.getProperties().get(REFERENCE_SYSTEM_KEY);
                    if (Utilities.deepEquals(current, system, mode)) {
                        system = (ReferenceSystem) current;
                        super.set(system);
                        ((RadioMenuItem) item).setSelected(true);
                        action.changed(this, old, system);
                        return;
                    }
                }
            }
            super.set(system);
            group.selectToggle(null);
            action.owner().addSelected(system);
            /*
             * Do not invoke action.changed(…) since we have no non-null value to provide.
             * Invoking that method with a null value would cause the CRSChooser to popup.
             */
        }
    }
}
