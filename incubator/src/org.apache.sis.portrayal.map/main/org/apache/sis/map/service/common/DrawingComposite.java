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
package org.apache.sis.map.service.common;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;


/**
 * Drawings painted in order, as a single drawing.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DrawingComposite implements Drawing {
    /**
     * The drawings, in painting order.
     */
    private final List<Drawing> components;

    /**
     * Creates a drawing made of the given drawings.
     *
     * @param  components  the drawings, in painting order.
     */
    public DrawingComposite(final List<? extends Drawing> components) {
        this.components = List.copyOf(components);
    }

    /**
     * Returns a drawing made of the given drawings, ignoring the null ones.
     *
     * @param  components  the drawings in painting order, possibly null.
     * @return the combined drawing, or {@code null} if all drawings are null.
     */
    public static Drawing of(final Drawing... components) {
        final List<Drawing> drawings = new ArrayList<>(components.length);
        for (final Drawing drawing : components) {
            if (drawing != null) {
                drawings.add(drawing);
            }
        }
        switch (drawings.size()) {
            case 0:  return null;
            case 1:  return drawings.get(0);
            default: return new DrawingComposite(drawings);
        }
    }

    @Override
    public void paint(final Graphics2D graphics) {
        for (final Drawing drawing : components) {
            drawing.paint(graphics);
        }
    }

    @Override
    public boolean intersects(final Shape mask) {
        for (final Drawing drawing : components) {
            if (drawing.intersects(mask)) {
                return true;
            }
        }
        return false;
    }
}
