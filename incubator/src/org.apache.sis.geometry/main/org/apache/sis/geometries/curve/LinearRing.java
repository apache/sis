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
package org.apache.sis.geometries.curve;

import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.internal.shared.DefaultLinearRing;
import org.apache.sis.maths.Tuple;


/**
 * A LinearRing is a LineString that is both closed and simple.
 *
 * @author Johann Sorel (Geomatys)
 */
public sealed interface LinearRing extends LineString
        permits DefaultLinearRing
{

    static final String TYPE = "LINEARRING";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@code true}: a linear ring is closed by definition.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.6.2
     */
    @Override
    default boolean isClosed() {
        return true;
    }

    /**
     * Returns {@code true}: a linear ring is simple by definition.
     *
     * @see ISO 19107:2019 - 6.4.4.15
     */
    @Override
    default boolean isSimple() {
        return true;
    }

    /**
     * Returns {@code true}: a linear ring is both closed and simple, therefore a ring.
     *
     * @see ISO 19107:2019 - 6.4.18.8
     */
    @Override
    default boolean isRing() {
        return true;
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("LINEARRING (");
        final DataPoints points = getDataPoints();
        for (int i = 0, n = points.size() ; i < n; i++) {
            final Tuple pt = points.getPosition(i);
            if (i > 0) sb.append(',');
            AbstractGeometry.toText(sb, pt);
        }
        sb.append(')');
        return sb.toString();
    }
}
