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
package org.apache.sis.internal.gui;

import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link GUIUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class GUIUtilitiesTest extends TestCase {
    /**
     * Tests {@link GUIUtilities#longestCommonSubsequence(List, List)}.
     */
    @Test
    public void testLongestCommonSubsequence() {
        final List<Integer> x = Arrays.asList(1, 2, 4, 6, 7,    9);
        final List<Integer> y = Arrays.asList(1, 2,    3, 7, 8);
        assertEquals(Arrays.asList(1, 2, 7), GUIUtilities.longestCommonSubsequence(x, y));
    }

    /**
     * Tests {@link GUIUtilities#fromARGB(int)}.
     */
    @Test
    public void testFromARGB() {
        final java.awt.Color reference = java.awt.Color.ORANGE;
        final Color color = GUIUtilities.fromARGB(reference.getRGB());
        assertEquals(reference.getRed(),   StrictMath.round(255 * color.getRed()));
        assertEquals(reference.getGreen(), StrictMath.round(255 * color.getGreen()));
        assertEquals(reference.getBlue(),  StrictMath.round(255 * color.getBlue()));
        assertEquals(reference.getAlpha(), StrictMath.round(255 * color.getOpacity()));
   }

    /**
     * Tests {@link GUIUtilities#toARGB(Color)}.
     */
    @Test
    public void testToARGB() {
        final int ARGB = GUIUtilities.toARGB(Color.ORANGE);
        final java.awt.Color reference = new java.awt.Color(ARGB);
        assertEquals(0xFF, reference.getRed());
        assertEquals(0xA5, reference.getGreen());
        assertEquals(0x00, reference.getBlue());
        assertEquals(0xFF, reference.getAlpha());
    }
}
