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
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeometryVisitor {

    public void visit(Geometry geometry) {
        if (geometry instanceof Point candidate) visit(candidate);

        else if (geometry instanceof LinearRing candidate) visit(candidate);
        else if (geometry instanceof LineString candidate) visit(candidate);
        else if (geometry instanceof Curve candidate) visit(candidate);

        else if (geometry instanceof Triangle candidate) visit(candidate);
        else if (geometry instanceof Polygon candidate) visit(candidate);
        else if (geometry instanceof Surface candidate) visit(candidate);
        else if (geometry instanceof TIN candidate) visit(candidate);
        else if (geometry instanceof PolyhedralSurface candidate) visit(candidate);

        else if (geometry instanceof MultiPolygon candidate) visit(candidate);
        else if (geometry instanceof MultiLineString candidate) visit(candidate);
        else if (geometry instanceof MultiSurface candidate) visit(candidate);
        else if (geometry instanceof MultiCurve candidate) visit(candidate);
        else if (geometry instanceof MultiPoint candidate) visit(candidate);
        else if (geometry instanceof GeometryCollection candidate) visit(candidate);

        else throw new IllegalArgumentException("Unknown geometry type " + geometry.getClass().getName());
    }

    public void visit(Point geometry) {
    }

    public void visit(Curve geometry) {
    }

    public void visit(Surface geometry) {
    }

    public void visit(LineString geometry) {
        visit(geometry.getPoints());
    }

    public void visit(Polygon geometry) {
        visit(geometry.getExteriorRing());
        geometry.getInteriorRings().stream().forEach(this::visit);
    }

    public void visit(Triangle geometry) {
        visit(geometry.getExteriorRing());
    }

    public void visit(LinearRing geometry) {
        visit(geometry.getPoints());
    }

    public void visit(TIN geometry) {
        for (int i = 0, n = geometry.getNumPatches(); i < n; i++) {
            visit(geometry.getPatchN(i));
        }
    }

    public void visit(PolyhedralSurface geometry) {
        for (int i = 0, n = geometry.getNumPatches(); i < n; i++) {
            visit(geometry.getPatchN(i));
        }
    }

    public void visit(GeometryCollection geometry) {
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            visit(geometry.getGeometryN(i));
        }
    }

    public void visit(MultiPolygon geometry) {
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            visit(geometry.getGeometryN(i));
        }
    }

    public void visit(MultiLineString geometry) {
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            visit(geometry.getGeometryN(i));
        }
    }

    public void visit(MultiSurface geometry) {
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            visit(geometry.getGeometryN(i));
        }
    }

    public void visit(MultiCurve geometry) {
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            visit(geometry.getGeometryN(i));
        }
    }

    public void visit(MultiPoint geometry) {
        for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
            visit(geometry.getGeometryN(i));
        }
    }

    public void visit(PointSequence sequence) {
        for (int i = 0, n = sequence.size(); i < n; i++) {
            visit(sequence.getPoint(i));
        }
    }
}
