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
import java.util.Map;
import java.util.Locale;
import java.util.MissingResourceException;
import javax.annotation.Generated;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for single words or short sentences.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.7
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
         * Accuracy
         */
        public static final short Accuracy = 109;

        /**
         * Aliases
         */
        public static final short Aliases = 74;

        /**
         * Angle
         */
        public static final short Angle = 0;

        /**
         * Degrees
         */
        public static final short AngularDegrees = 1;

        /**
         * Minutes
         */
        public static final short AngularMinutes = 2;

        /**
         * Seconds
         */
        public static final short AngularSeconds = 3;

        /**
         * Attributes
         */
        public static final short Attributes = 4;

        /**
         * Axis changes
         */
        public static final short AxisChanges = 98;

        /**
         * Barometric altitude
         */
        public static final short BarometricAltitude = 5;

        /**
         * Cardinality
         */
        public static final short Cardinality = 76;

        /**
         * Caused by {0}
         */
        public static final short CausedBy_1 = 107;

        /**
         * Character encoding
         */
        public static final short CharacterEncoding = 6;

        /**
         * Characteristics
         */
        public static final short Characteristics = 85;

        /**
         * Classpath
         */
        public static final short Classpath = 7;

        /**
         * {0} code
         */
        public static final short Code_1 = 8;

        /**
         * Commands
         */
        public static final short Commands = 9;

        /**
         * Constant pressure surface
         */
        public static final short ConstantPressureSurface = 19;

        /**
         * Container
         */
        public static final short Container = 92;

        /**
         * Correlation
         */
        public static final short Correlation = 83;

        /**
         * Current date and time
         */
        public static final short CurrentDateTime = 10;

        /**
         * Current directory
         */
        public static final short CurrentDirectory = 11;

        /**
         * Cycle omitted
         */
        public static final short CycleOmitted = 12;

        /**
         * Database
         */
        public static final short DataBase = 96;

        /**
         * Data directory
         */
        public static final short DataDirectory = 88;

        /**
         * Datum
         */
        public static final short Datum = 99;

        /**
         * Datum shift
         */
        public static final short DatumShift = 100;

        /**
         * Daylight time
         */
        public static final short DaylightTime = 13;

        /**
         * Default value
         */
        public static final short DefaultValue = 71;

        /**
         * Derived from {0}
         */
        public static final short DerivedFrom_1 = 108;

        /**
         * Description
         */
        public static final short Description = 75;

        /**
         * Destination
         */
        public static final short Destination = 14;

        /**
         * Details
         */
        public static final short Details = 110;

        /**
         * Dimensions
         */
        public static final short Dimensions = 15;

        /**
         * Directory
         */
        public static final short Directory = 16;

        /**
         * ″
         */
        public static final short DittoMark = 82;

        /**
         * Dublin Julian
         */
        public static final short DublinJulian = 17;

        /**
         * Ellipsoid
         */
        public static final short Ellipsoid = 70;

        /**
         * Ellipsoid change
         */
        public static final short EllipsoidChange = 101;

        /**
         * Ellipsoidal height
         */
        public static final short EllipsoidalHeight = 18;

        /**
         * Geocentric
         */
        public static final short Geocentric = 102;

        /**
         * Geocentric conversion
         */
        public static final short GeocentricConversion = 103;

        /**
         * Geocentric radius
         */
        public static final short GeocentricRadius = 104;

        /**
         * Geodetic dataset
         */
        public static final short GeodeticDataset = 95;

        /**
         * Height
         */
        public static final short Height = 69;

        /**
         * Identifier
         */
        public static final short Identifier = 20;

        /**
         * Identity
         */
        public static final short Identity = 105;

        /**
         * Implementation
         */
        public static final short Implementation = 21;

        /**
         *  in 
         */
        public static final short InBetweenWords = 87;

        /**
         * Index
         */
        public static final short Index = 22;

        /**
         * Invalid
         */
        public static final short Invalid = 89;

        /**
         * Inverse operation
         */
        public static final short InverseOperation = 106;

        /**
         * Java extensions
         */
        public static final short JavaExtensions = 23;

        /**
         * Java home directory
         */
        public static final short JavaHome = 24;

        /**
         * Julian
         */
        public static final short Julian = 25;

        /**
         * Latitude
         */
        public static final short Latitude = 26;

        /**
         * Legend
         */
        public static final short Legend = 97;

        /**
         * Level
         */
        public static final short Level = 94;

        /**
         * Libraries
         */
        public static final short Libraries = 27;

        /**
         * Local configuration
         */
        public static final short LocalConfiguration = 28;

        /**
         * Locale
         */
        public static final short Locale = 29;

        /**
         * Localization
         */
        public static final short Localization = 30;

        /**
         * Logging
         */
        public static final short Logging = 31;

        /**
         * Longitude
         */
        public static final short Longitude = 32;

        /**
         * Mandatory
         */
        public static final short Mandatory = 77;

        /**
         * Maximum value
         */
        public static final short MaximumValue = 33;

        /**
         * Mean value
         */
        public static final short MeanValue = 34;

        /**
         * Methods
         */
        public static final short Methods = 111;

        /**
         * Minimum value
         */
        public static final short MinimumValue = 35;

        /**
         * Modified Julian
         */
        public static final short ModifiedJulian = 36;

        /**
         * Name
         */
        public static final short Name = 37;

        /**
         * None
         */
        public static final short None = 91;

        /**
         * Number of ‘NaN’
         */
        public static final short NumberOfNaN = 38;

        /**
         * Number of values
         */
        public static final short NumberOfValues = 39;

        /**
         * Obligation
         */
        public static final short Obligation = 78;

        /**
         * {0} ({1} of {2})
         */
        public static final short Of_3 = 40;

        /**
         * Offset
         */
        public static final short Offset = 41;

        /**
         * Operating system
         */
        public static final short OperatingSystem = 42;

        /**
         * Optional
         */
        public static final short Optional = 79;

        /**
         * Options
         */
        public static final short Options = 43;

        /**
         * Other surface
         */
        public static final short OtherSurface = 44;

        /**
         * Others
         */
        public static final short Others = 45;

        /**
         * Paths
         */
        public static final short Paths = 46;

        /**
         * “{0}”
         */
        public static final short Quoted_1 = 86;

        /**
         * Remote configuration
         */
        public static final short RemoteConfiguration = 93;

        /**
         * Root
         */
        public static final short Root = 47;

        /**
         * Root Mean Square
         */
        public static final short RootMeanSquare = 48;

        /**
         * Scale
         */
        public static final short Scale = 49;

        /**
         * Source
         */
        public static final short Source = 50;

        /**
         * Standard deviation
         */
        public static final short StandardDeviation = 51;

        /**
         * Superseded by {0}.
         */
        public static final short SupersededBy_1 = 84;

        /**
         * Temporal
         */
        public static final short Temporal = 66;

        /**
         * Temporary files
         */
        public static final short TemporaryFiles = 52;

        /**
         * Time
         */
        public static final short Time = 67;

        /**
         * {0} time
         */
        public static final short Time_1 = 53;

        /**
         * Timezone
         */
        public static final short Timezone = 54;

        /**
         * Transformation
         */
        public static final short Transformation = 80;

        /**
         * Transformation accuracy
         */
        public static final short TransformationAccuracy = 81;

        /**
         * Truncated Julian
         */
        public static final short TruncatedJulian = 55;

        /**
         * Type
         */
        public static final short Type = 56;

        /**
         * Unavailable content.
         */
        public static final short UnavailableContent = 57;

        /**
         * Units
         */
        public static final short Units = 72;

        /**
         * Unnamed
         */
        public static final short Unnamed = 65;

        /**
         * Untitled
         */
        public static final short Untitled = 58;

        /**
         * User home directory
         */
        public static final short UserHome = 59;

        /**
         * Value
         */
        public static final short Value = 60;

        /**
         * Value domain
         */
        public static final short ValueDomain = 73;

        /**
         * Variables
         */
        public static final short Variables = 61;

        /**
         * {0} version {1}
         */
        public static final short Version_2 = 62;

        /**
         * Versions
         */
        public static final short Versions = 63;

        /**
         * Vertical
         */
        public static final short Vertical = 68;

        /**
         * Warnings
         */
        public static final short Warnings = 90;

        /**
         * World
         */
        public static final short World = 64;
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
     * Returns resources in the locale specified in the given property map. This convenience method looks
     * for the {@link #LOCALE_KEY} entry. If the given map is null, or contains no entry for the locale key,
     * or the value is not an instance of {@link Locale}, then this method fallback on the default locale.
     *
     * @param  properties  the map of properties, or {@code null} if none.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources can not be found.
     *
     * @since 0.7
     */
    public static Vocabulary getResources(final Map<?,?> properties) throws MissingResourceException {
        return getResources(getLocale(properties));
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return getResources((Locale) null).getString(key);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -5423999784169092823L;

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
