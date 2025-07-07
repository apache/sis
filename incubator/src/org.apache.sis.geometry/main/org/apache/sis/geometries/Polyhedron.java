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

import java.util.List;

/**
 * A solid is defined by its bounding surfaces. Each bounding surface is a closed, simple surface, also called a shell.
 *
 * Each solid has a unique exterior shell and any number of shells that are inside the exterior shell
 * and that describe voids. The interior shells do not intersect each other and cannot contain another interior shell.
 *
 * A polyhedron is a solid where each shell is a multi-polygon. 'Closed' means that the multi-polygon
 * shell is watertight, it splits space into two distinct regions: inside and outside of the shell. 'Simple' means
 * that the polygons that make up the shell do not intersect, they only touch each other along their common boundaries.
 *
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#polyhedron
 */
public interface Polyhedron extends Geometry {

    public static final String TYPE = "POLYHEDRON";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    public default AttributesType getAttributesType() {
        return getExteriorShell().getAttributesType();
    }

    /**
     * Returns the exterior shell of this polyhedron.
     *
     * @return exterior shell of this polyhedron.
     */
    MultiPolygon getExteriorShell();

    /**
     * Returns a list of void shell inside the polyhedron.
     *
     * @return all void shell inside the polyhedron
     */
    List<MultiPolygon> getInteriorShells();

}
