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
package org.apache.sis.profile.japan;

import java.util.ServiceLoader;
import org.apache.sis.internal.earth.netcdf.GCOM_C;
import org.apache.sis.internal.earth.netcdf.GCOM_W;
import org.apache.sis.internal.netcdf.Convention;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 * Tests the Japanese profile.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class JapanProfileTest extends TestCase {
    /**
     * Verifies that GCOM-C and GCOM-W conventions are registered.
     */
    @Test
    public void testRegistration() {
        boolean foundGCOM_C = false;
        boolean foundGCOM_W = false;
        for (final Convention c : ServiceLoader.load(Convention.class)) {
            foundGCOM_C |= (c instanceof GCOM_C);
            foundGCOM_W |= (c instanceof GCOM_W);
        }
        assertTrue("GCOM C", foundGCOM_C);
        assertTrue("GCOM W", foundGCOM_W);
    }
}
