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

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Date;
import java.util.StringJoiner;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.ControlledVocabulary;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;

import static org.apache.sis.internal.util.CollectionsExt.nonNull;


/**
 * A panel showing a summary of metadata.
 *
 * @author  Smaniotto Enzo
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class MetadataOverview {
    /**
     * Minimal size of rectangles to be drawn by {@link IdentificationInfo#drawOnMap(GeographicBoundingBox)}.
     * If a rectangle is smaller, it will be expanded to this size. We use a minimal size because otherwise
     * small rectangles may be practically invisible.
     */
    private static final double MIN_RECT_SIZE = 6;

    /**
     * Titles panes for different metadata sections (identification info, spatial information, <i>etc</i>).
     * This is similar to {@link javafx.scene.control.Accordion} except that we allow an arbitrary amount
     * of titled panes to be opened in same time.
     */
    private final ScrollPane content;

    /**
     * The resources for localized strings. Stored because needed often.
     */
    final Resources localized;

    /**
     * The locale to use for date/number formatters.
     */
    private final Locale formatLocale;

    /**
     * The format to use for writing numbers, created when first needed.
     *
     * @see #getNumberFormat()
     */
    private NumberFormat numberFormat;

    /**
     * The format to use for writing dates, created when first needed.
     *
     * @see #getDateFormat()
     */
    private DateFormat dateFormat;

    /**
     * An image of size 360×180 pixels showing a map of the world.
     * This is loaded when first needed.
     *
     * @see #getWorldMap()
     */
    private Image worldMap;

    /**
     * Whether we already tried to load {@link #worldMap}.
     */
    private boolean worldMapLoaded;

    /**
     * If this {@link MetadataOverview} is loading metadata, the worker doing this task.
     * Otherwise {@code null}. This is used for cancelling the currently running loading
     * process if {@link #setMetadata(Resource)} is invoked again before completion.
     */
    private Worker<Metadata> loader;

    /**
     * If the metadata or the grid geometry can not be obtained, the reason.
     *
     * @todo show in this control.
     */
    private Throwable error;

    /**
     * The pane where to show information about resource identification, spatial representation, etc.
     * Those panes will be added in the {@link #content} when we determined that they are not empty.
     * The content of those panes is updated by {@link #setMetadata(Metadata)}.
     */
    private final TitledPane[] information;

    /**
     * Creates an initially empty metadata overview.
     */
    MetadataOverview(final Locale locale) {
        localized    = Resources.forLocale(locale);
        formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        information  = new TitledPane[] {
            new TitledPane(localized.getString(Resources.Keys.ResourceIdentification), new IdentificationInfo(this)),
            new TitledPane(localized.getString(Resources.Keys.SpatialRepresentation),  new RepresentationInfo(this))
        };
        content = new ScrollPane(new VBox());
        content.setFitToWidth(true);
    }

    /**
     * Returns the region containing the visual components managed by this {@code MetadataOverview}.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    public final Region getView() {
        return content;
    }

    /**
     * Returns the format to use for writing numbers.
     */
    final NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance(formatLocale);
        }
        return numberFormat;
    }

    /**
     * Returns the format to use for writing dates.
     */
    private DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, formatLocale);
        }
        return dateFormat;
    }

    /**
     * Fetches the metadata in a background thread and delegates to
     * {@link #setMetadata(Metadata)} when ready.
     *
     * @param  resource  the resource for which to show metadata, or {@code null}.
     */
    public void setMetadata(final Resource resource) {
        assert Platform.isFxApplicationThread();
        if (loader != null) {
            loader.cancel();
            loader = null;
        }
        if (resource == null) {
            setMetadata((Metadata) null);
        } else {
            final class Getter extends Task<Metadata> {
                /**
                 * Invoked in a background thread for fetching metadata,
                 * eventually with other information like grid geometry.
                 */
                @Override protected Metadata call() throws DataStoreException {
                    return resource.getMetadata();
                }

                /**
                 * Shows the result, unless another {@link #setMetadata(Resource)} has been invoked.
                 */
                @Override protected void succeeded() {
                    if (!isCancelled()) {
                        setMetadata(getValue());
                    }
                }

                /**
                 * Invoked when an error occurred while fetching metadata.
                 */
                @Override protected void failed() {
                    if (!isCancelled()) {
                        setMetadata((Metadata) null);
                        error = getException();
                    }
                }
            }
            BackgroundThreads.execute(new Getter());
        }
    }

    /**
     * Sets the content of this pane to the given metadata.
     *
     * @param  metadata  the metadata to show, or {@code null}.
     */
    public void setMetadata(final Metadata metadata) {
        assert Platform.isFxApplicationThread();
        error = null;
        final ObservableList<Node> children = ((VBox) content.getContent()).getChildren();
        /*
         * We want to include only the non-empty panes in the children list. But instead of
         * removing everything and adding back non-empty panes, we check case-by-case if a
         * child should be added or removed. It will often result in no modification at all.
         */
        int i = 0;
        for (TitledPane pane : information) {
            final MetadataSection<?> info = (MetadataSection<?>) pane.getContent();
            info.setInformation(metadata);
            final boolean isEmpty   = info.isEmpty();
            final boolean isPresent = (i < children.size()) && children.get(i) == pane;
            if (isEmpty == isPresent) {     // Should not be present if empty, or should be present if non-empty.
                if (isEmpty) {
                    children.remove(i);
                } else {
                    children.add(i, pane);
                }
            }
            if (!isEmpty) i++;
        }
    }




    /**
     * The pane where to show the values of {@link Identification} objects.
     * The same pane can be used for an arbitrary amount of identifications.
     * Each instance is identified by its title.
     */
    private static final class IdentificationInfo extends MetadataSection<Identification> {
        /**
         * The resource title, or if non the identifier as a fallback.
         */
        private final Label title;

        /**
         * The canvas where to draw geographic bounding boxes over a world map.
         * Shall never be null, but need to be recreated for each new map.
         * A canvas of size (0,0) is available for drawing a new map.
         *
         * @see #isWorldMapEmpty()
         * @see #drawOnMap(GeographicBoundingBox)
         */
        private Canvas extentOnMap;

        /**
         * Creates an initially empty view for identification information.
         */
        IdentificationInfo(final MetadataOverview owner) {
            super(owner);
            title = new Label();
            title.setFont(Font.font(null, FontWeight.BOLD, 14));
            add(title, 0, 0, NUM_CHILD_PER_LINE, 1);

            extentOnMap = new Canvas();                     // Size of (0,0) by default.
            add(extentOnMap, 0, 0, NUM_CHILD_PER_LINE, 1);  // Will be moved to a different location by buildContent(…).
            setHalignment(extentOnMap, HPos.CENTER);
            finished();
        }

        /**
         * If the world map contains a map from some previous metadata, discards the old canvas and create a new one.
         * We do that because as of JavaFX 13, we found no way to clear the content of an existing {@link Canvas}.
         */
        @Workaround(library = "JavaFX", version = "13")
        private void clearWorldMap() {
            if (!isWorldMapEmpty()) {
                final int p = linesStartIndex() - 1;
                assert getChildren().get(p) == extentOnMap;
                getChildren().set(p, extentOnMap = new Canvas());
                setColumnSpan(extentOnMap, NUM_CHILD_PER_LINE);
                setHalignment(extentOnMap, HPos.CENTER);
            }
        }

        /**
         * Returns whether {@link #extentOnMap} is considered empty and available for use.
         */
        private boolean isWorldMapEmpty() {
            return extentOnMap.getWidth() == 0 && extentOnMap.getHeight() == 0;
        }

        /**
         * Returns {@code true} if this section contains no data.
         */
        @Override
        boolean isEmpty() {
            return super.isEmpty() && isWorldMapEmpty();
        }

        /**
         * Sets the identification information from the given metadata.
         */
        @Override
        void setInformation(final Metadata metadata) {
            setInformation(nonNull(metadata == null ? null : metadata.getIdentificationInfo()), Identification[]::new);
        }

        /**
         * Invoked when new identification information should be shown.
         * This method updates all fields in this section with the content of given identification information.
         */
        @Override
        void buildContent(final Identification info) {
            clearWorldMap();
            String text = null;
            final Citation citation = info.getCitation();
            if (citation != null) {
                text = owner.string(citation.getTitle());
                if (text == null) {
                    text = Citations.getIdentifier(citation);
                }
            }
            if (text == null) {
                text = vocabulary(Vocabulary.Keys.Untitled);
            }
            title.setText(text);
            /*
             * The abstract, or if there is no abstract the purpose, or if no purpose the credit as a fallback.
             * We use those fallback because they can provide some hints about the product.
             * The topic category (climatology, health, etc.) follows.
             */
            short label = Resources.Keys.Abstract;
            text = owner.string(info.getAbstract());
            if (text == null) {
                label = Resources.Keys.Purpose;
                text = owner.string(info.getPurpose());
                if (text == null) {
                    for (final InternationalString c : nonNull(info.getCredits())) {
                        text = owner.string(c);
                        if (text != null) {
                            label = Resources.Keys.Credit;
                            break;
                        }
                    }
                }
            }
            addLine(label, text);
            addLine(Resources.Keys.TopicCategory, owner.string(nonNull(info.getTopicCategories())));
            /*
             * Select a single, arbitrary date. We take the release or publication date if available.
             * If no publication date is found, fallback on the creation date. If no creation date is
             * found neither, fallback on the first date regardless its type.
             */
            if (citation != null) {
                Date date = null;
                label = Resources.Keys.Date;
                for (final CitationDate c : nonNull(citation.getDates())) {
                    final Date cd = c.getDate();
                    if (cd != null) {
                        final DateType type = c.getDateType();
                        if (DateType.PUBLICATION.equals(type) || DateType.RELEASED.equals(type)) {
                            label = Resources.Keys.PublicationDate;
                            date  = cd;
                            break;                      // Take the first publication or release date.
                        }
                        final boolean isCreation = DateType.CREATION.equals(type);
                        if (date == null || isCreation) {
                            label = isCreation ? Resources.Keys.CreationDate : Resources.Keys.Date;
                            date  = cd;     // Fallback date: creation date, or the first date otherwise.
                        }
                    }
                }
                if (date != null) {
                    addLine(label, owner.getDateFormat().format(date));
                }
            }
            /*
             * Type of resource: vector, grid, table, tin, video, etc. It gives a slight overview
             * of the next section, "Spatial representation". For that reason we put it close to
             * that next section, i.e. last in this section but just before the map.
             */
            addLine(Resources.Keys.TypeOfResource, owner.string(nonNull(info.getSpatialRepresentationTypes())));
            /*
             * Write the first description about the spatio-temporal extent, then draw all geographic bounding boxes
             * on a world map. If the bounding box encompasses the whole world, replace it by a "World" description.
             * The reason is that drawing a box over the whole world is not very informative; it rather looks like a
             * border around the image.
             */
            text = null;
            Identifier identifier = null;
            boolean isWorld = false;
            for (final Extent extent : nonNull(info.getExtents())) {
                if (extent != null) {
                    if (text == null) {
                        text = owner.string(extent.getDescription());
                    }
                    for (final GeographicExtent ge : nonNull(extent.getGeographicElements())) {
                        if (identifier == null && ge instanceof GeographicDescription) {
                            identifier = ((GeographicDescription) ge).getGeographicIdentifier();
                        }
                        if (!isWorld && ge instanceof GeographicBoundingBox) {
                            isWorld = drawOnMap((GeographicBoundingBox) ge);
                        }
                    }
                }
            }
            if (text == null) {
                text = IdentifiedObjects.toString(identifier);
            }
            if (isWorld) {
                clearWorldMap();
                if (text == null) {
                    text = vocabulary(Vocabulary.Keys.World);
                }
            }
            addLine(Resources.Keys.Extent, text);
            setRowIndex(extentOnMap, nextRowIndex());
        }

        /**
         * Returns a localized word from the {@link Vocabulary} resources.
         */
        private String vocabulary(final short key) {
            return Vocabulary.getResources(owner.localized.getLocale()).getString(key);
        }

        /**
         * Draws the given geographic bounding box on the map. This method can be invoked many times
         * if there is many bounding boxes on the same map.
         *
         * @param  bbox  the bounding box to draw.
         * @return {@code true} if the given bounding box encompasses the whole world.
         */
        private boolean drawOnMap(final GeographicBoundingBox bbox) {
            double north = Latitude.clamp(bbox.getNorthBoundLatitude());
            double south = Latitude.clamp(bbox.getSouthBoundLatitude());
            double east  =                bbox.getEastBoundLongitude();
            double west  =                bbox.getWestBoundLongitude();
            if (Math.abs(east - west) >= (Longitude.MAX_VALUE - Longitude.MIN_VALUE - 2*Formulas.ANGULAR_TOLERANCE)) {
                if (north >= Latitude.MAX_VALUE - Formulas.ANGULAR_TOLERANCE &&
                    south <= Latitude.MIN_VALUE + Formulas.ANGULAR_TOLERANCE)
                {
                    return true;                            // Bounding box encompasses the whole world.
                }
                west = Longitude.MIN_VALUE;
                east = Longitude.MAX_VALUE;                 // Whole world in longitudes, but not in latitude.
            } else {
                if (west != 180) west = Longitude.normalize(west);
                if (east != 180) east = Longitude.normalize(east);
                if (east < west) {
                    east += Longitude.MAX_VALUE - Longitude.MIN_VALUE;      // Box crosses the anti-meridian.
                }
            }
            if (north >= south && Double.isFinite(east) && Double.isFinite(west)) {
                double x = (Longitude.MAX_VALUE - Longitude.MIN_VALUE)  / 2 + west;
                double y =  (Latitude.MAX_VALUE -  Latitude.MIN_VALUE)  / 2 - north;
                double w = east  - west;
                double h = north - south;
                if (w < MIN_RECT_SIZE) {
                    x -= (MIN_RECT_SIZE - w) / 2;
                    w  =  MIN_RECT_SIZE;
                }
                if (h < MIN_RECT_SIZE) {
                    y -= (MIN_RECT_SIZE - h) / 2;
                    h  =  MIN_RECT_SIZE;
                }
                final double wi = Math.min(w, Longitude.MAX_VALUE - x);         // Width of part inside [-180 … +180]°.
                w -= wi;                                                        // Width of part not drawn by `wi`.
                /*
                 * At this point we got the coordinates of the rectangle to draw, adjusted for making sure
                 * that they are inside valid ranges. The `w` variable is usually 0, unless we had to cut
                 * the rectangle in two parts because of anti-meridian crossing.
                 */
                if (isWorldMapEmpty()) {
                    final Image image = owner.getWorldMap();
                    if (image == null) {
                        return false;                   // Failed to load the image.
                    }
                    extentOnMap.setWidth (image.getWidth());
                    extentOnMap.setHeight(image.getHeight());
                    extentOnMap.getGraphicsContext2D().drawImage(image, 0, 0);
                }
                final GraphicsContext gc = extentOnMap.getGraphicsContext2D();
                gc.setStroke(Color.DARKBLUE);
                gc.setGlobalAlpha(0.1);
                gc.fillRect(x, y, wi, h);
                if (w > 0) {
                    gc.fillRect(Longitude.MIN_VALUE, y, w, h);
                }
                gc.setGlobalAlpha(1.0);
                gc.strokeRect(x, y, wi, h);
                if (w > 0) {
                    gc.strokeRect(Longitude.MIN_VALUE, y, w, h);
                }
            }
            return false;
        }
    }

    /**
     * Returns an image of size 360×180 pixels showing a map of the world,
     * or {@code null} if we failed to load the image.
     */
    private Image getWorldMap() {
        if (!worldMapLoaded) {
            worldMapLoaded = true;                  // Set now for avoiding retries in case of failure.
            Exception error;
            try (InputStream in = MetadataOverview.class.getResourceAsStream("WorldMap360x180.png")) {
                worldMap = new Image(in);
                error = worldMap.getException();
            } catch (IOException e) {
                error = e;
            }
            if (error != null) {
                Logging.unexpectedException(Logging.getLogger(Modules.APPLICATION), MetadataOverview.class, "getWorldMap", error);
            }
        }
        return worldMap;
    }




    /**
     * The pane where to show the values of {@link SpatialRepresentation} objects.
     * The same pane can be used for an arbitrary amount of spatial representations.
     */
    private static final class RepresentationInfo extends MetadataSection<SpatialRepresentation> {
        /**
         * The reference system, or {@code null} if none.
         */
        private ReferenceSystem referenceSystem;

        /**
         * Creates an initially empty view for spatial representation information.
         */
        RepresentationInfo(final MetadataOverview owner) {
            super(owner);
            finished();
        }

        /**
         * Sets the spatial representation information from the given metadata.
         */
        @Override
        void setInformation(final Metadata metadata) {
            referenceSystem = null;
            setInformation(nonNull(metadata == null ? null : metadata.getSpatialRepresentationInfo()), SpatialRepresentation[]::new);
            if (metadata != null) {
                for (final ReferenceSystem rs : nonNull(metadata.getReferenceSystemInfo())) {
                    if (rs != null) {
                        referenceSystem = rs;
                        break;
                    }
                }
            }
        }

        /**
         * Invoked when new spatial representation information should be shown.
         * This method updates all fields in this section with the content of given information.
         * The information to show depends on the {@code info} subtype.
         */
        @Override
        void buildContent(final SpatialRepresentation info) {
            if (info instanceof GridSpatialRepresentation) {
                build((GridSpatialRepresentation) info);
            }
            addLine(Resources.Keys.ReferenceSystem, IdentifiedObjects.getName(referenceSystem, null));
        }

        /**
         * Adds information specific to the {@link GridSpatialRepresentation} subtype.
         *
         * @todo Change the representation for using a table instead. In addition of dimension name and size,
         *       the metadata may also provide a more meaningful title (e.g. "longitude"), a description and
         *       the resolution. Note that VectorSpatialRepresentation would also needs a table.
         */
        private void build(final GridSpatialRepresentation info) {
            int dimensionCount = 0;
            final StringBuffer buffer = new StringBuffer();
            for (final Dimension dim : nonNull(info.getAxisDimensionProperties())) {
                final String  name = owner.string(Types.getCodeTitle(dim.getDimensionName()));
                final Integer size = dim.getDimensionSize();
                if (size != null) {
                    owner.getNumberFormat().format(size, buffer, new FieldPosition(0));
                    if (name != null) buffer.append(' ');
                }
                if (name != null) {
                    buffer.append(name);
                } else if (size == null) {
                    buffer.append('?');
                }
                buffer.append(" × ");
                dimensionCount++;
            }
            if (dimensionCount != 0) {
                buffer.setLength(buffer.length() - 3);
                addLine(Resources.Keys.Dimensions, buffer.toString());
            }
            final Integer nd = info.getNumberOfDimensions();
            if (nd != null && nd != dimensionCount) {
                addLine(Resources.Keys.NumberOfDimensions, owner.getNumberFormat().format(dimensionCount));
            }
            addLine(Resources.Keys.CellGeometry, owner.string(Types.getCodeTitle(info.getCellGeometry())));
        }
    }

    /**
     * Returns all code lists in a comma-separated list.
     */
    private String string(final Collection<? extends ControlledVocabulary> codes) {
        final StringJoiner buffer = new StringJoiner(", ");
        for (final ControlledVocabulary c : codes) {
            final String text = string(Types.getCodeTitle(c));
            if (text != null) buffer.add(text);
        }
        return buffer.length() != 0 ? buffer.toString() : null;
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private String string(final InternationalString i18n) {
        if (i18n != null) {
            String t = i18n.toString(localized.getLocale());
            if (t != null && !(t = t.trim()).isEmpty()) {
                return t;
            }
        }
        return null;
    }
}
