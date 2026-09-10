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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * The kind of geometry which a particular {@link BSplineSurface} represents.
 *
 * <p>Note: this code list is given for information only, to convey the original intent, and should
 * be consistent with the other properties of the spline surface.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.7.3
 */
@UML(identifier="BSplineSurfaceForm", specification=ISO_19107)
public enum BSplineSurfaceForm {
    /**
     * A bounded portion of a plane, represented by a b-spline surface of degree 1 in each parameter.
     */
    PLANAR,
    /**
     * A bounded portion of a cylindrical surface.
     */
    CYLINDRICAL,
    /**
     * A bounded portion of the surface of a right circular cone.
     */
    CONICAL,
    /**
     * A bounded portion of a sphere, or a complete sphere.
     */
    SPHERICAL,
    /**
     * A torus, or a portion of a torus.
     */
    TOROIDAL,
    /**
     * No particular surface is approximated.
     */
    UNSPECIFIED
}
