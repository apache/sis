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

import org.apache.sis.geometries.privy.AbstractGeometry;
import org.apache.sis.geometries.math.Tuple;


/**
 * A MultiLineString is a MultiCurve whose elements are LineStrings.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface MultiLineString extends MultiCurve<LineString> {

    public static final String TYPE = "MULTILINESTRING";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    default String asText() {
        final StringBuilder sb = new StringBuilder("MULTILINESTRING (");
        for (int k = 0, kn = getNumGeometries(); k < kn; k++){
            if (k > 0) sb.append(',');
            sb.append('(');
            final LineString line = getGeometryN(k);
            final PointSequence points = line.getPoints();
            for (int i = 0, n = points.size() ; i < n; i++) {
                final Tuple pos = points.getPosition(i);
                if (i > 0) sb.append(',');
                AbstractGeometry.toText(sb, pos);
            }
            sb.append(')');
        }
        sb.append(')');
        return sb.toString();
    }
}
