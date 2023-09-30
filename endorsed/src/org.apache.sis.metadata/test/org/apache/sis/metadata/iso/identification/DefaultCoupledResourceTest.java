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
import org.apache.sis.xml.bind.metadata.replace.ServiceParameterTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.evolution.UnsupportedCodeList;


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
        final DefaultOperationMetadata operation = new DefaultOperationMetadata();
        operation.setOperationName("Get Map");
        operation.setDistributedComputingPlatforms(Set.of(UnsupportedCodeList.valueOf("WEB_SERVICES")));
        operation.setParameters(Set.of((ParameterDescriptor<?>) ServiceParameterTest.create()));
        operation.setConnectPoints(Set.of(NilReason.MISSING.createNilObject(OnlineResource.class)));

        final DefaultCoupledResource resource = new DefaultCoupledResource();
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
        final DefaultOperationMetadata operation = resource.getOperation();
        /*
         * Test OperationName replacement when the name matches.
         */
        resource.setOperation(new OperationName(operation.getOperationName()));
        assertNotSame("Before resolve", operation, resource.getOperation());
        OperationName.resolve(Set.of(operation), Set.of(resource));
        assertSame("After resolve", operation, resource.getOperation());
        /*
         * If the name doesn't match, no replacement shall be done.
         */
        final OperationName other = new OperationName("Other");
        resource.setOperation(other);
        assertSame("Before resolve", other, resource.getOperation());
        OperationName.resolve(Set.of(operation), Set.of(resource));
        assertSame("After resolve", other, resource.getOperation());
    }
}
