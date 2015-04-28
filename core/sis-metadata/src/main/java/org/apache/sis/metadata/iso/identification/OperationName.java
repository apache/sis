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


/**
 * An {@code OperationMetadata} placeholder to be replaced later by a reference to an other {@link OperationMetadata}.
 * This temporary place holder is used when the operation name is unmarshalled before the actual operation definition.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
final class OperationName extends DefaultOperationMetadata {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7221543581387125873L;

    /**
     * Creates a new placeholder for the operation of the given name.
     */
    OperationName(final String operationName) {
        setOperationName(operationName);
    }

    /**
     * Returns a string representation of this placeholder.
     */
    @Override
    public String toString() {
        return "OperationMetadata[“" + getOperationName() + "”]";
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
    static void resolve(final Collection<DefaultOperationMetadata> containsOperations, final Collection<DefaultCoupledResource> coupledResources) {
        final Map<String,DefaultOperationMetadata> byName = new HashMap<String,DefaultOperationMetadata>();
        for (final DefaultOperationMetadata operation : containsOperations) {
            add(byName, operation.getOperationName(), operation);
        }
        for (final DefaultCoupledResource resource : coupledResources) {
            DefaultOperationMetadata operation = resource.getOperation();
            if (operation instanceof OperationName) {
                final String name = operation.getOperationName();
                operation = byName.get(name);
                if (operation == null) {
                    operation = byName.get(name);
                    if (operation == null) {
                        continue;
                    }
                }
                resource.setOperation(operation);
            }
        }
    }

    /**
     * Adds the given operation in the given map under the given name. If an entry already exists for the given name,
     * then this method sets the value to {@code null} for meaning that we have duplicated values for that name.
     */
    private static void add(final Map<String,DefaultOperationMetadata> byName, final String name, final DefaultOperationMetadata operation) {
        final boolean exists = byName.containsKey(name);
        final DefaultOperationMetadata previous = byName.put(name, operation);
        if (previous != operation && (previous != null || exists)) {
            byName.put(name, null); // Mark the entry as duplicated.
        }
    }
}
