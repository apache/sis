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
package org.apache.sis.metadata.iso.identification;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import org.apache.sis.util.internal.shared.Strings;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Collections;
import java.io.Serializable;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.identification.CoupledResource;
import org.opengis.metadata.identification.DistributedComputingPlatform;
import org.opengis.metadata.identification.OperationMetadata;


/**
 * An {@code OperationMetadata} placeholder to be replaced later by a reference to another {@link OperationMetadata}.
 * This temporary place holder is used when the operation name is unmarshalled before the actual operation definition.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OperationName implements OperationMetadata, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7958898214063034276L;

    /**
     * The operation name.
     */
    private final String operationName;

    /**
     * Creates a new placeholder for the operation of the given name.
     */
    OperationName(final String operationName) {
        this.operationName = operationName;
    }

    /**
     * Returns the operation name.
     */
    @Override public String                                   getOperationName()                 {return operationName;}
    @Override public Collection<DistributedComputingPlatform> getDistributedComputingPlatforms() {return Collections.emptySet();}
    @Override public Collection<OnlineResource>               getConnectPoints()                 {return Collections.emptySet();}

    /**
     * Returns a string representation of this placeholder.
     */
    @Override
    public String toString() {
        return Strings.bracket("OperationMetadata", operationName);
    }

    /**
     * For every instance of {@link DefaultCoupledResource} associated to an operation of kind {@code OperationName},
     * replaces the operation by a "real" {@link DefaultOperationMetadata} of the same name, if any.
     *
     * <p>This method updates the elements in the {@code coupledResources} collection in-place.
     * The other collection is unmodified.</p>
     *
     * <p>This method is invoked at unmarshalling time for resolving the {@code OperationMetadata} instance which
     * were identified only by a name in a {@code <srv:operationName>} element.</p>
     */
    static void resolve(final Collection<OperationMetadata> containsOperations, final Collection<CoupledResource> coupledResources) {
        final Map<String,OperationMetadata> byName = new HashMap<>();
        for (final OperationMetadata operation : containsOperations) {
            add(byName, operation.getOperationName(), operation);
        }
        for (final CoupledResource resource : coupledResources) {
            if (resource instanceof DefaultCoupledResource) {
                OperationMetadata operation = resource.getOperation();
                if (operation instanceof OperationName) {
                    final String name = operation.getOperationName();
                    operation = byName.get(name);
                    if (operation == null) {
                        operation = byName.get(name);
                        if (operation == null) {
                            continue;
                        }
                    }
                    ((DefaultCoupledResource) resource).setOperation(operation);
                }
            }
        }
    }

    /**
     * Adds the given operation in the given map under the given name. If an entry already exists for the given name,
     * then this method sets the value to {@code null} for meaning that we have duplicated values for that name.
     */
    private static void add(final Map<String,OperationMetadata> byName, final String name, final OperationMetadata operation) {
        final boolean exists = byName.containsKey(name);
        final OperationMetadata previous = byName.put(name, operation);
        if (previous != operation && (previous != null || exists)) {
            byName.put(name, null);                                         // Mark the entry as duplicated.
        }
    }
}
