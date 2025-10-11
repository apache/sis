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
package org.apache.sis.gui.internal;

import java.util.List;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link GUIUtilities}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GUIUtilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GUIUtilitiesTest() {
    }

    /**
     * Tests {@link GUIUtilities#appendPathSorted(TreeItem, Comparable...)}
     * and   {@link GUIUtilities#removePathSorted(TreeItem, Comparable...)}.
     */
    @Test
    public void testPathSorted() {
        final TreeItem<Integer> root = new TreeItem<>();
        GUIUtilities.appendPathSorted(root, 5, 2, 6);
        GUIUtilities.appendPathSorted(root, 5, 1, 7);
        GUIUtilities.appendPathSorted(root, 5, 2, 4);
        GUIUtilities.appendPathSorted(root, 5, 1, 7);       // Should be a no-operation.
        /*
         * root
         *  └─5
         *    ├─1
         *    │ └─7
         *    └─2
         *      ├─4
         *      └─6
         */
        {
            TreeItem<Integer> item = assertSingleton(root.getChildren());
            assertEquals(Integer.valueOf(5), item.getValue());

            List<TreeItem<Integer>> list = item.getChildren();
            assertEquals(2, list.size());
            assertEquals(Integer.valueOf(1), list.get(0).getValue());
            assertEquals(Integer.valueOf(2), list.get(1).getValue());
            assertEquals(Integer.valueOf(7), assertSingleton(list.get(0).getChildren()).getValue());

            list = list.get(1).getChildren();
            assertEquals(2, list.size());
            assertEquals(Integer.valueOf(4), list.get(0).getValue());
            assertEquals(Integer.valueOf(6), list.get(1).getValue());
        }
        GUIUtilities.removePathSorted(root, 5, 2, 7);       // Should be a no-operation.
        GUIUtilities.removePathSorted(root, 5, 1, 7);
        /*
         * root
         *  └─5
         *    └─2
         *      ├─4
         *      └─6
         */
        {
            TreeItem<Integer> item = assertSingleton(root.getChildren());
            assertEquals(Integer.valueOf(5), item.getValue());

            item = assertSingleton(item.getChildren());
            assertEquals(Integer.valueOf(2), item.getValue());

            List<TreeItem<Integer>> list = item.getChildren();
            assertEquals(2, list.size());
            assertEquals(Integer.valueOf(4), list.get(0).getValue());
            assertEquals(Integer.valueOf(6), list.get(1).getValue());
        }
        GUIUtilities.removePathSorted(root, 5, 2, 4);
        /*
         * root
         *  └─5
         *    └─2
         *      └─6
         */
        {
            TreeItem<Integer> item = assertSingleton(root.getChildren());
            assertEquals(Integer.valueOf(5), item.getValue());

            item = assertSingleton(item.getChildren());
            assertEquals(Integer.valueOf(2), item.getValue());

            item = assertSingleton(item.getChildren());
            assertEquals(Integer.valueOf(6), item.getValue());
        }
        GUIUtilities.removePathSorted(root, 5, 2, 6);
        assertTrue(root.getChildren().isEmpty());
    }

    /**
     * Tests {@link GUIUtilities#longestCommonSubsequence(List, List)}.
     */
    @Test
    public void testLongestCommonSubsequence() {
        final List<Integer> x = List.of(1, 2, 4, 6, 7,    9);
        final List<Integer> y = List.of(1, 2,    3, 7, 8);
        assertEquals(List.of(1, 2, 7), GUIUtilities.longestCommonSubsequence(x, y));
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
