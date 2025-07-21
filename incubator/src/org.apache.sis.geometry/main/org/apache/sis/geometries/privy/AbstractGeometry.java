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
package org.apache.sis.geometries.privy;

import java.util.HashMap;
import java.util.Map;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.Tuple;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrayCursor;


/**
 * Abstract geometry, manages crs only.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractGeometry implements Geometry {

    private Map<String,Object> properties;

    @Override
    public synchronized Map<String, Object> userProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public String toString() {
        return asText();
    }

    public static void toText(StringBuilder sb, Tuple tuple) {
        sb.append(tuple.get(0));
        for (int i = 1, n = tuple.getDimension(); i < n; i++) {
            sb.append(' ');
            sb.append(tuple.get(i));
        }
    }

    public static void toText(StringBuilder sb, TupleArray array) {
        final TupleArrayCursor cursor = array.cursor();
        boolean first = true;
        while (cursor.next()) {
            if (!first) {
                sb.append(", ");
            }
            toText(sb, cursor.samples());
            first = false;
        }
    }

    public static void toText(StringBuilder sb, PointSequence array) {
        final int size = array.size();
        if (size == 0) return;

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            toText(sb, array.getPosition(i));
        }
    }
}
