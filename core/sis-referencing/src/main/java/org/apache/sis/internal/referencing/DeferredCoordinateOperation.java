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

import java.util.Map;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;


/**
 * Place-holder for a {@code CoordinateOperation} whose creation is deferred. Used for iterating on instances returned by
 * {@link org.apache.sis.referencing.factory.sql.EPSGDataAccess#createFromCoordinateReferenceSystemCodes(String, String)}
 * where many operations may exist but only one (typically) will be retained.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-327">SIS-327</a>
 *
 * @since 0.8
 * @module
 */
@SuppressWarnings("serial")
public final class DeferredCoordinateOperation extends AbstractCoordinateOperation {
    /**
     * The factory to use for creating the actual coordinate operation.
     */
    private final CoordinateOperationAuthorityFactory factory;

    /**
     * Creates a deferred coordinate operation.
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  sourceCRS   the source CRS, or {@code null} if unspecified.
     * @param  targetCRS   the target CRS, or {@code null} if unspecified.
     * @param  factory     the factory to use for creating the actual coordinate operation.
     */
    public DeferredCoordinateOperation(final Map<String,?>             properties,
                                       final CoordinateReferenceSystem sourceCRS,
                                       final CoordinateReferenceSystem targetCRS,
                                       final CoordinateOperationAuthorityFactory factory)
    {
        super(properties, sourceCRS, targetCRS, null, null);
        this.factory = factory;
    }

    /**
     * Creates the actual coordinate operation.
     *
     * @return the coordinate operation.
     * @throws FactoryException if the factory failed to create the coordinate operation.
     */
    public CoordinateOperation create() throws FactoryException {
        return factory.createCoordinateOperation(getIdentifiers().iterator().next().getCode());
    }
}
