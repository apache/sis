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


/**
 * A MultiCurve is a 1-dimensional GeometryCollection whose elements are Curves
 *
 * MultiCurve is a non-instantiable class in this standard; it defines a set of methods for its subclasses and is
 * included for reasons of extensibility.
 *
 * A MultiCurve is simple if and only if all of its elements are simple and the only intersections between any
 * two elements occur at Points that are on the boundaries of both elements.
 *
 * The boundary of a MultiCurve is obtained by applying the “mod 2” union rule: A Point is in the boundary of a
 * MultiCurve if it is in the boundaries of an odd number of elements of the MultiCurve (Reference [1], section 3.12.3.2).
 *
 * A MultiCurve is closed if all of its elements are closed.
 *
 * The boundary of a closed MultiCurve is always empty.
 *
 * A MultiCurve is defined as topologically closed.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#multi_curve
 */
public interface MultiCurve<T extends Curve> extends GeometryCollection<T>{

    public static final String TYPE = "MULTICURVE";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns TRUE if this MultiCurve is closed [StartPoint () = EndPoint () for each Curve in this MultiCurve].
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.8.2
     * @return true if multicurve is closed.
     */
    default boolean isClosed() {
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            if (!getGeometryN(i).isClosed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The Length of this MultiCurve which is equal to the sum of the lengths of the element Curves.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.8.2
     * @return length of the multicurve.
     */
    default double getLength() {
        double length = 0.0;
        for (int i = 0, n = getNumGeometries(); i < n; i++) {
            length += getGeometryN(i).getLength();
        }
        return length;
    }

}
