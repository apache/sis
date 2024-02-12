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

import java.util.Set;
import org.opengis.util.ScopedName;
import org.opengis.util.NameFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.xml.NilReason;
import org.apache.sis.util.iso.DefaultNameFactory;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.bind.metadata.replace.ServiceParameterTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.identification.OperationMetadata;
import org.opengis.metadata.identification.DistributedComputingPlatform;


/**
 * Tests {@link DefaultCoupledResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(ServiceParameterTest.class)
public final class DefaultCoupledResourceTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultCoupledResourceTest() {
    }

    /**
     * Creates the resource to use for testing purpose.
     */
    static DefaultCoupledResource create(final NameFactory factory) {
        final var operation = new DefaultOperationMetadata("Get Map", DistributedComputingPlatform.WEB_SERVICES, null);
        operation.setParameters(Set.of((ParameterDescriptor<?>) ServiceParameterTest.create()));
        operation.setConnectPoints(Set.of(NilReason.MISSING.createNilObject(OnlineResource.class)));

        final var resource = new DefaultCoupledResource();
        resource.setScopedName((ScopedName) factory.createGenericName(null, "mySpace", "ABC-123"));
        resource.setOperation(operation);
        return resource;
    }

    /**
     * Tests {@link OperationName#resolve(Collection, Collection)}.
     */
    @Test
    public void testOperationNameResolve() {
        final DefaultCoupledResource resource  = create(DefaultNameFactory.provider());
        final OperationMetadata      operation = resource.getOperation();
        /*
         * Test OperationName replacement when the name matches.
         */
        resource.setOperation(new OperationName(operation.getOperationName()));
        assertNotSame(operation, resource.getOperation(), "Before resolve");
        OperationName.resolve(Set.of(operation), Set.of(resource));
        assertSame(operation, resource.getOperation(), "After resolve");
        /*
         * If the name doesn't match, no replacement shall be done.
         */
        final var other = new OperationName("Other");
        resource.setOperation(other);
        assertSame(other, resource.getOperation(), "Before resolve");
        OperationName.resolve(Set.of(operation), Set.of(resource));
        assertSame(other, resource.getOperation(), "After resolve");
    }
}
