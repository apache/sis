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

import java.util.List;
import java.util.Arrays;
import java.util.EnumSet;
import java.awt.image.RenderedImage;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import org.apache.sis.image.ResampledImage;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.GUIUtilities;

import static org.apache.sis.image.ResampledImage.POSITIONAL_ERRORS_KEY;


/**
 * Predefined operations that can be applied on image.
 * The resulting images use the same coordinate system than the original image.
 *
 * <p>This class may be temporary. We need a better way to handle replacement of original image by result of image
 * operations. A difficulty is to recreate a full {@code GridCoverage} from an image, in particular for the result
 * of image derived from resampled image (because the original grid geometry is no longer valid).</p>
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
    NONE(Vocabulary.format(Vocabulary.Keys.None), 0),

    /**
     * Produces an image showing an estimation of positional error for each pixel.
     */
    POSITIONAL_ERROR(Resources.format(Resources.Keys.PositionalErrors), 2) {
        @Override final boolean isEnabled(final RenderedImage source) {
            return (source instanceof ResampledImage) && !((ResampledImage) source).isLinear();
        }

        @Override final RenderedImage apply(final RenderedImage source) {
            final Object value = source.getProperty(POSITIONAL_ERRORS_KEY);
            return (value instanceof RenderedImage) ? (RenderedImage) value : source;
        }
    };

    /**
     * The label to show in the list view.
     */
    private final String label;

    /**
     * Number of fraction digits to use when formatting sample values.
     * Ignored in the special case of {@link #NONE}.
     */
    final int fractionDigits;

    /**
     * Creates a new operation.
     */
    private ImageOperation(final String label, final int fractionDigits) {
        this.label = label;
        this.fractionDigits = fractionDigits;
    }

    /**
     * Updates the given list of operations with operations that are enabled for the given image.
     * Operations that are not enabled are removed from the list.
     *
     * @param  list   the list to update.
     * @param  image  the new image.
     */
    static void update(final ListView<ImageOperation> list, final RenderedImage image) {
        final EnumSet<ImageOperation> updated = EnumSet.allOf(ImageOperation.class);
        updated.removeIf((op) -> !op.isSourceEnabled(image));
        final MultipleSelectionModel<ImageOperation> selection = list.getSelectionModel();
        final boolean unselect = !updated.contains(selection.getSelectedItem());
        GUIUtilities.copyAsDiff(Arrays.asList(updated.toArray(new ImageOperation[updated.size()])), list.getItems());
        if (unselect) {
            selection.select(0);
        }
    }

    /**
     * Returns whether this operation can be applied for the given image.
     * The given image should be the same than the one given to {@link #apply(RenderedImage)}.
     */
    boolean isEnabled(final RenderedImage source) {
        return true;
    }

    /**
     * Returns whether this operation can be applied for the given image of a parent of this image.
     */
    private boolean isSourceEnabled(final RenderedImage source) {
        if (source != null) {
            if (isEnabled(source)) {
                return true;
            }
            final List<RenderedImage> sources = source.getSources();
            if (sources != null) {
                for (final RenderedImage parent : sources) {
                    if (isSourceEnabled(parent)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Applies the operation on given image. If a map projection or a zoom has been applied, then the given
     * image should be the resampled image (instead than the image read directly from the {@code DataStore}).
     * If the operation can not be applied, then {@code source} is returned as-is.
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
