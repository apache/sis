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
package org.apache.sis.internal.referencing;

import java.util.Set;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Implemented by extension modules capable to provide coordinate operations for some particular pairs of CRS.
 * Implementations of this interface are not general-purpose factories;
 * they should not process any pair of CRS other than the one for which they are designed.
 *
 * <div class="note"><b>Example:</b>
 * a module doing the bindings between Apache SIS and another map projection library may create wrappers
 * around the transformation method of that other library when {@code findOperations(…)} recognizes the
 * given CRS as wrappers around their data structures.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public interface SpecializedOperationFactory {
    /**
     * Returns operations between the given pair of CRS, or an empty set if this factory does not recognize them.
     * Non-empty sets have precedence over EPSG geodetic dataset or other mechanism used by Apache SIS, so should
     * be used sparsely.
     *
     * @param  sourceCRS  the coordinate reference system of source points (before transformation).
     * @param  targetCRS  the coordinate reference system of target points (after transformation).
     * @return the coordinate operations from source to target CRS, or an empty set if this factory
     *         does not recognize the given pair of CRSs.
     * @throws FactoryException if this factory recognizes the CRSs given in arguments,
     *         but an error occurred while creating the coordinate operation.
     */
    Set<CoordinateOperation> findOperations(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS)
            throws FactoryException;
}
