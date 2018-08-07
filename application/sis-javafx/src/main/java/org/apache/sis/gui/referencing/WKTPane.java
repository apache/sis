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
package org.apache.sis.gui.referencing;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.FormattableObject;


/**
 * Small panel to display an object as WKT in various conventions.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class WKTPane extends BorderPane {

    private final ChoiceBox<Convention> choice = new ChoiceBox<>(FXCollections.observableArrayList(Convention.values()));
    private final TextArea text = new TextArea();

    public WKTPane(final FormattableObject obj) {
        setTop(choice);
        setCenter(text);

        choice.valueProperty().addListener(new ChangeListener<Convention>() {
            @Override
            public void changed(ObservableValue<? extends Convention> observable, Convention oldValue, Convention newValue) {
                text.setText(obj.toString(newValue));
            }
        });
        choice.getSelectionModel().select(Convention.WKT1);
    }

    public static void showDialog(Object parent, FormattableObject candidate){
        final WKTPane chooser = new WKTPane(candidate);

        final Alert alert = new Alert(Alert.AlertType.NONE);
        final DialogPane pane = alert.getDialogPane();
        pane.setContent(chooser);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.setResizable(true);
        alert.showAndWait();
    }
}
