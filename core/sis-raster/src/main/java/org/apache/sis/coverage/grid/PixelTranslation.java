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
import java.util.HashMap;
import java.io.Serializable;

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.metadata.spatial.PixelOrientation;
import static org.opengis.metadata.spatial.PixelOrientation.*;

import org.apache.sis.util.Static;
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
 * which apply the pixel translation on a given {@link MathTransform} instance.
 *
 * <div class="note"><b>Example:</b>
 * if the following code snippet, {@code gridToCRS} is an {@link java.awt.geom.AffineTransform} from
 * <cite>grid cell</cite> coordinates (typically pixel coordinates) to some arbitrary CRS coordinates.
 * In this example, the transform maps pixels {@linkplain PixelOrientation#CENTER center},
 * while the {@linkplain PixelOrientation#UPPER_LEFT upper left} corner is desired.
 * This code will switch the affine transform from the <cite>pixel center</cite> to
 * <cite>upper left corner</cite> convention:
 *
 * {@preformat java
 *   final AffineTransform  gridToCRS = ...;
 *   final PixelOrientation current   = PixelOrientation.CENTER;
 *   final PixelOrientation desired   = PixelOrientation.UPPER_LEFT;
 *
 *   // Switch the transform from 'current' to 'desired' convention.
 *   final PixelTranslation source = getPixelTranslation(current);
 *   final PixelTranslation target = getPixelTranslation(desired);
 *   gridToCRS.translate(target.dx - source.dx,
 *                       target.dy - source.dy);
 * }
 * </div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 *
 * @see PixelInCell
 * @see PixelOrientation
 *
 * @since 1.0
 * @module
 */
public final class PixelTranslation extends Static implements Serializable {
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
    private static final Map<PixelOrientation, PixelTranslation> ORIENTATIONS = new HashMap<>(12);
    static {
        add(CENTER,       0.0,  0.0);
        add(UPPER_LEFT,  -0.5, -0.5);
        add(UPPER_RIGHT,  0.5, -0.5);
        add(LOWER_LEFT,  -0.5,  0.5);
        add(LOWER_RIGHT,  0.5,  0.5);
    }

    /** For {@link #ORIENTATIONS} construction only. */
    private static void add(final PixelOrientation orientation, final double dx, final double dy) {
        if (ORIENTATIONS.put(orientation, new PixelTranslation(orientation, dx, dy)) != null) {
            throw new AssertionError();
        }
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
     * @throws IllegalArgumentException if the given {@code anchor} is not a known code list value.
     */
    public static PixelOrientation getPixelOrientation(final PixelInCell anchor) {
        if (anchor == null) {
            return null;
        } else if (anchor.equals(PixelInCell.CELL_CENTER)) {
            return CENTER;
        } else if (anchor.equals(PixelInCell.CELL_CORNER)) {
            return UPPER_LEFT;
        } else {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "anchor", anchor));
        }
    }

    /**
     * Returns the position relative to the pixel center.
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
     * @throws IllegalArgumentException if the given {@code anchor} is not a known code list value.
     */
    public static double getPixelTranslation(final PixelInCell anchor) {
        if (PixelInCell.CELL_CENTER.equals(anchor)) {
            return 0;
        } else if (PixelInCell.CELL_CORNER.equals(anchor)) {
            return -0.5;
        } else {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "anchor", anchor));
        }
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
     * This method concatenates −½, 0 or +½ translations on <em>all</em> dimensions before the given transform.
     * If the two given conventions are the same, then this method returns the given transform unchanged.
     *
     * <div class="note"><b>Example:</b>
     * if a given {@code gridToCRS} transform was mapping the <em>cell corner</em> to "real world" coordinates, then a call to
     * <code>translate(gridToCRS, {@link PixelInCell#CELL_CORNER CELL_CORNER}, {@link PixelInCell#CELL_CENTER CELL_CENTER})</code>
     * will return a new transform performing the following steps: first convert grid coordinates from <var>cell center</var>
     * convention ({@code desired}) to <var>cell corner</var> convention ({@code current}), then concatenate the given
     * {@code gridToCRS} transform which was designed for the <em>cell corner</em> convention.
     * The above-cited <var>cell center</var> → <var>cell corner</var> conversion is done by translating the grid coordinates
     * by +½, because the grid coordinates (0,0) relative to cell center is (½,½) relative to cell corner.</div>
     *
     * If the given {@code gridToCRS} is null, then this method ignores all other arguments and returns {@code null}.
     * Otherwise {@code current} and {@code desired} arguments must be non-null.
     *
     * @param  gridToCRS  a math transform from <cite>pixel</cite> coordinates to any CRS, or {@code null}.
     * @param  current    the pixel orientation of the given {@code gridToCRS} transform.
     * @param  desired    the pixel orientation of the desired transform.
     * @return the translation from {@code current} to {@code desired}, or {@code null} if {@code gridToCRS} was null.
     * @throws IllegalArgumentException if {@code current} or {@code desired} is not a known code list value.
     */
    public static MathTransform translate(final MathTransform gridToCRS, final PixelInCell current, final PixelInCell desired) {
        if (gridToCRS == null || desired.equals(current)) {
            return gridToCRS;
        }
        final int dimension = gridToCRS.getSourceDimensions();
        final double offset = getPixelTranslation(desired) - getPixelTranslation(current);
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
            mt = translate(dimension, offset);
        } else synchronized (translations) {
            mt = translations[ci];
            if (mt == null) {
                mt = translate(dimension, offset);
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
     * <div class="note"><b>Example:</b>
     * if a given {@code gridToCRS} transform was mapping the upper-left corner to "real world" coordinates, then a call to
     * <code>translate(gridToCRS, {@link PixelOrientation#UPPER_LEFT UPPER_LEFT}, {@link PixelOrientation#CENTER CENTER}, 0, 1)</code>
     * will return a new transform translating grid coordinates by +0.5 before to apply the given {@code gridToCRS} transform.
     * See example in above {@link #translate(MathTransform, PixelInCell, PixelInCell) translate} method for more details.</div>
     *
     * If the given {@code gridToCRS} is null, then this method ignores all other arguments and returns {@code null}.
     * Otherwise {@code current} and {@code desired} arguments must be non-null.
     *
     * @param  gridToCRS   a math transform from <cite>pixel</cite> coordinates to any CRS, or {@code null}.
     * @param  current     the pixel orientation of the given {@code gridToCRS} transform.
     * @param  desired     the pixel orientation of the desired transform.
     * @param  xDimension  the dimension of <var>x</var> coordinates (pixel columns). Often 0.
     * @param  yDimension  the dimension of <var>y</var> coordinates (pixel rows). Often 1.
     * @return the translation from {@code current} to {@code desired}, or {@code null} if {@code gridToCRS} was null.
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
                    mt = translate(dimension, dx);
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
     * Creates an affine transform that apply the same linear conversion for all dimensions.
     * For each dimension, input values <var>x</var> are converted into output values <var>y</var>
     * using the following equation:
     *
     * <blockquote><var>y</var> &nbsp;=&nbsp; <var>x</var> × {@code scale} + {@code offset}</blockquote>
     *
     * @param  dimension  the input and output dimensions.
     * @param  offset     the {@code offset} term in the linear equation.
     * @return the linear transform for the given scale and offset.
     */
    private static MathTransform translate(final int dimension, final double offset) {
        final Matrix matrix = Matrices.createIdentity(dimension + 1);
        for (int i=0; i<dimension; i++) {
            matrix.setElement(i, dimension, offset);
        }
        return MathTransforms.linear(matrix);
    }

    /**
     * Returns a string representation of this pixel translation.
     */
    @Override
    public String toString() {
        return String.valueOf(orientation) + '[' + dx + ", " + dy + ']';
    }
}
