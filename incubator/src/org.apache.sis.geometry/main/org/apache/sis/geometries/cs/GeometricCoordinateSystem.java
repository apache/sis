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
package org.apache.sis.geometries.cs;

import javax.measure.quantity.Length;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometries.Bearing;
import org.apache.sis.geometries.math.Vector;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="GeometricCoordinateSystem", specification=ISO_19107) // section 6.2.8
public interface GeometricCoordinateSystem {

    @UML(identifier="name", specification=ISO_19107) // section 6.2.8.2
    String getName();

    @UML(identifier="spatialDimension", specification=ISO_19107) // section 6.2.8.3
    int getSpatialDimension();

    @UML(identifier="temporalDimension", specification=ISO_19107) // section 6.2.8.4
    int getTemporalDimension();

    @UML(identifier="parametricDimension", specification=ISO_19107) // section 6.2.8.5
    int getParametricDimension();

    @UML(identifier="permutation", specification=ISO_19107) // section 6.2.8.6
    int[] getPermutation();

    @UML(identifier="spatialProjection", specification=ISO_19107) // section 6.2.8.7
    Projection getSpatialProjection();

    @UML(identifier="temporalProjection", specification=ISO_19107) // section 6.2.8.8
    Projection getTemporalProjection();

    @UML(identifier="csDistance", specification=ISO_19107) // section 6.2.8.9
    Number csDistance(DirectPosition p1, DirectPosition p2);

    @UML(identifier="distance", specification=ISO_19107) // section 6.2.8.10
    Length distance(DirectPosition p1, DirectPosition p2);

    @UML(identifier="pointAtDistance", specification=ISO_19107) // section 6.2.8.11
    DirectPosition pointAtDistance(DirectPosition center, Vector vector);

    @UML(identifier="geoLocate", specification=ISO_19107) // section 6.2.8.12
    DirectPosition geoLocate(DirectPosition pos);

    @UML(identifier="bearing", specification=ISO_19107) // section 6.2.8.13
    Bearing bearing(DirectPosition center, DirectPosition target);

    @UML(identifier="csInnerProduct", specification=ISO_19107) // section 6.2.8.14
    Number csInnerProduct(DirectPosition center, Vector v1, Vector v2);

}
