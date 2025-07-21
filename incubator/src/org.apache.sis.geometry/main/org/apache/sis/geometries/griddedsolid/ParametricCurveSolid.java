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
package org.apache.sis.geometries.griddedsolid;

import java.util.List;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Solid;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="ParametricCurveSolid", specification=ISO_19107) // section 9.3.1
public interface ParametricCurveSolid extends Solid {

    @UML(identifier="horizontalCurveType", specification=ISO_19107) // section 9.3.1.2
    GeometryType getHorizontalCurveType();

    @UML(identifier="verticalCurveType", specification=ISO_19107) // section 9.3.1.2
    GeometryType getVerticalCurveType();

    @UML(identifier="depthCurveType", specification=ISO_19107) // section 9.3.1.2
    GeometryType getDepthCurveType();

    @UML(identifier="rows", specification=ISO_19107) // section 9.3.1.2
    int getRows();

    @UML(identifier="columns", specification=ISO_19107) // section 9.3.1.2
    int getColumns();

    @UML(identifier="files", specification=ISO_19107) // section 9.3.1.2
    int getFiles();

    @UML(identifier="dataPoints", specification=ISO_19107) // section 9.3.1.3
    @Override
    List<DirectPosition> getDataPoints();

    @UML(identifier="controlPoints", specification=ISO_19107) // section 9.3.1.3
    @Override
    List<DirectPosition> getControlPoints();

    Curve getHorizontalCurve(double b, double c);

    Curve getVerticalCurve(double a, double c);

    Curve getDepthCurve(double a, double b);

    DirectPosition getSurface(double a, double b, double c);
}
