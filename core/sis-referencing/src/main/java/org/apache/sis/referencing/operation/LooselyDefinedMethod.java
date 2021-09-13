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
package org.apache.sis.referencing.operation;

import java.util.HashMap;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;


/**
 * Operation methods that do not match exactly any of the predefined "standard" methods and for which
 * we do not define unambiguous sets of parameters. Those methods could be replaced by concatenations
 * of standard methods, but doing so would require more development work.  In the meantime, we define
 * those methods for avoiding to mislead users. For example we should not call a coordinate operation
 * "Affine" if it also performs conversion from geographic to geocentric coordinates.
 *
 * <h2>Restrictions</h2>
 * We do not provide any mechanism for instantiating a {@code CoordinateOperation} from those methods.
 * Consequently a coordinate operation can be formatted in WKT with those operation methods, but can
 * not be parsed. Attempt to parse such WKT will result in an error saying that the method is unknown.
 * This is better than formatting WKT with a standard but wrong operation name, in which case parsing
 * the WKT would produce unexpected results.
 *
 * <h2>Future evolution</h2>
 * A cleaner approach would be to replace those methods by a concatenation of standard methods.
 * {@link org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#getLastMethodUsed()}
 * can be invoked after {@code factory.createCoordinateSystemChange(…)} for getting pieces of information
 * needed, but we still have to put all the pieces together.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-462">SIS-462</a>
 *
 * @since 0.5
 * @module
 */
final class LooselyDefinedMethod {
    /**
     * The <cite>"Affine parametric transformation in geocentric domain"</cite> method.
     * It could be done by a concatenation of the following standard methods:
     *
     * <ol>
     *   <li><cite>"Geographic 2D to 3D conversion"</cite></li>
     *   <li><cite>"Geographic/geocentric conversions"</cite> (EPSG:9602)</li>
     *   <li><cite>"Affine parametric transformation"</cite> (EPSG:9624)</li>
     *   <li>Inverse of <cite>"Geographic/geocentric conversions"</cite></li>
     *   <li><cite>"Geographic 3D to 2D conversion"</cite> (EPSG:9659)</li>
     * </ol>
     *
     * It is not implemented that way for now because we need more work for analyzing the source and target
     * coordinate systems (they are not necessarily ellipsoidal, so some "Geographic/geocentric conversions"
     * may need to be skipped or replaced by a conversion from/to a spherical CS), check the number of dimension,
     * <i>etc.</i>
     */
    static final DefaultOperationMethod AFFINE_GEOCENTRIC;

    static {
        final HashMap<String,Object> properties = new HashMap<>(4);
        properties.put(DefaultOperationMethod.NAME_KEY,    "Undefined parameters");
        properties.put(DefaultOperationMethod.REMARKS_KEY, "Placeholder for what should be a chain of coordinate operations.");
        final DefaultParameterDescriptorGroup parameters = new DefaultParameterDescriptorGroup(properties, 0, 1);

        properties.put(DefaultOperationMethod.NAME_KEY,    "Affine parametric transformation in geocentric domain");
        properties.put(DefaultOperationMethod.REMARKS_KEY, parameters.getRemarks());
        properties.put(DefaultOperationMethod.FORMULA_KEY, new DefaultFormula(
                "This operation method is currently an implementation dependent black box. " +
                "A future version may redefine this method in terms of more standard methods."));
        AFFINE_GEOCENTRIC = new DefaultOperationMethod(properties, parameters);
    }

    /**
     * Do not allow instantiation of this class.
     */
    private LooselyDefinedMethod() {
    }
}
