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

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.apache.sis.internal.gui.Styles;


/**
 * Stores the code of a coordinate reference system (CRS) together with its name or description.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Code {
    /**
     * The CRS code. Usually defined by EPSG, but other authorities are allowed.
     * This is the value returned by {@link #toString()}.
     */
    final String code;

    /**
     * The CRS object description for the {@linkplain #code}.
     * In Apache SIS implementation of EPSG factory, this is the CRS name.
     */
    private ReadOnlyStringWrapper name;

    /**
     * Creates a code from the specified value.
     */
    Code(final String code) {
        this.code = code;
    }

    /**
     * Returns the property where to store the name or description of this authority code.
     */
    final ReadOnlyStringWrapper name() {
        if (name == null) {
            name = new ReadOnlyStringWrapper();
        }
        return name;
    }

    /**
     * Returns {@link #code}. This behavior is required for {@link CRSChooser} since it
     * will invoke {@link Object#toString()} directly for the column of authority codes.
     */
    @Override
    public String toString() {
        return code;
    }

    /*
     * Do not override equals(Object) and hashCode(). We rely on identity comparisons
     * when using this object as keys in HashMap.
     */

    /**
     * A cell displaying a code value.
     */
    static final class Cell extends TableCell<Code,Code> {
        /**
         * Creates a new cell for feature property value.
         *
         * @param  column  the column where the cell will be shown.
         */
        Cell(final TableColumn<Code,Code> column) {
            // Column not used at this time, but we need it in method signature.
            setAlignment(Pos.BASELINE_RIGHT);
            setTextFill(Styles.CODE_TEXT);
        }

        /**
         * Invoked when a new value needs to be show.
         *
         * @todo I didn't found how to get white text color when the row is selected.
         *       Current color (blue~gray on blue) is hard to read.
         */
        @Override
        protected void updateItem(final Code value, final boolean empty) {
            if (value == getItem()) return;
            super.updateItem(value, empty);
            String text = null;
            if (value != null) {
                text = value.toString();
            }
            setText(text);
        }
    }
}
