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
package org.apache.sis.referencing.gazetteer;

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import org.opengis.referencing.operation.TransformException;


/**
 * Tests {@link MilitaryGridReferenceSystem}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(MGRSEncoderTest.class)
public final strictfp class MilitaryGridReferenceSystemTest extends TestCase {
    /**
     * Tests encoding of coordinates.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @org.junit.Ignore
    public void testEncoding() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = new MilitaryGridReferenceSystem.Coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.UTM(13, 103));
        position.x = 377299;
        position.y = 1483035;
        assertEquals("48PUV7729883034", coder.encode(position));
    }
}
