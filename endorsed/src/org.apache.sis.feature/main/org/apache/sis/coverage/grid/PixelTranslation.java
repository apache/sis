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

import java.util.Map;
import java.io.Serializable;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.metadata.spatial.PixelOrientation;
import static org.opengis.metadata.spatial.PixelOrientation.*;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * The translation to apply for different values of {@link PixelOrientation} or {@link PixelInCell}.
 * The translation are returned by a call to one of the following static methods:
 *
 * <ul>
 *   <li>{@link #getPixelTranslation(PixelOrientation)} for the two-dimensional case.</li>
 *   <li>{@link #getPixelTranslation(PixelInCell)} for the <var>n</var>-dimensional case.</li>
 * </ul>
 *
 * This class provides also a few {@code translate(…)} convenience methods,
 * which apply the translation on a given {@link MathTransform} instance.
 *
 * <h2>Example</h2>
 * In the following code snippet, {@code gridToCRS} is an {@link java.awt.geom.AffineTransform} from
 * <i>grid cell</i> coordinates (typically pixel coordinates) to some arbitrary CRS coordinates.
 * In this example, the transform maps pixels {@linkplain PixelOrientation#CENTER center},
 * while the {@linkplain PixelOrientation#UPPER_LEFT upper left} corner is desired.
 * This code will switch the affine transform from the <i>pixel center</i> to
 * <i>upper left corner</i> convention:
 *
 * {@snippet lang="java" :
 *     public AffineTransform getGridToPixelCorner() {
 *         AffineTransform  gridToCRS = ...;
 *         PixelOrientation current   = PixelOrientation.CENTER;
 *         PixelOrientation desired   = PixelOrientation.UPPER_LEFT;
 *
 *         // Switch the transform from 'current' to 'desired' convention.
 *         PixelTranslation source = getPixelTranslation(current);
 *         PixelTranslation target = getPixelTranslation(desired);
 *         return gridToCRS.translate(target.dx - source.dx,
 *                                    target.dy - source.dy);
 *     }
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see PixelInCell
 * @see PixelOrientation
 *
 * @since 1.0
 */
public final class PixelTranslation implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5671620211497720808L;

    /**
     * Math transforms created by {@link #translate(MathTransform, PixelInCell, PixelInCell)}
     * for dimensions 1 to 6. Each element in this array will be created when first needed.
     * Even indices are translations by -0.5 while odd indices are translations by +0.5.
     */
    private static final MathTransform[] translations = new MathTransform[10];

    /**
     * The pixel orientation for this translation.
     * Most common values are {@link PixelOrientation#UPPER_LEFT} and {@link PixelOrientation#CENTER}.
     */
    public final PixelOrientation orientation;

    /**
     * The translation among the <var>x</var> axis relative to pixel center.
     * The value is typically −½, 0 or +½.
     */
    public final double dx;

    /**
     * The translation among the <var>y</var> axis relative to pixel center.
     * The value is typically −½, 0 or +½.
     */
    public final double dy;

    /**
     * The offset for various pixel orientations. Keys must be upper-case names.
     */
    private static final Map<PixelOrientation, PixelTranslation> ORIENTATIONS = Map.ofEntries(
            entry(CENTER,       0.0,  0.0),
            entry(UPPER_LEFT,  -0.5, -0.5),
            entry(UPPER_RIGHT,  0.5, -0.5),
            entry(LOWER_LEFT,  -0.5,  0.5),
            entry(LOWER_RIGHT,  0.5,  0.5));

    /** For {@link #ORIENTATIONS} construction only. */
    private static Map.Entry<PixelOrientation, PixelTranslation> entry(
                    PixelOrientation orientation, double dx, double dy)
    {
        return Map.entry(orientation, new PixelTranslation(orientation, dx, dy));
    }

    /**
     * Creates a new pixel translation.
     */
    private PixelTranslation(final PixelOrientation orientation, final double dx, final double dy) {
        this.orientation = orientation;
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Returns the pixel orientation which is equivalent to the given {@code PixelInCell} code.
     * This equivalence can be used for converting <var>n</var>-dimensional parameters to the
     * more specific two-dimensional case. This method implements the following mapping:
     *
     * <table class="sis">
     *   <caption>Pixel orientation equivalences</caption>
     *   <tr><th>Pixel in cell</th><th>Pixel orientation</th></tr>
     *   <tr><td>{@link PixelInCell#CELL_CENTER  CELL_CENTER}</td><td>{@link PixelOrientation#CENTER      CENTER}</td></tr>
     *   <tr><td>{@link PixelInCell#CELL_CORNER  CELL_CORNER}</td><td>{@link PixelOrientation#UPPER_LEFT  UPPER_LEFT}</td></tr>
     *   <tr><td>{@code null}</td><td>{@code null}</td></tr>
     * </table>
     *
     * @param  anchor  the {@code PixelInCell} code, or {@code null}.
     * @return the corresponding pixel orientation, or {@code null} if the argument was null.
     */
    public static PixelOrientation getPixelOrientation(final PixelInCell anchor) {
        return (anchor == null) ? null : anchor.orientation;
    }

    /**
     * Returns the position relative to the cell center.
     * This method is typically used for <var>n</var>-dimensional grids, where the number of dimension is unknown.
     * The translation is determined from the following table, with the same value applied to all dimensions:
     *
     * <table class="sis">
     *   <caption>Translations</caption>
     *   <tr><th>Pixel in cell</th><th>offset</th></tr>
     *   <tr><td>{@link PixelInCell#CELL_CENTER  CELL_CENTER}</td><td>{@code  0.0}</td></tr>
     *   <tr><td>{@link PixelInCell#CELL_CORNER  CELL_CORNER}</td><td>{@code -0.5}</td></tr>
     * </table>
     *
     * @param  anchor  the "pixel in cell" value.
     * @return the translation for the given "pixel in cell" value.
     */
    public static double getPixelTranslation(final PixelInCell anchor) {
        return anchor.translationFromCentre;
    }

    /**
     * Returns the specified position relative to the pixel center.
     * This method can be used for grid restricted to 2 dimensions.
     * The translation vector is determined from the following table:
     *
     * <table class="sis">
     *   <caption>Translations</caption>
     *   <tr><th>Pixel orientation</th>                               <th> dx </th><th> dy </th></tr>
     *   <tr><td>{@link PixelOrientation#CENTER      CENTER}</td>     <td>{@code  0.0}</td><td>{@code  0.0}</td></tr>
     *   <tr><td>{@link PixelOrientation#UPPER_LEFT  UPPER_LEFT}</td> <td>{@code -0.5}</td><td>{@code -0.5}</td></tr>
     *   <tr><td>{@link PixelOrientation#UPPER_RIGHT UPPER_RIGHT}</td><td>{@code +0.5}</td><td>{@code -0.5}</td></tr>
     *   <tr><td>{@link PixelOrientation#LOWER_LEFT  LOWER_LEFT}</td> <td>{@code -0.5}</td><td>{@code +0.5}</td></tr>
     *   <tr><td>{@link PixelOrientation#LOWER_RIGHT LOWER_RIGHT}</td><td>{@code +0.5}</td><td>{@code +0.5}</td></tr>
     * </table>
     *
     * @param  anchor  the pixel orientation.
     * @return the position relative to the pixel center.
     * @throws IllegalArgumentException if the given {@code anchor} is not a known code list value.
     */
    public static PixelTranslation getPixelTranslation(final PixelOrientation anchor) {
        final PixelTranslation offset = ORIENTATIONS.get(anchor);
        if (offset == null) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "anchor", anchor));
        }
        return offset;
    }

    /**
     * Converts a math transform from a "pixel in cell" convention to another "pixel in cell" convention.
     * The given transform is from the grid coordinates of a {@link GridGeometry} to the grid coordinates
     * of another {@code GridGeometry}.
     *
     * @param  gridToGrid  the transform from a grid geometry to another grid geometry.
     * @param  current     the pixel in cell convention of the given {@code gridToCRS} transform.
     * @param  desired     the pixel in cell convention of the desired transform.
     * @return the "grid to grid" transform using the new convention, or {@code null} if {@code gridToGrid} was null.
     */
    static MathTransform translateGridToGrid(MathTransform gridToGrid, final PixelInCell current, final PixelInCell desired) {
        final double offset = desired.translationFromCentre - current.translationFromCentre;
        if (offset != 0) {
            gridToGrid = MathTransforms.concatenate(
                    MathTransforms.uniformTranslation(gridToGrid.getSourceDimensions(),  offset), gridToGrid,
                    MathTransforms.uniformTranslation(gridToGrid.getTargetDimensions(), -offset));
        }
        return gridToGrid;
    }

    /**
     * Converts a math transform from a "pixel in cell" convention to another "pixel in cell" convention.
     * This method concatenates −½, 0 or +½ translations on <em>all</em> dimensions before the given transform.
     * If the two given conventions are the same, then this method returns the given transform unchanged.
     *
     * <p>If the given {@code gridToCRS} is null, then this method ignores all other arguments and returns {@code null}.
     * Otherwise {@code current} and {@code desired} arguments must be non-null.</p>
     *
     * <h4>Example</h4>
     * If a given {@code gridToCRS} transform was mapping the <em>cell corner</em> to "real world" coordinates, then a call to
     * <code>translate(gridToCRS, {@link PixelInCell#CELL_CORNER CELL_CORNER}, {@link PixelInCell#CELL_CENTER CELL_CENTER})</code>
     * will return a new transform performing the following steps: first convert grid coordinates from <var>cell center</var>
     * convention ({@code desired}) to <var>cell corner</var> convention ({@code current}), then concatenate the given
     * {@code gridToCRS} transform which was designed for the <em>cell corner</em> convention.
     * The above-cited <var>cell center</var> → <var>cell corner</var> conversion is done by translating the grid coordinates
     * by +½, because the grid coordinates (0,0) relative to cell center is (½,½) relative to cell corner.
     *
     * @param  gridToCRS  a math transform from pixel coordinates to any CRS, or {@code null}.
     * @param  current    the pixel in cell convention of the given {@code gridToCRS} transform.
     * @param  desired    the pixel in cell convention of the desired transform.
     * @return the "grid to CRS" transform using the new convention, or {@code null} if {@code gridToCRS} was null.
     * @throws IllegalArgumentException if {@code current} or {@code desired} is not a known code list value.
     */
    public static MathTransform translate(final MathTransform gridToCRS, final PixelInCell current, final PixelInCell desired) {
        if (gridToCRS == null || desired == current) {
            return gridToCRS;
        }
        final int dimension = gridToCRS.getSourceDimensions();
        final double offset = desired.translationFromCentre - current.translationFromCentre;
        final int ci;               // Cache index.
        if (offset == -0.5) {
            ci = 2*dimension - 2;
        } else if (offset == 0.5) {
            ci = 2*dimension - 1;
        } else {
            ci = -1;
        }
        MathTransform mt;
        if (ci < 0 || ci >= translations.length) {
            mt = MathTransforms.uniformTranslation(dimension, offset);
        } else synchronized (translations) {
            mt = translations[ci];
            if (mt == null) {
                mt = MathTransforms.uniformTranslation(dimension, offset);
                translations[ci] = mt;
            }
        }
        return MathTransforms.concatenate(mt, gridToCRS);
    }

    /**
     * Converts a math transform from a "pixel orientation" convention to another "pixel orientation" convention.
     * This method concatenates −½, 0 or +½ translations on <em>two</em> dimensions before the given transform.
     * The given transform can have any number of input and output dimensions, but only two of them will be converted.
     *
     * <p>If the given {@code gridToCRS} is null, then this method ignores all other arguments and returns {@code null}.
     * Otherwise {@code current} and {@code desired} arguments must be non-null.</p>
     *
     * <h4>Example</h4>
     * If a given {@code gridToCRS} transform was mapping the upper-left corner to "real world" coordinates, then a call to
     * <code>translate(gridToCRS, {@link PixelOrientation#UPPER_LEFT UPPER_LEFT}, {@link PixelOrientation#CENTER CENTER}, 0, 1)</code>
     * will return a new transform translating grid coordinates by +0.5 before to apply the given {@code gridToCRS} transform.
     * See example in above {@link #translate(MathTransform, PixelInCell, PixelInCell) translate} method for more details.
     *
     * @param  gridToCRS   a math transform from pixel coordinates to any CRS, or {@code null}.
     * @param  current     the pixel orientation of the given {@code gridToCRS} transform.
     * @param  desired     the pixel orientation of the desired transform.
     * @param  xDimension  the dimension of <var>x</var> coordinates (pixel columns). Often 0.
     * @param  yDimension  the dimension of <var>y</var> coordinates (pixel rows). Often 1.
     * @return the "grid to CRS" transform using the new convention, or {@code null} if {@code gridToCRS} was null.
     * @throws IllegalArgumentException if {@code current} or {@code desired} is not a known code list value.
     */
    public static MathTransform translate(final MathTransform gridToCRS,
            final PixelOrientation current, final PixelOrientation desired,
            final int xDimension, final int yDimension)
    {
        if (gridToCRS == null || desired.equals(current)) {
            return gridToCRS;
        }
        final int dimension = gridToCRS.getSourceDimensions();
        if (xDimension < 0 || xDimension >= dimension) {
            throw illegalDimension("xDimension", xDimension);
        }
        if (yDimension < 0 || yDimension >= dimension) {
            throw illegalDimension("yDimension", yDimension);
        }
        if (xDimension == yDimension) {
            throw illegalDimension("xDimension", "yDimension");
        }
        final PixelTranslation source = getPixelTranslation(current);
        final PixelTranslation target = getPixelTranslation(desired);
        final double dx = target.dx - source.dx;
        final double dy = target.dy - source.dy;
        MathTransform mt;
        if (dimension == 2 && (xDimension | yDimension) == 1 && dx == dy && Math.abs(dx) == 0.5) {
            final int ci = (dx >= 0) ? 3 : 2;
            synchronized (translations) {
                mt = translations[ci];
                if (mt == null) {
                    mt = MathTransforms.uniformTranslation(dimension, dx);
                    translations[ci] = mt;
                }
            }
        } else {
            final Matrix matrix = Matrices.createIdentity(dimension + 1);
            matrix.setElement(xDimension, dimension, dx);
            matrix.setElement(yDimension, dimension, dy);
            mt = MathTransforms.linear(matrix);
        }
        return MathTransforms.concatenate(mt, gridToCRS);
    }

    /**
     * Formats an exception for an illegal dimension.
     */
    private static IllegalArgumentException illegalDimension(final String name, final Object dimension) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, name, dimension));
    }

    /**
     * Returns a string representation of this pixel translation.
     */
    @Override
    public String toString() {
        return String.valueOf(orientation) + '[' + dx + ", " + dy + ']';
    }
}
