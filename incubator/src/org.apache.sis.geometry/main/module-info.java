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

/**
 * Geometries.
 *
 * @author Johann Sorel (Geomatys)
 */
module org.apache.sis.geometry {
    requires esri.geometry.api;
    requires org.apache.sis.feature;
    requires org.apache.sis.util;
    requires transitive org.apache.sis.storage;


    exports org.apache.sis.geometries;
    exports org.apache.sis.geometries.operation;
    exports org.apache.sis.geometries.processor;
    exports org.apache.sis.geometries.math;

    uses org.apache.sis.geometries.processor.Processor;
    provides org.apache.sis.geometries.processor.Processor
        with org.apache.sis.geometries.processor.spatialanalysis2d.Distance.PointPoint,
             org.apache.sis.geometries.processor.spatialanalysis2d.Intersection.PrimitiveTrianglesPrimitivePoints,
             org.apache.sis.geometries.processor.spatialanalysis2d.Intersection.PrimitiveTrianglesPrimitiveLines,
             org.apache.sis.geometries.processor.spatialedition.To3D.Point,
             org.apache.sis.geometries.processor.spatialedition.To3D.LineString,
             org.apache.sis.geometries.processor.spatialedition.To3D.Primitive,
             org.apache.sis.geometries.processor.spatialedition.ComputeAttribute.Primitive,
             org.apache.sis.geometries.processor.spatialedition.ComputeAttribute.MultiPrimitive,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.Point,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.LineString,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.Polygon,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.MultiLineString,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.MultiPoint,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.MultiPrimitive,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.Primitive,
             org.apache.sis.geometries.processor.spatialedition.ToPrimitive.GeometryCollection,
             org.apache.sis.geometries.processor.spatialedition.Transform.MultiPrimitive,
             org.apache.sis.geometries.processor.spatialedition.Transform.Primitive,
             org.apache.sis.geometries.processor.spatialedition.Transform.Triangle,
             org.apache.sis.geometries.processor.spatialrelations2d.Contains.PolygonPoint;
}
