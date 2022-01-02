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

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.function.Predicate;
import java.text.NumberFormat;
import java.io.IOException;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.apache.sis.gui.Widget;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.ResampledImage;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.PropertyView;
import org.apache.sis.internal.gui.ImmutableObjectProperty;
import org.apache.sis.internal.gui.PropertyValueFormatter;
import org.apache.sis.internal.gui.Resources;


/**
 * Information about {@link RenderedImage} (sources, layout, properties).
 * The {@link #image} property value is shown as the root of a tree of images,
 * with image {@linkplain RenderedImage#getSources() sources} as children.
 * When an image is selected, its layout (image size, tile size, <i>etc.</i>) is described in a table.
 * Image {@linkplain RenderedImage#getPropertyNames() properties} are also available in a separated table.
 *
 * <p>This widget is useful mostly for debugging purposes or for advanced users.
 * For displaying a geospatial raster as a GIS application, see {@link CoverageCanvas} instead.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public class ImagePropertyExplorer extends Widget {
    /**
     * The root image to describe. This image will be the root of a tree;
     * children will be image {@linkplain RenderedImage#getSources() sources}.
     *
     * <div class="note"><b>API note:</b>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</div>
     */
    public final ObjectProperty<RenderedImage> image;

    /**
     * Implementation of {@link #image} property.
     */
    private final class ImageProperty extends ObjectPropertyBase<RenderedImage> {
        /** Returns the bean that contains this property. */
        @Override public Object getBean() {return ImagePropertyExplorer.this;}
        @Override public String getName() {return "image";}

        /** Sets this property to the given value with no sub-region. */
        @Override public void set(RenderedImage newValue) {setImage(newValue, null);}

        /** Do the actual set operation without invoking {@link ImagePropertyExplorer} setter method. */
        void assign(RenderedImage newValue) {super.set(newValue);}
    }

    /**
     * Image region which is currently visible, or {@code null} if unspecified.
     * Conceptually this field and {@link #image} should be set together. But we have not defined
     * a container object for those two properties. So we use that field as a workaround for now.
     *
     * @see #setImage(RenderedImage, Rectangle)
     */
    private Rectangle visibleImageBounds;

    /**
     * Whether {@link #visibleImageBounds} applies to the coordinate system of an image.
     * This is initially {@code true} for an image specified by {@link CoverageCanvas} and become {@code false}
     * after a {@link ResampledImage} is found. Images not present in this map are implicitly associated to the
     * {@code false} value.
     *
     * <p>This map is also opportunistically used for avoiding never-ending recursivity
     * during the traversal of image sources.</p>
     *
     * @see #setImage(RenderedImage, Rectangle)
     */
    private final Map<RenderedImage,Boolean> imageUseBoundsCS;

    /**
     * Whether to update {@code ImagePropertyExplorer} content when the {@link #image} changed.
     * This is usually {@code true} unless this {@code ImagePropertyExplorer} is hidden,
     * in which case it may be useful to temporary disable updates for saving CPU times.
     *
     * <div class="note"><b>Example:</b>
     * if this {@code ImagePropertyExplorer} is shown in a {@link TitledPane}, one can bind this property
     * to {@link TitledPane#expandedProperty()} for updating the content only if the pane is visible.
     * </div>
     *
     * Note that setting this property to {@code false} may have the effect of discarding current content
     * when the {@link #image} change. This is done for allowing the garbage collector to reclaim memory.
     * The content is reset to {@link #image} properties when {@code updateOnChange} become {@code true} again.
     *
     * <div class="note"><b>API note:</b>
     * We do not provide getter/setter for this property; use {@link BooleanProperty#set(boolean)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.</div>
     */
    public final BooleanProperty updateOnChange;

    /**
     * Whether to notify {@code ImagePropertyExplorer} about {@link #image} changes.
     * This may become {@code false} after {@link #updateOnChange} (not at the same time),
     * and reset to {@code true} when {@code updateOnChange} become {@code true} again.
     *
     * @see #updateOnChange
     * @see #startListening()
     */
    private boolean listening;

    /**
     * The root {@link #image} and its sources as a tree. The root value may be {@code null} and the children
     * removed if the tree needs to be rebuilt after an {@linkplain #image} change and this rebuild has been
     * deferred ({@link #updateOnChange} is {@code false}).
     */
    private final TreeItem<RenderedImage> sourcesRoot;

    /**
     * The selected item in the sources tree.
     *
     * @see #getSelectedImage()
     */
    private final ReadOnlyObjectProperty<TreeItem<RenderedImage>> selectedImage;

    /**
     * The rows in the table showing layout information (image size, tile size, image position, <i>etc</i>).
     * This list should be considered read-only.
     */
    private final ObservableList<LayoutRow> layoutRows;

    /**
     * A row in the table showing image layout. The inherited {@link String} property is the label to show in
     * the first column. That label never change, contrarily to the {@link #xp} and {@link #yp} property values
     * which are updated every time that we need to update the content for a new image.
     */
    private static final class LayoutRow extends ImmutableObjectProperty<String> {
        /**
         * Row indices where {@link LayoutRow} instances are shown, when all rows are present.
         * Rows {@link #DISPLAYED_SIZE} and {@link #MIN_VISIBLE} may be absent, in which case
         * next rows have their position shifted.
         */
        static final int IMAGE_SIZE = 0, DISPLAYED_SIZE = 1, TILE_SIZE = 2, NUM_TILES = 3,
                         MIN_PIXEL = 4, MIN_VISIBLE = 5, MIN_TILE = 6;

        /**
         * Creates all rows.
         */
        static LayoutRow[] values(final Vocabulary vocabulary, final Resources resources) {
            final LayoutRow[] rows = new LayoutRow[7];
            rows[IMAGE_SIZE]     = new LayoutRow(true,  vocabulary.getString(Vocabulary.Keys.ImageSize));
            rows[DISPLAYED_SIZE] = new LayoutRow(false, resources .getString(Resources .Keys.DisplayedSize));
            rows[TILE_SIZE]      = new LayoutRow(true,  vocabulary.getString(Vocabulary.Keys.TileSize));
            rows[NUM_TILES]      = new LayoutRow(true,  vocabulary.getString(Vocabulary.Keys.NumberOfTiles));
            rows[MIN_PIXEL]      = new LayoutRow(true,  resources .getString(Resources .Keys.ImageStart));
            rows[MIN_VISIBLE]    = new LayoutRow(false, resources .getString(Resources .Keys.DisplayStart));
            rows[MIN_TILE]       = new LayoutRow(true,  resources .getString(Resources .Keys.TileIndexStart));
            return rows;
        }

        /** Size or position along x and y axes, to show in second and third columns. */
        final IntegerProperty xp, yp;

        /**
         * Whether this property is a core property to keep always visible.
         */
        private final boolean core;

        /** Creates a new row with the given label in first column. */
        private LayoutRow(final boolean core, final String label) {
            super(label);
            this.core = core;
            xp = new SimpleIntegerProperty();
            yp = new SimpleIntegerProperty();
        }

        /**
         * Updates {@link #xp} and {@link #yp} property values for the given image.
         * The index <var>i</var> is the row index when no filtering is applied.
         */
        final void update(final RenderedImage image, final Rectangle visibleImageBounds, final int i) {
            int x = 0, y = 0;
            if (image != null) switch (i) {
                case IMAGE_SIZE:   x = image.getWidth();     y = image.getHeight();     break;
                case TILE_SIZE:    x = image.getTileWidth(); y = image.getTileHeight(); break;
                case NUM_TILES:    x = image.getNumXTiles(); y = image.getNumYTiles();  break;
                case MIN_TILE:     x = image.getMinTileX();  y = image.getMinTileY();   break;
                case MIN_PIXEL:    x = image.getMinX();      y = image.getMinY();       break;
                case MIN_VISIBLE:  if (visibleImageBounds != null) {
                                       x = visibleImageBounds.x;
                                       y = visibleImageBounds.y;
                                   }
                                   break;
                case DISPLAYED_SIZE: if (visibleImageBounds != null) {
                                       x = visibleImageBounds.width;
                                       y = visibleImageBounds.height;
                                   }
                                   break;
            }
            xp.set(x);
            yp.set(y);
        }

        /**
         * Filter for excluding the rows that need a non-null {@code visibleImageBounds} argument.
         */
        static Predicate<LayoutRow> EXCLUDE_VISIBILITY = (r) -> r.core;
    }

    /**
     * The predicate for filtering {@link #layoutRows}.
     *
     * @see LayoutRow#EXCLUDE_VISIBILITY
     */
    private final ObjectProperty<Predicate<? super LayoutRow>> layoutFilter;

    /**
     * The rows in the tables showing property values.
     * Rows in the list will be added and removed when the image changed.
     *
     * @see #updatePropertyList(RenderedImage)
     */
    private final ObservableList<PropertyRow> propertyRows;

    /**
     * The selected item in the table of properties.
     */
    private final ReadOnlyObjectProperty<PropertyRow> selectedProperty;

    /**
     * A row in the table showing image properties. The inherited {@link String} property is the property name.
     * The property value is fetched from the given image and can be updated for the value of a new image.
     * Updating an existing {@code PropertyRow} instead of creating a new instance is useful for keeping
     * the selected row unchanged if possible.
     */
    private static final class PropertyRow extends ImmutableObjectProperty<String> {
        /**
         * Image property value.
         */
        final ObjectProperty<Object> value;

        /**
         * Creates a new row for the given property in the given image.
         */
        PropertyRow(final RenderedImage image, final String property) {
            super(property);
            value = new SimpleObjectProperty<>(getProperty(image, property));
        }

        /**
         * If this property can be updated to a value for the given image, performs
         * the update and returns {@code true}. Otherwise returns {@code false}.
         */
        final boolean update(final RenderedImage image, final String property) {
            if (property.equals(super.get())) {
                value.set(getProperty(image, property));
                return true;
            }
            return false;
        }

        /**
         * Returns a property value of given image, or the exception if that operation failed.
         */
        private static Object getProperty(final RenderedImage image, final String property) {
            try {
                return image.getProperty(property);
            } catch (RuntimeException e) {
                return e;
            }
        }

        /**
         * Returns a human-readable variation of the property name for use in graphic interface.
         */
        @Override
        public String get() {
            final String property = super.get();
            return CharSequences.camelCaseToSentence(property.substring(property.lastIndexOf('.') + 1)).toString();
        }
    }

    /**
     * The tab where to show details about a property value. The content of tab may be different kinds
     * of node depending on the class of the property to be show.
     *
     * @see #propertyDetails
     * @see #updatePropertyDetails(Rectangle)
     */
    private final Tab detailsTab;

    /**
     * Viewer of property value. The different components of this viewer are created when first needed.
     *
     * @see #updatePropertyDetails(Rectangle)
     */
    private final PropertyView propertyDetails;

    /**
     * The view containing all visual components.
     * The exact class may change in any future version.
     */
    private final TabPane view;

    /**
     * Creates an initially empty explorer.
     */
    public ImagePropertyExplorer() {
        this(null, null);
    }

    /**
     * Creates a new explorer.
     *
     * @param  background  the image background color, or {@code null} if none.
     */
    ImagePropertyExplorer(final Locale locale,  final ObjectProperty<Background> background) {
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        final Resources  resources  = Resources.forLocale(locale);

        // Following variables could be class fields, but are not yet needed outside this constructor.
        final TreeView<RenderedImage> sources;
        final TableView<LayoutRow>    layout;
        final NumberFormat            integerFormat;
        final TableView<PropertyRow>  properties;

        image            = new ImageProperty();
        imageUseBoundsCS = new IdentityHashMap<>(4);
        updateOnChange   = new SimpleBooleanProperty(this, "updateOnChange", true);
        listening        = true;
        /*
         * Tree of image sources. The root is never changed after construction. All children nodes can
         * be created, removed or updated to new value at any time. At most one image can be selected.
         */
        {
            sourcesRoot   = new TreeItem<>();
            sources       = new TreeView<>(sourcesRoot);
            selectedImage = sources.getSelectionModel().selectedItemProperty();
            sources.setCellFactory(SourceCell::new);
            selectedImage.addListener((p,o,n) -> {
                RenderedImage selected = null;
                if (n != null) selected = n.getValue();
                imageSelected(selected != null ? selected : image.get());
            });
        }
        /*
         * Table of image layout built with a fixed set of rows: no row will be added or removed after
         * construction. Instead property values of existing rows will be modified when a new image is
         * selected. Row selection are not allowed since we have nothing to do with selected rows.
         */
        {
            final FilteredList<LayoutRow> filtered;
            layoutRows     = FXCollections.observableArrayList(LayoutRow.values(vocabulary, resources));
            filtered       = new FilteredList<>(layoutRows);
            layout         = new TableView<>(filtered);
            layoutFilter   = filtered.predicateProperty();
            integerFormat  = NumberFormat.getIntegerInstance();
            layout.setSelectionModel(null);

            final TableColumn<LayoutRow, String> label = new TableColumn<>(resources.getString(Resources.Keys.SizeOrPosition));
            final TableColumn<LayoutRow, Number> xCol  = new TableColumn<>(resources.getString(Resources.Keys.Along_1, "X"));
            final TableColumn<LayoutRow, Number> yCol  = new TableColumn<>(resources.getString(Resources.Keys.Along_1, "Y"));
            final Callback<TableColumn<LayoutRow, Number>,
                             TableCell<LayoutRow, Number>> cellFactory = (column) -> new LayoutCell(integerFormat);

            xCol .setCellFactory(cellFactory);
            yCol .setCellFactory(cellFactory);
            xCol .setCellValueFactory((cell) -> cell.getValue().xp);
            yCol .setCellValueFactory((cell) -> cell.getValue().yp);
            label.setCellValueFactory((cell) -> cell.getValue());
            layout.getColumns().setAll(label, xCol, yCol);
            layout.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            layout.getColumns().forEach((c) -> {
                c.setReorderable(false);
                c.setSortable(false);
            });
        }
        /*
         * Table of image properties. Contrarily to the layout table, the set of rows in
         * this property table may change at any time. At most one row can be selected.
         * We do not register a listener on the row selection; instead we wait for the
         * details pane to become visible.
         */
        {
            properties       = new TableView<>();
            propertyRows     = properties.getItems();
            selectedProperty = properties.getSelectionModel().selectedItemProperty();
            final TableColumn<PropertyRow, String> label = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Property));
            final TableColumn<PropertyRow, Object> value = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Value));
            label.setCellValueFactory((cell) -> cell.getValue());
            value.setCellValueFactory((cell) -> cell.getValue().value);
            value.setCellFactory((column) -> new PropertyCell(locale));
            properties.getColumns().setAll(label, value);
            properties.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            properties.getColumns().forEach((c) -> c.setReorderable(false));
        }
        /*
         * Tab where to show details about the currently selected property value.
         * The tab content is updated when it become visible. We can do that because
         * the property selection is done in another tab.
         */
        {
            detailsTab = new Tab(vocabulary.getString(Vocabulary.Keys.Details));
            selectedProperty.addListener((p,o,n) -> clearPropertyValues(false));
            propertyDetails = new PropertyView(locale, detailsTab.contentProperty(), background);
            detailsTab.selectedProperty().addListener((p,o,n) -> {
                if (n) updatePropertyDetails(getVisibleImageBounds(getSelectedImage()));
            });
        }
        /*
         * The view containing all visual components. In current version the sources is a tab like others.
         * A previous version was showing the sources on top (using SlidePane), so we could navigate easily
         * in the properties of different sources. It has been removed for simplifying the layout, but the
         * listeners are still updating layout and property panes immediately when a new source is selected.
         */
        view = new TabPane(
                new Tab(vocabulary.getString(Vocabulary.Keys.Source), sources),
                new Tab(vocabulary.getString(Vocabulary.Keys.Layout), layout),
                new Tab(vocabulary.getString(Vocabulary.Keys.Properties), properties),
                detailsTab);
        view.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        updateOnChange.addListener((p,o,n) -> {if (n) startListening();});
    }

    /**
     * Invoked when {@link #updateOnChange} became {@code true}.
     * This method updates the visual components for current image.
     *
     * <p>Note: there is no {@code stopListening()} method because setting {@link #listening} flag
     * to {@code false} will be done by the {@link #setImage(RenderedImage, Rectangle)} method.</p>
     */
    private void startListening() {
        listening = true;
        if (sourcesRoot.getValue() == null) {
            setTreeRoot(image.get());
            refreshTables();
        }
    }

    /**
     * Sets the image to show together with the coordinates of the region currently shown.
     * If {@link #updateOnChange} is true, then the tree view is updated.
     * Otherwise we will wait for the tree view to become visible before to update it.
     *
     * @param newValue       the new image, or {@code null} if none.
     * @param visibleBounds  image region which is currently visible, or {@code null} if unspecified.
     */
    final void setImage(final RenderedImage newValue, final Rectangle visibleBounds) {
        visibleImageBounds = visibleBounds;
        ((ImageProperty) image).assign(newValue);
        if (listening) {
            final boolean immediate = updateOnChange.get();
            setTreeRoot(immediate ? newValue : null);
            if (immediate) {
                refreshTables();
            } else {
                clearPropertyValues(true);
                listening = false;
            }
        }
    }

    /**
     * Returns the currently selected image. If no image is explicitly selected,
     * returns the root {@linkplain #image} (which may be null).
     */
    private RenderedImage getSelectedImage() {
        final TreeItem<RenderedImage> item = selectedImage.get();
        if (item != null) {
            final RenderedImage selected = item.getValue();
            if (selected != null) return selected;
        }
        return image.get();
    }

    /**
     * Refresh all visual components except the tree of sources. This includes the table of
     * image layouts, the table of property values and the details of selected property value.
     */
    private void refreshTables() {
        imageSelected(getSelectedImage());
    }

    /**
     * Invoked when an image is selected in the tree of image sources. The selected image
     * is not necessarily the {@link #image} property value; it may be one of its sources.
     * If no image is explicitly selected, defaults to the root image.
     */
    private void imageSelected(final RenderedImage selected) {
        final Rectangle bounds = getVisibleImageBounds(selected);
        final int n = layoutRows.size();
        for (int i=0; i<n; i++) {
            layoutRows.get(i).update(selected, bounds, i);
        }
        layoutFilter.set(bounds != null ? null : LayoutRow.EXCLUDE_VISIBILITY);
        updatePropertyList(selected);
        /*
         * The selected property value may have changed as a result of above.
         * If the details tab is visible, update immediately. Otherwise we will
         * wait for that tab to become visible.
         */
        if (detailsTab.isSelected()) {
            updatePropertyDetails(bounds);
        }
    }

    /**
     * Returns the pixel coordinates of the region shown on screen,
     * or {@code null} if none or does not apply to the currently selected image.
     */
    final Rectangle getVisibleImageBounds(final RenderedImage selected) {
        return Boolean.TRUE.equals(imageUseBoundsCS.get(selected)) ? visibleImageBounds : null;
    }

    /**
     * Sets the root image together with its tree of sources.
     */
    private void setTreeRoot(final RenderedImage newValue) {
        imageUseBoundsCS.clear();
        setTreeNode(sourcesRoot, newValue, imageUseBoundsCS, visibleImageBounds != null);
        /*
         * Remove entries associated to value `false` since our default value is `false`.
         * The intent is to avoid unnecessary `RenderedImage` references for reducing the
         * risk of memory retention.
         */
        imageUseBoundsCS.values().removeIf((b) -> !b);
    }

    /**
     * Invoked when tree under {@link #sourcesRoot} node needs to be updated. This method is not necessarily invoked
     * immediately after an {@linkplain #image} change; the update may be deferred until the tree become visible.
     *
     * @param  imageUseBoundsCS  the {@link #imageUseBoundsCS} as an initially empty map. This map is
     *         populated by this method and opportunistically used for avoiding infinite recursivity.
     */
    private static void setTreeNode(final TreeItem<RenderedImage> root, final RenderedImage image,
            final Map<RenderedImage,Boolean> imageUseBoundsCS, Boolean boundsApplicable)
    {
        root.setValue(image);
        if (imageUseBoundsCS.putIfAbsent(image, boundsApplicable) == null) {
            final ObservableList<TreeItem<RenderedImage>> children = root.getChildren();
            if (image != null) {
                final List<RenderedImage> sources = image.getSources();
                if (sources != null) {
                    /*
                     * If the image is an instance of `ResampledImage`, then its
                     * source is presumed to use a different coordinate system.
                     */
                    if (image instanceof ResampledImage) {
                        boundsApplicable = Boolean.FALSE;
                    }
                    final int numSrc = sources.size();
                    final int numDst = children.size();
                    final int n = Math.min(numSrc, numDst);
                    int i;
                    for (i=0; i<n; i++) {
                        setTreeNode(children.get(i), sources.get(i), imageUseBoundsCS, boundsApplicable);
                    }
                    for (; i<numSrc; i++) {
                        final TreeItem<RenderedImage> child = new TreeItem<>();
                        setTreeNode(child, sources.get(i), imageUseBoundsCS, boundsApplicable);
                        children.add(child);
                    }
                    if (i < numDst) {
                        children.remove(i, numDst);
                    }
                    return;
                }
            }
            children.clear();
        }
    }

    /**
     * Creates the renderer of cells in the tree of image sources.
     */
    private static final class SourceCell extends TreeCell<RenderedImage> {
        /**
         * Invoked by the cell factory (must have this exact signature).
         */
        SourceCell(final TreeView<RenderedImage> tree) {
        }

        /**
         * Invoked when a new image is shown in this cell node. This method also tests image consistency.
         * If an inconsistency is found, the line is shown in red (except for "width" and "height") with
         * a warning message. We do not use a red color for "width" and "height" because the mismatch may
         * be normal.
         */
        @Override protected void updateItem(final RenderedImage image, final boolean empty) {
            super.updateItem(image, empty);
            String text = null;
            Color  fill = Styles.NORMAL_TEXT;
            if (image != null) {
                /*
                 * Gets a simple top-level class name for an image class. If the image is an enclosed class,
                 * searches for a parent class because enclosed class names are often not very informative.
                 * For example `ImageRenderer.Untitled` is a `BufferedImage` subclass.
                 */
                Class<?> type = image.getClass();
                while (type.getEnclosingClass() != null) {
                    type = type.getSuperclass();
                }
                text = type.getSimpleName();
                if (image instanceof PlanarImage) {
                    final String check = ((PlanarImage) image).verify();
                    if (check != null) {
                        text = Resources.format(Resources.Keys.InconsistencyIn_2, text, check);
                        if (!(check.equals("width") || check.equals("height"))) {
                            fill = Styles.ERROR_TEXT;
                        }
                    }
                }
            }
            setText(text);
            setTextFill(fill);
        }
    }

    /**
     * Creates the renderer of cells in the table of image layout information.
     */
    private static final class LayoutCell extends TableCell<LayoutRow,Number> {
        /**
         * The formatter to use for numerical values in the table.
         */
        private final NumberFormat integerFormat;

        /**
         * Invoked by the cell factory.
         */
        LayoutCell(final NumberFormat integerFormat) {
            this.integerFormat = integerFormat;
            setAlignment(Pos.CENTER_RIGHT);
        }

        /**
         * Invoked when a new value is shown in this table cell.
         */
        @Override protected void updateItem(final Number value, final boolean empty) {
            super.updateItem(value, empty);
            setText(value != null ? integerFormat.format(value) : null);
        }
    }

    /**
     * Creates the renderer of cells in the table of image properties.
     */
    private static final class PropertyCell extends TableCell<PropertyRow,Object> {
        /**
         * The formatter to use for producing a short string representation of a property value.
         */
        private final PropertyValueFormatter format;

        /**
         * Temporary buffer user when formatting property values.
         */
        private final StringBuilder buffer;

        /**
         * Invoked by the cell factory.
         */
        PropertyCell(final Locale locale) {
            buffer = new StringBuilder();
            format = new PropertyValueFormatter(buffer, locale);
        }

        /**
         * Invoked when a new value is shown in this table cell.
         */
        @Override protected void updateItem(final Object value, final boolean empty) {
            super.updateItem(value, empty);
            String text = null;
            if (!empty) try {
                buffer.setLength(0);
                format.appendValue(value);
                format.flush();
                text = buffer.toString();
            } catch (IOException e) {           // Should never happen since we write in a StringBuilder.
                text = e.toString();
            }
            setText(text);
        }
    }

    /**
     * Update the list of properties for the given image.
     * The {@link #propertyRows} are updated with an effort for reusing existing items when
     * the property name is the same. The intent is to keep selection unchanged if possible
     * (because removing a selected row may make it unselected).
     */
    private void updatePropertyList(final RenderedImage selected) {
        if (selected != null) {
            final String[] properties = selected.getPropertyNames();
            if (properties != null) {
                int insertAt = 0;
nextProp:       for (final String property : properties) {
                    if (property != null) {
                        for (int i=insertAt; i < propertyRows.size(); i++) {
                            if (propertyRows.get(i).update(selected, property)) {
                                propertyRows.remove(insertAt, i);
                                insertAt = i + 1;
                                continue nextProp;
                            }
                        }
                        propertyRows.add(insertAt++, new PropertyRow(selected, property));
                    }
                }
                propertyRows.remove(insertAt, propertyRows.size());
                return;
            }
        }
        propertyRows.clear();
    }

    /**
     * Updates the {@link #detailsTab} with the value of currently selected property.
     * This method may be invoked after the selection changed (but not immediately),
     * or after the selected image changed (which indirectly changes the properties).
     *
     * @param  bounds  {@link #visibleImageBounds} or {@code null} if it does not apply to current image.
     */
    private void updatePropertyDetails(final Rectangle bounds) {
        final PropertyRow row = selectedProperty.get();
        propertyDetails.set((row != null) ? row.value.get() : null, bounds);
    }

    /**
     * Clears the table of property values and the content of {@link #detailsTab}.
     * We do that when the tab became hidden and the image changed, in order to give
     * a chance to the garbage collector to release memory.
     *
     * @param  full  whether to clears also the table in the "properties" tab (in addition of clearing the
     *         "details" tab). This parameter should be {@code false} if the properties tab is still visible.
     */
    private void clearPropertyValues(final boolean full) {
        if (propertyDetails != null) {
            propertyDetails.clear();
            detailsTab.setContent(null);
        }
        if (full) {
            propertyRows.clear();
        }
    }

    /**
     * Returns the view of this explorer. The subclass is implementation dependent
     * and may change in any future version.
     *
     * @return this explorer view.
     */
    @Override
    public Region getView() {
        return view;
    }

    /**
     * Returns the locale for controls and messages.
     *
     * @since 1.2
     */
    @Override
    public final Locale getLocale() {
        return propertyDetails.getLocale();
    }
}
