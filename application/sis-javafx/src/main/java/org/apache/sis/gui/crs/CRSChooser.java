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
package org.apache.sis.gui.crs;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.apache.sis.internal.gui.JavaFxUtilities;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.crs.AbstractCRS;

/**
 * Widget configuration panel used to select a {@link CoordinateReferenceSystem}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class CRSChooser extends BorderPane {

    @FXML
    private CheckBox uiLongFirst;
    @FXML
    private CheckBox uiAxisConv;
    @FXML
    private BorderPane uiPane;
    @FXML
    private TextField uiSearch;
    @FXML
    private ChoiceBox<AxesConvention> uiChoice;

    private CRSTable uiTable;

    private final ObjectProperty<CoordinateReferenceSystem> crsProperty = new SimpleObjectProperty<>();
    private boolean updateText;

    /**
     * Create a new CRSChooser with no {@link CoordinateReferenceSystem} defined.
     */
    public CRSChooser() {
        JavaFxUtilities.loadJRXML(this,CRSChooser.class);

        uiSearch.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (updateText) return;
                uiTable.searchCRS(uiSearch.getText());
            }
        });

        uiTable = new CRSTable();
        uiPane.setCenter(uiTable);

        uiTable.crsProperty().bindBidirectional(crsProperty);

        crsProperty.addListener((ObservableValue<? extends CoordinateReferenceSystem> observable,
                              CoordinateReferenceSystem oldValue, CoordinateReferenceSystem newValue) -> {
            uiTable.crsProperty().set(newValue);
            if (newValue != null) {
                updateText = true;
                uiSearch.setText(newValue.getName().toString());
                updateText = false;
            }
        });

        uiChoice.setItems(FXCollections.observableArrayList(AxesConvention.values()));

    }

    public CoordinateReferenceSystem getCorrectedCRS(){
        CoordinateReferenceSystem crs = crsProperty.get();
        if (crs == null) return null;

        //fix longitude first
        try {
            Integer epsg = IdentifiedObjects.lookupEPSG(crs);
            if (epsg != null) {
                crs = CRS.forCode("EPSG:"+epsg);
                if (uiLongFirst.isSelected()) {
                    crs = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
                }
            }
        } catch (FactoryException ex) {/*no important*/}

        //fix axes convention
        if (uiAxisConv.isSelected() && crs instanceof DefaultGeographicCRS && uiChoice.getValue() != null) {
            crs = ((DefaultGeographicCRS) crs).forConvention(uiChoice.getValue());
        }

        return crs;
    }

    /**
     * Returns the property containing the edited {@link CoordinateReferenceSystem}.
     * This property can be modified and will send events.
     * It can be used with JavaFx binding operations.
     *
     * @return Property containing the edited {@link CoordinateReferenceSystem}
     */
    public ObjectProperty<CoordinateReferenceSystem> crsProperty(){
        return crsProperty;
    }

    /**
     * Show a modal dialog to select a {@link CoordinateReferenceSystem}.
     *
     * @param parent parent frame of widget.
     * @param crs {@link CoordinateReferenceSystem} to edit.
     * @return modified {@link CoordinateReferenceSystem}.
     */
    public static CoordinateReferenceSystem showDialog(Object parent, CoordinateReferenceSystem crs){
        final CRSChooser chooser = new CRSChooser();
        chooser.crsProperty.set(crs);
        final Alert alert = new Alert(Alert.AlertType.NONE);
        final DialogPane pane = alert.getDialogPane();
        pane.setContent(chooser);
        alert.getButtonTypes().setAll(ButtonType.OK,ButtonType.CANCEL);
        alert.setResizable(true);
        final ButtonType res = alert.showAndWait().orElse(ButtonType.CANCEL);
        return res == ButtonType.CANCEL ? null : chooser.getCorrectedCRS();
    }

}
