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
package org.apache.sis.swing.internal;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Resources for the Swing widgets.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     */
    public static final class Keys extends KeyConstants {
        /**
         * The unique instance of key constants handler.
         */
        static final Keys INSTANCE = new Keys();

        /**
         * For {@link #INSTANCE} creation only.
         */
        private Keys() {
        }

        /**
         * Cancel
         */
        public static final short Cancel = 16;

        /**
         * Close
         */
        public static final short Close = 14;

        /**
         * Debug
         */
        public static final short Debug = 13;

        /**
         * Down
         */
        public static final short Down = 4;

        /**
         * Error
         */
        public static final short Error = 11;

        /**
         * Hide
         */
        public static final short Hide = 18;

        /**
         * Bad entry.
         */
        public static final short IllegalEntry = 15;

        /**
         * Left
         */
        public static final short Left = 1;

        /**
         * Magnifier
         */
        public static final short Magnifier = 20;

        /**
         * {0} (no details)
         */
        public static final short NoDetails_1 = 12;

        /**
         * Ok
         */
        public static final short Ok = 17;

        /**
         * Reset
         */
        public static final short Reset = 5;

        /**
         * Right
         */
        public static final short Right = 2;

        /**
         * Rotate left
         */
        public static final short RotateLeft = 6;

        /**
         * Rotate right
         */
        public static final short RotateRight = 7;

        /**
         * Show magnifier
         */
        public static final short ShowMagnifier = 19;

        /**
         * Up
         */
        public static final short Up = 3;

        /**
         * Zoom in
         */
        public static final short ZoomIn = 8;

        /**
         * Close zoom
         */
        public static final short ZoomMax = 9;

        /**
         * Zoom out
         */
        public static final short ZoomOut = 10;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there are no resources. The resources may be a file or an entry in a JAR file.
     */
    public Resources(final URL resources) {
        super(resources);
    }

    /**
     * Returns the handle for the {@code Keys} constants.
     *
     * @return a handler for the constants declared in the inner {@code Keys} class.
     */
    @Override
    protected KeyConstants getKeyConstants() {
        return Keys.INSTANCE;
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale  the locale, or {@code null} for the default locale.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources cannot be found.
     */
    public static Resources forLocale(final Locale locale) throws MissingResourceException {
        return getBundle(Resources.class, locale);
    }
}
