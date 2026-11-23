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
package org.apache.sis.referencing.dggs;

import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.referencing.dggs.s2.S2Dggrs;
import org.apache.sis.referencing.dggs.smc.SmcDggrs;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DiscreteGlobalGridReferenceSystems}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DiscreteGlobalGridReferenceSystemsTest {

    /**
     * Tests the code forms accepted by {@link DiscreteGlobalGridReferenceSystems#forCode(String)}.
     */
    @Test
    public void testForCode() throws FactoryException {
        assertInstanceOf(S2Dggrs.class,  DiscreteGlobalGridReferenceSystems.forCode("S2"));
        assertInstanceOf(S2Dggrs.class,  DiscreteGlobalGridReferenceSystems.forCode("[ogc-dggrs:S2]"));
        assertInstanceOf(S2Dggrs.class,  DiscreteGlobalGridReferenceSystems.forCode("https://www.opengis.net/def/dggrs/S2"));
        assertInstanceOf(SmcDggrs.class, DiscreteGlobalGridReferenceSystems.forCode("SMC"));
        assertInstanceOf(SmcDggrs.class, DiscreteGlobalGridReferenceSystems.forCode("smc"));
        assertInstanceOf(SmcDggrs.class, DiscreteGlobalGridReferenceSystems.forCode("sMc"));
        assertInstanceOf(S2Dggrs.class,  DiscreteGlobalGridReferenceSystems.forCode("https://www.opengis.net/def/dggrs/S2/"));
        assertThrows(NoSuchIdentifierException.class, () -> DiscreteGlobalGridReferenceSystems.forCode("unknown"));
    }
}
