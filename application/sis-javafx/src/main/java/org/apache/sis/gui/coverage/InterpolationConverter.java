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

import java.util.Locale;
import java.util.Objects;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import org.apache.sis.image.Interpolation;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Gives a localized {@link String} instance for a given {@link Interpolation} and conversely.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
final class InterpolationConverter extends StringConverter<Interpolation> {
    /**
     * Creates the controls for choosing an interpolation method on the given canvas.
     *
     * @param  view  the canvas for which to create a button for selecting the interpolation method.
     * @return a button for applying an interpolation method on the given view.
     */
    static ChoiceBox<Interpolation> button(final CoverageCanvas view) {
        final ChoiceBox<Interpolation> b = new ChoiceBox<>();
        b.setConverter(new InterpolationConverter(view.getLocale()));
        b.getItems().setAll(INTERPOLATIONS);
        b.getSelectionModel().select(view.getInterpolation());
        view.interpolationProperty.bind(b.getSelectionModel().selectedItemProperty());
        return b;
    }

    /**
     * The interpolation supported by this converter.
     */
    private static final Interpolation[] INTERPOLATIONS = {
        Interpolation.NEAREST, Interpolation.BILINEAR, Interpolation.LANCZOS
    };

    /**
     * Keys of localized names for each {@link #INTERPOLATIONS} element.
     */
    private static final short[] VOCABULARIES = {
        Vocabulary.Keys.NearestNeighbor, Vocabulary.Keys.Bilinear, 0
    };

    /**
     * The locale to use for string representation.
     */
    private final Locale locale;

    /**
     * Creates a new converter for the given locale.
     */
    private InterpolationConverter(final Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns a string representation of the given item.
     */
    @Override
    public String toString(final Interpolation item) {
        for (int i=0; i<INTERPOLATIONS.length; i++) {
            if (INTERPOLATIONS[i].equals(item)) {
                final short key = VOCABULARIES[i];
                if (key != 0) {
                    return Vocabulary.getResources(locale).getString(key);
                } else if (item == Interpolation.LANCZOS) {
                    return "Lanczos";
                }
            }
        }
        return Objects.toString(item);
    }

    /**
     * Returns the interpolation for the given text.
     */
    @Override
    public Interpolation fromString(final String text) {
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        for (int i=0; i<VOCABULARIES.length; i++) {
            final short key = VOCABULARIES[i];
            final Interpolation item = INTERPOLATIONS[i];
            if ((key != 0 && vocabulary.getString(key).equalsIgnoreCase(text))
                                    || item.toString().equalsIgnoreCase(text))
            {
                return item;
            }
        }
        return null;
    }
}
