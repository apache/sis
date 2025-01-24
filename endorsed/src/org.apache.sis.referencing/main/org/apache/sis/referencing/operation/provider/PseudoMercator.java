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
 * The provider for <q>Popular Visualisation Pseudo Mercator</q> projection (EPSG:1024).
 * This is also known as the "Google projection", defined by popular demand but not considered
 * a valid projection method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlTransient
public final class PseudoMercator extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8126827491349984471L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "1024";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier(IDENTIFIER)
                .addName("Popular Visualisation Pseudo Mercator")
                .createGroupForMapProjection(toArray(MercatorSpherical.PARAMETERS.descriptors(), 0));
    }

    /**
     * Constructs a new provider.
     */
    public PseudoMercator() {
        super(PARAMETERS);
    }

    /**
     * Returns the non-pseudo variant of this map projection.
     *
     * @return the non-pseudo variant of this map projection.
     */
    @Override
    public MapProjection sourceOfPseudo() {
        return new Mercator1SP();
    }
}
