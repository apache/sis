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

import java.util.Set;
import javax.print.attribute.standard.MediaSize.ISO;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.internal.shared.DefaultMultiCurve;


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
public sealed interface MultiCurve<T extends Curve> extends GeometryCollection<T>
        permits MultiLineString,
                DefaultMultiCurve
{

    static final String TYPE = "MULTICURVE";

    @Override
    default GeometryType getGeometryType() {
        return GeometryType.MULTICURVE;
    }

    /**
     * Returns {@link GeometryType#CURVE}: all the elements of this collection are curves.
     *
     * @see ISO 19107:2019 - 6.4.31.2
     */
    @Override
    default Set<GeometryType> getElementType() {
        return Set.of(GeometryType.CURVE);
    }

}
