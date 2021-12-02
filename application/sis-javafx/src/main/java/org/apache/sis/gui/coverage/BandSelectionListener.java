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

import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SelectionModel;


/**
 * Listener to selections in a table of sample dimensions. When a new row is selected,
 * the selection is forwarded to the {@link GridView#bandProperty}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class BandSelectionListener implements ChangeListener<Number> {
    /**
     * Applies a bidirectional binding between a property and the selection in a tabme of sample dimensions.
     *
     * @param  bandProperty   the property for currently selected band.
     * @param  bandSelection  the selection in a table of bands or sample dimensions.
     */
    static void bind(final IntegerProperty bandProperty, final SelectionModel<?> bandSelection) {
        bandSelection.selectedIndexProperty().addListener(new BandSelectionListener(bandProperty));
        bandProperty.addListener((p,o,n) -> bandSelection.clearAndSelect(n.intValue()));
    }

    /**
     * The {@link GridView#bandProperty} to update when a new band is selected.
     */
    private final IntegerProperty bandProperty;

    /**
     * Safety against recursive invocations, since a change in {@link #bandProperty}
     * may itself causes a change in the selected row.
     */
    private boolean isAdjusting;

    /**
     * Creates a new listener which will modify the given property.
     */
    private BandSelectionListener(final IntegerProperty bandProperty) {
        this.bandProperty = bandProperty;
    }

    /**
     * Invoked when the user selected a new row in the table of sample dimensions.
     */
    @Override
    public void changed(ObservableValue<? extends Number> property, Number oldValue, Number newValue) {
        final int row = newValue.intValue();
        if (row >= 0) {                         // Negative if table became empty after image became null.
            if (!isAdjusting) try {
                isAdjusting = true;
                bandProperty.set(row);
            } finally {
                isAdjusting = false;
            }
        }
    }
}
