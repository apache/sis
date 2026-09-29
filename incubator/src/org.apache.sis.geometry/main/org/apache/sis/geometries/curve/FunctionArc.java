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
package org.apache.sis.geometries.curve;

import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Range;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * The portion of a {@link FunctionCurve} corresponding to one interval of its knot space.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.7.3
 */
@UML(identifier="FunctionArc", specification=ISO_19107)
public interface FunctionArc {

    /**
     * Interval of the knot space over which this arc is defined.
     *
     * @return domain of this arc.
     *
     * @see ISO 19107:2019 - 7.7.3.2
     */
    @UML(identifier="domain", specification=ISO_19107)
    Range getDomain();

    /**
     * Returns the function giving the value of one coordinate offset over this arc.
     *
     * @param  arc               arc for which to return the function.
     * @param  coordinateOffset  coordinate offset for which to return the function.
     * @return function defining the given coordinate offset of the given arc.
     *
     * @see ISO 19107:2019 - 7.7.4
     */
    @UML(identifier="function", specification=ISO_19107)
    RealFunction function(FunctionArc arc, Vector coordinateOffset);
}
