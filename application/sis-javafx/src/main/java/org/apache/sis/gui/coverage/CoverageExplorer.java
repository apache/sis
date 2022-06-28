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

import java.util.EnumMap;
import java.util.Optional;
import java.util.Collections;
import java.awt.image.RenderedImage;
import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.scene.control.Control;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Toggle;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.event.ActionEvent;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.internal.gui.DataStoreOpener;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.ToolbarButton;
import org.apache.sis.internal.gui.NonNullObjectProperty;
import org.apache.sis.internal.gui.PrivateAccess;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.portrayal.RenderException;
import org.apache.sis.gui.referencing.RecentReferenceSystems;
import org.apache.sis.gui.dataset.WindowHandler;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.gui.Widget;


/**
 * An image or tabular view of {@link GridCoverage} together with controls for band selection and other operations.
 * The class contains two properties:
 *
 * <ul>
 *   <li>A {@link GridCoverage} supplied by user.
 *       May be specified indirectly with an {@link ImageRequest} for loading the coverage.</li>
 *   <li>A {@link View} type which specify how to show the coverage:
 *     <ul>
 *       <li>using {@link GridView} for showing numerical values in a table, or</li>
 *       <li>using {@link CoverageCanvas} for showing the coverage as an image.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * Controls are provided for allowing user to customize map projection, number formats, <i>etc.</i>.
 * The set of control depends on the view type.
 *
 * <h2>Limitations</h2>
 * Current implementation is restricted to {@link GridCoverage} instances, but a future
 * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see CoverageCanvas
 * @see GridView
 *
 * @since 1.1
 * @module
 */
@DefaultProperty("coverage")
public class CoverageExplorer extends Widget {
    /**
     * Type of view shown in the explorer.
     * It may be either an image or a table of numerical values.
     *
     * @see #viewTypeProperty
     */
    public enum View {
        // Enumeration order is the order in which buttons will appear on the button bar.

        /**
         * Shows the coverage visual as an image. This view uses {@link CoverageCanvas}.
         */
        IMAGE("\uD83D\uDDFA\uFE0F", Resources.Keys.TabularData),    // 🗺 — World map.

        /**
         * Shows the coverage numerical value in a table. This view uses {@link GridView}.
         */
        TABLE("\uD83D\uDD22\uFE0F", Resources.Keys.Visualize);      // 🔢 — Input symbol for numbers.

        /**
         * Number of enumeration values.
         */
        static final int COUNT = 2;

        /**
         * The Unicode characters to use as icon.
         */
        final String icon;

        /**
         * Key from {@link Resources} bundle for the localized text to use as tooltip.
         */
        final short tooltip;

        /**
         * Creates a new enumeration value.
         */
        private View(final String icon, short tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
        }
    }

    /**
     * The type of view (image or tabular data) shown in this explorer.
     *
     * @see #getViewType()
     * @see #setViewType(View)
     */
    public final ObjectProperty<View> viewTypeProperty;

    /**
     * The source of coverage data shown in this explorer. If this property value is non-null,
     * then {@link #coverageProperty} value will change at any time (potentially many times)
     * depending on the zoom level or other user interaction. Conversely if a value is set
     * explicitly on {@link #coverageProperty}, then this {@code resourceProperty} is cleared.
     *
     * <h4>Relationship with view properties</h4>
     * This property is "weakly bound" to {@link CoverageCanvas#resourceProperty}:
     * the two properties generally have the same value but are not necessarily updated in same time.
     * After a value is set on one property, the other property may be updated only after some background process
     * (e.g. loading) finished. If a view is not the {@linkplain #getViewType() currently visible view},
     * its property may be updated only when the view become visible.
     *
     * @see #getResource()
     * @see #setResource(GridCoverageResource)
     * @see CoverageCanvas#resourceProperty
     *
     * @since 1.2
     */
    public final ObjectProperty<GridCoverageResource> resourceProperty;

    /**
     * The data shown in this canvas. This property value may be set implicitly or explicitly:
     * <ul>
     *   <li>If the {@link #resourceProperty} value is non-null, then the value will change
     *       automatically at any time (potentially many times) depending on user interaction.</li>
     *   <li>Conversely if an explicit value is set on this property,
     *       then the {@link #resourceProperty} is cleared.</li>
     * </ul>
     *
     * Note that a change in this property value may not modify the canvas content immediately.
     * Instead, a background process will request the tiles and update the canvas content later,
     * when data are ready.
     *
     * <p>Current implementation is restricted to {@link GridCoverage} instances, but a future
     * implementation may generalize to {@link org.opengis.coverage.Coverage} instances.</p>
     *
     * <h4>Relationship with view properties</h4>
     * This property is "weakly bound" to {@link CoverageCanvas#coverageProperty}:
     * the two properties generally have the same value but are not necessarily updated in same time.
     * After a value is set on one property, the other property may be updated only after some background process
     * (e.g. loading) finished. If a view is not the {@linkplain #getViewType() currently visible view},
     * its property may be updated only when the view become visible.
     *
     * @see #getCoverage()
     * @see #setCoverage(GridCoverage)
     * @see #setCoverage(ImageRequest)
     * @see CoverageCanvas#coverageProperty
     */
    public final ObjectProperty<GridCoverage> coverageProperty;

    /**
     * Whether the {@link #coverageProperty} is in process of being set,
     * in which case some listeners should not react.
     */
    private boolean isCoverageAdjusting;

    /**
     * Handles the {@link javafx.scene.control.ChoiceBox} and menu items for selecting a CRS.
     */
    final RecentReferenceSystems referenceSystems;

    /**
     * The different views we can provide on {@link #coverageProperty}, together with associated controls.
     * Values in this map are initially null and created when first needed.
     * Concrete classes are {@link GridControls} and {@link CoverageControls}.
     *
     * @see #getDataView(View)
     * @see #getControls(View)
     */
    private final EnumMap<View,ViewAndControls> views;

    /**
     * The control that put everything together, created when first requested.
     * The type of control may change in any future SIS version.
     *
     * @see #getView()
     * @see #onViewTypeSet(View)
     */
    private SplitPane content;

    /**
     * Handler of the window showing this coverage view. This is used for creating new windows.
     * Created when first needed for giving to subclasses a chance to complete initialization.
     *
     * @see #getWindowHandler()
     */
    private WindowHandler window;

    /**
     * Creates an initially empty explorer with default view type.
     * By default {@code CoverageExplorer} will show a coverage as a table of values,
     * i.e. the default view type is {@link View#TABLE}.
     *
     * <div class="note"><b>API note:</b>
     * the reason for setting default value to tabular data is because it requires loading much less data with
     * {@link java.awt.image.RenderedImage}s supporting deferred tile loading. By contrast {@link View#IMAGE}
     * may require loading the full image.</div>
     *
     * @deprecated Use {@link #CoverageExplorer(View)}.
     */
    @Deprecated
    public CoverageExplorer() {
        this(View.TABLE);
    }

    /**
     * Creates an initially empty explorer with the specified view type.
     *
     * @param  type  the way to show coverages in this explorer.
     *
     * @see #setViewType(View)
     *
     * @since 1.2
     */
    public CoverageExplorer(final View type) {
        ArgumentChecks.ensureNonNull("type", type);
        views            = new EnumMap<>(View.class);
        viewTypeProperty = new NonNullObjectProperty<>(this, "viewType", type);
        resourceProperty = new SimpleObjectProperty<> (this, "resource");
        coverageProperty = new SimpleObjectProperty<> (this, "coverage");
        referenceSystems = new RecentReferenceSystems();
        referenceSystems.addUserPreferences();
        referenceSystems.addAlternatives("EPSG:4326", "EPSG:3395", "MGRS");     // WGS 84 / World Mercator
        viewTypeProperty.addListener((p,o,n) -> onViewTypeSet(n));
        resourceProperty.addListener((p,o,n) -> onPropertySet(n, null, coverageProperty));
        coverageProperty.addListener((p,o,n) -> onPropertySet(null, n, resourceProperty));
    }

    /**
     * Creates an explorer initialized with the same coverage or resource than the given explorer.
     *
     * @param  source  the source explorer from which to take the initial coverage or resource.
     *
     * @since 1.2
     */
    public CoverageExplorer(final CoverageExplorer source) {
        this(source.getViewType());
        window = PrivateAccess.newWindowHandler.apply(source.window, this);
        source.getImageRequest().ifPresent(this::setCoverage);
        PrivateAccess.finishWindowHandler.accept(window);
        if (getViewType() == View.IMAGE) {
            getCoverageControls().copyStyling(source.getCoverageControls());
        }
    }

    /**
     * Returns the handler of the window showing this coverage view.
     * Those windows are created when the user clicks on the "New window" button.
     * Each window provides the area where data are shown and where the user interacts.
     * The window can be a JavaFX top-level window ({@link Stage}), but not necessarily.
     * It may also be a tile in a mosaic of windows.
     *
     * @return the handler of the window showing this coverage view.
     *
     * @since 1.3
     */
    public final WindowHandler getWindowHandler() {
        assert Platform.isFxApplicationThread();
        /*
         * Created when first needed for giving to subclass constructors a chance to complete
         * their initialization before `this` reference is passed to `WindowHandler` constructor.
         */
        if (window == null) {
            window = WindowHandler.create(this);
        }
        return window;
    }

    /**
     * Returns the canvas where the image is shown.
     *
     * @return the canvas where the image is shown.
     *
     * @since 1.2
     */
    public final CoverageCanvas getCanvas() {
        return getCoverageControls().view;
    }

    /**
     * Returns the controls on the canvas where the image is shown.
     */
    private CoverageControls getCoverageControls() {
        return (CoverageControls) getViewAndControls(View.IMAGE, false);
    }

    /**
     * Returns the view-control pair for the given view type.
     * The view-control pair is created when first needed.
     * Invoking this method may cause data to be loaded in a background thread.
     *
     * @param  type  type of view to obtain.
     * @param  load  whether to force loading of data in the new type.
     */
    private ViewAndControls getViewAndControls(final View type, boolean load) {
        ViewAndControls c = views.get(type);
        if (c == null) {
            switch (type) {
                case TABLE: c = new GridControls(this); break;
                case IMAGE: c = new CoverageControls(this, getWindowHandler()); break;
                default: throw new AssertionError(type);
            }
            views.put(type, c);
            load = true;
        }
        /*
         * If this explorer is showing a coverage, load data in the newly created view.
         * Data may also be loaded because the view was previously unselected (hidden)
         * and became selected (visible).
         */
        if (load) {
            getImageRequest().ifPresent(c::load);
        }
        return c;
    }

    /**
     * Returns the region containing the grid or coverage view, band selector and any control managed by this
     * {@code CoverageExplorer}. The {@link Region} subclass returned by this method is implementation dependent
     * and may change in any future version.
     *
     * @return the region to show.
     *
     * @see #getDataView(View)
     * @see #getControls(View)
     */
    @Override
    public final Region getView() {
        assert Platform.isFxApplicationThread();
        /*
         * We build when first requested because `ResourceExplorer` for example will never request this view.
         * Instead it will invoke `getDataView(View)` or `getControls(View)` and layout those regions itself.
         */
        if (content == null) {
            /*
             * Prepare buttons to add on the toolbar. Those buttons are not managed by this class;
             * they are managed by org.apache.sis.gui.dataset.WindowHandler. We only declare here
             * the text and action for each button.
             */
            final ToggleGroup group   = new ToggleGroup();
            final Control[]   buttons = new Control[View.COUNT + 1];
            final Resources localized = Resources.forLocale(getLocale());
            buttons[0] = new Separator();
            for (final View type : View.values()) {
                buttons[1 + type.ordinal()] = new Selector(type).createButton(group, type.icon, localized, type.tooltip);
            }
            final View type = getViewType();
            final ViewAndControls c = getViewAndControls(type, false);
            group.selectToggle(group.getToggles().get(type.ordinal()));
            content = new SplitPane(c.controls(), c.viewAndNavigation);
            ToolbarButton.insert(content, buttons);
            /*
             * The divider position is supposed to be a fraction between 0 and 1. A value of 1 would mean
             * to give all the space to controls and no space to data, which is not what we want. However
             * experience with JavaFX 14 shows that this setting gives just a reasonable space to controls
             * and most space to data. I have not identified the cause of this surprising behavior.
             * A smaller value results in too few space for the controls.
             */
            content.setDividerPosition(0, 1);
        }
        return content;
    }

    /**
     * Returns the region containing the data visualization component, without controls other than navigation.
     * This is a {@link GridView} or {@link CoverageCanvas} together with their {@link StatusBar}
     * and navigation controls for selecting the slice in a <var>n</var>-dimensional data cube.
     * The {@link Region} subclass returned by this method is implementation dependent and may change
     * in any future version.
     *
     * @param  type  whether to obtain a {@link GridView} or {@link CoverageCanvas}.
     * @return the requested view for the value of {@link #resourceProperty} or {@link #coverageProperty}.
     */
    public final Region getDataView(final View type) {
        assert Platform.isFxApplicationThread();
        ArgumentChecks.ensureNonNull("type", type);
        return getViewAndControls(type, false).viewAndNavigation;
    }

    /**
     * Returns the panes containing the controls, without data visualization component.
     * The {@link TitledPane} contents are implementation dependent and may change in any future version.
     *
     * @param  type  whether to obtain controls for {@link GridView} or {@link CoverageCanvas}.
     * @return the controls on specified data view.
     */
    public final TitledPane[] getControls(final View type) {
        assert Platform.isFxApplicationThread();
        ArgumentChecks.ensureNonNull("type", type);
        return getViewAndControls(type, false).controlPanes.clone();
    }

    /**
     * The action to execute when the user selects a view.
     * This is used by the toolbar buttons in the widget created by {@link #getView()}.
     */
    private final class Selector extends ToolbarButton {
        /** The view to select when the button is pressed. */
        private final View type;

        /** Creates a new action which will show the view at the given index. */
        Selector(final View type) {
            this.type = type;
        }

        /** Invoked when the user selects another view to show (tabular data or the image). */
        @Override public void handle(final ActionEvent event) {
            final Toggle button = (Toggle) event.getSource();
            if (button.isSelected()) {
                setViewType(type);
                views.get(type).selector = button;          // Should never be null.
            } else {
                button.setSelected(true);       // Prevent situation where all buttons are unselected.
            }
        }
    }

    /**
     * Returns the type of view (image or tabular data) shown in this explorer.
     * The default value is {@link View#TABLE}.
     *
     * @return the way to show coverages in this explorer.
     *
     * @see #viewTypeProperty
     */
    public final View getViewType() {
        return viewTypeProperty.get();
    }

    /**
     * Sets the type of view to show in this explorer.
     *
     * @param  type  the new way to show coverages in this explorer.
     *
     * @see #viewTypeProperty
     */
    public final void setViewType(final View type) {
        viewTypeProperty.set(type);
    }

    /**
     * Invoked when a new view type has been specified.
     *
     * @param  type  the new way to show coverages in this explorer.
     */
    private void onViewTypeSet(final View type) {
        final ViewAndControls c = getViewAndControls(type, true);
        if (content != null) {
            content.getItems().setAll(c.controls(), c.viewAndNavigation);
            final Toggle selector = c.selector;
            if (selector != null) {
                selector.setSelected(true);
            }
        }
    }

    /**
     * Returns the source of coverages for this explorer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     *
     * @return the source of coverages shown in this explorer, or {@code null} if none.
     *
     * @see #resourceProperty
     *
     * @since 1.2
     */
    public final GridCoverageResource getResource() {
        return resourceProperty.get();
    }

    /**
     * Sets the source of coverages shown in this explorer.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and the {@link #coverageProperty}
     * value will be updated after an undetermined amount of time.
     *
     * @param  resource  the source of data to show in this explorer, or {@code null} if none.
     *
     * @see #resourceProperty
     *
     * @since 1.2
     */
    public final void setResource(final GridCoverageResource resource) {
        resourceProperty.set(resource);
        /*
         * `this.onPropertySet(…)` is indirectly invoked,
         * which in turn invokes `setCoverage(ImageRequest)`.
         */
    }

    /**
     * Returns the source of sample values for this explorer.
     * This method, like all other methods in this class, shall be invoked from the JavaFX thread.
     * Note that this value may change at any time (depending on user interaction)
     * if the {@link #resourceProperty} has a non-null value.
     *
     * @return the coverage shown in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     * @see CoverageCanvas#getCoverage()
     * @see GridView#getImage()
     */
    public final GridCoverage getCoverage() {
        return coverageProperty.get();
    }

    /**
     * Sets the coverage to show in this explorer.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The new data are loaded in a background thread and will appear after an
     * undetermined amount of time.
     *
     * <p>Invoking this method sets the {@link #resourceProperty} value to {@code null}.</p>
     *
     * @param  coverage  the data to show in this explorer, or {@code null} if none.
     *
     * @see #coverageProperty
     * @see CoverageCanvas#setCoverage(GridCoverage)
     * @see GridView#setImage(RenderedImage)
     */
    public final void setCoverage(final GridCoverage coverage) {
        coverageProperty.set(coverage);
        /*
         * `this.onPropertySet(…)` is indirectly invoked,
         * which in turn invokes `setCoverage(ImageRequest)`.
         */
    }

    /**
     * Loads coverage in a background thread from the given source.
     * This method shall be invoked from JavaFX thread and returns immediately.
     * The grid content may appear unmodified after this method returns;
     * the modifications will appear after an undetermined amount of time.
     *
     * @param  source  the coverage or resource to load, or {@code null} if none.
     *
     * @see GridView#setImage(ImageRequest)
     */
    public final void setCoverage(final ImageRequest source) {
        assert Platform.isFxApplicationThread();
        final ViewAndControls current = getViewAndControls(getViewType(), false);
        for (final ViewAndControls c : views.values()) {
            c.load(c == current ? source : null);
        }
        // `notifyDataChanged(…)` will be invoked later after background thread finishes its work.
    }

    /**
     * Invoked when a new value has been set on {@link #resourceProperty} or {@link #coverageProperty}.
     * This method sets the resource or coverage on the currently visible view, which in turn will load
     * data in its own background thread.
     *
     * @param  resource  the new resource, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     * @param  toClear   the property which is an alternative to the property that has been set.
     */
    private void onPropertySet(final GridCoverageResource resource, final GridCoverage coverage,
                               final ObjectProperty<?> toClear)
    {
        if (!isCoverageAdjusting) {
            isCoverageAdjusting = true;
            try {
                toClear.set(null);
            } finally {
                isCoverageAdjusting = false;
            }
            // Indirectly start a background thread which will invoke `notifyDataChanged(…)` later.
            setCoverage((resource != null || coverage != null) ? new ImageRequest(resource, coverage) : null);
        }
    }

    /**
     * Invoked in JavaFX thread after the current view finished to load the new coverage.
     * It is the responsibility of all {@link ViewAndControls} subclasses to listen to change events
     * emitted by their views ({@link GridView} or {@link CoverageCanvas}) and to invoke this method.
     * This method will then update the properties of this {@code CoverageExplorer} for the new data.
     *
     * <p>Note that view data may have been changed either by user changing directly a {@link GridView}
     * or {@link CoverageCanvas} property, or indirectly by user changing {@link #resourceProperty} or
     * {@link #coverageProperty}. In the latter case, the {@code resource} and {@code coverage} arguments
     * given to this method may be the value that the properties already have.</p>
     *
     * @param  resource  the new source of coverage, or {@code null} if none.
     * @param  coverage  the new coverage, or {@code null} if none.
     */
    final void notifyDataChanged(final GridCoverageResource resource, final GridCoverage coverage) {
        if (coverage != null) {
            String name;
            try {
                name = DataStoreOpener.findLabel(resource, getLocale(), true);
            } catch (DataStoreException e) {
                name = e.getLocalizedMessage();
                if (name == null) {
                    name = e.getClass().getSimpleName();
                }
            }
            referenceSystems.setGridReferencing(true,
                    Collections.singletonMap(name, coverage.getGridGeometry()));
        }
        /*
         * Following calls will NOT forward the new values to the views because this `notifyDataChanged(…)`
         * method is invoked as a consequence of view properties being updated. Those views should already
         * have the new property values at this moment.
         */
        isCoverageAdjusting = true;
        try {
            setResource(resource);
            setCoverage(coverage);
        } finally {
            isCoverageAdjusting = false;
        }
    }

    /**
     * Returns a request which represent the coverage or resource currently shown in this explorer.
     * This request can be used for showing the same data in another {@code CoverageExplorer} instance
     * by invoking the {@link #setCoverage(ImageRequest)} method.
     *
     * @return the request to give to another explorer for showing the same coverage.
     *
     * @see #setCoverage(ImageRequest)
     */
    private Optional<ImageRequest> getImageRequest() {
        final GridCoverageResource resource = getResource();
        final GridCoverage coverage = getCoverage();
        if (resource != null || coverage != null) {
            final ImageRequest request = new ImageRequest(resource, coverage);
            final CoverageControls c = (CoverageControls) views.get(View.IMAGE);
            if (c != null) try {
                request.zoom = c.view.getGridGeometry();
            } catch (RenderException e) {
                CoverageCanvas.unexpectedException("getGridGeometry", e);
            }
            return Optional.of(request);
        } else {
            return Optional.empty();
        }
    }
}
