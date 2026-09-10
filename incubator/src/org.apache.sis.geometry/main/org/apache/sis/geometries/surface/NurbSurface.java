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
package org.apache.sis.geometries.surface;

import java.util.List;
import org.apache.sis.geometries.SurfaceInterpolation;
import org.apache.sis.geometries.internal.shared.DefaultNurbSurface;

/**
 * TODO : missing in ISO:19107 ? need to recheck this one.
 *
 *
 * @author Johann Sorel (Geomatys)
 */
public sealed interface NurbSurface extends BSplineSurface
        permits DefaultNurbSurface
{

    static final String TYPE = "NURBSSURFACE";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@code false}: a NURBS is expressed in homogeneous coordinates,
     * therefore it is rational and not polynomial.
     *
     * @see ISO 19107:2019 - 8.7.2
     */
    @Override
    default boolean isPolynomial() {
        return false;
    }

    /**
     * Returns {@link SurfaceInterpolation#NURBS}.
     *
     * @see ISO 19107:2019 - 6.4.27
     */
    @Override
    default List<SurfaceInterpolation> getInterpolation() {
        return List.of(SurfaceInterpolation.NURBS);
    }

}
