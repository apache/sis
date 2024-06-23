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
package org.apache.sis.coverage.grid;

import org.opengis.metadata.spatial.PixelOrientation;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.ControlledVocabulary;


/**
 * Whether a "grid to real world" transform gives the coordinates of the cell corner or cell center.
 * This enumeration is equivalent to a subset of {@link PixelOrientation},
 * but applicable to any number of dimensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see PixelOrientation
 *
 * @since 1.5
 */
public enum PixelInCell implements ControlledVocabulary {
    /**
     * "Real world" coordinates give the location of the cell center.
     *
     * @see PixelOrientation#CENTER
     */
    CELL_CENTER("cell center", PixelOrientation.CENTER, 0),

    /**
     * "Real world" coordinates give the location of the cell corner having smallest coordinate values.
     * For a two-dimensional image having row indices (<var>y</var> coordinates) increasing downward,
     * this the upper-left corner.
     *
     * @see PixelOrientation#UPPER_LEFT
     */
    CELL_CORNER("cell corner", PixelOrientation.UPPER_LEFT, -0.5);

    /**
     * The identifier in legacy ISO 19111 specification.
     */
    private final String identifier;

    /**
     * The two-dimensional pixel orientation which is equivalent to this enumeration value.
     * This equivalence can be used for converting <var>n</var>-dimensional parameters to
     * the more specific two-dimensional case.
     *
     * <table class="sis">
     *   <caption>Pixel orientation equivalences</caption>
     *   <tr><th>Pixel in cell</th><th>Pixel orientation</th></tr>
     *   <tr><td>{@link #CELL_CENTER}</td><td>{@link PixelOrientation#CENTER}</td></tr>
     *   <tr><td>{@link #CELL_CORNER}</td><td>{@link PixelOrientation#UPPER_LEFT}</td></tr>
     * </table>
     *
     * @see PixelTranslation#getPixelOrientation(PixelInCell)
     */
    final PixelOrientation orientation;

    /**
     * The position relative to the cell center, in fractional number of cells.
     * This is typically used for <var>n</var>-dimensional grids, where the number of dimension is unknown.
     * The translation is determined from the following table, with the same value applied to all dimensions:
     *
     * <table class="sis">
     *   <caption>Translations</caption>
     *   <tr><th>Pixel in cell</th><th>offset</th></tr>
     *   <tr><td>{@link #CELL_CENTER}</td><td>{@code  0.0}</td></tr>
     *   <tr><td>{@link #CELL_CORNER}</td><td>{@code -0.5}</td></tr>
     * </table>
     *
     * @see PixelTranslation#getPixelTranslation(PixelInCell)
     */
    final double translationFromCentre;

    /**
     * Creates a new enumeration value.
     */
    private PixelInCell(final String identifier, final PixelOrientation orientation, final double translationFromCentre) {
        this.identifier = identifier;
        this.orientation = orientation;
        this.translationFromCentre = translationFromCentre;
    }

    /**
     * Returns the identifier declared in the legacy ISO 19111 specification.
     *
     * @return the legacy ISO/OGC identifier for this constant.
     */
    @Override
    public String identifier() {
        return identifier;
    }

    /**
     * Returns all the names of this enumeration value. The returned array contains
     * the {@linkplain #name() name} and the {@linkplain #identifier() identifier}.
     *
     * @return all names of this constant. This array is never null and never empty.
     */
    @Override
    public String[] names() {
        if (this == CELL_CENTER) {
            return new String[] {name(), identifier, "cell centre"};
        }
        return new String[] {name(), identifier};
    }

    /**
     * Returns the enumeration of the same kind as this item.
     * This is equivalent to {@link #values()}.
     *
     * @return the enumeration of the same kind as this item.
     */
    @Override
    public ControlledVocabulary[] family() {
        return values();
    }
}
