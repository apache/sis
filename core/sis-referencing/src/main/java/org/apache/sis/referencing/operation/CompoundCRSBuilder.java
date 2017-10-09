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

import org.apache.sis.internal.metadata.EllipsoidalHeightCombiner;
import org.opengis.referencing.operation.CoordinateOperationFactory;


/**
 * Helper class for combining two-dimensional geographic or projected CRS with an ellipsoidal height
 * into a three-dimensional CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class CompoundCRSBuilder extends EllipsoidalHeightCombiner {
    /**
     * The Apache SIS implementation of {@link CoordinateOperationFactory}.
     */
    private final DefaultCoordinateOperationFactory factorySIS;

    /**
     * Creates a new builder for the given coordinate operation factory.
     *
     * @param factory     the factory to use for creating coordinate operations.
     * @param factorySIS  used only when we need a SIS-specific method.
     */
    CompoundCRSBuilder(final CoordinateOperationFactory factory, final DefaultCoordinateOperationFactory factorySIS) {
        super(null, null, factory);
        this.factorySIS = factorySIS;
    }

    /**
     * Invoked when the builder needs a CRS or CS factory.
     */
    @Override
    public void initialize(final int factoryTypes) {
        if ((factoryTypes & CRS) != 0) crsFactory = factorySIS.getCRSFactory();
        if ((factoryTypes & CS)  != 0)  csFactory = factorySIS.getCSFactory();
    }
}
