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

import java.util.Locale;
import java.util.logging.Logger;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.layout.Region;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.gui.coverage.CoverageExplorer;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.PrivateAccess;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A separated window for visualizing a resource managed by {@link ResourceExplorer}.
 * A window provides the area where the data are shown and where the user interacts.
 * The window can be a JavaFX top-level window ({@link Stage}), but not necessarily.
 * It may also be a tile in a mosaic of windows.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public abstract class WindowHandler {
    /**
     * The window manager which contains this handler.
     * The manager contains the list of all windows created for the same widget.
     */
    public final WindowManager manager;

    /**
     * The window where the resource is visualized. This is created when first needed.
     * We assume that resource views do not change their window during their lifetime.
     *
     * @see #show()
     */
    private Stage window;

    /**
     * The property for a label that identify the view. If the resource is shown
     * in a top-level window, then this is typically the title of that window.
     */
    public final StringProperty title;

    /**
     * The listener to add and remove to/from the {@link #title} property. We use a static reference for avoiding
     * to retain a direct reference to {@link #window} in listeners, which would increase the risk of memory leak.
     */
    private static final ChangeListener<String> TITLE_CHANGED = (p,o,n) -> {
        final WindowHandler handler = (WindowHandler) ((StringProperty) p).getBean();
        handler.window.setTitle(n + " — Apache SIS");
    };

    /**
     * Creates a new handler for a window showing a resource.
     * Exactly one of {@code creator} or {@code locale} arguments should be non-null.
     *
     * @param creator  the handler which is duplicated, or {@code null} if none.
     * @param locale   language of texts. Used only if {@code creator} is null.
     */
    WindowHandler(final WindowHandler creator, final Locale locale) {
        manager = (creator != null) ? creator.manager : new WindowManager(this, locale);
        title = new SimpleStringProperty(this, "title");
    }

    /**
     * Methods to be invoked last by constructors, after everything else succeeded.
     * Construction must be completed before to invoke this method because this call will notify listeners.
     *
     * @return {@code this} for method call chaining.
     */
    final WindowHandler finish() {
        String text;
        if (manager.main == this) {
            text = Resources.forLocale(manager.locale).getString(Resources.Keys.MainWindow);
        } else try {
            text = ResourceTree.findLabel(getResource(), manager.locale, true);
        } catch (DataStoreException | RuntimeException e) {
            text = Vocabulary.getResources(manager.locale).getString(Vocabulary.Keys.Unknown);
            Logging.recoverableException(Logger.getLogger(Modules.APPLICATION), WindowHandler.class, "<init>", e);
        }
        title.set(text);
        manager.modifiableWindowList.add(this);
        return this;
    }

    /**
     * Creates a new handler for the window which is showing the given coverage viewer.
     *
     * @param  widget the widget for which to create a handler.
     * @return a handler for the window of the given widget.
     */
    public static WindowHandler create(final CoverageExplorer widget) {
        ArgumentChecks.ensureNonNull("widget", widget);
        return new ForCoverage(null, widget.getLocale(), widget).finish();
    }

    /**
     * Creates a new handler for the window which is showing the given table of features.
     *
     * @param  widget the widget for which to create a handler.
     * @return a handler for the window of the given widget.
     */
    public static WindowHandler create(final FeatureTable widget) {
        ArgumentChecks.ensureNonNull("widget", widget);
        return new ForFeatures(null, widget.textLocale, widget).finish();
    }

    /**
     * Prepares a new window with the same content than the window managed by this handler.
     * This method can be used for creating many windows over the same data.
     * Each window can do pans, zooms and rotations independently of other windows,
     * or be synchronized with other windows, at user's choice.
     *
     * <p>The new view is added to the {@link WindowManager#windows} list and will be removed
     * from that list if the window is closed. If the resource is closed in the window manager,
     * then all windows showing that resource will be closed.</p>
     *
     * <p>The new window is not initially visible.
     * To show the window, invoke {@link #show()} on the returned handler.</p>
     *
     * @return information about the new window.
     */
    public abstract WindowHandler duplicate();

    /**
     * Returns the JavaFX region where the resource is shown. This value shall be stable.
     */
    abstract Region getView();

    /**
     * Returns the window which is showing the resource.
     * This is used for fetching the main window.
     */
    Window getWindow() {
        return GUIUtilities.getWindow(getView());
    }

    /**
     * Shows the window and brings it to the front.
     * For handlers created by a {@code create(…)} method, this {@code show()} method can be invoked at any time.
     * For handlers created by {@link #duplicate()}, this {@code show()} method can be invoked as long as the window
     * has not been closed. After a duplicated window has been closed, it is not possible to show it again.
     *
     * @throws IllegalStateException if this handler is a {@linkplain #duplicate() duplicate}
     *         and the window has been closed.
     */
    public void show() {
        if (window == null) {
            if (manager.main == this) {
                Window w = getWindow();
                if (w instanceof Stage) {
                    window = (Stage) w;
                } else {
                    return;
                }
            } else {
                if (getResource() == null) {
                    throw new IllegalStateException(Errors.format(Errors.Keys.DisposedInstanceOf_1, getClass()));
                }
                window = manager.newWindow(getView());
                window.setOnHidden((e) -> dispose());
                title.addListener(TITLE_CHANGED);
                TITLE_CHANGED.changed(title, null, title.get());
            }
        }
        window.show();
        window.toFront();
    }

    /**
     * The resource shown in the {@linkplain #window}, or {@code null} if unspecified.
     * This is used for identifying which handlers to remove when a resource is closed.
     * This method is not yet public because a future version may need to return a full
     * map context instead of a single resource.
     */
    abstract Resource getResource();

    /**
     * Invoked when the window is hidden. After removing this handler from the windows list,
     * this method makes a "best effort" for helping the garbage-collector to release memory.
     */
    void dispose() {
        assert manager.main != this;                // Because listener is not registered for main window.
        manager.modifiableWindowList.remove(this);
        title.removeListener(TITLE_CHANGED);
        if (window != null) {
            window.setScene(null);
            window = null;
        }
    }



    /**
     * A visualization managed by a {@link CoverageExplorer} instance.
     * The initial {@code CoverageExplorer} instance (before duplication)
     * is itself produced by a {@link ResourceExplorer}.
     */
    private static final class ForCoverage extends WindowHandler {
        /**
         * The widget providing the view.
         */
        private final CoverageExplorer widget;

        /**
         * Creates a new handler for a window showing a resource.
         *
         * @param creator  the handler which is duplicated, or {@code null} if none.
         * @param locale   language of texts. Used only if {@code creator} is null.
         * @param widget   the widget providing the view of the resource.
         */
        ForCoverage(final WindowHandler creator, final Locale locale, final CoverageExplorer widget) {
            super(creator, locale);
            this.widget = widget;
        }

        /**
         * Returns the JavaFX region where the resource is shown.
         */
        @Override
        Region getView() {
            return widget.getView();
        }

        /**
         * Returns the window which is showing the resource. We avoid the call to {@link #getView()}
         * because in the particular case of {@link CoverageExplorer}, it causes the initialization
         * of a splitted pane which is not the one used by the main window.
         */
        @Override
        Window getWindow() {
            return GUIUtilities.getWindow(widget.getDataView(widget.getViewType()));
        }

        /**
         * Prepares (without showing) a new window with the same content than the window managed by this handler.
         */
        @Override
        public WindowHandler duplicate() {
            final CoverageExplorer explorer = new CoverageExplorer(widget.getViewType());
            final ForCoverage handler = new ForCoverage(this, null, explorer);
            PrivateAccess.initWindowHandler.accept(explorer, handler);
            widget.getImageRequest().ifPresent(explorer::setCoverage);
            return handler.finish();
        }

        /**
         * The resource shown in the {@linkplain #window window}, or {@code null} if unspecified.
         */
        @Override
        Resource getResource() {
            return widget.getResource();
        }

        /**
         * Makes a "best effort" for helping the garbage-collector to release resources.
         */
        @Override
        void dispose() {
            super.dispose();
            widget.setResource(null);
        }
    }




    /**
     * A visualization managed by a {@link FeatureTable} instance.
     * The initial {@code FeatureTable} instance (before duplication)
     * is itself produced by a {@link ResourceExplorer}.
     */
    private static final class ForFeatures extends WindowHandler {
        /**
         * The widget providing the view.
         */
        private final FeatureTable widget;

        /**
         * Creates a new handler for a window showing a resource.
         *
         * @param creator  the handler which is duplicated, or {@code null} if none.
         * @param locale   language of texts. Used only if {@code creator} is null.
         * @param widget   the widget providing the view of the resource.
         */
        ForFeatures(final WindowHandler creator, final Locale locale, final FeatureTable widget) {
            super(creator, locale);
            this.widget = widget;
        }

        /**
         * Returns the JavaFX region where the resource is shown.
         */
        @Override
        Region getView() {
            return widget;
        }

        /**
         * Prepares (without showing) a new window with the same content than the window managed by this handler.
         */
        @Override
        public WindowHandler duplicate() {
            return new ForFeatures(this, null, new FeatureTable(widget)).finish();
        }

        /**
         * The resource shown in the {@linkplain #window window}, or {@code null} if unspecified.
         */
        @Override
        Resource getResource() {
            return widget.getFeatures();
        }

        /**
         * Makes a "best effort" for helping the garbage-collector to release resources.
         */
        @Override
        void dispose() {
            super.dispose();
            widget.setFeatures(null);
        }
    }
}
