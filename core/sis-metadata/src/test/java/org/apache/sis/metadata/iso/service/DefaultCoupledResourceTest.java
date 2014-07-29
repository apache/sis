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
package org.apache.sis.metadata.iso.service;

import org.opengis.util.ScopedName;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.service.CoupledResource;
import org.opengis.metadata.service.DistributedComputingPlatform;
import org.opengis.metadata.service.OperationMetadata;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.xml.NilReason;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.internal.system.DefaultFactories.SIS_NAMES;
import static org.junit.Assert.*;


/**
 * Tests {@link DefaultCoupledResource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultParameterTest.class)
public final strictfp class DefaultCoupledResourceTest extends TestCase {
    /**
     * Creates the resource to use for testing purpose.
     */
    static DefaultCoupledResource create() {
        final DefaultOperationMetadata operation = new DefaultOperationMetadata("Get Map",
                DistributedComputingPlatform.WEB_SERVICES, null);
        operation.setParameters(singleton(DefaultParameterTest.create()));
        operation.setConnectPoints(singleton(NilReason.MISSING.createNilObject(OnlineResource.class)));

        final DefaultCoupledResource resource = new DefaultCoupledResource();
        resource.setScopedName((ScopedName) SIS_NAMES.createGenericName(null, "mySpace", "ABC-123"));
        resource.setOperation(operation);
        return resource;
    }

    /**
     * Tests {@link OperationName#resolve(Collection, Collection)}.
     */
    @Test
    public void testOperationNameResolve() {
        final DefaultCoupledResource resource  = DefaultCoupledResourceTest.create();
        final OperationMetadata      operation = resource.getOperation();
        /*
         * Test OperationName replacement when the name matches.
         */
        resource.setOperation(new OperationName(operation.getOperationName()));
        assertNotSame("Before resolve", operation, resource.getOperation());
        OperationName.resolve(singleton(operation), singleton((CoupledResource) resource));
        assertSame("After resolve", operation, resource.getOperation());
        /*
         * If the name doesn't match, no replacement shall be done.
         */
        final OperationName other = new OperationName(new SimpleInternationalString("Other"));
        resource.setOperation(other);
        assertSame("Before resolve", other, resource.getOperation());
        OperationName.resolve(singleton(operation), singleton((CoupledResource) resource));
        assertSame("After resolve", other, resource.getOperation());
    }
}
