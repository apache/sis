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
package org.apache.sis.internal.feature;

import java.net.URL;
import java.util.Map;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Warning and error messages that are specific to the {@code sis-feature} module.
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
         * Feature type ‘{0}’ is abstract.
         */
        public static final short AbstractFeatureType_1 = 1;

        /**
         * Can not assign characteristics to the “{0}” property.
         */
        public static final short CanNotAssignCharacteristics_1 = 2;

        /**
         * Property “{0}” is not a type that can be instantiated.
         */
        public static final short CanNotInstantiateProperty_1 = 3;

        /**
         * Can not set a value of type ‘{1}’ to characteristic “{0}”.
         */
        public static final short CanNotSetCharacteristics_2 = 4;

        /**
         * Type of the “{0}” property does not allow to set a value.
         */
        public static final short CanNotSetPropertyValue_1 = 5;

        /**
         * Characteristics “{1}” already exists in attribute “{0}”.
         */
        public static final short CharacteristicsAlreadyExists_2 = 6;

        /**
         * No characteristics named “{1}” has been found in “{0}” attribute.
         */
        public static final short CharacteristicsNotFound_2 = 7;

        /**
         * Operation “{0}” requires a “{1}” property, but no such property has been found in “{2}”.
         */
        public static final short DependencyNotFound_3 = 8;

        /**
         * Association “{0}” does not accept features of type ‘{2}’. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalFeatureType_3 = 9;

        /**
         * Type or result of “{0}” property can not be ‘{1}’ for this operation.
         */
        public static final short IllegalPropertyType_2 = 10;

        /**
         * Property “{0}” does not accept values of type ‘{2}’. Expected an instance of ‘{1}’ or
         * derived type.
         */
        public static final short IllegalPropertyValueClass_3 = 11;

        /**
         * Mismatched type for “{0}” property.
         */
        public static final short MismatchedPropertyType_1 = 12;

        /**
         * An attribute for ‘{1}’ values where expected, but the “{0}” attribute specifies values of
         * type ‘{2}’.
         */
        public static final short MismatchedValueClass_3 = 13;

        /**
         * Property “{0}” contains more than one value.
         */
        public static final short NotASingleton_1 = 14;

        /**
         * The {0} optional library is not available. Geometric operations will ignore that library.
         * Cause is {1}.
         */
        public static final short OptionalLibraryNotFound_2 = 19;

        /**
         * Property “{1}” already exists in feature “{0}”.
         */
        public static final short PropertyAlreadyExists_2 = 15;

        /**
         * No property named “{1}” has been found in “{0}” feature.
         */
        public static final short PropertyNotFound_2 = 16;

        /**
         * The {0} geometry library is not available in current runtime environment.
         */
        public static final short UnavailableGeometryLibrary_1 = 21;

        /**
         * The “{1}” value given to “{0}” property should be separable in {2} components, but we got
         * {3}.
         */
        public static final short UnexpectedNumberOfComponents_4 = 17;

        /**
         * Feature named “{0}” has not yet been resolved.
         */
        public static final short UnresolvedFeatureName_1 = 18;

        /**
         * Unsupported geometry {0}D object.
         */
        public static final short UnsupportedGeometryObject_1 = 20;
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
     * Returns resources in the locale specified in the given property map. This convenience method looks
     * for the {@link #LOCALE_KEY} entry. If the given map is null, or contains no entry for the locale key,
     * or the value is not an instance of {@link Locale}, then this method fallback on the default locale.
     *
     * @param  properties  the map of properties, or {@code null} if none.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     *
     * @since 0.4
     */
    public static Resources forProperties(final Map<?,?> properties) throws MissingResourceException {
        return forLocale(getLocale(properties));
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
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @param  arg2  value to substitute to "{2}".
     * @param  arg3  value to substitute to "{3}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2,
                                final Object arg3) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1, arg2, arg3);
    }
}
