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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.Resources;


/**
 * The kind of range to use for scaling the color palette of an image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
enum RangeType {
    /**
     * As declared in the data store.
     * This is the default value.
     */
    DECLARED,

    /**
     * Computed from statistics (minimum and maximum values).
     */
    AUTOMATIC;

    /**
     * Creates the button for selecting a type of range.
     * The initial value is {@link #DECLARED}.
     */
    static ChoiceBox<RangeType> createButton(final ChangeListener<RangeType> listener) {
        final ChoiceBox<RangeType> button = new ChoiceBox<>();
        button.getItems().addAll(values());
        final SingleSelectionModel<RangeType> select = button.getSelectionModel();
        select.select(0);
        select.selectedItemProperty().addListener(listener);
        return button;
    }

    /**
     * Returns the string representation for this enumeration value.
     */
    @Override
    public String toString() {
        switch (this) {
            case DECLARED:  return Resources .format(Resources.Keys.FromMetadata);
            case AUTOMATIC: return Vocabulary.format(Vocabulary.Keys.Automatic);
            default:        return super.toString();
        }
    }
}
