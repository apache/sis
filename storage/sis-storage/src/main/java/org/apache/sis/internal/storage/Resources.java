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
package org.apache.sis.internal.storage;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code sis-storage} module.
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
     * @since   0.3
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
         * Name “{3}” is ambiguous because it can be understood as either “{1}” or “{2}” in the context
         * of “{0}” data.
         */
        public static final short AmbiguousName_4 = 15;

        /**
         * Can not read “{1}” as a file in the {0} format.
         */
        public static final short CanNotReadFile_2 = 1;

        /**
         * Can not read line {2} of “{1}” as part of a file in the {0} format.
         */
        public static final short CanNotReadFile_3 = 2;

        /**
         * Can not read line {2} (after column {3}) of “{1}” as part of a file in the {0} format.
         */
        public static final short CanNotReadFile_4 = 3;

        /**
         * This {0} reader is closed.
         */
        public static final short ClosedReader_1 = 4;

        /**
         * This {0} writer is closed.
         */
        public static final short ClosedWriter_1 = 5;

        /**
         * One or more read operations are in progress in the “{0}” data store.
         */
        public static final short ConcurrentRead_1 = 19;

        /**
         * A write operation is in progress in the “{0}” data store.
         */
        public static final short ConcurrentWrite_1 = 20;

        /**
         * Character string in the “{0}” file is too long. The string has {2} characters while the
         * limit is {1}.
         */
        public static final short ExcessiveStringSize_3 = 6;

        /**
         * A feature named “{1}” is already present in the “{0}” data store.
         */
        public static final short FeatureAlreadyPresent_2 = 16;

        /**
         * Feature “{1}” has not been found in the “{0}” data store.
         */
        public static final short FeatureNotFound_2 = 17;

        /**
         * The {0} data store does not accept features of type “{1}”.
         */
        public static final short IllegalFeatureType_2 = 7;

        /**
         * The {0} reader does not accept inputs of type ‘{1}’.
         */
        public static final short IllegalInputTypeForReader_2 = 8;

        /**
         * The {0} writer does not accept outputs of type ‘{1}’.
         */
        public static final short IllegalOutputTypeForWriter_2 = 9;

        /**
         * Components of the “{1}” name are inconsistent with those of the name previously binded in
         * “{0}” data store.
         */
        public static final short InconsistentNameComponents_2 = 10;

        /**
         * Missing scheme in “{0}” URI.
         */
        public static final short MissingSchemeInURI_1 = 11;

        /**
         * Processing executed on {0}.
         */
        public static final short ProcessingExecutedOn_1 = 12;

        /**
         * The “{1}” element must be declared before “{0}”.
         */
        public static final short ShallBeDeclaredBefore_2 = 22;

        /**
         * Can not move backward in the “{0}” stream.
         */
        public static final short StreamIsForwardOnly_1 = 13;

        /**
         * The “{0}” data store can be read only once.
         */
        public static final short StreamIsReadOnce_1 = 18;

        /**
         * Can not modify previously written data in “{0}”.
         */
        public static final short StreamIsWriteOnce_1 = 21;

        /**
         * Format of “{0}” is not recognized.
         */
        public static final short UnknownFormatFor_1 = 14;
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
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return forLocale(null).getString(key);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}"
     * with values of {@code arg0}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
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

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @param  arg2  value to substitute to "{2}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1, arg2);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -7265791441872360274L;

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
