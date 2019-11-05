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
import javafx.geometry.HPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
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
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Vocabulary;

import static org.apache.sis.internal.util.CollectionsExt.nonNull;


/**
 * The pane where to show the values of {@link Identification} objects.
 * The same pane can be used for an arbitrary amount of identifications.
 * Each instance is identified by its title.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
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
     * A canvas of size (0,0) is available for drawing a new map.
     *
     * @see #isWorldMapEmpty()
     * @see #drawOnMap(GeographicBoundingBox)
     */
    private Canvas extentOnMap;

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
        } else if (CharSequences.isUnicodeIdentifier(text)) {
            text = CharSequences.camelCaseToSentence(text).toString();
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
            /*
             * At this point we got the coordinates of the rectangle to draw, adjusted for making sure
             * that they are inside valid ranges. The `w` variable is usually 0, unless we had to cut
             * the rectangle in two parts because of anti-meridian crossing.
             */
            if (isWorldMapEmpty()) {
                final Image image = MetadataSummary.getWorldMap();
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
                gc.fillRect(0, y, w, h);
            }
            gc.setGlobalAlpha(1.0);
            gc.strokeRect(x, y, wi, h);
            if (w > 0) {
                gc.strokeRect(0, y, w, h);
            }
        }
        return false;
    }
}
