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

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javafx.scene.paint.Color;


/**
 * Provides a name for a given {@link Color} instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ColorName {
    /**
     * Do not allow instantiation of this class.
     */
    private ColorName() {
    }

    /**
     * The color names.
     */
    private static final Map<Color,String> NAMES = new HashMap<>(175);
    static {
        final StringBuilder buffer = new StringBuilder();
        for (final Field field : Color.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Color.class.equals(field.getType())) try {
                final String name = field.getName();
                buffer.append(name.toLowerCase());          // Default locale is okay here.
                buffer.setCharAt(0, name.charAt(0));        // Code point not used in Color API.
                NAMES.put((Color) field.get(null), buffer.toString());
                buffer.setLength(0);
            } catch (Exception e) {
                // Ignore. The map is only informative.
            }
        }
    }

    /**
     * Returns the name of given color.
     *
     * @param  color  color for which to get a name.
     * @return name of given color, or hexadecimal code if the given code does not have a known name.
     */
    public static String of(final Color color) {
        String name = NAMES.get(color);
        if (name == null) {
            name = Integer.toHexString(GUIUtilities.toARGB(color));
        }
        return name;
    }

    /**
     * Returns the name of given ARGB code.
     *
     * @param  color  color for which to get a name.
     * @return name of given color, or hexadecimal code if the given code does not have a known name.
     */
    public static String of(final int color) {
        String name = NAMES.get(GUIUtilities.fromARGB(color));
        if (name == null) {
            name = Integer.toHexString(color);
        }
        return name;
    }
}
