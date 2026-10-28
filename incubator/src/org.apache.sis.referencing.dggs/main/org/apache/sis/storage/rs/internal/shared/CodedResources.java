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
package org.apache.sis.storage.rs.internal.shared;

import java.util.List;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;


/**
 * Utility methods for coded resources implementations.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CodedResources {

    private CodedResources(){}

    /**
     * Aligns a query on the resource geometry.
     * If the query uses a Discrete Global Grid declared in one of the
     * {@linkplain CodedResource#getAlternateGridGeometry() alternate geometries},
     * this geometry is used, otherwise the default resource geometry is used.
     *
     * @param resource the resource to read
     * @param query the user query, may not match the resource geometry
     * @return query aligned on the resource geometry
     * @throws NoSuchDataException if the query does not intersect the resource geometry
     * @throws DataStoreException if the query can not be aligned
     */
    public static CodedGeometry align(CodedResource resource, CodedGeometry query) throws DataStoreException {
        final CodedGeometry base = selectGeometry(resource, query);
        try {
            return base.derive().subgrid(query).build();
        } catch (DisjointExtentException ex) {
            throw new NoSuchDataException(ex.getMessage(), ex);
        } catch (IllegalGridGeometryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the resource geometry sharing the query Discrete Global Grid, or the default geometry.
     */
    private static CodedGeometry selectGeometry(CodedResource resource, CodedGeometry query) throws DataStoreException {
        final ReferenceSystem queryRs = query.getReferenceSystem();
        if (queryRs != null) {
            final List<ReferenceSystem> queryComponents = ReferenceSystems.getSingleComponents(queryRs, true);
            for (CodedGeometry candidate : resource.getAlternateGridGeometry()) {
                final ReferenceSystem rs = candidate.getReferenceSystem();
                if (rs == null) continue;
                for (ReferenceSystem component : ReferenceSystems.getSingleComponents(rs, true)) {
                    if (component instanceof DiscreteGlobalGridReferenceSystem && queryComponents.contains(component)) {
                        return candidate;
                    }
                }
            }
        }
        return resource.getGridGeometry();
    }
}
