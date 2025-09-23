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

import java.util.List;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.Resources;
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
     * The list of reference systems to show in the root menu, not including items in sub-menus.
     * This is the list of most recently used reference systems, so its content may change often.
     * {@code MenuSync} does not register listeners on this list;
     * if the content is changed, then {@link #notifyChanges()} should be invoked explicitly.
     */
    private final List<ReferenceSystem> recentSystems;

    /**
     * The list of reference systems to show in the "Referencing by cell indices" sub-menu.
     * The content of this list depends on the grid coverages shown in the widget.
     * This is {@code null} if that sub-menu is omitted.
     */
    private final List<DerivedCRS> cellIndicesSystems;

    /**
     * The list of menu items to keep up-to-date with {@link #recentSystems}.
     * Contains also non-radio items such as "Other…" menu, and sub-menus for
     * referencing by identifiers and referencing by cell indices.
     */
    private final ObservableList<MenuItem> rootMenus;

    /**
     * The list of menu items to keep up-to-date with {@link #cellIndicesSystems}.
     * This is {@code null} if that sub-menu is omitted.
     */
    private final ObservableList<MenuItem> cellIndicesMenus;

    /**
     * The group of selectable menu items. Only one items can be selected at a time.
     * The items may be distributed in the root menus and in sub-menus.
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
     * @param  byIds    whether to add a sub-menu for "Referencing by identifiers".
     * @param  derived  content of "Referencing by cell indices" sub-menu, or {@code null} for omitting that sub-menu.
     * @param  bean     the menu to keep synchronized with the list of reference systems.
     * @param  action   the user-specified action to execute when a reference system is selected.
     */
    MenuSync(final List<ReferenceSystem> systems, final boolean byIds, final List<DerivedCRS> derived,
             final Menu bean, final RecentReferenceSystems.SelectionListener action)
    {
        super(bean, "value");
        recentSystems      = systems;
        cellIndicesSystems = derived;
        rootMenus          = bean.getItems();
        group              = new ToggleGroup();
        this.action        = action;
        /*
         * Root menu. The list of recent reference system is dynamic and will change according user actions.
         */
        final List<MenuItem> items = new ArrayList<>(systems.size() + 1);
        final Locale locale = action.owner().locale;
        for (final ReferenceSystem system : systems) {
            if (system == RecentReferenceSystems.OTHER) {
                items.add(new SeparatorMenuItem());
            }
            items.add(createItem(system, locale));
        }
        rootMenus.addAll(items);
        initialize();
        if (byIds) {
            addReferencingByIdentifiers(locale);
        }
        /*
         * Creates new menu items for referencing by cell indices. Choices are offered in a separated sub-menu.
         * This list of reference systems depends on the coverages shown in the widget.
         */
        if (derived != null) {
            final Menu menu = new Menu(Resources.forLocale(locale).getString(Resources.Keys.ReferenceByCellIndices));
            cellIndicesMenus = menu.getItems();
            updateCellIndicesMenus(locale);
            rootMenus.add(menu);
        } else {
            cellIndicesMenus = null;
        }
    }

    /**
     * Sets the initial value to the first two-dimensional item in the {@link #recentSystems} list, if any.
     * This method is invoked in JavaFX thread at construction time or, if it didn't work,
     * at some later time when the list of recent reference systems may contain an element.
     * This method should not be invoked anymore after initialization succeeded.
     */
    private void initialize() {
        for (final ReferenceSystem system : recentSystems) {
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
     * This list of reference systems is fixed; items are not added or removed following user's selection.
     */
    private void addReferencingByIdentifiers(final Locale locale) {
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
        rootMenus.add(menu);
    }

    /**
     * Updates the {@link #cellIndicesMenus} list with current content of {@link #cellIndicesSystems}.
     * This method recycles existing menu items, creates new ones if needed and discards the ones that
     * are no longer in use.
     *
     * @param  systems  all CRS for grid indices to show in the "Referencing by cell indices" sub-menu.
     */
    private void updateCellIndicesMenus(final Locale locale) {
        final int n = cellIndicesSystems.size();
        for (int i=0; i<n; i++) {
            final DerivedCRS crs = cellIndicesSystems.get(i);
            final RadioMenuItem item;
            if (i < cellIndicesMenus.size()) {
                item = (RadioMenuItem) cellIndicesMenus.get(i);
            } else {
                item = new RadioMenuItem();
                item.setToggleGroup(group);
                item.setOnAction(this);
                cellIndicesMenus.add(item);
            }
            if (item.getProperties().put(REFERENCE_SYSTEM_KEY, crs) != crs) {
                item.setText(IdentifiedObjects.getDisplayName(crs, locale));
            }
        }
        for (int i = cellIndicesMenus.size(); --i >= n;) {
            final RadioMenuItem item = (RadioMenuItem) cellIndicesMenus.remove(i);
            item.setToggleGroup(null);
            item.setOnAction(null);
        }
    }

    /**
     * Must be invoked after removing a menu item for avoiding memory leak.
     */
    private static void dispose(final MenuItem item) {
        if (item != null) {
            item.setOnAction(null);
            if (item instanceof RadioMenuItem) {
                ((RadioMenuItem) item).setToggleGroup(null);
            }
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
        SeparatorMenuItem separator = null;
        final var subMenus = new ArrayList<Menu>(2);
        final var mapping  = new IdentityHashMap<Object,MenuItem>(10);
        for (final Iterator<MenuItem> it = rootMenus.iterator(); it.hasNext();) {
            final MenuItem item = it.next();
            if (item instanceof Menu) {
                subMenus.add((Menu) item);
            } else if (item instanceof SeparatorMenuItem) {
                separator = (SeparatorMenuItem) item;           // Should have only one.
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
        final int newCount = recentSystems.size();
        final var items = new ArrayList<MenuItem>(newCount + 4);
        for (Object key : recentSystems) {
            if (key == RecentReferenceSystems.OTHER) key = CHOOSER;
            items.add(mapping.remove(key));         // May be null.
        }
        dispose(mapping.remove(CHOOSER));           // Safety for avoiding AssertionError in block below.
        /*
         * Previous loop took all items that could be reused as-is. Now search for all items that are new.
         * For each new item to create, recycle an arbitrary `mapping` element (in any order) if some exist.
         * When creating new items, it may happen that one of those items represent the currently selected CRS.
         */
        ReferenceSystem selected = get();
        final Iterator<MenuItem> recycle = mapping.values().iterator();
        final Locale locale = action.owner().locale;
        for (int i=0; i<newCount; i++) {
            if (items.get(i) == null) {
                final MenuItem item;
                final ReferenceSystem system = recentSystems.get(i);
                if (system != RecentReferenceSystems.OTHER && recycle.hasNext()) {
                    item = recycle.next();
                    assert item instanceof RadioMenuItem : item;
                    item.setText(IdentifiedObjects.getDisplayName(system, locale));
                    item.getProperties().put(REFERENCE_SYSTEM_KEY, system);
                } else {
                    item = createItem(system, locale);
                }
                if (selected != null && system == selected) {
                    ((RadioMenuItem) item).setSelected(true);       // ClassCastException should never occur here.
                    selected = null;
                }
                items.set(i, item);
            }
        }
        /*
         * If there is any item left, we must remove them from the ToggleGroup for avoiding memory leak.
         * Add separator before "Other…" item. The sub-menus (if any) are appended last with no change.
         */
        while (recycle.hasNext()) {
            dispose(recycle.next());
        }
        for (int i = items.size(); --i >= 0;) {
            if (items.get(i).getClass() == MenuItem.class) {
                if (separator == null) {
                    separator = new SeparatorMenuItem();
                }
                items.add(i, separator);
                break;
            }
        }
        items.addAll(subMenus);
        GUIUtilities.copyAsDiff(items, rootMenus);
        /*
         * If we had no previously selected item, selects it now.
         */
        if (get() == null) {
            initialize();
        }
        if (cellIndicesSystems != null) {
            updateCellIndicesMenus(locale);
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
        /*
         * ClassCastException should not happen because this listener is registered only on MenuItem,
         * and REFERENCE_SYSTEM_KEY is a property which should be read and written only by SIS.
         */
        final MenuItem source = (MenuItem) event.getSource();
        final Object value = source.getProperties().get(REFERENCE_SYSTEM_KEY);
        if (value == CHOOSER) {
            action.changed(this, get(), RecentReferenceSystems.OTHER);
        } else {
            final ReferenceSystem system = (ReferenceSystem) value;
            if (cellIndicesMenus != null && cellIndicesMenus.contains(source)) {
                final ReferenceSystem old = get();
                super.set(system);                          // Set without adding to `recentSystems` list.
                action.action.changed(this, old, system);   // Skip the work done by `action.changed(…)`.
            } else {
                set(system);
            }
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
            for (final MenuItem item : rootMenus) {
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
            if (system != RecentReferenceSystems.OTHER) {
                action.changed(this, old, system);
            }
        }
    }
}
