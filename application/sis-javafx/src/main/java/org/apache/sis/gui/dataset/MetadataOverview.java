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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Party;
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
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation;
import org.apache.sis.util.iso.Types;


/**
 * Metadata Viewer.
 *
 * @author  Smaniotto Enzo
 * @version 1.1
 * @since   1.1
 * @module
 */
class MetadataOverview extends StackPane {

    private Metadata metadata;
    private final Locale locale = Locale.getDefault();

    public MetadataOverview() {
    }

    public void setMetadata(final DefaultMetadata md) {
        this.metadata = md;
        VBox root = new VBox();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #aeb7c4, #fafafa);");

        // Creation of the differents views.
        VBox summaryView = createSummaryView();
        MetadataNode advancedView = new MetadataNode(md.asTreeTable());
        advancedView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(advancedView, Priority.ALWAYS);

        // Create and configure view selection buttons.
        ToggleGroup buttonGroup = new ToggleGroup();
        ToggleButton tb1 = new ToggleButton("Summary");
        ToggleButton tb2 = new ToggleButton("Advanced");
        tb1.setStyle("-fx-text-fill: white; -fx-font-family: Arial Narrow;-fx-font-weight: bold; -fx-background-color: linear-gradient(#61a2b1, #2A5058); -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 ); -fx-padding: 0.8em;");
        tb2.setStyle("-fx-text-fill: white; -fx-font-family: Arial Narrow;-fx-font-weight: bold; -fx-background-color: linear-gradient(#61a2b1, #2A5058); -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 ); -fx-padding: 0.8em;");

        tb1.setToggleGroup(buttonGroup);
        tb1.setSelected(true);
        tb1.setDisable(true);
        tb2.setToggleGroup(buttonGroup);
        buttonGroup.selectToggle(tb1);
        buttonGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            if (tb2.isSelected()) {
                tb2.setDisable(true);
                root.getChildren().remove(summaryView);
                root.getChildren().add(advancedView);
                tb1.setDisable(false);
            } else {
                tb1.setDisable(true);
                root.getChildren().add(summaryView);
                root.getChildren().remove(advancedView);
                tb2.setDisable(false);
            }
        });

        HBox toggleGroupLayout = new HBox();
        toggleGroupLayout.getChildren().addAll(tb1, tb2);
        toggleGroupLayout.setPadding(new Insets(0, 0, 10, 0));

        root.getChildren().add(toggleGroupLayout);
        root.getChildren().add(summaryView);

        this.getChildren().setAll(root);
    }

    private VBox createSummaryView() {
        VBox vb = new VBox();
        TitledPane idPane = new TitledPane("Identification info", createIdGridPane());
        GridPane createSpatialGridPane = createSpatialGridPane();
        vb.getChildren().add(idPane);
        if (!createSpatialGridPane.getChildren().isEmpty()) {
            TitledPane spatialPane = new TitledPane("Spatial representation", createSpatialGridPane);
            vb.getChildren().add(spatialPane);
        }
        return vb;
    }

    private GridPane createIdGridPane() {
        GridPane gp = new GridPane();
        gp.setHgap(10.00);
        int j = 0, k = 1;

        HashMap<String, Identification> m = new HashMap<>();
        ComboBox<String> comboBox = createComboBox(m);
        comboBox.setStyle("-fx-font-weight: bold; -fx-font-size: 2em;");
        if (!comboBox.isVisible()) {
            Label la = new Label(comboBox.getValue());
            la.setStyle("-fx-font-weight: bold; -fx-font-size: 2em;");
            gp.add(la, j, k++, 2, 1);
        } else {
            gp.add(comboBox, j, k++, 2, 1);
        }

        // Show author information.
        Collection<? extends Responsibility> contacts = this.metadata.getContacts();
        if (!contacts.isEmpty()) {
            Responsibility contact = contacts.iterator().next();
            Collection<? extends Party> parties = contact.getParties();
            if (!parties.isEmpty()) {
                Party party = parties.iterator().next();
                if (party.getName() != null) {
                    Label partyType = new Label("Party");
                    Label partyValue = new Label(party.getName().toString());
                    partyValue.setWrapText(true);
                    if (party instanceof DefaultOrganisation) {
                        partyType.setText("Organisation");
                    } else if (party instanceof DefaultIndividual) {
                        partyType.setText("Author");
                    }
                    gp.add(partyType, j, k);
                    gp.add(partyValue, ++j, k++);
                    j = 0;
                }
            }
        }

        GridPane gpi = new GridPane();
        gpi.setHgap(10.00);

        comboBox.setOnAction(e -> {
            gpi.getChildren().clear();
            Identification id = m.get(comboBox.getValue());
            if (comboBox.getValue().equals("No data to show")) {
                return;
            }

            // Show the abstract or the credit, the topic category, the creation date, the type of data, the representation system info and also the geographical area.
            Object ab = id.getAbstract();
            if (ab != null) {
                InternationalString abs = (InternationalString) ab;
                Label crd = new Label("Abstract");
                Label crdValue = new Label(abs.toString(locale));
                crdValue.setWrapText(true);
                gpi.add(crd, 0, 1);
                gpi.add(crdValue, 1, 1);
            } else {
                Collection<? extends InternationalString> credits = id.getCredits();
                if (!credits.isEmpty()) {
                    InternationalString credit = credits.iterator().next();
                    Label crd = new Label("Credit");
                    Label crdValue = new Label(credit.toString());
                    crdValue.setWrapText(true);
                    gpi.add(crd, 0, 1);
                    gpi.add(crdValue, 1, 1);
                }
            }

            Collection<TopicCategory> tcs = id.getTopicCategories();
            if (!tcs.isEmpty()) {
                TopicCategory tc = tcs.iterator().next();
                Label topicC = new Label("Topic Category");
                Label topicValue = new Label(tc.toString());
                topicValue.setWrapText(true);
                gpi.add(topicC, 0, 2);
                gpi.add(topicValue, 1, 2);
            }

            if (!id.getCitation().getDates().isEmpty()) {
                CitationDate dateAndType = id.getCitation().getDates().iterator().next();
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, locale);
                String dateStr = dateFormat.format(dateAndType.getDate());
                String s = dateAndType.getDateType().toString();
                s = s.replace("DateType[", "");
                s = s.replace("]", "");
                Label dt = new Label("Date type: " + s.toLowerCase());
                Label dtValue = new Label(dateStr);
                dtValue.setWrapText(true);
                gpi.add(dt, 0, 3);
                gpi.add(dtValue, 1, 3);
            }

            if (id instanceof DataIdentification) {
                Label topicC = new Label("Object type");
                Label topicValue = new Label("Data");
                topicValue.setWrapText(true);
                gpi.add(topicC, 0, 4);
                gpi.add(topicValue, 1, 4);
            } else {
                Label topicC = new Label("Object type");
                Label topicValue = new Label("Service");
                topicValue.setWrapText(true);
                gpi.add(topicC, 0, 4);
                gpi.add(topicValue, 1, 4);
            }

            Collection<SpatialRepresentationType> spatialRepresentationTypes = id.getSpatialRepresentationTypes();
            Iterator<SpatialRepresentationType> its = spatialRepresentationTypes.iterator();
            String typeList = "Spatial representation type: ";
            while (its.hasNext()) {
                SpatialRepresentationType spatialRepresentationType = its.next();
                typeList += spatialRepresentationType.toString().toLowerCase(locale).replace("spatialrepresentationtype[", "").replace(']', '\0') + ", ";
            }
            if (!typeList.equals("Spatial representation type: ")) {
                Label list = new Label(typeList.substring(0, typeList.length() - 2));
                list.setWrapText(true);
                gpi.add(list, 0, 5, 2, 1);
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
                            gpi.add(c, 0, 6, 2, 1);
                        } else {
                            geoEx.setText("Impossible to load the map.");
                            gpi.add(geoEx, 0, 6);
                            gpi.add(geoExValue, 1, 6);
                        }
                    } else if (ge instanceof GeographicDescription) {
                        geoEx.setText("Geographic description");
                        GeographicDescription gd = (GeographicDescription) ge;
                        geoExValue.setText(gd.getGeographicIdentifier().getCode());
                    }
                }
            }
        });

        Event.fireEvent(comboBox, new ActionEvent());
        gp.add(gpi, j, k++, 2, 1);

        int ind = 0;
        for (Node n : gp.getChildren()) {
            if (ind++ != 0) {
                n.setStyle("-fx-padding: 0 83 10 0;");
            } else {
                n.setStyle("-fx-padding: 0 0 10 0; -fx-font-weight: bold; -fx-font-size: 2em;");
            }
        }
        gpi.getChildren().forEach(n -> n.setStyle("-fx-padding: 0 0 10 0;"));

        return gp;
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
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        for (SpatialRepresentation sri : sris) {
            String currentValue = "• ";
            if (sri instanceof DefaultGridSpatialRepresentation) {
                DefaultGridSpatialRepresentation sr = (DefaultGridSpatialRepresentation) sri;

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

    private ComboBox<String> createComboBox(final Map<String, Identification> m) {
        ComboBox<String> cb = new ComboBox<>();
        Collection<? extends Identification> ids = this.metadata.getIdentificationInfo();
        ObservableList<String> options = FXCollections.observableArrayList();
        int i = 1;
        if (ids.size() > 1) {
            for (Identification id : ids) {
                String currentName;
                if (id.getCitation() != null) {
                    currentName = id.getCitation().getTitle().toString();
                } else {
                    currentName = Integer.toString(i);
                }
                options.add(currentName);
                m.put(currentName, id);
            }
            cb.setItems(options);
            cb.setValue(ids.iterator().next().getCitation().getTitle().toString());
        } else if (ids.size() == 1) {
            if (ids.iterator().next().getCitation() != null) {
                m.put(ids.iterator().next().getCitation().getTitle().toString(), ids.iterator().next());
                cb.setValue(ids.iterator().next().getCitation().getTitle().toString());
                cb.setVisible(false);
            }
        } else {
            cb.setValue("No data to show");
            cb.setVisible(false);
        }
        return cb;
    }
}
