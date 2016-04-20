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

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for miscellaneous (often logging) messages.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class Messages extends IndexedResourceBundle {
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
         * {0} “{1}” is already registered. The second instance will be ignored.
         */
        public static final short AlreadyRegistered_2 = 0;

        /**
         * Ambiguity between inverse flattening and semi minor axis length for “{0}”. Using inverse
         * flattening.
         */
        public static final short AmbiguousEllipsoid_1 = 30;

        /**
         * Can not create the {0} schema in database.
         */
        public static final short CanNotCreateSchema_1 = 41;

        /**
         * Can not instantiate the object of type ‘{0}’ identified by “{1}”. Reason is:{2}
         */
        public static final short CanNotInstantiateForIdentifier_3 = 35;

        /**
         * Changed the container capacity from {0} to {1} elements.
         */
        public static final short ChangedContainerCapacity_2 = 1;

        /**
         * Configuration of “{0}” received on {1} in {2} seconds.
         */
        public static final short ConfigurationOf_3 = 31;

        /**
         * This result indicates if a datum shift method has been applied.
         */
        public static final short ConformanceMeansDatumShift = 8;

        /**
         * This parameter is shown for completeness, but should never have a value different than {0}
         * for this projection.
         */
        public static final short ConstantProjParameterValue_1 = 11;

        /**
         * Created an instance of ‘{0}’ from the “{1}” identifier in {2} seconds.
         */
        public static final short CreateDurationFromIdentifier_3 = 33;

        /**
         * Created an instance of ‘{0}’ in {1} seconds.
         */
        public static final short CreateDuration_2 = 34;

        /**
         * Created an instance of ‘{0}’ named “{1}” with the “{2}” identifier.
         */
        public static final short CreatedIdentifiedObject_3 = 17;

        /**
         * Created an instance of ‘{0}’ named “{1}”.
         */
        public static final short CreatedNamedObject_2 = 16;

        /**
         * Creating {0} schema in the “{1}” database.
         */
        public static final short CreatingSchema_2 = 39;

        /**
         * {0} dataset version {1} on “{2}” version {3}.
         */
        public static final short DataBase_4 = 28;

        /**
         * The {0} environment variable is defined, but the given “{1}” value is not an existing
         * directory.
         */
        public static final short DataDirectoryDoesNotExist_2 = 20;

        /**
         * Apache SIS is not authorized to access the “{1}” sub-directory in the directory given by the
         * {0} environment variable.
         */
        public static final short DataDirectoryNotAccessible_2 = 26;

        /**
         * Apache SIS is not authorized to read information given by the “{0}” environment variable.
         */
        public static final short DataDirectoryNotAuthorized_1 = 25;

        /**
         * The “{1}” directory specified by the {0} environment variable exists but is not readable.
         */
        public static final short DataDirectoryNotReadable_2 = 21;

        /**
         * The “{0}” environment variable is not set.
         */
        public static final short DataDirectoryNotSpecified_1 = 22;

        /**
         * Apache SIS can not write in the “{1}” directory given by the {0} environment variable.
         */
        public static final short DataDirectoryNotWritable_2 = 23;

        /**
         * Environment variable {0} specifies the “{1}” data directory.
         */
        public static final short DataDirectory_2 = 24;

        /**
         * Code “{0}” is deprecated and replaced by code {1}. Reason is: {2}
         */
        public static final short DeprecatedCode_3 = 29;

        /**
         * Property “{0}” has been discarded in favor of “{1}”, because those two properties are
         * mutually exclusive.
         */
        public static final short DiscardedExclusiveProperty_2 = 2;

        /**
         * There is no factory for version {1} of “{0}” authority. Fallback on default version for
         * objects creation.
         */
        public static final short FallbackDefaultFactoryVersion_2 = 38;

        /**
         * Ignored properties after the first occurrence of ‘{0}’.
         */
        public static final short IgnoredPropertiesAfterFirst_1 = 6;

        /**
         * Ignored property associated to ‘{0}’.
         */
        public static final short IgnoredPropertyAssociatedTo_1 = 7;

        /**
         * More than one service provider of type ‘{0}’ are declared for “{1}”. Only the first provider
         * (an instance of ‘{2}’) will be used.
         */
        public static final short IgnoredServiceProvider_3 = 36;

        /**
         * Parsing of “{0}” done, but some elements were ignored.
         */
        public static final short IncompleteParsing_1 = 14;

        /**
         * Inserted {0} records in {1} seconds.
         */
        public static final short InsertDuration_2 = 40;

        /**
         * Inverse operation uses this parameter value with opposite sign.
         */
        public static final short InverseOperationUsesOppositeSign = 42;

        /**
         * Inverse operation uses the same parameter value.
         */
        public static final short InverseOperationUsesSameSign = 43;

        /**
         * No object associated to the “{0}” JNDI name.
         */
        public static final short JNDINotSpecified_1 = 32;

        /**
         * Loading datum shift file “{0}”.
         */
        public static final short LoadingDatumShiftFile_1 = 27;

        /**
         * Text were discarded for some locales.
         */
        public static final short LocalesDiscarded = 3;

        /**
         * The “{1}” parameter could have been omitted. But it has been given a value of {2} which does
         * not match the definition of the “{0}” ellipsoid.
         */
        public static final short MismatchedEllipsoidAxisLength_3 = 9;

        /**
         * No coordinate operation from “{0}” to “{1}” because of mismatched factories.
         */
        public static final short MismatchedOperationFactories_2 = 37;

        /**
         * Despite its name, this parameter is effectively “{0}”.
         */
        public static final short MisnamedParameter_1 = 18;

        /**
         * Formatting of “{0}” is not conform to the format standard.
         */
        public static final short NonConformFormatting_1 = 15;

        /**
         * This parameter borrowed from the “{0}” projection is not formally a parameter of this
         * projection.
         */
        public static final short NotFormalProjectionParameter_1 = 10;

        /**
         * Optional module “{0}” requested but not found.
         */
        public static final short OptionalModuleNotFound_1 = 12;

        /**
         * Property “{0}” is hidden by “{1}”.
         */
        public static final short PropertyHiddenBy_2 = 4;

        /**
         * The text contains unknown elements:
         */
        public static final short UnknownElementsInText = 13;

        /**
         * Loading of “{0}” done, but some records were ignored because of unrecognized keywords: {1}
         */
        public static final short UnknownKeywordInRecord_2 = 19;

        /**
         * Can not parse “{1}” as an instance of ‘{0}’. The value is stored as plain text instead, but
         * will be ignored by some processing.
         */
        public static final short UnparsableValueStoredAsText_2 = 5;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources The path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    Messages(final URL resources) {
        super(resources);
    }

    /**
     * Returns the handle for the {@code Keys} constants.
     */
    @Override
    final KeyConstants getKeyConstants() {
        return Keys.INSTANCE;
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Messages getResources(final Locale locale) throws MissingResourceException {
        return getBundle(Messages.class, locale);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
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
    public static String format(final short  key,
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
    public static String format(final short  key,
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
    public static String format(final short  key,
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
    public static String format(final short  key,
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
        private static final long serialVersionUID = 4553487496835099424L;

        International(short key)                 {super(key);}
        International(short key, Object args)    {super(key, args);}
        @Override KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override IndexedResourceBundle getBundle(final Locale locale) {
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
    public static InternationalString formatInternational(final short key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * <div class="note"><b>API note:</b>
     * This method is redundant with the one expecting {@code Object...}, but avoid the creation
     * of a temporary array. There is no risk of confusion since the two methods delegate their
     * work to the same {@code format} method anyway.</div>
     *
     * @param  key The key for the desired string.
     * @param  arg Values to substitute to "{0}".
     * @return An international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object arg) {
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
    public static InternationalString formatInternational(final short key, final Object... args) {
        return new International(key, args);
    }
}
