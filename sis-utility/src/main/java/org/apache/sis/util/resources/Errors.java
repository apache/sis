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
package org.apache.sis.util.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for error messages.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
 * @module
 */
public final class Errors extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @since   0.3 (derived from geotk-2.2)
     * @version 0.3
     * @module
     */
    public static final class Keys {
        private Keys() {
        }

        /**
         * Argument ‘{0}’ shall not be empty.
         */
        public static final int EmptyArgument_1 = 1;

        /**
         * Argument ‘{0}’ can not be an instance of ‘{1}’.
         */
        public static final int IllegalArgumentClass_2 = 17;

        /**
         * Argument ‘{0}’ can not be an instance of ‘{1}’. Expected an instance of ‘{2}’ or derived
         * type.
         */
        public static final int IllegalArgumentClass_3 = 2;

        /**
         * Argument ‘{0}’ can not take the “{1}” value.
         */
        public static final int IllegalArgumentValue_2 = 14;

        /**
         * Illegal value for argument ‘{0}’.
         */
        public static final int IllegalArgument_1 = 15;

        /**
         * Illegal bits pattern: {0}.
         */
        public static final int IllegalBitsPattern_1 = 16;

        /**
         * Class ‘{0}’ is illegal. It must be ‘{1}’ or a derived class.
         */
        public static final int IllegalClass_2 = 3;

        /**
         * The “{0}” language is not recognized.
         */
        public static final int IllegalLanguageCode_1 = 12;

        /**
         * Range [{0} … {1}] is not valid.
         */
        public static final int IllegalRange_2 = 11;

        /**
         * Index {0} is out of bounds.
         */
        public static final int IndexOutOfBounds_1 = 4;

        /**
         * Argument ‘{0}’ shall not be negative. The given value was {1}.
         */
        public static final int NegativeArgument_2 = 8;

        /**
         * Argument ‘{0}’ shall not be NaN (Not-a-Number).
         */
        public static final int NotANumber_1 = 9;

        /**
         * Class ‘{0}’ is not a primitive type wrapper.
         */
        public static final int NotAPrimitiveWrapper_1 = 10;

        /**
         * Argument ‘{0}’ shall not be null.
         */
        public static final int NullArgument_1 = 0;

        /**
         * Recursive call while creating an object for the “{0}” key.
         */
        public static final int RecursiveCreateCallForKey_1 = 18;

        /**
         * Argument ‘{0}’ has {1} dimensions, while {2} was expected.
         */
        public static final int UnexpectedArgumentDimension_3 = 5;

        /**
         * A value is already defined for “{0}”.
         */
        public static final int ValueAlreadyDefined_1 = 13;

        /**
         * Value ‘{0}’={1} is invalid. Expected a number greater than 0.
         */
        public static final int ValueNotGreaterThanZero_2 = 7;

        /**
         * Value ‘{0}’={1} is invalid. Expected a value in the [{2} … {3}] range.
         */
        public static final int ValueOutOfRange_4 = 6;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param filename The file or the JAR entry containing resources.
     */
    Errors(final String filename) {
        super(filename);
    }

    /**
     * Returns the {@code Keys} class.
     */
    @Override
    final Class<?> getKeysClass() throws ClassNotFoundException {
        assert super.getKeysClass() == Keys.class;
        return Keys.class;
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Errors getResources(final Locale locale) throws MissingResourceException {
        return getBundle(Errors.class, locale);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int key) throws MissingResourceException {
        return getResources(null).getString(key);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}"
     * with values of {@code arg0}.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int    key,
                                final Object arg0) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int    key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @param  arg2 Value to substitute to "{2}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int    key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1, arg2);
    }

    /**
     * Gets a string for the given key are replace all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key The key for the desired string.
     * @param  arg0 Value to substitute to "{0}".
     * @param  arg1 Value to substitute to "{1}".
     * @param  arg2 Value to substitute to "{2}".
     * @param  arg3 Value to substitute to "{3}".
     * @return The formatted string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int    key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2,
                                final Object arg3) throws MissingResourceException
    {
        return getResources(null).getString(key, arg0, arg1, arg2, arg3);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -229348959712294902L;

        International(int key)              {super(key);}
        International(int key, Object args) {super(key, args);}
        @Override IndexedResourceBundle getBundle(Locale locale) {
            return getResources(locale);
        }
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key The key for the desired string.
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final int key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * {@note This method is redundant with the one expecting <code>Object...</code>, but avoid
     *        the creation of a temporary array. There is no risk of confusion since the two
     *        methods delegate their work to the same <code>format</code> method anyway.}
     *
     * @param  key The key for the desired string.
     * @param  arg Values to substitute to "{0}".
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final int key, final Object arg) {
        return new International(key, arg);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key  The key for the desired string.
     * @param  args Values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final int key, final Object... args) {
        return new International(key, args);
    }
}
