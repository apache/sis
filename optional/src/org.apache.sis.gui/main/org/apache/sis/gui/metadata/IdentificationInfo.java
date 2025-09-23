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
package org.apache.sis.gui.metadata;

import java.util.Date;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.StringJoiner;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javax.measure.Unit;
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
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.InvalidMetadataException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Vocabulary;
import static org.apache.sis.util.internal.shared.CollectionsExt.nonNull;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * The pane where to show the values of {@link Identification} objects.
 * The same pane can be used for an arbitrary number of identifications.
 * Each instance is identified by its title.
 * The content is:
 *
 * <ol>
 *   <li>The title in bold font.</li>
 *   <li>Identifiers.</li>
 *   <li>Abstract, or purpose, or credit (in this preference order).</li>
 *   <li>Topic category.</li>
 *   <li>Release date, or publication date, or creation date, or any date (in this preference order).</li>
 *   <li>Type of resource.</li>
 *   <li>Resource format.</li>
 *   <li>Spatiotemporal extent as a textual description.</li>
 *   <li>Extent shown as a rectangle on a world map.</li>
 * </ol>
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class IdentificationInfo extends Section<Identification> {
    /**
     * Size of the map. Current version uses a size of 360×180° so that the scale factor
     * from the map to degrees is 1. Future version may use a different size, in which
     * case the scale factor will need to be added in the code.
     */
    private static final double MAP_WIDTH  = Longitude.MAX_VALUE - Longitude.MIN_VALUE,
                                MAP_HEIGHT =  Latitude.MAX_VALUE -  Latitude.MIN_VALUE;

    /**
     * Minimal size of rectangles to be drawn by {@link IdentificationInfo#drawOnMap(GeographicBoundingBox)}.
     * If a rectangle is smaller, it will be expanded to this size. We use a minimal size because otherwise
     * small rectangles may be practically invisible.
     */
    private static final double MIN_RECT_SIZE = 6;

    /**
     * The resource title, or if non the identifier as a fallback.
     */
    private final Label title;

    /**
     * The canvas where to draw geographic bounding boxes over a world map.
     * Shall never be null, but need to be recreated for each new map.
     * A canvas of size (0,0) is available at initialization time for drawing a new map.
     *
     * @see #isWorldMapEmpty()
     * @see #drawOnMap(GeographicBoundingBox)
     */
    private Canvas extentOnMap;

    /**
     * Whether the geographic bounding box covers the world.
     */
    private boolean isWorld;

    /**
     * Whether the map was visible with previous data, before {@link #buildContent(Identification)} call.
     * We use this information for avoiding flicker effect when a map is removed, then added back after a
     * slight delay by {@link #completeMissingGeographicBounds(Aggregate)}.
     */
    private boolean mapWasVisible;

    /**
     * The task which is running in background thread for searching bounding boxes in {@link Aggregate} children.
     * We use this reference for cancelling the task if a new resource is selected before the previous task finished.
     */
    private Task<?> aggregateWalker;

    /**
     * Creates an initially empty view for identification information.
     */
    IdentificationInfo(final MetadataSummary owner) {
        super(owner);
        title = new Label();
        title.setFont(Font.font(null, FontWeight.BOLD, 15));
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
        // Do not clear `isWorld` because caller may want the clear the map because it is world map.
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
     * If this pane has no geographic bounds information, search for geographic bounds in the child resources.
     * This method is used as a fallback when {@link #buildContent(Identification)} did not find bounding box
     * in the metadata directly provided. If bounds has been found, then this method does nothing.
     *
     * <p>The method does nothing if there is more than one {@link Identification} metadata element,
     * because we would not know to which element to assign the extent of children resources.</p>
     */
    final void completeMissingGeographicBounds(final Aggregate resource) {
        if (!isWorld && isWorldMapEmpty() && !super.isEmpty() && numPages() == 1) {
            /*
             * If a map was visible previously, add back an empty map for avoiding flicking effect.
             * If it appears that the map has no bounding box to show, it will be removed after the
             * background thread finished its work.
             */
            if (mapWasVisible) {
                drawMapBackground();
            }
            BackgroundThreads.execute(aggregateWalker = new Task<Set<GeographicBoundingBox>>() {
                /** Invoked in a background thread for fetching bounding boxes. */
                @Override protected Set<GeographicBoundingBox> call() throws DataStoreException {
                    final var boxes = new LinkedHashSet<GeographicBoundingBox>();
                    try {
                        for (final Resource child : resource.components()) {
                            final Metadata metadata = child.getMetadata();
                            if (isCancelled()) break;
                            if (metadata != null) {
                                for (final Identification id : nonNull(metadata.getIdentificationInfo())) {
                                    if (id != null) {
                                        for (final Extent extent : id.getExtents()) {
                                            final GeographicBoundingBox b = Extents.getGeographicBoundingBox(extent);
                                            if (b != null) boxes.add(b);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (BackingStoreException e) {
                        // This exception can be thrown by the iterator.
                        throw e.unwrapOrRethrow(DataStoreException.class);
                    }
                    return boxes;
                }

                /** Shows the result in JavaFX thread. */
                @Override protected void succeeded() {
                    aggregateWalker = null;
                    drawOnMap(getValue());
                }

                /** Invoked in JavaFX thread if metadata loading failed. */
                @Override protected void failed() {
                    aggregateWalker = null;
                    owner.setError(getException());
                }
            });
        }
    }

    /**
     * Sets the identification information from the given metadata.
     */
    @Override
    void setInformation(final Metadata metadata) {
        if (aggregateWalker != null) {
            aggregateWalker.cancel(BackgroundThreads.NO_INTERRUPT_DURING_IO);
            aggregateWalker = null;
        }
        final Collection<? extends Identification> info;
        if (metadata == null) {
            clearWorldMap();
            info = null;
        } else {
            info = metadata.getIdentificationInfo();
        }
        setInformation(nonNull(info), Identification[]::new);
    }

    /**
     * Invoked when new identification information should be shown.
     * This method updates all fields in this section with the content of given identification information.
     * The content is summarized in {@linkplain IdentificationInfo class javadoc}.
     */
    @Override
    void buildContent(final Identification info) {
        mapWasVisible = !isWorldMapEmpty();
        isWorld = false;
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
            text = owner.vocabulary.getString(Vocabulary.Keys.Untitled);
        } else if (CharSequences.isUnicodeIdentifier(text)) {
            text = CharSequences.camelCaseToSentence(text).toString();
        }
        title.setText(text);
        /*
         * Identifiers as a comma-separated list on a single line. Each identifier
         * is formatted as "codespace:code" or only "code" if there is no codespace.
         */
        if (citation != null) {
            final StringJoiner buffer = new StringJoiner(", ");
            for (final Identifier id : citation.getIdentifiers()) {
                buffer.add(IdentifiedObjects.toString(id));
            }
            if (buffer.length() != 0) {
                addLine(Vocabulary.Keys.Identifiers, buffer.toString());
            }
        }
        /*
         * The abstract, or if there is no abstract the purpose, or if no purpose the credit as a fallback.
         * We use those fallback because they can provide some hints about the product.
         * The topic category (climatology, health, etc.) follows.
         */
        short label = Vocabulary.Keys.Abstract;
        text = owner.string(info.getAbstract());
        if (text == null) {
            label = Vocabulary.Keys.Purpose;
            text = owner.string(info.getPurpose());
            if (text == null) {
                for (final String c : nonNull(info.getCredits())) {
                    text = c;
                    if (text != null) {
                        label = Vocabulary.Keys.Credit;
                        break;
                    }
                }
            }
        }
        addLine(label, text);
        /*
         * Topic category.
         */
        addLine(Vocabulary.Keys.TopicCategory, owner.string(nonNull(info.getTopicCategories())));
        /*
         * Type of resource: vector, grid, table, tin, video, etc. It gives a slight overview
         * of the next section, "Spatial representation". For that reason we put it close to
         * that next section, i.e. last in this section but just before the map.
         */
        addLine(Vocabulary.Keys.TypeOfResource, owner.string(nonNull(info.getSpatialRepresentationTypes())));
        /*
         * Resource format. Current implementation shows only the first format found.
         */
        for (final Format format : nonNull(info.getResourceFormats())) {
            final Citation c = format.getFormatSpecificationCitation();
            if (c != null) {
                text = owner.string(c.getTitle());
                if (text != null) {
                    addLine(Vocabulary.Keys.Format, text);
                    break;
                }
            }
        }
        /*
         * Select a single, arbitrary date. We take the release or publication date if available.
         * If no publication date is found, fallback on the creation date. If no creation date is
         * found neither, fallback on the first date regardless its type.
         */
        if (citation != null) {
            Date date = null;
            label = Vocabulary.Keys.Date;
            for (final CitationDate c : nonNull(citation.getDates())) {
                final Date cd = c.getDate();
                if (cd != null) {
                    final DateType type = c.getDateType();
                    if (type == DateType.PUBLICATION || type == DateType.RELEASED) {
                        label = Vocabulary.Keys.PublicationDate;
                        date  = cd;
                        break;                      // Take the first publication or release date.
                    }
                    final boolean isCreation = (type == DateType.CREATION);
                    if (date == null || isCreation) {
                        label = isCreation ? Vocabulary.Keys.CreationDate : Vocabulary.Keys.Date;
                        date  = cd;     // Fallback date: creation date, or the first date otherwise.
                    }
                }
            }
            addLine(label, owner.format(date));
        }
        /*
         * Fetch the first description about the spatio-temporal extent, then draw all geographic bounding boxes
         * on a world map. If the bounding box encompasses the whole world, replace it by a "World" description.
         * The reason is that drawing a box over the whole world is not very informative; it rather looks like a
         * border around the image.
         */
        text = null;
        Identifier identifier = null;
        Range<Date> timeRange = null;
        Range<Double> heights = null;
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
                try {
                    final MeasurementRange<Double> v = Extents.getVerticalRange(extent);
                    if (v != null) heights = (heights != null) ? heights.union(v) : v;
                } catch (InvalidMetadataException e) {
                    // `MetadataSummary` is (indirectly) the public caller of this method.
                    Logging.recoverableException(LOGGER, MetadataSummary.class, "setMetadata", e);
                }
                final Range<Date> t = Extents.getTimeRange(extent);
                if (t != null) timeRange = (timeRange != null) ? timeRange.union(t) : t;
            }
        }
        if (text == null) {
            text = IdentifiedObjects.toString(identifier);
        }
        if (isWorld) {
            clearWorldMap();
            if (text == null) {
                text = owner.vocabulary.getString(Vocabulary.Keys.World);
            }
        }
        /*
         * Write the temporal, vertical and geographic extents fetched above.
         */
        addLine(Vocabulary.Keys.Extent, text);
        if (timeRange != null) {
            label = Vocabulary.Keys.StartDate;
            Date t = timeRange.getMinValue();
            if (t == null) {
                t = timeRange.getMaxValue();
                label = Vocabulary.Keys.EndDate;
            }
            addLine(label, owner.format(t));
        }
        if (heights != null) {
            final Double min = heights.getMinValue();
            final Double max = heights.getMaxValue();
            if (min != null || max != null) {
                final var b = new StringBuffer(20);
                if (min != null && max != null && !min.equals(max)) {
                    owner.formats.formatPair(min, " … ", max, b);
                } else {
                    owner.format(min != null ? min : max, b);
                }
                if (heights instanceof MeasurementRange<?>) {
                    final Unit<?> unit = ((MeasurementRange<?>) heights).unit();
                    if (unit != null) {
                        owner.format(unit, b.append(' '));
                    }
                }
                addLine(Vocabulary.Keys.Height, b.toString());
            }
        }
        setRowIndex(extentOnMap, nextRowIndex());
    }

    /**
     * Draws all given geographic bounding boxes on the map.
     */
    private void drawOnMap(final Set<GeographicBoundingBox> boxes) {
        if (boxes.isEmpty()) {
            clearWorldMap();
            return;
        }
        for (final GeographicBoundingBox box : boxes) {
            isWorld = drawOnMap(box);
            if (isWorld) {
                clearWorldMap();
                return;
            }
        }
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
            double x = MAP_WIDTH  / 2 + west;
            double y = MAP_HEIGHT / 2 - north;
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
            final double wi = Math.min(w, MAP_WIDTH - x);               // Width of part inside [-180 … +180]°.
            w -= wi;                                                    // Width of part not drawn by `wi`.
            final boolean crossAntimeridian = (w > 0);
            /*
             * At this point we got the coordinates of the rectangle to draw, adjusted for making sure
             * that they are inside valid ranges. The `w` variable is usually 0, unless we had to cut
             * the rectangle in two parts because of anti-meridian crossing.
             */
            if (isWorldMapEmpty()) {
                drawMapBackground();
            }
            final GraphicsContext gc = extentOnMap.getGraphicsContext2D();
            gc.setStroke(Color.DARKBLUE);
            gc.setGlobalAlpha(0.1);
            gc.fillRect(x, y, wi, h);
            if (crossAntimeridian) {
                gc.fillRect(0, y, w, h);            // Second half of rectangle crossing anti-meridian.
            }
            gc.setGlobalAlpha(1.0);
            if (!crossAntimeridian) {
                gc.strokeRect(x, y, wi, h);
            } else {
                double xw = x + wi;
                double yh = y + h;
                gc.strokePolyline(new double[] {xw, x, x,  xw},
                                  new double[] {y,  y, yh, yh}, 4);
                gc.strokePolyline(new double[] {0, w, w,  0},
                                  new double[] {y, y, yh, yh}, 4);
            }
        }
        return false;
    }

    /**
     * Draws the map where bounding boxes will be overlay.
     */
    private boolean drawMapBackground() {
        final Image image = MetadataSummary.getWorldMap();
        if (image == null) {
            return false;
        }
        extentOnMap.setWidth (image.getWidth());
        extentOnMap.setHeight(image.getHeight());
        extentOnMap.getGraphicsContext2D().drawImage(image, 0, 0);
        return true;
    }
}
