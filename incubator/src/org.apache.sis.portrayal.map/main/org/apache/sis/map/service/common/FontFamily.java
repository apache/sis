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
package org.apache.sis.map.service.common;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.map.service.Scene2D;


/**
 * A font family name of a style, resolved to the name of a Java font family.
 *
 * <ul>
 *   <li>The generic names {@code serif}, {@code sans-serif} and {@code monospace} are mapped to the Java logical fonts.</li>
 *   <li>Other names are the names of the installed font families.</li>
 *   <li>Names are case-insensitive and leading or trailing spaces are ignored.</li>
 * </ul>
 *
 * @param name  the family name in the style, or {@code null}.
 *
 * @author Johann Sorel (Geomatys)
 */
public record FontFamily(String name) {
    /**
     * Java logical fonts by lower-case generic family name.
     */
    private static final Map<String,String> GENERIC_FAMILIES = Map.of(
            "serif",       Font.SERIF,
            "sans-serif",  Font.SANS_SERIF,
            "sansserif",   Font.SANS_SERIF,
            "monospace",   Font.MONOSPACED,
            "monospaced",  Font.MONOSPACED,
            "dialog",      Font.DIALOG,
            "dialoginput", Font.DIALOG_INPUT);

    /**
     * Installed font families by lower-case name. Loaded when first needed.
     */
    private static final class Installed {
        /**
         * Installed font families by lower-case name.
         */
        static final Map<String,String> FAMILIES = new HashMap<>();
        static {
            try {
                for (final String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)) {
                    FAMILIES.put(family.toLowerCase(Locale.ROOT), family);
                }
            } catch (RuntimeException | Error e) {
                Scene2D.LOGGER.log(Level.WARNING, "Cannot list the installed fonts.", e);
            }
        }

        /**
         * Do not allow instantiation of this class.
         */
        private Installed() {
        }
    }

    /**
     * Returns the Java font family of this name.
     *
     * @return the Java logical font or the installed family, or {@code null} if not available.
     */
    public String javaName() {
        if (name == null) {
            return null;
        }
        final String key = name.strip().toLowerCase(Locale.ROOT);
        final String logical = GENERIC_FAMILIES.get(key);
        return (logical != null) ? logical : Installed.FAMILIES.get(key);
    }
}
