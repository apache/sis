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
package org.apache.sis.internal.geotiff;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the {@code sis-geotiff} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.8
     * @module
     */
    @Generated("org.apache.sis.util.resources.IndexedResourceCompiler")
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
         * Apache SIS implementation stipulate that the height of the dithering or halftoning matrix
         * used to create a dithered or halftoned bilevel file is: CellHeight =  “{0}” .
         */
        public static final short CellHeight_1 = 7;

        /**
         * Apache SIS implementation stipulate that the width of the dithering or halftoning matrix
         * used to create a dithered or halftoned bilevel file is: CellWidth =  “{0}” .
         */
        public static final short CellWidth_1 = 8;

        /**
         * Apache SIS implementation requires that all “{0}” elements have the same value, but the
         * element found in “{1}” are {2}.
         */
        public static final short ConstantValueRequired_3 = 0;

        /**
         * Apache SIS implementation stipulate that following tiff tag, named: “{0}”, has not been
         * define corectly. The tag is initialized to the following default value “{1}”.
         */
        public static final short DefaultAttribut_2 = 4;

        /**
         * Apache SIS implementation stipulate that following tag “{0}” , is ignored.
         */
        public static final short IgnoredTag_1 = 6;

        /**
         * Apache SIS implementation stipulate that the Key named  “{0}” , has not been define
         * corectly. Expected value:  “{1}” , found:  “{2}” .
         */
        public static final short KeyValue_3 = 12;

        /**
         * Apache SIS implementation stipulate that length of tiff tag attribut value “{0}” mismatch
         * from other following  “{1}” tiff tag(s) values, expected: “{2}” , found: “{3}”.
         */
        public static final short MismatchLength_4 = 5;

        /**
         * Apache SIS implementation can't read image from  “{0}” because tile and strip tags are
         * missing.
         */
        public static final short MissingTileStrip_1 = 2;

        /**
         * Apache SIS implementation can't read image from  “{1}” because  “{0}” tag is missing.
         */
        public static final short MissingValueRequired_2 = 1;

        /**
         * Apache SIS implementation try to re-build missing “{0}” tiff tag, from other “{1}” tags
         * values.
         */
        public static final short ReBuildAttribut_2 = 3;

        /**
         * Apache SIS implementation stipulate that Threshholding = 1(default value). No dithering or
         * halftoning has been applied to the image data.
         */
        public static final short Threshholding1_0 = 9;

        /**
         * Apache SIS implementation stipulate that Threshholding = 2. An ordered dither or halftone
         * technique has been applied to the image data.
         */
        public static final short Threshholding2_0 = 10;

        /**
         * Apache SIS implementation stipulate that Threshholding = 3. A randomized process such as
         * error diffusion has been applied to the image data.
         */
        public static final short Threshholding3_0 = 11;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
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
     * @throws MissingResourceException if resources can not be found.
     */
    public static Resources forLocale(final Locale locale) throws MissingResourceException {
        return getBundle(Resources.class, locale);
    }
}
