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
package org.apache.sis.map.service.se1;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;

/**
 * Combine an AWT Shape and it's rendering options.
 *
 * @author Johann Sorel (Geomatys)
 */
final class RenderedShape {

    public static final AlphaComposite ALPHA_COMPOSITE_0F = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f);
    public static final AlphaComposite ALPHA_COMPOSITE_1F = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);

    public AlphaComposite fillComposite = ALPHA_COMPOSITE_1F;
    public AlphaComposite strokeComposite = ALPHA_COMPOSITE_1F;

    /**
     * Shape to render.
     * If null, paint and hit methods will always return false.
     */
    public Shape shape;
    /**
     * The fill paint of the shape.
     * If null, no fill rendering is made.
     */
    public Paint fillPaint;
    /**
     * The stroke paint of the shape.
     * If null, no stroke rendering is made.
     */
    public Paint strokePaint;
    /**
     * The stroke of the shape.
     * If null, no stroke rendering is made.
     */
    public Stroke stroke;

    /**
     * Paint this shape with given Graphics2D.
     *
     * @param g2d not null
     * @return true if something was painted.
     */
    public boolean paint(Graphics2D g2d) {
        if (shape == null) return false;

        boolean painted = false;
        if (fillPaint != null) {
            g2d.setComposite(fillComposite);
            g2d.setPaint(fillPaint);
            g2d.fill(shape);
            painted = true;
        }
        if (stroke != null && strokePaint != null) {
            g2d.setComposite(strokeComposite);
            g2d.setPaint(strokePaint);
            g2d.setStroke(stroke);
            g2d.draw(shape);
            painted = true;
        }
        return painted;
    }

    /**
     * Test intersection of the shape and it's style options with the searched mask.
     * @param mask not null.
     * @return true if rendered shape intersects the mask.
     */
    public boolean intersects(Shape mask) {

        //test intersection with fill if defined
        if (fillPaint != null) {
            final Area maskArea = new Area(mask);
            final Area shapeArea = new Area(shape);
            maskArea.intersect(shapeArea);
            if (!maskArea.isEmpty()) {
                return true;
            }
        }
        //test intersection with stroke if defined
        if (stroke != null && strokePaint != null) {
            final Area maskArea = new Area(mask);
            final Area shapeArea = new Area(stroke.createStrokedShape(shape));
            maskArea.intersect(shapeArea);
            return !maskArea.isEmpty();
        }
        return false;
    }

}
