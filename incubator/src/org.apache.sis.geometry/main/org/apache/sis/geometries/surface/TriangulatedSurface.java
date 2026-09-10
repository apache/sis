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
 * A polyhedral surface whose patches are all triangles.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Every patch is a {@link Triangle}.</li>
 *   <li>How the triangulation is derived is not restricted.</li>
 * </ul>
 *
 * <p>Note: an implementation which only stores the positions and applies a specific triangulation
 * technique to build the surface satisfies this interface just as well.</p>
 *
 * @param  <T>  type of the triangular patches of this surface.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.1.8
 */
@UML(identifier="TriangulatedSurface", specification=ISO_19107)
public sealed interface TriangulatedSurface<T extends Polygon> extends PolyhedralSurface<T>
        permits TIN
{

}
