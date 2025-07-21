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

import java.util.List;
import java.util.HashSet;
import java.util.BitSet;
import java.util.Locale;
import java.text.NumberFormat;
import javafx.concurrent.Task;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import org.apache.sis.gui.coverage.CoverageCanvas;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Provider of textual content to show in {@link StatusBar} for {@link GridCoverage} values under cursor position.
 * This object can be registered as a listener of e.g. {@link CoverageCanvas#coverageProperty} for updating the
 * values to show when the coverage is changed.
 *
 * <h2>Multi-threading</h2>
 * This class fetches values and formats them in a background thread because calls
 * to {@link GridCoverage#render(GridExtent)} can take an arbitrary amount of time.
 * The {@link ValuesFormatter#buffer} is used as a synchronization lock.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ValuesFromCoverage extends ValuesUnderCursor implements ChangeListener<GridCoverage> {
    /**
     * The task to execute in a background thread for fetching values from the coverage and formatting them.
     * This is {@code null} if there is no coverage. A new instance shall be created when the coverage changed.
     */
    private ValuesFormatter formatter;

    /**
     * The selection status of each band.
     */
    private final BitSet selectedBands;

    /**
     * {@code true} if {@link ValuesFormatter#setSelectedBands(BitSet, String[], HashSet)}
     * needs to be invoked again.
     */
    private boolean needsBandRefresh;

    /**
     * {@code true} if {@link ValuesFormatter#setSelectedBands(BitSet, String[], HashSet)}
     * is under execution in a background thread.
     */
    private boolean refreshing;

    /**
     * Creates a new provider of textual values for a {@link GridCoverage}.
     */
    public ValuesFromCoverage() {
        selectedBands = new BitSet();
        valueChoices.setText(Vocabulary.format(Vocabulary.Keys.SampleDimensions));
    }

    /**
     * Resets this {@code ValuesFromCoverage} to its initial state.
     * This is invoked when there is no coverage to show, or in case of failure.
     */
    private void clear() {
        formatter = null;
        refreshing = false;
        needsBandRefresh = false;
        selectedBands.clear();
        valueChoices.getItems().clear();
    }

    /**
     * Sets the slice in grid coverages where sample values should be evaluated for next positions.
     * This method is invoked when {@link CoverageCanvas#sliceExtentProperty} changed its value.
     */
    final void setSlice(final GridExtent extent) {
        if (formatter != null) {
            formatter.setSlice(extent);
        }
    }

    /**
     * Returns the slice extent specified in the canvas which contains the given property.
     * This is a workaround for the fact that the selected slice is not an information
     * provided directly by the {@link GridCoùverage} values.
     */
    private static GridExtent getSelectedSlice(final ObservableValue<?> property) {
        if (property instanceof ReadOnlyProperty<?>) {
            final Object bean = ((ReadOnlyProperty<?>) property).getBean();
            if (bean instanceof CoverageCanvas) {
                return ((CoverageCanvas) bean).getSliceExtent();
            }
        }
        return null;
    }

    /**
     * Notifies this {@code ValuesUnderCursor} object that it needs to display values for a new coverage.
     * The {@code previous} argument should be the argument given in the last call to this method and is
     * used as an optimization hint. In case of doubt, it can be {@code null}.
     *
     * @param  property  the property which has been updated, or {@code null} if unknown.
     * @param  previous  previous property value, of {@code null} if none or unknown.
     * @param  coverage  new coverage for which to show sample values, or {@code null} if none.
     */
    @Override
    public void changed(final ObservableValue<? extends GridCoverage> property,
                        final GridCoverage previous, final GridCoverage coverage)
    {
        if (coverage == null) {
            clear();
            return;
        }
        final GridExtent slice = getSelectedSlice(property);        // Need to be invoked in JavaFX thread.
        final Locale locale = GUIUtilities.getLocale(property);
        BackgroundThreads.execute(new Task<ValuesFormatter>() {
            /**
             * The formatter from which to inherit configuration if the sample dimensions did not changed.
             * The initial {@link #formatter} value needs to be assigned from JavaFX thread.
             */
            private ValuesFormatter inherit = formatter;

            /**
             * Sample dimensions of the coverage, fetched in background thread in case it is costly to compute.
             */
            private List<SampleDimension> bands;

            /**
             * Invoked in a background thread for reconfiguring {@link ValuesFromCoverage}.
             * This method creates a new formatter with new {@link NumberFormat} instances.
             * If successful, the JavaFX components are updated by {@link #succeeded()}.
             */
            @Override protected ValuesFormatter call() {
                bands = coverage.getSampleDimensions();
                if (!(previous != null && bands.equals(previous.getSampleDimensions()))) {
                    inherit = null;
                }
                return new ValuesFormatter(ValuesFromCoverage.this, inherit, coverage, slice, bands, locale);
            }

            /**
             * Invoked in JavaFX thread after successful configuration by background thread.
             * The formatter created in background thread is assigned to {@link #formatter},
             * then the new menu items are created (unless they did not changed).
             */
            @Override protected void succeeded() {
                formatter = getValue();
                if (inherit == null) {
                    /*
                     * Only the first band is initially selected, unless the image has only 2 or 3 bands
                     * in which case all bands are selected. An image with two bands is often giving the
                     * (u,v) components of velocity vectors, which we want to keep together by default.
                     */
                    final int numBands = bands.size();
                    final var menuItems = new CheckMenuItem[numBands];
                    final BitSet selection = selectedBands;
                    selection.clear();
                    selection.set(0, (numBands <= 3) ? numBands : 1, true);
                    for (int b=0; b<numBands; b++) {
                        menuItems[b] = createMenuItem(b, bands.get(b), locale);
                    }
                    valueChoices.getItems().setAll(menuItems);
                    needsBandRefresh = true;
                }
            }

            /**
             * Invoked in JavaFX thread if an error occurred while initializing the formatter.
             */
            @Override protected void failed() {
                clear();
                setError(getException());
            }
        });
    }

    /**
     * Creates a new menu item for the given sample dimension.
     * This method shall be invoked from JavaFX thread.
     *
     * @param  index   index of the sample dimension.
     * @param  sd      the sample dimension for which to create a menu item.
     * @param  locale  the locale to use for fetching the sample dimension name.
     */
    private CheckMenuItem createMenuItem(final int index, final SampleDimension sd, final Locale locale) {
        final CheckMenuItem item = new CheckMenuItem(sd.getName().toInternationalString().toString(locale));
        item.setSelected(selectedBands.get(index));
        item.selectedProperty().addListener((p,o,n) -> {
            selectedBands.set(index, n);
            needsBandRefresh = true;
        });
        return item;
    }

    /**
     * Returns the task for fetching and formatting values in a background thread, or {@code null} if none.
     * The formatter is created in a background thread as soon as the {@link MapCanvas} data are known.
     * This method is invoked in JavaFX thread and may return {@code null} if the formatter is still
     * under construction.
     */
    @Override
    protected Formatter formatter() {
        if (refreshing) {
            return null;
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ValuesFormatter formatter = this.formatter;
        if (formatter != null && needsBandRefresh && usePrototype()) {
            final ObservableList<MenuItem> menus = valueChoices.getItems();
            final var labels = new String[menus.size()];
            for (int i=0; i<labels.length; i++) {
                labels[i] = menus.get(i).getText();
            }
            final var others = new HashSet<String>();
            final var selection = (BitSet) selectedBands.clone();
            BackgroundThreads.execute(new Task<String>() {
                /** Invoked in background thread for configuring the formatter. */
                @Override protected String call() {
                    return formatter.setSelectedBands(selection, labels, others);
                }

                /** Invoked in JavaFX thread if the configuration succeeded. */
                @Override protected void succeeded() {
                    needsBandRefresh = !prototype(getValue(), others);
                    refreshing = false;
                }

                /** Invoked in JavaFX thread if the configuration failed. */
                @Override protected void failed() {
                    clear();
                    setError(getException());
                }
            });
            refreshing = true;
        }
        return formatter;
    }
}
