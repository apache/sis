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
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.logging.Logging;


/**
 * A view of {@link FeatureSet} data organized as a table. The features are specified by a call
 * to {@link #setFeatures(FeatureSet)}, which will load the features in a background thread.
 * At first only a limited amount of features are loaded.
 * More features will be loaded only when the user scroll down.
 *
 * <p>If this view is removed from scene graph, then {@link #interrupt()} should be called
 * for stopping any loading process that may be under progress.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class FeatureTable extends TableView<Feature> {
    /**
     * The locale to use for texts.
     */
    final Locale textLocale;

    /**
     * The locale to use for dates/numbers.
     * This is often the same than {@link #textLocale}.
     */
    private final Locale dataLocale;

    /**
     * The type of features, or {@code null} if not yet determined.
     * This type determines the columns that will be shown.
     *
     * @see #setFeatureType(FeatureType)
     */
    private FeatureType featureType;

    /**
     * Creates an initially empty table.
     */
    public FeatureTable() {
        textLocale = Locale.getDefault(Locale.Category.DISPLAY);
        dataLocale = Locale.getDefault(Locale.Category.FORMAT);
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setTableMenuButtonVisible(true);
        setItems(new FeatureList());
    }

    /**
     * Sets the features to show in this table. This method loads an arbitrary amount of
     * features in a background thread. It does not load all features if the feature set
     * is large, unless the user scroll down.
     *
     * <p>If the loading of another {@code FeatureSet} was in progress at the time this method is invoked,
     * then that previous loading process is cancelled.</p>
     *
     * <p><b>Note:</b> the table content may appear unmodified after this method returns.
     * The modifications will appear at an undetermined amount of time later.</p>
     *
     * @param  features  the features to show in this table, or {@code null} if none.
     */
    public void setFeatures(final FeatureSet features) {
        final FeatureList items = (FeatureList) getItems();
        if (!items.setFeatures(this, features)) {
            featureType = null;
            items.clear();
            getColumns().clear();
        }
    }

    /**
     * Invoked in JavaFX thread after the feature type has been determined.
     * This method clears all rows and replaces all columns by new columns
     * determined from the given type.
     */
    final void setFeatureType(final FeatureType type) {
        getItems().clear();
        if (type != null && !type.equals(featureType)) {
            final Collection<? extends PropertyType> properties = type.getProperties(true);
            final List<TableColumn<Feature,?>> columns = new ArrayList<>(properties.size());
            for (final PropertyType pt : properties) {
                final String name = pt.getName().toString();
                String title = string(pt.getDesignation());
                if (title == null) {
                    title = string(pt.getName().toInternationalString());
                    if (title == null) title = name;
                }
                final TableColumn<Feature, Object> column = new TableColumn<>(title);
                column.setCellValueFactory(new ValueGetter(name));
                columns.add(column);
            }
            getColumns().setAll(columns);       // Change columns in an all or nothing operation.
        }
        featureType = type;
    }

    /**
     * Fetch values to show in the table cells.
     */
    private static final class ValueGetter implements Callback<TableColumn.CellDataFeatures<Feature,Object>, ObservableValue<Object>> {
        /**
         * The name of the feature property for which to fetch values.
         */
        final String name;

        /**
         * Creates a new getter of property values.
         *
         * @param  name  name of the feature property for which to fetch values.
         */
        ValueGetter(final String name) {
            this.name = name;
        }

        /**
         * Returns the value of the feature property wrapped by the given argument.
         * This method is invoked by JavaFX when a new cell needs to be rendered.
         */
        @Override
        public ObservableValue<Object> call(final TableColumn.CellDataFeatures<Feature, Object> cell) {
            Object value = null;
            final Feature feature = cell.getValue();
            if (feature != null) {
                value = feature.getPropertyValue(name);
                if (value instanceof Collection<?>) {
                    value = "collection";               // TODO
                }
            }
            return new ReadOnlyObjectWrapper<>(value);
        }
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private String string(final InternationalString i18n) {
        return (i18n != null) ? Strings.trimOrNull(i18n.toString(textLocale)) : null;
    }

    /**
     * If a loading process was under way, interrupts it and close the feature stream.
     * This method returns immediately; the release of resources happens in a background thread.
     */
    public void interrupt() {
        ((FeatureList) getItems()).interrupt();
    }

    /**
     * Reports an exception that we can not display in this widget, for example because it applies
     * to different data than the one currently viewed. The {@code method} argument should be the
     * public API (if possible) invoking the method where the exception is caught.
     */
    static void unexpectedException(final String method, final Throwable exception) {
        Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), FeatureTable.class, method, exception);
    }

    /**
     * Reports an exception that we choose to ignore.
     */
    static void recoverableException(final String method, final Exception exception) {
        Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), FeatureTable.class, method, exception);
    }
}
