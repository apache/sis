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
package org.apache.sis.gui.dataset;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.gui.Widget;


/**
 * Manages the list of opened {@link DataWindow}s.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
abstract class WindowManager extends Widget {
    /**
     * The contextual menu items for creating a new window showing selected data.
     * All menus in this list will be enabled or disabled depending on whether there is data to show.
     */
    private final List<MenuItem> newWindowMenus;

    /**
     * The menu items for navigating to different windows. {@link WindowManager} will automatically
     * add or remove elements in that list when new windows are created or closed.
     *
     * @see #setWindowsItems(ObservableList)
     */
    private ObservableList<MenuItem> showWindowMenus;

    /**
     * A property telling whether at least one data window created by this class is still visible.
     *
     * @see #createNewWindowMenu()
     * @see #setWindowsItems(ObservableList)
     */
    public final ReadOnlyBooleanProperty hasWindowsProperty;

    /**
     * The {@link WindowManager#hasWindowsProperty} property implementation.
     */
    private final class WindowsProperty extends ReadOnlyBooleanPropertyBase {
        /** The property value. */
        private boolean hasWindows;

        /** Sets this property to the given value. */
        final void set(final boolean value) {
            hasWindows = value;
            fireValueChangedEvent();
        }

        /** Returns the current property value. */
        @Override public boolean get()    {return hasWindows;}
        @Override public Object getBean() {return WindowManager.this;}
        @Override public String getName() {return "hasWindows";}
    }

    /**
     * Creates a new manager of windows.
     */
    WindowManager() {
        newWindowMenus     = new ArrayList<>(3);
        hasWindowsProperty = new WindowsProperty();
    }

    /**
     * Returns resources for current locale. We could fetch this information ourselves,
     * but we currently ask to subclass because it has this information anyway.
     */
    abstract Resources localized();

    /**
     * Returns the locale for controls and messages.
     */
    @Override
    public final Locale getLocale() {
        return localized().getLocale();
    }

    /**
     * Creates a menu item for creating new windows for the currently selected resource.
     * The new menu item is initially disabled. Its will become enabled automatically when
     * a resource is selected.
     *
     * <p>Note: current implementation keeps a strong reference to created menu.
     * Use this method only for menus that are expected to exist for application lifetime.</p>
     *
     * @return a "new window" menu item.
     *
     * @see #hasWindowsProperty
     */
    public final MenuItem createNewWindowMenu() {
        final MenuItem menu = localized().menu(Resources.Keys.NewWindow, this::newDataWindow);
        menu.setDisable(true);
        newWindowMenus.add(menu);
        return menu;
    }

    /**
     * Sets the list where to add or remove the name of data windows. New data windows are created when user
     * selects a menu item given by {@link #createNewWindowMenu()}. {@code WindowManager} will automatically
     * add or remove elements in the given list. The position of the new menu item will be just before the
     * last {@link SeparatorMenuItem} instance. If no {@code SeparatorMenuItem} is found, then one will be
     * inserted at the beginning of the given list when needed.
     *
     * @param  items  the list where to add and remove the name of windows.
     *
     * @see #hasWindowsProperty
     * @see #createNewWindowMenu()
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setWindowsItems(final ObservableList<MenuItem> items) {
        showWindowMenus = items;
    }

    /**
     * Enables or disables all "new window" menus. Those menu should be disabled when the current selection
     * does not contain any data we can show.
     */
    final void setNewWindowDisabled(final boolean disabled) {
        for (final MenuItem m : newWindowMenus) {
            m.setDisable(disabled);
        }
    }

    /**
     * Returns the set of currently selected data, or {@code null} if none.
     */
    abstract SelectedData getSelectedData();

    /**
     * Invoked when user asked to show the data in a new window. This method may be invoked from various sources:
     * contextual menu on the tab, contextual menu in the explorer tree, or from the "new window" menu item.
     *
     * @param  event  ignored (can be {@code null}).
     */
    private void newDataWindow(final ActionEvent event) {
        final SelectedData selection = getSelectedData();
        if (selection != null) {
            final DataWindow window = new DataWindow(event, (Stage) getView().getScene().getWindow(), selection);
            window.setTitle(selection.title + " — Apache SIS");
            window.show();
            if (showWindowMenus != null) {
                /*
                 * Search for insertion point just before the menu separator.
                 * If no menu separator is found, add one.
                 */
                int insertAt = showWindowMenus.size();
                do if (--insertAt < 0) {
                    showWindowMenus.add(insertAt = 0, new SeparatorMenuItem());
                    ((WindowsProperty) hasWindowsProperty).set(true);
                    break;
                } while (!(showWindowMenus.get(insertAt) instanceof SeparatorMenuItem));
                final MenuItem menu = new MenuItem(selection.title);
                menu.setOnAction((e) -> window.toFront());
                showWindowMenus.add(insertAt, menu);
                window.setOnHidden((e) -> removeDataWindow(menu));
            }
        }
    }

    /**
     * Invoked when a window has been hidden. This method removes the window title from the "Windows" menu.
     * The hidden window will be garbage collected at some later time.
     */
    private void removeDataWindow(final MenuItem menu) {
        final ObservableList<MenuItem> items = showWindowMenus;
        if (items != null) {
            for (int i = items.size(); --i >= 0;) {
                if (items.get(i) == menu) {
                    items.remove(i);
                    if (i == 0) {
                        if (!items.isEmpty()) {
                            if (items.get(0) instanceof SeparatorMenuItem) {
                                items.remove(0);
                            } else {
                                break;      // Some other windows are still present.
                            }
                        }
                        ((WindowsProperty) hasWindowsProperty).set(false);
                    }
                    break;
                }
            }
        }
    }
}
