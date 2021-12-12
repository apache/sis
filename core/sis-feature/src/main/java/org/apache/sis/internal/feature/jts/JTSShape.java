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
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import org.apache.sis.internal.feature.j2d.EmptyShape;


/**
 * A thin wrapper that adapts a JTS geometry to the Shape interface so that the
 * geometry can be used by java2d without coordinate cloning.
 *
 * @author  Johann Sorel (Puzzle-GIS, Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
class JTSShape extends AbstractJTSShape {

    public JTSShape(final Geometry geom) {
        super(geom);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        if (geometry.isEmpty()) {
            return EmptyShape.INSTANCE;
        } else {
            return new JTSPathIterator(geometry, at);
        }
    }
}
