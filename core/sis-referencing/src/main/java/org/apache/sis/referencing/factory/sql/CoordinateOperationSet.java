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
package org.apache.sis.referencing.factory.sql;

import java.util.Map;
import java.util.HashMap;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.IdentifiedObjectSet;


/**
 * A lazy set of {@link CoordinateOperation} objects to be returned by the
 * {@link EPSGDataAccess#createFromCoordinateReferenceSystemCodes(String, String)} method.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class CoordinateOperationSet extends IdentifiedObjectSet<CoordinateOperation> {
    /**
     * The codes of {@link ProjectedCRS} objects for the specified {@link Conversion} codes.
     */
    private final Map<String,Integer> projections;

    /**
     * Creates a new instance of this lazy set.
     */
    CoordinateOperationSet(final AuthorityFactory factory) {
        super(factory, CoordinateOperation.class);
        projections = new HashMap<String,Integer>();
    }

    /**
     * Adds the specified authority code.
     *
     * @param code The code for the {@link CoordinateOperation} to add.
     * @param crs  The code for the CRS to create instead of the operation, or {@code null} if none.
     */
    final void addAuthorityCode(final String code, final Integer crs) {
        if (crs != null) {
            projections.put(code, crs);
        }
        addAuthorityCode(code);
    }

    /**
     * Same as the default implementation in parent class,
     * but avoid to call the costly {@link EPSGDataAccess#getAuthority()} method.
     */
    @Override
    protected String getAuthorityCode(final CoordinateOperation object) {
        final Identifier id = IdentifiedObjects.getIdentifier(object, Citations.EPSG);
        return (id != null) ? id.getCode() : IdentifiedObjects.getIdentifierOrName(object);
    }

    /**
     * Creates an object for the specified code.
     */
    @Override
    protected CoordinateOperation createObject(final String code) throws FactoryException {
        final Integer crs = projections.get(code);
        if (crs != null) {
            return ((CRSAuthorityFactory) factory).createProjectedCRS(String.valueOf(crs)).getConversionFromBase();
        } else {
            return ((CoordinateOperationAuthorityFactory) factory).createCoordinateOperation(code);
        }
    }
}
