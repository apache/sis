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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.privy.ArraySequence;
import org.apache.sis.geometries.privy.DefaultGeometryCollection;
import org.apache.sis.geometries.privy.DefaultLineString;
import org.apache.sis.geometries.privy.DefaultLinearRing;
import org.apache.sis.geometries.privy.DefaultMultiLineString;
import org.apache.sis.geometries.privy.DefaultMultiPoint;
import org.apache.sis.geometries.privy.DefaultMultiPolygon;
import org.apache.sis.geometries.privy.DefaultMultiSurface;
import org.apache.sis.geometries.privy.DefaultPoint;
import org.apache.sis.geometries.privy.DefaultPolygon;
import org.apache.sis.geometries.privy.DefaultTriangle;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeometryFactory {

    private GeometryFactory(){}

    public static Point createPoint(CoordinateReferenceSystem crs) {
        return new DefaultPoint(crs);
    }

    public static Point createPoint(CoordinateReferenceSystem crs, double ... position) {
        return new DefaultPoint(crs, position);
    }

    public static Point createPoint(SampleSystem ss, double ... position) {
        return new DefaultPoint(ss, position);
    }

    public static Point createPoint(PointSequence sequence) {
        return new DefaultPoint(sequence);
    }

    public static LineString createLineString(PointSequence sequence) {
        return new DefaultLineString(sequence);
    }

    public static LinearRing createLinearRing(PointSequence sequence) {
        return new DefaultLinearRing(sequence);
    }

    public static Polygon createPolygon(LinearRing exterior, List<LinearRing> interiors) {
        return new DefaultPolygon(exterior, interiors);
    }

    public static Triangle createTriangle(LinearRing exterior) {
        return new DefaultTriangle(exterior);
    }

    public static MultiPoint createMultiPoint(PointSequence sequence) {
        return new DefaultMultiPoint(sequence);
    }

    public static MultiLineString createMultiLineString(LineString ... geometries) {
        return new DefaultMultiLineString(geometries);
    }

    public static MultiPolygon createMultiPolygon(Polygon ... geometries) {
        return new DefaultMultiPolygon(geometries);
    }

    public static <T extends Surface> MultiSurface<T> createMultiSurface(T ... geometries) {
        return new DefaultMultiSurface<>(geometries);
    }

    public static <T extends Geometry> GeometryCollection<T> createGeometryCollection(T ... geometries) {
        return new DefaultGeometryCollection<>(geometries);
    }

    public static PointSequence createSequence(TupleArray positions) {
        return createSequence(Collections.singletonMap(AttributesType.ATT_POSITION, positions));
    }

    public static PointSequence createSequence(Map<String, TupleArray> attributes) {
        return new ArraySequence(attributes);
    }

}
