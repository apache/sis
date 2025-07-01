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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import org.apache.sis.geometries.mesh.MeshPrimitive;

/**
 * Enum used in {@linkplain ISOBand isoband computation} to manage the integration of
 * {@linkplain MeshPrimitive.Triangles triangles} whose vertices are all located at the intersection altitude of
 * isobands.
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public enum IsoInclusion {
    /**
     * On {@link ISOBand} computation, triangles with ALL points on upper isoline are included
     */
    MAX,
    /**
     * On {@link ISOBand} computation, triangles with ALL points on lower isoline are included.
     */
    MIN,

    /**
     * On {@link ISOBand} computation, keep triangles on both upper and lower isoline of the band.
     */
    BOTH
}
