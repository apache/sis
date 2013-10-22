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
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for single words or short sentences.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
 * @module
 */
public final class Vocabulary extends IndexedResourceBundle {
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
         * Angle
         */
        public static final int Angle = 9;

        /**
         * Degrees
         */
        public static final int AngularDegrees = 10;

        /**
         * Minutes
         */
        public static final int AngularMinutes = 11;

        /**
         * Seconds
         */
        public static final int AngularSeconds = 12;

        /**
         * Attributes
         */
        public static final int Attributes = 45;

        /**
         * Barometric altitude
         */
        public static final int BarometricAltitude = 60;

        /**
         * Character encoding
         */
        public static final int CharacterEncoding = 17;

        /**
         * Classpath
         */
        public static final int Classpath = 29;

        /**
         * {0} code
         */
        public static final int Code_1 = 21;

        /**
         * Commands
         */
        public static final int Commands = 48;

        /**
         * Current date and time
         */
        public static final int CurrentDateTime = 25;

        /**
         * Current directory
         */
        public static final int CurrentDirectory = 33;

        /**
         * Cycle omitted
         */
        public static final int CycleOmitted = 54;

        /**
         * Daylight time
         */
        public static final int DaylightTime = 24;

        /**
         * Destination
         */
        public static final int Destination = 38;

        /**
         * Dimensions
         */
        public static final int Dimensions = 46;

        /**
         * Directory
         */
        public static final int Directory = 36;

        /**
         * Dublin Julian
         */
        public static final int DublinJulian = 56;

        /**
         * Ellipsoidal
         */
        public static final int Ellipsoidal = 61;

        /**
         * Geoidal
         */
        public static final int Geoidal = 62;

        /**
         * Identifier
         */
        public static final int Identifier = 42;

        /**
         * Implementation
         */
        public static final int Implementation = 52;

        /**
         * Index
         */
        public static final int Index = 44;

        /**
         * Java extensions
         */
        public static final int JavaExtensions = 26;

        /**
         * Java home directory
         */
        public static final int JavaHome = 30;

        /**
         * Julian
         */
        public static final int Julian = 57;

        /**
         * Latitude
         */
        public static final int Latitude = 40;

        /**
         * Libraries
         */
        public static final int Libraries = 35;

        /**
         * Local configuration
         */
        public static final int LocalConfiguration = 14;

        /**
         * Locale
         */
        public static final int Locale = 18;

        /**
         * Localization
         */
        public static final int Localization = 19;

        /**
         * Logging
         */
        public static final int Logging = 51;

        /**
         * Longitude
         */
        public static final int Longitude = 41;

        /**
         * Maximum value
         */
        public static final int MaximumValue = 5;

        /**
         * Mean value
         */
        public static final int MeanValue = 6;

        /**
         * Minimum value
         */
        public static final int MinimumValue = 4;

        /**
         * Modified Julian
         */
        public static final int ModifiedJulian = 58;

        /**
         * Name
         */
        public static final int Name = 0;

        /**
         * Number of ‘NaN’
         */
        public static final int NumberOfNaN = 3;

        /**
         * Number of values
         */
        public static final int NumberOfValues = 2;

        /**
         * {0} ({1} of {2})
         */
        public static final int Of_3 = 43;

        /**
         * Offset
         */
        public static final int Offset = 22;

        /**
         * Operating system
         */
        public static final int OperatingSystem = 16;

        /**
         * Options
         */
        public static final int Options = 49;

        /**
         * Other surface
         */
        public static final int OtherSurface = 63;

        /**
         * Others
         */
        public static final int Others = 34;

        /**
         * Paths
         */
        public static final int Paths = 27;

        /**
         * Root
         */
        public static final int Root = 28;

        /**
         * Root Mean Square
         */
        public static final int RootMeanSquare = 7;

        /**
         * Scale
         */
        public static final int Scale = 23;

        /**
         * Source
         */
        public static final int Source = 39;

        /**
         * Standard deviation
         */
        public static final int StandardDeviation = 8;

        /**
         * Temporary files
         */
        public static final int TemporaryFiles = 31;

        /**
         * {0} time
         */
        public static final int Time_1 = 64;

        /**
         * Timezone
         */
        public static final int Timezone = 20;

        /**
         * Truncated Julian
         */
        public static final int TruncatedJulian = 59;

        /**
         * Type
         */
        public static final int Type = 1;

        /**
         * Unavailable content.
         */
        public static final int UnavailableContent = 53;

        /**
         * Untitled
         */
        public static final int Untitled = 37;

        /**
         * User home directory
         */
        public static final int UserHome = 32;

        /**
         * Value
         */
        public static final int Value = 13;

        /**
         * Variables
         */
        public static final int Variables = 47;

        /**
         * {0} version {1}
         */
        public static final int Version_2 = 50;

        /**
         * Versions
         */
        public static final int Versions = 15;

        /**
         * World
         */
        public static final int World = 55;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources The path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    Vocabulary(final URL resources) {
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
    public static Vocabulary getResources(final Locale locale) throws MissingResourceException {
        return getBundle(Vocabulary.class, locale);
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
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -5423999784169092823L;

        International(int key)                   {super(key);}
        International(int key, Object args)      {super(key, args);}
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
