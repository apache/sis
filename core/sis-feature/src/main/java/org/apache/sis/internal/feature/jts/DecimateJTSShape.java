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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.opengis.referencing.operation.MathTransform;

/**
 * A thin wrapper that adapts a JTS geometry to the Shape interface so that the
 * geometry can be used by java2d without coordinate cloning, coordinate
 * decimation apply on the fly.
 *
 * @author Johann Sorel (Puzzle-GIS + Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
class DecimateJTSShape extends JTSShape {

    private final double[] resolution;

    /**
     * Creates a new GeometryJ2D object.
     *
     * @param geom - the wrapped geometry
     */
    public DecimateJTSShape(final Geometry geom, final double[] resolution) {
        super(geom);
        this.resolution = resolution;
    }

    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        MathTransform t = (at == null) ? null : new AffineTransform2D(at);
        if (iterator == null) {
            if (this.geometry.isEmpty()) {
                iterator = JTSPathIterator.Empty.INSTANCE;
            } else if (this.geometry instanceof Point) {
                iterator = new JTSPathIterator.Point((Point) geometry, t);
            } else if (this.geometry instanceof Polygon) {
                iterator = new JTSPathIterator.Polygon((Polygon) geometry, t);
            } else if (this.geometry instanceof LineString) {
                iterator = new DecimateJTSPathIterator.LineString((LineString) geometry, t, resolution);
            } else if (this.geometry instanceof GeometryCollection) {
                iterator = new DecimateJTSPathIterator.GeometryCollection((GeometryCollection) geometry, t, resolution);
            }
        } else {
            iterator.setTransform(t);
        }
        return iterator;
    }
}
