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
 * @version 1.0
 * @since   0.3
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
        public static final short Accuracy = 1;

        /**
         * Administrator
         */
        public static final short Administrator = 130;

        /**
         * Aliases
         */
        public static final short Aliases = 2;

        /**
         * Alternative identifiers
         */
        public static final short AlternativeIdentifiers = 131;

        /**
         * Angle
         */
        public static final short Angle = 3;

        /**
         * Degrees
         */
        public static final short AngularDegrees = 4;

        /**
         * Minutes
         */
        public static final short AngularMinutes = 5;

        /**
         * Seconds
         */
        public static final short AngularSeconds = 6;

        /**
         * Attributes
         */
        public static final short Attributes = 7;

        /**
         * Axis changes
         */
        public static final short AxisChanges = 8;

        /**
         * Band {0}
         */
        public static final short Band_1 = 161;

        /**
         * Barometric altitude
         */
        public static final short BarometricAltitude = 9;

        /**
         * Cardinality
         */
        public static final short Cardinality = 10;

        /**
         * Caused by {0}
         */
        public static final short CausedBy_1 = 11;

        /**
         * {0} cells
         */
        public static final short CellCount_1 = 149;

        /**
         * Character encoding
         */
        public static final short CharacterEncoding = 12;

        /**
         * Characteristics
         */
        public static final short Characteristics = 13;

        /**
         * Classpath
         */
        public static final short Classpath = 14;

        /**
         * Code
         */
        public static final short Code = 128;

        /**
         * {0} code
         */
        public static final short Code_1 = 15;

        /**
         * Commands
         */
        public static final short Commands = 16;

        /**
         * Constant pressure surface
         */
        public static final short ConstantPressureSurface = 17;

        /**
         * Container
         */
        public static final short Container = 18;

        /**
         * Conversion
         */
        public static final short Conversion = 150;

        /**
         * Coordinate
         */
        public static final short Coordinate = 129;

        /**
         * Coordinate reference system
         */
        public static final short CoordinateRefSys = 132;

        /**
         * Correlation
         */
        public static final short Correlation = 19;

        /**
         * Current date and time
         */
        public static final short CurrentDateTime = 20;

        /**
         * Current directory
         */
        public static final short CurrentDirectory = 21;

        /**
         * Cycle omitted
         */
        public static final short CycleOmitted = 22;

        /**
         * Database
         */
        public static final short DataBase = 23;

        /**
         * Data directory
         */
        public static final short DataDirectory = 24;

        /**
         * Data formats
         */
        public static final short DataFormats = 119;

        /**
         * Datum
         */
        public static final short Datum = 25;

        /**
         * Datum shift
         */
        public static final short DatumShift = 26;

        /**
         * Daylight time
         */
        public static final short DaylightTime = 27;

        /**
         * Default value
         */
        public static final short DefaultValue = 28;

        /**
         * Deprecated
         */
        public static final short Deprecated = 29;

        /**
         * Derived from {0}
         */
        public static final short DerivedFrom_1 = 30;

        /**
         * Description
         */
        public static final short Description = 31;

        /**
         * Designation
         */
        public static final short Designation = 142;

        /**
         * Destination
         */
        public static final short Destination = 32;

        /**
         * Details
         */
        public static final short Details = 33;

        /**
         * Digital elevation model
         */
        public static final short DigitalElevationModel = 146;

        /**
         * Dimension {0}
         */
        public static final short Dimension_1 = 148;

        /**
         * Dimensions
         */
        public static final short Dimensions = 34;

        /**
         * Directory
         */
        public static final short Directory = 35;

        /**
         * ″
         */
        public static final short DittoMark = 36;

        /**
         * Domain
         */
        public static final short Domain = 37;

        /**
         * Dublin Julian
         */
        public static final short DublinJulian = 38;

        /**
         * East bound
         */
        public static final short EastBound = 133;

        /**
         * Ellipsoid
         */
        public static final short Ellipsoid = 39;

        /**
         * Ellipsoid change
         */
        public static final short EllipsoidChange = 40;

        /**
         * Ellipsoidal height
         */
        public static final short EllipsoidalHeight = 41;

        /**
         * End date
         */
        public static final short EndDate = 134;

        /**
         * {0} entr{0,choice,0#y|2#ies}
         */
        public static final short EntryCount_1 = 121;

        /**
         * Envelope
         */
        public static final short Envelope = 151;

        /**
         * Exit
         */
        public static final short Exit = 143;

        /**
         * File
         */
        public static final short File = 144;

        /**
         * Fill value
         */
        public static final short FillValue = 159;

        /**
         * Geocentric
         */
        public static final short Geocentric = 42;

        /**
         * Geocentric conversion
         */
        public static final short GeocentricConversion = 43;

        /**
         * Geocentric radius
         */
        public static final short GeocentricRadius = 44;

        /**
         * Geodetic dataset
         */
        public static final short GeodeticDataset = 45;

        /**
         * Geographic identifier
         */
        public static final short GeographicIdentifier = 135;

        /**
         * Grid extent
         */
        public static final short GridExtent = 152;

        /**
         * Height
         */
        public static final short Height = 46;

        /**
         * Identifier
         */
        public static final short Identifier = 47;

        /**
         * Identity
         */
        public static final short Identity = 48;

        /**
         * Implementation
         */
        public static final short Implementation = 49;

        /**
         *  in 
         */
        public static final short InBetweenWords = 50;

        /**
         * Index
         */
        public static final short Index = 51;

        /**
         * Invalid
         */
        public static final short Invalid = 52;

        /**
         * Inverse operation
         */
        public static final short InverseOperation = 53;

        /**
         * Java extensions
         */
        public static final short JavaExtensions = 54;

        /**
         * Java home directory
         */
        public static final short JavaHome = 55;

        /**
         * Julian
         */
        public static final short Julian = 56;

        /**
         * Latitude
         */
        public static final short Latitude = 57;

        /**
         * Legend
         */
        public static final short Legend = 58;

        /**
         * Level
         */
        public static final short Level = 59;

        /**
         * Libraries
         */
        public static final short Libraries = 60;

        /**
         * Local configuration
         */
        public static final short LocalConfiguration = 61;

        /**
         * Locale
         */
        public static final short Locale = 62;

        /**
         * Localization
         */
        public static final short Localization = 63;

        /**
         * Location type
         */
        public static final short LocationType = 136;

        /**
         * Logging
         */
        public static final short Logging = 64;

        /**
         * Longitude
         */
        public static final short Longitude = 65;

        /**
         * Mandatory
         */
        public static final short Mandatory = 66;

        /**
         * Mapping
         */
        public static final short Mapping = 127;

        /**
         * Maximum value
         */
        public static final short MaximumValue = 67;

        /**
         * Mean value
         */
        public static final short MeanValue = 68;

        /**
         * Measures
         */
        public static final short Measures = 157;

        /**
         * Methods
         */
        public static final short Methods = 69;

        /**
         * Minimum value
         */
        public static final short MinimumValue = 70;

        /**
         * Missing value
         */
        public static final short MissingValue = 160;

        /**
         * Modified Julian
         */
        public static final short ModifiedJulian = 71;

        /**
         * Multiplicity
         */
        public static final short Multiplicity = 147;

        /**
         * Name
         */
        public static final short Name = 72;

        /**
         * No data
         */
        public static final short Nodata = 156;

        /**
         * None
         */
        public static final short None = 73;

        /**
         * North bound
         */
        public static final short NorthBound = 137;

        /**
         * Note
         */
        public static final short Note = 74;

        /**
         * Number of ‘NaN’
         */
        public static final short NumberOfNaN = 75;

        /**
         * Number of values
         */
        public static final short NumberOfValues = 76;

        /**
         * Obligation
         */
        public static final short Obligation = 77;

        /**
         * {0} ({1} of {2})
         */
        public static final short Of_3 = 78;

        /**
         * Offset
         */
        public static final short Offset = 79;

        /**
         * Open
         */
        public static final short Open = 145;

        /**
         * Operating system
         */
        public static final short OperatingSystem = 80;

        /**
         * Operations
         */
        public static final short Operations = 81;

        /**
         * Optional
         */
        public static final short Optional = 82;

        /**
         * Options
         */
        public static final short Options = 83;

        /**
         * Origin in a cell center
         */
        public static final short OriginInCellCenter = 155;

        /**
         * Other surface
         */
        public static final short OtherSurface = 84;

        /**
         * Others
         */
        public static final short Others = 85;

        /**
         * {0} ({1})
         */
        public static final short Parenthesis_2 = 126;

        /**
         * Paths
         */
        public static final short Paths = 86;

        /**
         * Plug-ins
         */
        public static final short Plugins = 120;

        /**
         * “{0}”
         */
        public static final short Quoted_1 = 87;

        /**
         * Read
         */
        public static final short Read = 122;

        /**
         * Remarks
         */
        public static final short Remarks = 88;

        /**
         * Remote configuration
         */
        public static final short RemoteConfiguration = 89;

        /**
         * Representative value
         */
        public static final short RepresentativeValue = 141;

        /**
         * Resolution
         */
        public static final short Resolution = 153;

        /**
         * Root
         */
        public static final short Root = 90;

        /**
         * Root Mean Square
         */
        public static final short RootMeanSquare = 91;

        /**
         * Scale
         */
        public static final short Scale = 92;

        /**
         * {0}/{1}
         */
        public static final short SlashSeparatedList_2 = 124;

        /**
         * Source
         */
        public static final short Source = 93;

        /**
         * South bound
         */
        public static final short SouthBound = 138;

        /**
         * Standard deviation
         */
        public static final short StandardDeviation = 94;

        /**
         * Start date
         */
        public static final short StartDate = 139;

        /**
         * Subset of {0}
         */
        public static final short SubsetOf_1 = 95;

        /**
         * Superseded by {0}.
         */
        public static final short SupersededBy_1 = 96;

        /**
         * Temporal
         */
        public static final short Temporal = 97;

        /**
         * Temporary files
         */
        public static final short TemporaryFiles = 98;

        /**
         * Time
         */
        public static final short Time = 99;

        /**
         * {0} time
         */
        public static final short Time_1 = 100;

        /**
         * Timezone
         */
        public static final short Timezone = 101;

        /**
         * Transformation
         */
        public static final short Transformation = 102;

        /**
         * Transformation accuracy
         */
        public static final short TransformationAccuracy = 103;

        /**
         * Truncated Julian
         */
        public static final short TruncatedJulian = 104;

        /**
         * Type
         */
        public static final short Type = 105;

        /**
         * Unavailable content.
         */
        public static final short UnavailableContent = 106;

        /**
         * Units
         */
        public static final short Units = 107;

        /**
         * Unknown
         */
        public static final short Unknown = 125;

        /**
         * Unnamed
         */
        public static final short Unnamed = 108;

        /**
         * Unspecified
         */
        public static final short Unspecified = 154;

        /**
         * Untitled
         */
        public static final short Untitled = 109;

        /**
         * User home directory
         */
        public static final short UserHome = 110;

        /**
         * Value
         */
        public static final short Value = 111;

        /**
         * Value domain
         */
        public static final short ValueDomain = 112;

        /**
         * Values
         */
        public static final short Values = 158;

        /**
         * Variables
         */
        public static final short Variables = 113;

        /**
         * {0} version {1}
         */
        public static final short Version_2 = 114;

        /**
         * Versions
         */
        public static final short Versions = 115;

        /**
         * Vertical
         */
        public static final short Vertical = 116;

        /**
         * Warnings
         */
        public static final short Warnings = 117;

        /**
         * West bound
         */
        public static final short WestBound = 140;

        /**
         * World
         */
        public static final short World = 118;

        /**
         * Write
         */
        public static final short Write = 123;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    Vocabulary(final URL resources) {
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
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return getResources((Locale) null).getString(key);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -5423999784169092823L;

        International(short key)                           {super(key);}
        International(short key, Object args)              {super(key, args);}
        @Override protected KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override protected IndexedResourceBundle getBundle(final Locale locale) {
            return getResources(locale);
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
     * <div class="note"><b>API note:</b>
     * This method is redundant with the one expecting {@code Object...}, but avoid the creation
     * of a temporary array. There is no risk of confusion since the two methods delegate their
     * work to the same {@code format} method anyway.</div>
     *
     * @param  key  the key for the desired string.
     * @param  arg  values to substitute to "{0}".
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object arg) {
        return new International(key, arg);
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
