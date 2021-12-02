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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TitledPane;


/**
 * Invoked the first time that the "Properties" pane is opened for building the JavaFX visual components.
 * We deffer the creation of this pane because it is often not requested at all, since this is more for
 * developers than users.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class PropertyPaneCreator implements ChangeListener<Boolean> {
    /**
     * A copy of {@link CoverageControls#view} reference.
     */
    private final CoverageCanvas view;

    /**
     * The pane where to set the content.
     */
    private final TitledPane pane;

    /**
     * Creates a new {@link ImagePropertyExplorer} constructor.
     */
    PropertyPaneCreator(final CoverageCanvas view, final TitledPane pane) {
        this.view = view;
        this.pane = pane;
    }

    /**
     * Creates the {@link ImagePropertyExplorer} when {@link TitledPane#expandedProperty()} changed.
     */
    @Override
    public void changed(ObservableValue<? extends Boolean> property, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            pane.expandedProperty().removeListener(this);
            final ImagePropertyExplorer properties = view.createPropertyExplorer();
            properties.updateOnChange.bind(pane.expandedProperty());
            pane.setContent(properties.getView());
        }
    }
}
