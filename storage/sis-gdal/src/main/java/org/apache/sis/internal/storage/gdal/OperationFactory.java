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
package org.apache.sis.internal.storage.gdal;

import java.util.Set;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.internal.referencing.SpecializedOperationFactory;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.gdal.Proj4;


/**
 * The factory for operations between pair of {@literal Proj.4} wrappers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class OperationFactory implements SpecializedOperationFactory {
    /**
     * Creates a new factory.
     */
    public OperationFactory() {
    }

    /**
     * Returns an operation between the given pair of CRS, or an empty set if this factory does not recognize
     * the given CRSs.
     *
     * @return the coordinate operation, or an empty set if the given CRSs are not Proj.4 wrappers.
     * @throws FactoryException if the given CRSs are Proj.4 wrappers and an error occurred while
     *         creating the coordinate operation.
     */
    @Override
    public Set<CoordinateOperation> findOperations(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        return CollectionsExt.singletonOrEmpty(Proj4.createOperation(sourceCRS, targetCRS, false));
    }
}
