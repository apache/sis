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
package org.apache.sis.geometries;

import org.apache.sis.geometries.internal.shared.AbstractGeometry;
import org.apache.sis.geometries.math.Tuple;


/**
 * A LinearRing is a LineString that is both closed and simple.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface LinearRing extends LineString {

    public static final String TYPE = "LINEARRING";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("LINEARRING (");
        final PointSequence points = getPoints();
        for (int i = 0, n = points.size() ; i < n; i++) {
            final Tuple pt = points.getPosition(i);
            if (i > 0) sb.append(',');
            AbstractGeometry.toText(sb, pt);
        }
        sb.append(')');
        return sb.toString();
    }
}
