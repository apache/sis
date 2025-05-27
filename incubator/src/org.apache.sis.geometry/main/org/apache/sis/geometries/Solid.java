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
import javax.measure.quantity.Area;
import javax.measure.quantity.Volume;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;

/**
 *
 * @author Johann Sorel (Geomatys
 */
@UML(identifier="Solid", specification=ISO_19107) // section 6.4.28
public interface Solid extends Primitive {

    @UML(identifier="boundary", specification=ISO_19107) // section 6.4.28.2
    Geometry getBoundary();

    @UML(identifier="area", specification=ISO_19107) // section 6.4.28.3
    Area getArea();

    @UML(identifier="volume", specification=ISO_19107) // section 6.4.28.4
    Volume getVolume();

    @UML(identifier="dataPoint", specification=ISO_19107) // section 6.4.28.5
    List<DirectPosition> getDataPoints();

    @UML(identifier="controlPoint", specification=ISO_19107) // section 6.4.28.6
    List<DirectPosition> getControlPoints();

    @UML(identifier="interpolation", specification=ISO_19107) // section 6.4.28.7
    SolidInterpolation getInterpolation();

    @UML(identifier="knot", specification=ISO_19107) // section 6.4.28.8
    List<Knot> getKnots();

}
