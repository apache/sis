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
package org.apache.sis.gui.internal;

import java.util.ArrayList;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.VLineTo;


/**
 * Converts a Java2D shape to a JavaFX shape.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ShapeConverter {
    /**
     * Do not allow instantiation of this class.
     */
    private ShapeConverter() {
    }

    /**
     * Converts the given Java2D shape to a JavaFX shape.
     * If possible, this method simplifies the shape for example by replacing it with a rectangle.
     *
     * @param  shape  the shape to convert.
     * @param  tr  an optional affine transform to apply to the coordinates, {@code null} if none.
     * @return the JavaFX shape.
     */
    public static Shape convert(final java.awt.Shape shape, final AffineTransform tr) {
        if (tr == null || (tr.getType() & (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM)) == 0) {
            if (shape instanceof Rectangle2D) {
                return convert((Rectangle2D) shape, tr);
            }
        }
        final PathIterator it = shape.getPathIterator(tr);
        final var elements = new ArrayList<PathElement>();
        double x = Double.NaN, y = Double.NaN;
        final double[] coords = new double[6];
        while (!it.isDone()) {
            final PathElement e;
            switch (it.currentSegment(coords)) {
                /*
                 * MoveTo with the following optimizations:
                 *   - Omit "move to" that does not actually move.
                 *   - If consecutive "move to", keep only the last one.
                 */
                case PathIterator.SEG_MOVETO: {
                    if (x == (x = coords[0])  &  y == (y = coords[1])) continue;    // Really &, not &&.
                    if (!elements.isEmpty() && elements.getLast() instanceof MoveTo) {
                        elements.removeLast();
                    }
                    e = new MoveTo(x, y);
                    break;
                }
                /*
                 * LineTo with the following optimizations:
                 *   - Omit "line to" that does not actually move.
                 *   - Horizontal line to if the y coordinate does no change.
                 *   - Vertical line to if the x coordinate does no change.
                 */
                case PathIterator.SEG_LINETO: {
                    int change = 0;
                    if (x == (x = coords[0])) change  = 1;
                    if (y == (y = coords[1])) change |= 2;
                    switch (change) {
                        case 1: e = new HLineTo(x); break;
                        case 2: e = new VLineTo(y); break;
                        case 3: e = new LineTo(x, y); break;
                        default: continue;
                    }
                    break;
                }
                case PathIterator.SEG_QUADTO: {
                    e = new QuadCurveTo(coords[0], coords[1], x = coords[2], y = coords[3]);
                    break;
                }
                case PathIterator.SEG_CUBICTO: {
                    e = new CubicCurveTo(coords[0], coords[1], coords[2], coords[3], x = coords[4], y = coords[5]);
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    x = y = Double.NaN;
                    if (!elements.isEmpty() && elements.getLast() instanceof ClosePath) {
                        continue;   // Avoid repeating `ClosePath`.
                    }
                    e = new ClosePath();
                    break;
                }
                default: continue;
            }
            elements.add(e);
            it.next();
        }
        final var path = new Path(elements);
        switch (it.getWindingRule()) {
            case PathIterator.WIND_EVEN_ODD: path.setFillRule(FillRule.EVEN_ODD); break;
            case PathIterator.WIND_NON_ZERO: path.setFillRule(FillRule.NON_ZERO); break;
        }
        return path;
    }

    /**
     * Converts the given Java2D rectangle to a JavaFX rectangle. The given affine transform, if non-null,
     * should not contain a general rotation or general transform, otherwise the returned rectangle will
     * not be a good description of the returned shape.
     *
     * @param  shape  the rectangle to convert.
     * @param  tr  an optional affine transform to apply to the coordinates, {@code null} if none.
     * @return the JavaFX rectangle.
     */
    private static Rectangle convert(final Rectangle2D shape, final AffineTransform tr) {
        final double[] coords = {
            shape.getMinX(), shape.getMinY(),
            shape.getMaxX(), shape.getMaxY()
        };
        if (tr != null) {
            tr.transform(coords, 0, coords, 0, 2);
        }
        return new Rectangle(
                Math.min(coords[0],  coords[2]),
                Math.min(coords[1],  coords[3]),
                Math.abs(coords[2] - coords[0]),
                Math.abs(coords[3] - coords[1]));
    }
}
