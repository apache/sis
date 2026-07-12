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
package org.apache.sis.storage.geoheif.internal;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.MissingResourceException;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the GeoHEIF module.
 * Resources in this file should not be used by any other module.
 * For resources shared by many modules in the Apache <abbr>SIS</abbr> project,
 * see the {@code org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
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
         * Returns the value of a field declared in this {@code Keys} class.
         * This method is needed for encapsulation reason, because classes in
         * other modules cannot access this class even by reflection.
         */
        @Override
        protected Object getStaticValue(final Field field) throws IllegalAccessException {
            if (field.getDeclaringClass() == Keys.class) {
                return field.get(null);
            }
            throw new IllegalAccessException();
        }

        /**
         * The ‘{0}’ box is longer than expected.
         */
        public static final short BoxLongerThanExpected_1 = 1;

        /**
         * Cannot decode the Coordinate Reference System.
         */
        public static final short CannotDecodeCRS = 2;

        /**
         * Cannot read the ‘{0}’ box.
         */
        public static final short CannotReadBox_1 = 3;

        /**
         * The ‘{0}’ box appears more than once.
         */
        public static final short DuplicatedBox_1 = 4;

        /**
         * Cannot create the “{0}” resource because the following essential boxes are not handled: {1}.
         */
        public static final short EssentialBoxesIgnored_2 = 5;

        /**
         * Container box ‘{0}’ cannot contain elements of type ‘{1}’.
         */
        public static final short IllegalChildForBox_2 = 6;

        /**
         * Cannot find a {0} image reader.
         */
        public static final short ImageReaderNotFound_1 = 7;

        /**
         * All bands shall be of the same data type.
         */
        public static final short InconsistentBandDataType = 8;

        /**
         * Malformed HEIF file because of invalid box size ({0} bytes).
         */
        public static final short InvalidBoxSize_1 = 9;

        /**
         * Many locations have been found for the “{0}” resource.
         */
        public static final short ManyLocationsForResource_1 = 10;

        /**
         * Missing unit of compressed data.
         */
        public static final short MissingCompressedUnit = 11;

        /**
         * No data has been found for the “{0}” resource.
         */
        public static final short NoDataFoundForResource_1 = 12;

        /**
         * Not an image input stream.
         */
        public static final short NotImageInputStream = 13;

        /**
         * The “{0}” resource has been created but the following boxes have been ignored: {1}.
         */
        public static final short OptionalBoxesIgnored_2 = 14;

        /**
         * The “{0}” resource is empty.
         */
        public static final short ResourceIsEmpty_1 = 15;

        /**
         * The “{0}” resource is protected.
         */
        public static final short ResourceIsProtected_1 = 16;

        /**
         * Stream has an unknown length.
         */
        public static final short StreamOfUnknownLength = 17;

        /**
         * Unexpected construction method.
         */
        public static final short UnexpectedConstructionMethod = 18;

        /**
         * The sample model of the image is unspecified.
         */
        public static final short UnspecifiedSampleModel = 19;

        /**
         * The ‘{0}’ type of box is unsupported.
         */
        public static final short UnsupportedBoxType_1 = 20;

        /**
         * The ‘{0}’ image component type is unsupported.
         */
        public static final short UnsupportedComponent_1 = 21;

        /**
         * This compression is unsupported.
         */
        public static final short UnsupportedCompression = 22;

        /**
         * The ‘{0}’ compression is unsupported.
         */
        public static final short UnsupportedCompression_1 = 23;

        /**
         * The ‘{0}’ reference system encoding is unsupported.
         */
        public static final short UnsupportedCrsEncoding_1 = 24;

        /**
         * The ‘{0}’ image profile is unsupported.
         */
        public static final short UnsupportedImageProfile_1 = 25;

        /**
         * The ‘{0}’ interleave type is unsupported.
         */
        public static final short UnsupportedInterleave_1 = 26;

        /**
         * Unsupported type ‘{1}’ for the “{0}” resource.
         */
        public static final short UnsupportedResourceType_2 = 27;

        /**
         * Resource “{0}” uses an unsupported image model.
         */
        public static final short UnsupportedSampleModel_1 = 28;

        /**
         * Version {1} of the ‘{0}’ box is unsupported.
         */
        public static final short UnsupportedVersion_2 = 29;
    }

    /**
     * Constructs a new resource bundle loading data from
     * the resource file of the same name as this class.
     */
    public Resources() {
    }

    /**
     * Opens the binary file containing the localized resources to load.
     * This method delegates to {@link Class#getResourceAsStream(String)},
     * but this delegation must be done from the same module as the one
     * that provides the binary file.
     */
    @Override
    protected InputStream getResourceAsStream(final String name) {
        return getClass().getResourceAsStream(name);
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
    public static Resources forLocale(final Locale locale) {
        /*
         * We cannot factorize this method into the parent class, because we need to call
         * `ResourceBundle.getBundle(String)` from the module that provides the resources.
         * We do not cache the result because `ResourceBundle` already provides a cache.
         */
        return (Resources) getBundle(Resources.class.getName(), nonNull(locale));
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) {
        return forLocale(null).getString(key);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}"
     * with value of {@code arg0}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0)
    {
        return forLocale(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1);
    }
}
