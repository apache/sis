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
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Individual;
import org.opengis.metadata.citation.Organisation;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.iso.Types;


/**
 * A panel showing a summary of metadata.
 *
 * @author  Smaniotto Enzo
 * @version 1.1
 * @since   1.1
 * @module
 */
final class MetadataOverview {
    /**
     * Titles panes for different metadata sections (identification info, spatial information, <i>etc</i>).
     * This is similar to {@link javafx.scene.control.Accordion} except that we allow an arbitrary amount
     * of titled panes to be opened in same time.
     */
    private final VBox panes;

    /**
     * The locale to use for international strings.
     */
    private final Locale textLocale;

    /**
     * The locale to use for date/number formatters.
     */
    private final Locale formatLocale;

    /**
     * The metadata to show, or {@code null} if none.
     * This is set by {@link #setMetadata(Metadata)}.
     */
    private Metadata metadata;

    /**
     * If the metadata can not be obtained, the reason.
     */
    private Throwable failure;

    /**
     * Incremented every time that a new metadata needs to be shown.
     * This is used in case two calls to {@link #setMetadata(Resource)}
     * are run concurrently and do not finish in the order they were started.
     */
    private int selectionCounter;

    /**
     * Creates an initially empty metadata overview.
     */
    MetadataOverview(final Locale locale) {
        textLocale   = locale;
        formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        panes        = new VBox();
    }

    /**
     * Returns the region containing the visual components managed by this {@code MetadataOverview}.
     * The subclass is implementation dependent and may change in any future version.
     *
     * @return the region to show.
     */
    public final Region getView() {
        return panes;
    }

    /**
     * Fetches the metadata in a background thread and delegates to {@link #setMetadata(Metadata)} when ready.
     *
     * @param  resource  the resource for which to show metadata, or {@code null}.
     */
    public void setMetadata(final Resource resource) {
        assert Platform.isFxApplicationThread();
        if (resource == null) {
            setMetadata((Metadata) null);
        } else {
            final int sequence = ++selectionCounter;
            final class Getter extends Task<Metadata> {
                /** Invoked in a background thread for fetching metadata. */
                @Override protected Metadata call() throws DataStoreException {
                    return resource.getMetadata();
                }

                /** Shows the result, unless another {@link #setMetadata(Resource)} has been invoked. */
                @Override protected void succeeded() {
                    if (sequence == selectionCounter) {
                        setMetadata(getValue());
                    }
                }

                /** Invoked when an error occurred while fetching metadata. à*/
                @Override protected void failed() {
                    setMetadata((Metadata) null);
                    failure = getException();
                }
            }
            BackgroundThreads.execute(new Getter());
        }
    }

    /**
     * Sets the content of this pane to the given metadata.
     *
     * @param  md  the metadata to show, or {@code null}.
     */
    public void setMetadata(final Metadata md) {
        assert Platform.isFxApplicationThread();
        metadata = md;
        failure  = null;
        final ObservableList<Node> children = panes.getChildren();
        children.clear();
        if (md != null) {
            addIfNonEmpty(children, "Identification info",    createIdGridPane());
            addIfNonEmpty(children, "Spatial representation", createSpatialGridPane());
        }
    }

    /**
     * Adds the given pane to the list of children if the pane is non-null and non-empty.
     * If added, a {@link TitledPane} is created with the given title.
     */
    private static void addIfNonEmpty(final ObservableList<Node> children, final String title, final GridPane pane) {
        if (pane != null && !pane.getChildren().isEmpty()) {
            children.add(new TitledPane(title, pane));
        }
    }




    /**
     * The pane where to show the values of {@link Identification} objects.
     * The same pane can be used for an arbitrary amount of identifications.
     * Each instance is identified by its title.
     */
    private final class IdentificationInfo extends GridPane implements EventHandler<ActionEvent> {
        /**
         * The citation titles, one per {@link Identification} instance to show.
         * This combo box usually has only one element.
         */
        private final ComboBox<String> choices;

        /**
         * Identification of resources. Shall have the same number of elements than {@link #choices}.
         */
        private Identification[] identifications;

        /**
         * Creates an initially empty view for identification information.
         */
        IdentificationInfo() {
            setPadding(new Insets(10));
            setVgap(10);
            setHgap(10);
            choices = new ComboBox<>();
            choices.setOnAction(this);
            add(new Label("Title"), 0, 0); add(choices, 1, 0);
        }

        /**
         * Sets the identification information to show.
         * The given collection usually contains only one element.
         *
         * @param  info  identification information, or {@code null} if none.
         */
        void setInfo(Collection<? extends Identification> info) {
            if (info == null) {
                info = Collections.emptyList();
            }
            identifications = info.toArray(new Identification[info.size()]);
            /*
             * Setup the combo box with the title of all identification.
             * If no title is found, identifiers are used as fallback.
             */
            int firstWithTitle = -1;
            final String[] titles = new String[identifications.length];
            for (int i=0; i<titles.length; i++) {
                String title = null;
                final Identification id = identifications[i];
                if (id != null) {
                    final Citation citation = id.getCitation();
                    if (citation != null) {
                        title = string(citation.getTitle());
                        if (title == null) {
                            title = Citations.getIdentifier(citation);
                        }
                    }
                }
                if (title == null) {
                    title = Vocabulary.getResources(textLocale).getString(Vocabulary.Keys.Untitled);
                } else if (firstWithTitle < 0) {
                    firstWithTitle = i;
                }
                titles[i] = title;
            }
            /*
             * At this point we prepared all titles. If the titles were missing in all objects,
             * take the first "untitled" element as the initial selection.
             */
            choices.getItems().setAll(titles);
            if (titles.length != 0) {
                choices.getSelectionModel().clearAndSelect​(Math.max(firstWithTitle, 0));
            }
            handle(null);               // For forcing a refrech of pane content.
        }

        /**
         * Invoked when the user selected a new title.
         *
         * @param  event  ignored, can be null.
         */
        @Override
        public void handle(final ActionEvent event) {
            Identification id = null;
            if (identifications != null) {
                final int selected = choices.getSelectionModel().getSelectedIndex();
                if (selected >= 0 && selected < identifications.length) {
                    id = identifications[selected];
                }
            }
            onIdSelected(this, id);
        }
    }

    /**
     * The pane when to show the values of {@link Identification} objects.
     * The same pane can be used for an arbitrary amount of identifications.
     * Each instance is identified by its title.
     */
    private GridPane createIdGridPane() {
        final IdentificationInfo info = new IdentificationInfo();
        info.setInfo(metadata.getIdentificationInfo());

        // Show author information.
        Collection<? extends Responsibility> contacts = metadata.getContacts();
        if (false && !contacts.isEmpty()) {     // TODO
            Responsibility contact = contacts.iterator().next();
            Collection<? extends Party> parties = contact.getParties();
            if (!parties.isEmpty()) {
                Party party = parties.iterator().next();
                if (party.getName() != null) {
                    Label partyType = new Label("Party");
                    Label partyValue = new Label(party.getName().toString());
                    partyValue.setWrapText(true);
                    if (party instanceof Organisation) {
                        partyType.setText("Organisation");
                    } else if (party instanceof Individual) {
                        partyType.setText("Author");
                    }
                    info.add(partyType,  0, 1);
                    info.add(partyValue, 1, 2);
                }
            }
        }
        return info;
    }

    private void onIdSelected(final GridPane content, final Identification id) {
        if (id == null) return;
        final ObservableList<Node> children = content.getChildren();
        children.subList(2, children.size()).clear();   // Do not remove the 2 first elements, which are the combo box.

        int row = 1;

        // Show the abstract or the credit, the topic category, the creation date, the type of data, the representation system info and also the geographical area.
        Object ab = id.getAbstract();
        if (ab != null) {
            InternationalString abs = (InternationalString) ab;
            Label crd = new Label("Abstract");
            Label crdValue = new Label(string(abs));
            crdValue.setWrapText(true);
            content.add(crd,      0, row);
            content.add(crdValue, 1, row++);
        } else {
            Collection<? extends InternationalString> credits = id.getCredits();
            if (!credits.isEmpty()) {
                InternationalString credit = credits.iterator().next();
                Label crd = new Label("Credit");
                Label crdValue = new Label(credit.toString());
                crdValue.setWrapText(true);
                content.add(crd,      0, row);
                content.add(crdValue, 1, row++);
            }
        }

        Collection<TopicCategory> tcs = id.getTopicCategories();
        if (!tcs.isEmpty()) {
            TopicCategory tc = tcs.iterator().next();
            Label topicC = new Label("Topic Category");
            Label topicValue = new Label(tc.toString());
            topicValue.setWrapText(true);
            content.add(topicC,     0, row);
            content.add(topicValue, 1, row++);
        }

        if (!id.getCitation().getDates().isEmpty()) {
            CitationDate dateAndType = id.getCitation().getDates().iterator().next();
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, formatLocale);
            String dateStr = dateFormat.format(dateAndType.getDate());
            String s = dateAndType.getDateType().toString();
            s = s.replace("DateType[", "");
            s = s.replace("]", "");
            Label dt = new Label("Date type: " + s.toLowerCase());
            Label dtValue = new Label(dateStr);
            dtValue.setWrapText(true);
            content.add(dt,      0, row);
            content.add(dtValue, 1, row++);
        }

        if (id instanceof DataIdentification) {
            Label topicC = new Label("Object type");
            Label topicValue = new Label("Data");
            topicValue.setWrapText(true);
            content.add(topicC,     0, row);
            content.add(topicValue, 1, row++);
        } else {
            Label topicC = new Label("Object type");
            Label topicValue = new Label("Service");
            topicValue.setWrapText(true);
            content.add(topicC,     0, row);
            content.add(topicValue, 1, row++);
        }

        Collection<SpatialRepresentationType> spatialRepresentationTypes = id.getSpatialRepresentationTypes();
        Iterator<SpatialRepresentationType> its = spatialRepresentationTypes.iterator();
        String typeList = "Spatial representation type: ";
        while (its.hasNext()) {
            SpatialRepresentationType spatialRepresentationType = its.next();
            typeList += spatialRepresentationType.toString().toLowerCase(textLocale).replace("spatialrepresentationtype[", "").replace(']', '\0') + ", ";
        }
        if (!typeList.equals("Spatial representation type: ")) {
            Label list = new Label(typeList.substring(0, typeList.length() - 2));
            list.setWrapText(true);
            content.add(list, 0, 5, 2, 1);
        }

        Collection<? extends Extent> exs = id.getExtents();
        if (!exs.isEmpty()) {
            Extent ex = exs.iterator().next();
            Collection<? extends GeographicExtent> ges = ex.getGeographicElements();
            Iterator<? extends GeographicExtent> it = ges.iterator();
            while (it.hasNext()) {
                GeographicExtent ge = it.next();
                Label geoEx = new Label("Zone");
                Label geoExValue = new Label(ge.toString());
                geoExValue.setWrapText(true);
                if (ge instanceof GeographicBoundingBox) {
                    geoEx.setText("");
                    GeographicBoundingBox gbd = (GeographicBoundingBox) ge;
                    geoExValue.setText("");
                    Canvas c = createMap(gbd.getNorthBoundLatitude(), gbd.getEastBoundLongitude(), gbd.getSouthBoundLatitude(), gbd.getWestBoundLongitude());
                    if (c != null) {
                        content.add(c, 0, 6, 2, 1);
                    } else {
                        geoEx.setText("Impossible to load the map.");
                        content.add(geoEx,      0, row);
                        content.add(geoExValue, 1, row++);
                    }
                } else if (ge instanceof GeographicDescription) {
                    geoEx.setText("Geographic description");
                    GeographicDescription gd = (GeographicDescription) ge;
                    geoExValue.setText(gd.getGeographicIdentifier().getCode());
                }
            }
        }
    }

    private Canvas createMap(double north, double east, double south, double west) {
        Canvas can = new Canvas();
        Image image = null;
        try (InputStream in = MetadataOverview.class.getResourceAsStream("WorldMap360x180.png")) {
            image = new Image(in);
        } catch (IOException e) {
            // TODO
        }
        if (image.errorProperty().getValue()) {
            return null;
        }

        double height = image.getHeight();
        double width = image.getWidth();

        can.setHeight(height);
        can.setWidth(width);
        can.getGraphicsContext2D().drawImage(image, 0, 0, width, height);
        can.getGraphicsContext2D().setStroke(Color.DARKBLUE);
        can.getGraphicsContext2D().setGlobalAlpha(0.1);
        double x = west + width / 2, y = height / 2 - north, w = east - west, h = north - south;
        can.getGraphicsContext2D().strokeRect(x, y, w, h);
        final double minRectSize = 6.0;
        if (w < minRectSize) {
            double difX = minRectSize - w;
            x -= difX / 2;
            w = minRectSize;
        }
        if (h < minRectSize) {
            double difY = minRectSize - h;
            y -= difY / 2;
            h = minRectSize;
        }
        can.getGraphicsContext2D().fillRect(x, y, w, h);
        can.getGraphicsContext2D().setGlobalAlpha(1.0);
        can.getGraphicsContext2D().setStroke(Color.DARKBLUE);
        can.getGraphicsContext2D().strokeRect(x, y, w, h);

        return can;
    }

    private GridPane createSpatialGridPane() {
        GridPane gp = new GridPane();
        gp.setHgap(10.00);
        gp.setVgap(10.00);
        int j = 0, k = 1;

        Collection<? extends ReferenceSystem> referenceSystemInfos = metadata.getReferenceSystemInfo();
        if (!referenceSystemInfos.isEmpty()) {
            ReferenceSystem referenceSystemInfo = referenceSystemInfos.iterator().next();
            Label rsiValue = new Label("Reference system infos: " + referenceSystemInfo.getName().toString());
            rsiValue.setWrapText(true);
            gp.add(rsiValue, j, k++);
        }

        Collection<? extends SpatialRepresentation> sris = this.metadata.getSpatialRepresentationInfo();
        if (sris.isEmpty()) {
            return gp;
        }
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(formatLocale);
        for (SpatialRepresentation sri : sris) {
            String currentValue = "• ";
            if (sri instanceof GridSpatialRepresentation) {
                GridSpatialRepresentation sr = (GridSpatialRepresentation) sri;

                Iterator<? extends Dimension> it = sr.getAxisDimensionProperties().iterator();
                while (it.hasNext()) {
                    Dimension dim = it.next();
                    currentValue += numberFormat.format(dim.getDimensionSize()) + " " + Types.getCodeTitle(dim.getDimensionName()) + " * ";
                }
                currentValue = currentValue.substring(0, currentValue.length() - 3);
                Label spRep = new Label(currentValue);
                gp.add(spRep, j, k++, 2, 1);
                if (sr.getCellGeometry() != null) {
                    Label cellGeo = new Label("Cell geometry:");
                    Label cellGeoValue = new Label(Types.getCodeTitle(sr.getCellGeometry()).toString());
                    cellGeoValue.setWrapText(true);
                    gp.add(cellGeo, j, k);
                    gp.add(cellGeoValue, ++j, k++);
                    j = 0;
                }
            }
        }
        return gp;
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private String string(final InternationalString i18n) {
        if (i18n != null) {
            String t = i18n.toString(textLocale);
            if (t != null && !(t = t.trim()).isEmpty()) {
                return t;
            }
        }
        return null;
    }
}
