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
 * A multi-polyhedron is a collection of polyhedron objects. These are arbitrary aggregations.
 * There is no assumption regarding the topological relationships between the polyhedron objects,
 * but in most cases the polyhedron objects will not intersect each other.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#multi_polyhedron
 */
public interface MultiPolyhedron extends GeometryCollection<Polyhedron> {

    public static final String TYPE = "MULTIPOLYHEDRON";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

}
