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

import java.util.Map;
import java.util.EnumMap;
import java.awt.image.RenderedImage;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListView;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.gui.map.ValuesUnderCursor;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Vocabulary;
import org.opengis.geometry.DirectPosition;


/**
 * Methods for configuring the {@link StatusBar} associated to a {@link CoverageCanvas}.
 * This is used for changing the {@link ValuesUnderCursor} instance used by the status bar
 * when the canvas shows an {@link ImageDerivative} instead than the original image.
 *
 * <p>The fields in this class could have been declared directly into {@link CoverageCanvas}.
 * But we keep this class separated because it is a workaround for the lack of good public API
 * for describing coverage operations. We may remove this class in a future Apache SIS version
 * if SIS provides a more complete coverage operation framework.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class StatusBarSupport {
    /**
     * The object to use for providing values under cursor. The default evaluator is the one created by
     * {@link ValuesUnderCursor#create(MapCanvas)}. If the image to display is the result of an operation,
     * then the default evaluator is temporarily replaced by an {@link ImageOperation}-specific evaluator.
     *
     * @see StatusBar#sampleValuesProvider
     */
    private final ObjectProperty<ValuesUnderCursor> selectedProvider;

    /**
     * The objects to use for providing values under cursor in image computed by an operation.
     * Evaluators are created when first needed and retained for {@link CoverageCanvas} lifetime.
     * We need to keep those instances after creation in order to preserve user settings in
     * {@link Evaluator#valueChoices} menu items.
     */
    private final Map<ImageOperation,ValuesUnderCursor> sampleValuesProviders;

    /**
     * The list of operations that can be applied on the image. Items may be added or removed in
     * this list depending on whether the operation is possible for current {@linkplain #image}.
     */
    private final ListView<ImageOperation> operations;

    /**
     * The image from which sample values will be read, or {@code null} if none. This reference is declared here
     * instead than in {@link #sampleValuesProviders} for having a single reference to update when image changes.
     * This is desired for avoiding memory leaks.
     */
    private RenderedImage image;

    /**
     * Creates a new support class for the given status bar.
     */
    StatusBarSupport(final StatusBar bar, final ListView<ImageOperation> operations) {
        this.operations = operations;
        selectedProvider = bar.sampleValuesProvider;
        sampleValuesProviders = new EnumMap<>(ImageOperation.class);
        sampleValuesProviders.put(ImageOperation.NONE, selectedProvider.get());
    }

    /**
     * Invoked after each rendering event for updating {@link StatusBar#sampleValuesProvider}
     * with an instance appropriate for the image shown.
     *
     * @param  operation  the operation applied on the image.
     * @param  data       the image resulting from the operation.
     */
    final void notifyImageShown(final ImageOperation operation, final RenderedImage data) {
        image = data;
        selectedProvider.set(sampleValuesProviders.computeIfAbsent(operation, (o) -> new Evaluator(o.fractionDigits)));
        ImageOperation.update(operations, data);
    }

    /**
     * Provides sample values for result of image operation.
     * Current implementation assumes a single-banded image with a fixed number of fraction digits.
     */
    private final class Evaluator extends ValuesUnderCursor {
        /**
         * The object to use for formatting numbers.
         */
        private final NumberFormat format;

        /**
         * Dummy object recycled for each evaluation.
         */
        private final FieldPosition pos;

        /**
         * Where to format the number.
         */
        private final StringBuffer buffer;

        /**
         * Array where to store sample values computed by {@link #evaluateAtPixel(double, double)}.
         * For performance reasons, the same array may be recycled on every method call.
         */
        private double[] values;

        /**
         * The exponent separator used in the format, or {@code null} if none.
         * This is a workaround for forcing a position sign in exponent
         * (there is no {@link DecimalFormat} API for doing that).
         */
        @Workaround(library="JDK", version="14")
        private final String exponentSeparator;

        /**
         * Creates a new evaluator for the given number of fraction digits.
         *
         * @param  fractionDigits  number of fraction digits.
         */
        Evaluator(final int fractionDigits) {
            buffer = new StringBuffer();
            pos    = new FieldPosition(0);
            format = NumberFormat.getNumberInstance();
            format.setMinimumFractionDigits(fractionDigits);
            format.setMaximumFractionDigits(fractionDigits);
            /*
             * Following menu items are hard-coded for now, may need to become configurable in a future version.
             */
            valueChoices.setText(Resources.format(Resources.Keys.PositionalErrors));
            if (format instanceof DecimalFormat) {
                final DecimalFormat decimal = (DecimalFormat) format;
                final String pattern = decimal.toPattern();
                final CheckMenuItem item = new CheckMenuItem(Vocabulary.format(Vocabulary.Keys.ScientificNotation));
                valueChoices.getItems().add(item);
                item.selectedProperty().addListener((p,o,n) -> {
                    decimal.applyPattern(n ? "0.000E00" : pattern);
                });
                exponentSeparator = decimal.getDecimalFormatSymbols().getExponentSeparator();
            } else {
                exponentSeparator = null;
            }
        }

        /**
         * Returns {@code false} since this evaluator is never empty.
         */
        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         * Returns {@code null} for telling the caller to invoke {@link #evaluateAtPixel(double, double)} instead.
         */
        @Override
        public String evaluate(final DirectPosition point) {
            return null;
        }

        /**
         * Returns a string representation of data under given pixel coordinates in the canvas.
         * Coordinates (0,0) are located in the upper-left canvas corner.
         *
         * @param  fx  0-based column index in the canvas. May be negative for coordinates out of bounds.
         * @param  fy  0-based row index in the canvas. May be negative for coordinates out of bounds.
         * @return string representation of data under given pixel.
         */
        @Override
        public String evaluateAtPixel(final double fx, final double fy) {
            final RenderedImage data = image;
            if (data != null) try {
                final int  x = Math.toIntExact(Math.round(fx));
                final int  y = Math.toIntExact(Math.round(fy));
                final int tx = ImageUtilities.pixelToTileX(data, x);
                final int ty = ImageUtilities.pixelToTileY(data, y);
                synchronized (buffer) {
                    buffer.setLength(0);
                    values = data.getTile(tx, ty).getPixel(x, y, values);
                    format.format(values[0], buffer, pos);
                    /*
                     * Insert a positive sign after exponent separator if needed.
                     * E.g. instead of "1.234E05" we want "1.234E+05".
                     * The intent is to have number of fixed length.
                     */
                    if (exponentSeparator != null) {
                        int i = buffer.lastIndexOf(exponentSeparator);
                        if (i >= 0) {
                            i += exponentSeparator.length();
                            if (i < buffer.length() && Character.isDigit(buffer.codePointAt(i))) {
                                buffer.insert(i, '+');
                            }
                        }
                    }
                    // Unit of measurement ("px") hard coded for now.
                    return buffer.append(" px").toString();
                }
            } catch (ArithmeticException | IllegalArgumentException | IndexOutOfBoundsException e) {
                // Position outside image. No value to show.
            }
            return null;
        }
    }
}
