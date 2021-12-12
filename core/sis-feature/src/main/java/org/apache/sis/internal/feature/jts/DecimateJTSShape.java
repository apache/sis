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
package org.apache.sis.internal.feature.jts;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * A shape that apply a simple decimation on-the-fly for faster drawing.
 * Current implementation assumes that the shape is flattened.
 * There is some tolerance for quadratic and cubic curves,
 * but the result may not be correct.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class DecimateJTSShape implements Shape {
    /**
     * The shape to decimate.
     */
    private final Shape source;

    /**
     * The desired resolution on each axis.
     */
    private final double xRes, yRes;

    /**
     * Creates a new wrapper which will decimate the coordinates of the given source.
     *
     * @param  source  the shape to decimate.
     */
    public DecimateJTSShape(final Shape source, final double[] resolution) {
        this.source = source;
        xRes = Math.abs(resolution[0]);
        yRes = Math.abs(resolution[1]);
    }

    /**
     * Returns {@code true} if resolutions are strictly positive and finite numbers.
     */
    final boolean isValid() {
        return xRes > 0 && yRes > 0 && xRes < Double.MAX_VALUE && yRes < Double.MAX_VALUE;
    }

    @Override
    public Rectangle getBounds() {
        return source.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return source.getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return source.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return source.contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return source.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return source.intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return source.contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return source.contains(r);
    }

    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        return new DecimateJTSPathIterator(source.getPathIterator(at), xRes, yRes);
    }

    @Override
    public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return new DecimateJTSPathIterator(source.getPathIterator(at, flatness), xRes, yRes);
    }
}
