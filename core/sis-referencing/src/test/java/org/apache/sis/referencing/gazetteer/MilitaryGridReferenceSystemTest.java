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

import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


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
     * Returns a coder instance to test.
     */
    private MilitaryGridReferenceSystem.Coder coder() {
        return new MilitaryGridReferenceSystem().createCoder();
    }

    /**
     * Tests encoding of various coordinates.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    public void testEncoding() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D();
        /*
         * 41°N 10°E (UTM zone 32)
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.UTM(41, 10));
        position.x =  584102;
        position.y = 4539239;
        assertEquals("32TNL8410239239", coder.encode(position));
        /*
         * 82°N 10°W (UTM zone 29) — should instantiate a new MGRSEncoder.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.UTM(82, -10));
        position.x =  484463;
        position.y = 9104963;
        assertEquals("29XMM8446304963", coder.encode(position));
        /*
         * 41°S 10°E (UTM zone 32) — should reuse the MGRSEncoder created in first test.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.UTM(-41, 10));
        position.x =  584102;
        position.y = 5460761;
        assertEquals("32GNV8410260761", coder.encode(position));
        /*
         * 82°N 10°E (UTM zone 32) — in this special case, zone 32 is replaced by zone 33.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.UTM(82, 10));
        position.x =  515537;
        position.y = 9104963;
        assertEquals("33XVM2240708183", coder.encode(position));
        /*
         * Same position as previously tested, but using geographic coordinates.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.geographic());
        position.x = 82;
        position.y = 10;
        assertEquals("33XVM2240608183", coder.encode(position));
        position.x = -41;
        position.y = 10;
        assertEquals("32GNV8410260761", coder.encode(position));
    }

    /**
     * Tests encoding of the same coordinate at various precision.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @DependsOnMethod("testEncoding")
    public void testPrecision() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.UTM(13, 103));
        position.x =  377299;
        position.y = 1483035;
        assertEquals("precision", 1, coder.getPrecision(), STRICT);
        assertEquals("48PUV7729983035", coder.encode(position));
        coder.setPrecision(10);
        assertEquals("precision", 10, coder.getPrecision(), STRICT);
        assertEquals("48PUV77298303", coder.encode(position));
        coder.setPrecision(304);
        assertEquals("precision", 100, coder.getPrecision(), STRICT);
        assertEquals("48PUV772830", coder.encode(position));
        coder.setPrecision(1002);
        assertEquals("precision", 1000, coder.getPrecision(), STRICT);
        assertEquals("48PUV7783", coder.encode(position));
        coder.setPrecision(10000);
        assertEquals("precision", 10000, coder.getPrecision(), STRICT);
        assertEquals("48PUV78", coder.encode(position));
        coder.setPrecision(990004);
        assertEquals("precision", 100000, coder.getPrecision(), STRICT);
        assertEquals("48PUV", coder.encode(position));
        coder.setPrecision(1000000);
        assertEquals("precision", 1000000, coder.getPrecision(), STRICT);
        assertEquals("48P", coder.encode(position));
    }

    /**
     * Tests encoding of the same coordinate with various separators, mixed with various precisions.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @DependsOnMethod("testPrecision")
    public void testSeparator() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.UTM(13, 103));
        position.x =  377299;
        position.y = 1483035;
        assertEquals("separator", "", coder.getSeparator());
        assertEquals("48PUV7729983035", coder.encode(position));

        coder.setSeparator(" ");
        assertEquals("separator", " ", coder.getSeparator());
        assertEquals("48 P UV 77299 83035", coder.encode(position));

        coder.setSeparator("/");
        coder.setPrecision(100000);
        assertEquals("separator", "/", coder.getSeparator());
        assertEquals("48/P/UV", coder.encode(position));
    }
}
