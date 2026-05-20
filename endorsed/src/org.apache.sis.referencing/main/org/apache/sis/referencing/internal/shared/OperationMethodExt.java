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
package org.apache.sis.referencing.internal.shared;

import java.util.Map;
import org.opengis.metadata.extent.Extent;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.CoordinateOperationContext;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.metadata.iso.extent.Extents;


/**
 * Extension of {@code OperationMethod} kept in a separated interface
 * because we are not sure if it is ready for public <abbr>API</abbr>.
 * Note: it could as well be a protected method in {@code DefaultOperationMethod}.
 */
public interface OperationMethodExt extends OperationMethod {
    /**
     * Optionally updates the metadata of a coordinate operation between a given pair of <abbr>CRS</abbr>s.
     * This method may be invoked when a coordinate operation is or contains a {@link SingleOperation} step
     * which uses this {@code OperationMethod}.
     * The {@code source} and {@code target} arguments contains the <abbr>CRS</abbr>s of the operation which
     * will be constructed with the given {@code properties} map.
     *
     * <p>This method can be implemented for purposes such as restricting the domain of validity or reducing the
     * declared accuracy of any coordinate operation which uses (potentially indirectly) this {@code OperationMethod}.
     * The given {@code properties} map contains the metadata that the caller intends to give to the operation to create.
     * This method can update the given {@code properties} map and returns {@code true},
     * or returns {@code false} if this method did nothing.</p>
     *
     * <p>This method may need to merge map values instead of replacing them.
     * For example, a domain of validity may need to be set to the {@linkplain Extents#intersection(Extent, Extent)
     * intersection} of the domain provided by this method with the domain already contained in the map (if any).</p>
     *
     * <p>The recognized keys and the valid values of the {@code properties} map are documented in the
     * {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map, CoordinateReferenceSystem,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform) operation constructor}.</p>
     *
     * @param  context     context of the coordinate operation to create, or {@code null} if none.
     * @param  source      the source <abbr>CRS</abbr> of the operation which will use the given {@code properties}.
     * @param  target      the target <abbr>CRS</abbr> of the operation which will use the given {@code properties}.
     * @param  properties  a modifiable map of metadata to be given to the operation between the given <abbr>CRS</abbr>s.
     * @return whether this method has modified the {@code properties} map.
     *
     * @todo If this method move to public <abbr>API</abbr>, {@code CoordinateReferenceSystem}
     *       should be replaced by {@code CoordinateMetadata}.
     */
    default boolean completeOperationMetadata(CoordinateOperationContext context,
                                              CoordinateReferenceSystem  source,
                                              CoordinateReferenceSystem  target,
                                              Map<String, Object> properties)
    {
        return false;
    }
}
