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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Widget button used to select a {@link CoordinateReferenceSystem}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class CRSButton extends Button{

    private final ObjectProperty<CoordinateReferenceSystem> crsProperty = new SimpleObjectProperty<>();

    /**
     * Create a new CRSButton with no {@link CoordinateReferenceSystem} defined.
     */
    public CRSButton() {
        setText("-");

        setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                final CoordinateReferenceSystem crs = CRSChooser.showDialog(CRSButton.this, crsProperty.get());
                crsProperty.set(crs);
            }
        });

        //update button text when needed
        crsProperty.addListener((ObservableValue<? extends CoordinateReferenceSystem> observable,
                CoordinateReferenceSystem oldValue, CoordinateReferenceSystem newValue) -> {
            if (newValue!=null) {
                setText(newValue.getName().toString());
            } else {
                setText(" - ");
            }
        });
    }

    /**
     * Returns the property containing the edited {@link CoordinateReferenceSystem}.
     * This property can be modified and will send events.
     * It can be used with JavaFx binding operations.
     *
     * @return Property containing the edited {@link CoordinateReferenceSystem}
     */
    public ObjectProperty<CoordinateReferenceSystem> crsProperty() {
        return crsProperty;
    }

}
