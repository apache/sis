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
package org.apache.sis.internal.referencing.provider;

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for <cite>"axis order reversal (geographic3D horizontal)"</cite> (EPSG:9844).
 * This is a trivial operation that just swap the two first axes.
 * The inverse operation is this operation itself.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
public final class AxisOrderReversal3D extends AxisOrderReversal {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7236499637763125168L;

    /**
     * The group of all parameters expected by this coordinate operation (in this case, none).
     */
    private static final ParameterDescriptorGroup PARAMETERS = builder()
            .addIdentifier("9844").addName("Axis order reversal (geographic3D horizontal)").createGroup();

    /**
     * Constructs a provider with default parameters.
     */
    public AxisOrderReversal3D() {
        super(3, PARAMETERS);
    }
}
