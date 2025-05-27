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
package org.apache.sis.geometries.parametriccurvesurfaces;

import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Knot;
import org.apache.sis.geometries.Surface;
import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="ParametricCurveSurface", specification=ISO_19107) // section 8.3.2
public interface ParametricCurveSurface extends Surface, ReferenceSystem {

    @UML(identifier="rows", specification=ISO_19107) // section 8.3.2.3
    int getRows();

    @UML(identifier="columns", specification=ISO_19107) // section 8.3.2.4
    int getColumns();

    @UML(identifier="controlPoints", specification=ISO_19107) // section 8.3.2.6
    @Override
    List<DirectPosition> getControlPoints();

    @UML(identifier="dataPoints", specification=ISO_19107) // section 8.3.2.5
    @Override
    List<DirectPosition> getDataPoints();

    @UML(identifier="horizontalCurveType", specification=ISO_19107) // section 8.3.2.2, 8.3.2.7
    GeometryType getHorizontalCurveType();

    @UML(identifier="verticalCurveType", specification=ISO_19107) // section 8.3.2.2, 8.3.2.8
    GeometryType getVerticalCurveType();

    @Override
    List<Knot> getKnots();

    @UML(identifier="horizontalCurve", specification=ISO_19107) // section 8.3.2.9
    Curve getHorizontalCurve(double v);

    @UML(identifier="verticalCurve", specification=ISO_19107) // section 8.3.2.10
    Curve getVerticalCurve(double u);

    @UML(identifier="surface", specification=ISO_19107) // section 8.3.2.11
    DirectPosition getSurface(double u, double v);


}
