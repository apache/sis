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
package org.apache.sis.referencing.operation.provider;

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for <q>Axis Order Reversal (Geographic3D horizontal)</q> (EPSG:9844).
 * This is a trivial operation that just swap the two first axes.
 * The inverse operation is this operation itself.
 *
 * @author  Martin Desruisseaux (Geomatys)
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
            .addIdentifier("9844").addName("Axis Order Reversal (Geographic3D horizontal)").createGroup();

    /**
     * The canonical instance of this operation method.
     *
     * @see #provider()
     */
    static final AxisOrderReversal3D INSTANCE = new AxisOrderReversal3D();

    /**
     * Returns the canonical instance of this operation method.
     * This method is invoked by {@link java.util.ServiceLoader} using reflection.
     *
     * @return the canonical instance of this operation method.
     */
    public static AxisOrderReversal provider() {
        return INSTANCE;
    }

    /**
     * Constructs a provider with default parameters.
     *
     * @todo Delete this constructor after we stop class-path support.
     *       Implementation will be moved to {@link #INSTANCE}, and {@code AxisOrderReversal3D}
     *       should no longer extend {@code AxisOrderReversal} (it would extend nothing).
     */
    public AxisOrderReversal3D() {
        super(PARAMETERS, (byte) 3);
    }
}
