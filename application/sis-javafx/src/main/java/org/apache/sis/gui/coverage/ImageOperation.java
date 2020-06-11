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

import java.awt.image.RenderedImage;
import javafx.scene.control.ListView;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.MultipleSelectionModel;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;

import static org.apache.sis.image.ResampledImage.POSITIONAL_ERRORS_KEY;


/**
 * Predefined operations that can be applied on image.
 * The resulting images use the same coordinate system than the original image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
enum ImageOperation {
    /**
     * No operation applied.
     */
    NONE(Vocabulary.format(Vocabulary.Keys.None)),

    /**
     * Produces an image showing an estimation of positional error for each pixel.
     */
    POSITIONAL_ERROR(Resources.format(Resources.Keys.PositionalErrors)) {
        @Override final RenderedImage apply(final RenderedImage source) {
            final Object value = source.getProperty(POSITIONAL_ERRORS_KEY);
            return (value instanceof RenderedImage) ? (RenderedImage) value : null;
        }
    };

    /**
     * The label to show in menu.
     */
    private final String label;

    /**
     * Creates a new operation.
     */
    private ImageOperation(final String label) {
        this.label = label;
    }

    /**
     * Creates the widget to be shown in {@link CoverageControls} for selecting an operation.
     */
    static ListView<ImageOperation> list(final ChangeListener<ImageOperation> listener) {
        final ListView<ImageOperation> list = new ListView<>();
        list.getItems().setAll(values());
        final MultipleSelectionModel<ImageOperation> select = list.getSelectionModel();
        select.select(0);
        select.selectedItemProperty().addListener(listener);
        return list;
    }

    /**
     * Applies the operation on given image.
     */
    RenderedImage apply(final RenderedImage source) {
        return source;
    }

    /**
     * Returns the label to show in menu.
     */
    @Override
    public String toString() {
        return label;
    }
}
