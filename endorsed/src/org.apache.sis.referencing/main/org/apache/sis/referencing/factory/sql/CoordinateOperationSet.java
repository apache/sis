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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.IdentifiedObjectSet;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;


/**
 * A lazy set of coordinate operations identified by their <abbr>EPSG</abbr> codes.
 * There are two different ways in which {@link EPSGDataAccess} gets coordinate operations:
 *
 * <ol>
 *   <li>The coordinate operation may be the <i>conversion from base</i> property of a projected <abbr>CRS</abbr>.
 *       Those conversions are obtained by a <abbr>SQL</abbr> query like below (note that this query can return at
 *       most one result, because {@code COORD_REF_SYS_CODE} is a primary key):
 *
 *       {@snippet lang="sql" :
 *         SELECT PROJECTION_CONV_CODE FROM "Coordinate Reference System" WHERE BASE_CRS_CODE = ? AND COORD_REF_SYS_CODE = ?
 *         }
 *   </li>
 *
 *   <li>The coordinate operation may be standalone. This is the case of coordinate transformations having stochastic errors.
 *       Those transformations are obtained by a <abbr>SQL</abbr> query like below (note that it may return many results):
 *
 *       {@snippet lang="sql" :
 *         SELECT COORD_OP_CODE FROM "Coordinate_Operation" … WHERE … AND SOURCE_CRS_CODE = ? AND TARGET_CRS_CODE = ?
 *         }
 *   </li>
 * </ol>
 *
 * We distinguish those two cases by the presence or absence of a coordinate operation code in the {@link #projections} map.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see EPSGDataAccess#createFromCoordinateReferenceSystemCodes(String, String)
 */
final class CoordinateOperationSet extends IdentifiedObjectSet<CoordinateOperation> {
    /**
     * The codes of {@link org.opengis.referencing.crs.ProjectedCRS} objects for
     * the specified {@link org.opengis.referencing.operation.Conversion} codes.
     *
     * <ul>
     *   <li>Keys a coordinate operation codes.</li>
     *   <li>Values are coordinate reference system codes. They are usually {@code ProjectedCRS},
     *       but the EPSG database sometimes use this mechanisms for other kind of CRS.</li>
     * </ul>
     *
     * This map does <strong>not</strong> contain all operations to be returned by this {@code CoordinateOperationSet},
     * but only the ones to be returned by the first <abbr>SQL</abbr> query documented in the class Javadoc.
     */
    private final Map<String,Integer> projections;

    /**
     * Creates a new instance of this lazy set.
     */
    CoordinateOperationSet(final AuthorityFactory factory) {
        super(factory, CoordinateOperation.class);
        projections = new HashMap<>();
    }

    /**
     * Adds the specified authority code.
     *
     * @param  code  the code for the {@link CoordinateOperation} to add.
     * @param  crs   the code for the CRS to create instead of the operation, or {@code null} if none.
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
     * Creates a coordinate operation for the specified <abbr>EPSG</abbr> code.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected CoordinateOperation createObject(final String code) throws FactoryException {
        final Integer base = projections.get(code);
        if (base != null) {
            /*
             * First case documented in class Javadoc:
             *
             *     SELECT PROJECTION_CONV_CODE FROM "Coordinate Reference System" …
             *
             * The result is usually a ProjectedCRS, but not always.
             */
            CoordinateReferenceSystem crs;
            crs = ((CRSAuthorityFactory) factory).createCoordinateReferenceSystem(String.valueOf(base));
            if (crs instanceof GeneralDerivedCRS) {
                return ((GeneralDerivedCRS) crs).getConversionFromBase();
            }
        }
        /*
         * Following line is either for the second case documented in class Javadoc, or the first case
         * when the result is not a derived CRS. Note that we could create a derived CRS here as below:
         *
         *     CoordinateOperation op = …,
         *     if (crs != null && op instanceof Conversion) {
         *         return DefaultDerivedCRS.create(IdentifiedObjects.getProperties(crs), baseCRS,
         *                 (Conversion) op, crs.getCoordinateSystem()).getConversionFromBase();
         *     }
         *
         * We don't do that for now because because EPSGDataAccess.createCoordinateReferenceSystem(String)
         * would be a better place, by generalizing the work done for ProjectedCRS.
         *
         * https://issues.apache.org/jira/browse/SIS-357
         */
        return ((CoordinateOperationAuthorityFactory) factory).createCoordinateOperation(code);
    }
}
