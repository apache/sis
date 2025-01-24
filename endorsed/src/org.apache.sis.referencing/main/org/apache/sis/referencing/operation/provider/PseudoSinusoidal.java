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
 * The provider for <q>Pseudo sinusoidal equal-area</q> projection.
 * This is similar to Pseudo-Mercator: uses spherical formulas but apply the result on an ellipsoid.
 * This is sometimes used with remote sensing data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public final class PseudoSinusoidal extends Sinusoidal {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6523477856049963388L;

    /**
     * Name of this projection.
     */
    public static final String NAME = "Pseudo sinusoidal";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static ParameterDescriptorGroup parameters() {
        return builder().addName(NAME)
                .createGroupForMapProjection(AbstractMercator.toArray(PARAMETERS.descriptors(), 0));
    }

    /**
     * Constructs a new provider.
     */
    public PseudoSinusoidal() {
        super(parameters());
    }

    /**
     * Returns the non-pseudo variant of this map projection.
     *
     * @return the non-pseudo variant of this map projection.
     */
    @Override
    public MapProjection sourceOfPseudo() {
        return new Sinusoidal();
    }
}
