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
import java.util.Iterator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.util.Callback;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.gui.internal.IdentityValueFactory;
import org.apache.sis.gui.internal.ExceptionReporter;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;


/**
 * A view of {@link FeatureSet} data organized as a table. The features are specified by a call
 * to {@link #setFeatures(FeatureSet)}, which will load the features in a background thread.
 * At first only a limited number of features are loaded.
 * More features will be loaded only when the user scroll down.
 *
 * <p>If this view is removed from scene graph, then {@link #interrupt()} should be called
 * for stopping any loading process that may be under progress.</p>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The {@link #itemsProperty() itemsProperty} should be considered read-only,
 *       and {@link #setItems(ObservableList)} should never be invoked explicitly.
 *       For changing content, use the {@link #featuresProperty} instead.</li>
 *   <li>The list returned by {@link #getItems()} should be considered read-only.</li>
 * </ul>
 *
 * @todo This class does not yet handle {@link FeatureAssociationRole}. We could handle them with
 *       {@link javafx.scene.control.SplitPane} with the main feature table in the upper part and
 *       the feature table of selected cell in the bottom part. Bottom part could put tables in a
 *       {@link javafx.scene.control.Accordion} since there is possibly different tables to show
 *       depending on the column of selected cell.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
@DefaultProperty("features")
public class FeatureTable extends TableView<Feature> {
    /**
     * The locale to use for texts. This is usually {@link Locale#getDefault()}.
     * This value is given to {@link InternationalString#toString(Locale)} calls.
     */
    final Locale textLocale;

    /**
     * The type of features, or {@code null} if not yet determined.
     * This type determines the columns that will be shown.
     *
     * @see #setFeatureType(FeatureType)
     */
    private FeatureType featureType;

    /**
     * The data shown in this table. Note that setting this property to a non-null value
     * does not modify the table content immediately. Instead, a background process will
     * load the feature instances.
     *
     * @see #getFeatures()
     * @see #setFeatures(FeatureSet)
     */
    @SuppressWarnings("this-escape")
    public final ObjectProperty<FeatureSet> featuresProperty = new SimpleObjectProperty<>(this, "features");

    /**
     * Whether the {@link #getItems()} list may be shared by another {@link FeatureTable} instance.
     * In such case, {@link #setFeatureType(FeatureType)} should create a new list instead of invoking
     * {@link FeatureList#clear()} on the existing list.
     */
    private boolean isSharingList;

    /**
     * Creates an initially empty table.
     */
    @SuppressWarnings("this-escape")    // `this` appears in a cyclic graph.
    public FeatureTable() {
        super(new FeatureList());
        textLocale = Locale.getDefault(Locale.Category.DISPLAY);
        initialize();
    }

    /**
     * Creates a new table initialized to the same data as the specified table.
     * The two tables will share the same list as long as they are viewing the same data source:
     * as the data loading progresses, new features will appear in both tables.
     *
     * @param  other  the other table from which to get the feature list.
     */
    @SuppressWarnings("this-escape")                // `this` appears in a cyclic graph.
    public FeatureTable(final FeatureTable other) {
        super(other.getFeatureList());
        other.isSharingList = true;
        isSharingList = true;
        textLocale    = other.textLocale;
        featureType   = other.featureType;
        setFeatures(other.getFeatures());           // Shall be invoked before to install the listener.
        initialize();                               // Install listener.
        if (featureType != null) {
            createColumns();
        } else if (getFeatures() != null) {
            /*
             * It may not be possible to create the columns immediately because the table is still loading
             * in a background thread. In such case, we will create the columns later when the feature type
             * will become known (which we identify by the other feature table updating its own columns).
             */
            other.getColumns().addListener(new InvalidationListener() {
                @Override public void invalidated(final Observable list) {
                    list.removeListener(this);                              // This event is needed only once.
                    if (other.getFeatures() == getFeatures()) {
                        featureType = other.featureType;
                        if (featureType != null) {
                            createColumns();
                        }
                    }
                }
            });
        }
    }

    /**
     * Completes the initialization of this {@link FeatureTable}.
     * This is common code shared by both constructors.
     */
    private void initialize() {
        featuresProperty.addListener((p,o,n) -> startFeaturesLoading(n));
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setTableMenuButtonVisible(true);
    }

    /**
     * Returns the list where to add features.
     * All methods on the returned list shall be invoked from JavaFX thread.
     */
    final FeatureList getFeatureList() {
        final ObservableList<Feature> items = getItems();
        if (items instanceof FeatureList) {
            return (FeatureList) items;
        } else {
            return (FeatureList) ((ExpandableList) items).getSource();
        }
    }

    /**
     * Returns the list where to add features when some properties may be multi-valued.
     * This method wraps the {@link FeatureList} into an {@link ExpandableList} if needed.
     */
    private ExpandableList getExpandableList() {
        final ObservableList<Feature> items = getItems();
        if (items instanceof ExpandableList) {
            return (ExpandableList) items;
        } else {
            return new ExpandableList((FeatureList) items);
        }
    }

    /**
     * Returns the source of features for this table.
     *
     * @return the features shown in this table, or {@code null} if none.
     *
     * @see #featuresProperty
     */
    public final FeatureSet getFeatures() {
        return featuresProperty.get();
    }

    /**
     * Sets the features to show in this table. This method loads an arbitrary number of
     * features in a background thread. It does not load all features if the feature set
     * is large, unless the user scroll down.
     *
     * <p>If the loading of another {@code FeatureSet} was in progress at the time this
     * method is invoked, then that previous loading process is cancelled.</p>
     *
     * <p><b>Note:</b> the table content may appear unmodified after this method returns.
     * The modifications will appear at an undetermined amount of time later.</p>
     *
     * @param  features  the features to show in this table, or {@code null} if none.
     *
     * @see #featuresProperty
     */
    public final void setFeatures(final FeatureSet features) {
        featuresProperty.set(features);
    }

    /**
     * Invoked (indirectly) when the user sets a new {@link FeatureSet}.
     * See {@link #setFeatures(FeatureSet)} for method description.
     */
    private void startFeaturesLoading(final FeatureSet features) {
        final FeatureList items;
        if (isSharingList) {
            items = new FeatureList();
            isSharingList = false;
            setItems(items);
        } else {
            items = getFeatureList();
            items.interrupt();
        }
        if (!items.startFeaturesLoading(this, features)) {
            featureType = null;
            getColumns().clear();
            setPlaceholder(null);
        }
    }

    /**
     * Invoked in JavaFX thread after the feature type has been determined.
     * This method clears all rows and replaces all columns by new columns
     * determined from the given type.
     */
    final void setFeatureType(final FeatureType type) {
        setPlaceholder(null);
        getItems().clear();
        final boolean update = (type != null) && !type.equals(featureType);
        /*
         * The feature type must be set before to invoke `createColumns(…)` because it is used not only
         * by that method, but also by the listener registered in `FeatureTable(other)` constructor.
         */
        featureType = type;
        if (update) {
            createColumns();
        }
    }

    /**
     * Creates table columns for the current {@link #featureType}.
     */
    private void createColumns() {
        final Collection<? extends PropertyType> properties = featureType.getProperties(true);
        final List<TableColumn<Feature,?>> columns = new ArrayList<>(properties.size());
        final List<String> multiValued = new ArrayList<>(columns.size());
        for (final PropertyType pt : properties) {
            /*
             * Get localized text to show in column header. Also remember
             * the plain property name; it will be needed for ValueGetter.
             */
            final GenericName qualifiedName = pt.getName();
            final String name = qualifiedName.toString();
            String title = string(pt.getDesignation().orElse(null));
            if (title == null) {
                title = string(qualifiedName.toInternationalString());
                if (title == null) title = name;
            }
            /*
             * If the property may contain more than one value, we will
             * need a specialized cell getter.
             *
             * TODO: we should also handle FeatureAssociationRole here.
             *       See comment in class javadoc.
             */
            boolean isMultiValued = false;
            if (pt instanceof AttributeType<?>) {
                isMultiValued = ((AttributeType<?>) pt).getMaximumOccurs() > 1;
            }
            if (isMultiValued) {
                multiValued.add(name);
            }
            /*
             * Create and configure the column. For multi-valued properties, ValueGetter always
             * gives the whole collection. Fetching a particular element in that collection will
             * be ElementCell's work.
             */
            final TableColumn<Feature,Object> column = new TableColumn<>(title);
            column.setCellValueFactory(new ValueGetter(name));
            column.setCellFactory(isMultiValued ? ElementCell::new : ValueCell::new);
            if (AttributeConvention.contains(qualifiedName)) {
                column.setVisible(false);                       // Hide synthetic properties.
            }
            columns.add(column);
        }
        /*
         * If there is at least one multi-valued property, insert a column which will contain
         * an icon in front of rows having a property with more than one value.
         */
        if (multiValued.isEmpty()) {
            setItems(getFeatureList());         // Will fire a change event only if the list is not the same.
        } else {
            final ExpandableList list = getExpandableList();
            list.setMultivaluedColumns(multiValued);
            final TableColumn<Feature,Feature> column = new TableColumn<>("▤");
            column.setCellValueFactory(IdentityValueFactory.instance());
            column.setCellFactory(list);
            column.setReorderable(false);
            column.setSortable   (false);
            column.setResizable  (false);
            column.setMinWidth(20);
            column.setMaxWidth(20);
            columns.add(0, column);
            setItems(list);
        }
        getColumns().setAll(columns);       // Change columns in an all or nothing operation.
    }




    /**
     * Given a {@link Feature}, returns the value of the property having the name specified at construction time.
     * Note that if the property is multi-valued, then this getter returns the whole collection since we have no
     * easy way to know the current row number. Fetching a particular element in that collection will be done by
     * {@link ExpandedFeature}.
     */
    private static final class ValueGetter implements Callback<TableColumn.CellDataFeatures<Feature,Object>, ObservableValue<Object>> {
        /**
         * The name of the feature property for which to fetch values.
         */
        private final String name;

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
         * This method is invoked by JavaFX when a cell needs to be rendered with a new value.
         */
        @Override
        public ObservableValue<Object> call(final TableColumn.CellDataFeatures<Feature, Object> cell) {
            Object value = null;
            final Feature feature = cell.getValue();
            if (feature != null) {
                value = feature.getPropertyValue(name);
            }
            return new ReadOnlyObjectWrapper<>(value);
        }
    }

    /**
     * A cell displaying a value in {@link FeatureTable}. This base class expects single values.
     * If the property values are collections, then {@link ElementCell} should be used instead.
     */
    private static class ValueCell extends TableCell<Feature,Object> {
        /**
         * Creates a new cell for feature property value.
         *
         * @param  column  the column where the cell will be shown.
         */
        ValueCell(final TableColumn<Feature,Object> column) {
            // Column not used at this time, but we need it in method signature.
        }

        /**
         * Invoked when a new value needs to be show.
         *
         * @todo Needs to check for object type (number, date, etc.).
         *       Should share with {@link org.apache.sis.gui.metadata.MetadataTree}.
         *
         * @todo For points, use {@link TableColumn#getColumns() nested columns} (one per dimension)
         *       with labels fetched from the CRS. For geometries, consider expanding points as we do
         *       for collections.
         *
         * @todo For {@link ValueCell} only (not {@link ElementCell}), if the feature is {@link ExpandedFeature}
         *       with {@code index != 0}, write text in gray. We could also use the value formatted at index 0
         *       for avoiding to format the same thing many times.
         *
         * @param  value  the new item for the cell.
         * @param  empty  whether this cell is used to render an empty row.
         */
        @Override
        protected void updateItem(final Object value, final boolean empty) {
            if (value == getItem()) return;
            super.updateItem(value, empty);
            String text = null;
            if (value != null) {
                text = value.toString();
            }
            setText(text);
        }
    }

    /**
     * Fetch single elements from multi-valued properties.
     */
    private static final class ElementCell extends ValueCell {
        /**
         * Creates a new cell for multi-values feature property value.
         *
         * @param  column  the column where the cell will be shown.
         */
        ElementCell(final TableColumn<Feature,Object> column) {
            super(column);
        }

        /**
         * Invoked when a new value needs to be show.
         *
         * @param  value  the new item for the cell.
         * @param  empty  whether this cell is used to render an empty row.
         */
        @Override
        protected void updateItem(Object value, final boolean empty) {
            if (value instanceof List<?>) {
                final List<?> c = (List<?>) value;
                value = c.isEmpty() ? null : c.get(0);
            } else if (value instanceof Iterable<?>) {
                final Iterator<?> c = ((Iterable<?>) value).iterator();
                value = c.hasNext() ? c.next() : null;
            }
            super.updateItem(value, empty);
        }
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private String string(final InternationalString i18n) {
        return (i18n != null) ? Strings.trimOrNull(i18n.toString(textLocale)) : null;
    }

    /**
     * If a loading process was under way, interrupts it and closes the feature stream.
     * This method returns immediately; the release of resources happens in a background thread.
     */
    public void interrupt() {
        getFeatureList().interrupt();
    }

    /**
     * Replaces the table content by an exception message.
     * This method is invoked after a loading process failed.
     */
    final void setException(final Throwable exception) {
        getItems().clear();
        final Region trace = new ExceptionReporter(exception).getView();
        StackPane.setAlignment(trace, Pos.TOP_LEFT);
        setPlaceholder(trace);
    }

    /**
     * Reports an exception that we cannot display in this widget, for example because it applies
     * to different data than the one currently viewed. The {@code method} argument should be the
     * public API (if possible) invoking the method where the exception is caught.
     */
    static void unexpectedException(final String method, final Throwable exception) {
        Logging.unexpectedException(LOGGER, FeatureTable.class, method, exception);
    }

    /**
     * Reports an exception that we choose to ignore.
     */
    static void recoverableException(final String method, final Exception exception) {
        Logging.recoverableException(LOGGER, FeatureTable.class, method, exception);
    }
}
