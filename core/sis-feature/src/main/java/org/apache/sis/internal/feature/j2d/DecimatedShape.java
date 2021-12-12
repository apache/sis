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
package org.apache.sis.internal.feature.j2d;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;


/**
 * A shape that apply a simple decimation on-the-fly for faster drawing.
 *
 * <h2>Limitations</h2>
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
public final class DecimatedShape extends ShapeWrapper {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3842608333341518892L;

    /**
     * The desired resolution on each axis.
     */
    private final double xRes, yRes;

    /**
     * Creates a new wrapper which will decimate the coordinates of the given source.
     *
     * @param  source      the shape to decimate.
     * @param  resolution  the desired resolution on each axis.
     */
    public DecimatedShape(final Shape source, final double[] resolution) {
        super(source);
        xRes = Math.abs(resolution[0]);
        yRes = Math.abs(resolution[1]);
    }

    /**
     * Returns {@code true} if resolutions are strictly positive and finite numbers.
     *
     * @return whether this object can effectively apply decimation.
     */
    public boolean isValid() {
        return xRes > 0 && yRes > 0 && xRes < Double.MAX_VALUE && yRes < Double.MAX_VALUE;
    }

    /**
     * Returns an iterator over the coordinates of this shape after decimation.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        return new DecimatedPathIterator(source.getPathIterator(at), xRes, yRes);
    }

    /**
     * Returns an iterator over the coordinates of this shape, approximated by decimated line segments.
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return new DecimatedPathIterator(source.getPathIterator(at, flatness), xRes, yRes);
    }
}
