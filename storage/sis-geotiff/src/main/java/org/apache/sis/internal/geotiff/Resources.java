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
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code sis-geotiff} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.8
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
         * TIFF file “{0}” has circular references in its chain of images.
         */
        public static final short CircularImageReference_1 = 1;

        /**
         * No value specified for the “{0}” TIFF tag. Computed the {1} value from other tags.
         */
        public static final short ComputedValueForAttribute_2 = 2;

        /**
         * Apache SIS implementation requires that all “{0}” elements have the same value, but the
         * element found in “{1}” are {2}.
         */
        public static final short ConstantValueRequired_3 = 3;

        /**
         * No value specified for the “{0}” TIFF tag. The {1} default value will be used.
         */
        public static final short DefaultValueForAttribute_2 = 4;

        /**
         * An ordered dither or halftone technique has been applied to the image data. The dithering or
         * halftoning matrix size is {0}×{1}.
         */
        public static final short DitheringOrHalftoningApplied_2 = 5;

        /**
         * The following GeoTIFF keys have been ignored: {0}
         */
        public static final short IgnoredGeoKeys_1 = 6;

        /**
         * The “{0}” TIFF tag has been ignored.
         */
        public static final short IgnoredTag_1 = 7;

        /**
         * Map projection parameter “{2}” will be ignored because its value ({3}) is inconsistent with
         * the “{0} = {1}” parameter.
         */
        public static final short InconsistentMapProjParameter_4 = 24;

        /**
         * TIFF image “{0}” shall be either tiled or organized into strips.
         */
        public static final short InconsistentTileStrip_1 = 8;

        /**
         * “{1}” is not a valid value for the “{0}” GeoTIFF key.
         */
        public static final short InvalidGeoValue_2 = 9;

        /**
         * TIFF tag “{0}” shall contain at least {1} values but found only {2}.
         */
        public static final short ListTooShort_3 = 10;

        /**
         * TIFF tags “{0}” and “{1}” have values of different lengths. Found “{2}” and “{3}” elements
         * respectively.
         */
        public static final short MismatchedLength_4 = 11;

        /**
         * No value has been found for the “{0}” GeoTIFF key.
         */
        public static final short MissingGeoValue_1 = 12;

        /**
         * Can not read TIFF image from “{0}” because the “{1}” tag is missing.
         */
        public static final short MissingValue_2 = 13;

        /**
         * The file defines “{2}” with value {3}{4}, but that value should be {1}{4} according parent
         * definition ({0}).
         */
        public static final short NotTheEpsgValue_5 = 14;

        /**
         * A randomized process such as error diffusion has been applied to the image data.
         */
        public static final short RandomizedProcessApplied = 15;

        /**
         * The “{0}” GeoTIFF file does not specify the values format.
         */
        public static final short UndefinedDataFormat_1 = 25;

        /**
         * A single value was expected for the “{0}” key but {1} values have been found.
         */
        public static final short UnexpectedListOfValues_2 = 16;

        /**
         * The “{1}” parameter was not expected for the “{0}” projection method.
         */
        public static final short UnexpectedParameter_2 = 17;

        /**
         * Found {2} tiles or strips in the “{0}” file while {1} were expected.
         */
        public static final short UnexpectedTileCount_3 = 18;

        /**
         * TIFF file “{0}” uses an unknown coordinate reference system.
         */
        public static final short UnknownCRS_1 = 22;

        /**
         * Coordinate system kind {0} is unsupported.
         */
        public static final short UnsupportedCoordinateSystemKind_1 = 19;

        /**
         * Version {0} of GeoTIFF key directory is not supported.
         */
        public static final short UnsupportedGeoKeyDirectory_1 = 20;

        /**
         * Unsupported storage location for the “{0}” GeoTIFF value.
         */
        public static final short UnsupportedGeoKeyStorage_1 = 21;

        /**
         * TIFF file “{0}” uses an unsupported map projection.
         */
        public static final short UnsupportedProjectionMethod_1 = 23;
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

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = 8489130907339662434L;

        International(short key)                           {super(key);}
        International(short key, Object args)              {super(key, args);}
        @Override protected KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override protected IndexedResourceBundle getBundle(final Locale locale) {
            return forLocale(locale);
        }
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key  the key for the desired string.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key   the key for the desired string.
     * @param  args  values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object... args) {
        return new International(key, args);
    }
}
