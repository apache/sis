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
package org.apache.sis.profile.france;

import org.opengis.referencing.ReferenceSystem;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.SecurityConstraints;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.constraint.DefaultSecurityConstraints;
import org.apache.sis.xml.bind.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.xml.bind.fra.IndirectReferenceSystem;
import org.apache.sis.xml.bind.fra.DirectReferenceSystem;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;

// Specific to the geoapi-4.0 branch:
import org.apache.sis.metadata.iso.DefaultIdentifier;


/**
 * Tests {@link FrenchProfile}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class FrenchProfileTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public FrenchProfileTest() {
    }

    /**
     * Tests {@link FrenchProfile#toAFNOR(Object)} with {@link Constraints},
     * {@link LegalConstraints} and  {@link SecurityConstraints} objects.
     */
    @Test
    public void testConstraintsToAFNOR() {
        Constraints std, fra;

        std = new DefaultConstraints("Some constraints.");
        fra = (Constraints) FrenchProfile.toAFNOR(std);
        assertNotSame(std, fra);
        assertSame   (fra, FrenchProfile.toAFNOR(fra));
        assertEquals ("Some constraints.", assertSingleton(fra.getUseLimitations()).toString());

        std = new DefaultLegalConstraints("Some legal constraints.");
        fra = (LegalConstraints) FrenchProfile.toAFNOR(std);
        assertNotSame(std, fra);
        assertSame   (fra, FrenchProfile.toAFNOR(fra));
        assertEquals ("Some legal constraints.", assertSingleton(fra.getUseLimitations()).toString());

        std = new DefaultSecurityConstraints("Some security constraints.");
        fra = (SecurityConstraints) FrenchProfile.toAFNOR(std);
        assertNotSame(std, fra);
        assertSame   (fra, FrenchProfile.toAFNOR(fra));
        assertEquals ("Some security constraints.", assertSingleton(fra.getUseLimitations()).toString());
    }

    /**
     * Tests {@link FrenchProfile#toAFNOR(ReferenceSystem, boolean)}.
     */
    @Test
    public void testReferenceSystemToAFNOR() {
        ReferenceSystem std, fra;

        std = new ReferenceSystemMetadata(new DefaultIdentifier("EPSG", "4326", null));
        fra = FrenchProfile.toAFNOR(std, false);
        assertInstanceOf(DirectReferenceSystem.class, fra);
        assertSame(fra, FrenchProfile.toAFNOR(fra));

        fra = FrenchProfile.toAFNOR(std, true);
        assertInstanceOf(IndirectReferenceSystem.class, fra);
        assertSame(fra, FrenchProfile.toAFNOR(fra));
    }
}
