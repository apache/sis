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
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.opengis.referencing.operation.MathTransform;

/**
 * A thin wrapper that adapts a JTS geometry to the Shape interface so that the
 * geometry can be used by java2d without coordinate cloning.
 *
 * @author Johann Sorel (Puzzle-GIS + Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
class JTSShape extends AbstractJTSShape<Geometry> {

    protected JTSPathIterator<? extends Geometry> iterator;

    public JTSShape(final Geometry geom) {
        super(geom);
    }

    /**
     * Creates a new GeometryJ2D object.
     *
     * @param geom - the wrapped geometry
     */
    public JTSShape(final Geometry geom, final MathTransform trs) {
        super(geom, trs);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at) {

        final MathTransform concat;
        if (at == null) {
            concat = transform;
        } else {
            concat = MathTransforms.concatenate(transform, new AffineTransform2D(at));
        }

        if (iterator == null) {
            if (this.geometry.isEmpty()) {
                iterator = JTSPathIterator.Empty.INSTANCE;
            } else if (this.geometry instanceof Point) {
                iterator = new JTSPathIterator.Point((Point) geometry, concat);
            } else if (this.geometry instanceof Polygon) {
                iterator = new JTSPathIterator.Polygon((Polygon) geometry, concat);
            } else if (this.geometry instanceof LineString) {
                iterator = new JTSPathIterator.LineString((LineString) geometry, concat);
            } else if (this.geometry instanceof MultiLineString) {
                iterator = new JTSPathIterator.MultiLineString((MultiLineString) geometry, concat);
            } else if (this.geometry instanceof GeometryCollection) {
                iterator = new JTSPathIterator.GeometryCollection((GeometryCollection) geometry, concat);
            }
        } else {
            iterator.setTransform(concat);
        }

        return iterator;
    }

    @Override
    public AbstractJTSShape clone() {
        return new JTSShape(this.geometry, this.transform);
    }

}
