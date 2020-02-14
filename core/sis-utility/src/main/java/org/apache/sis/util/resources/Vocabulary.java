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
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for single words or short sentences.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
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
        public static final short Administrator = 2;

        /**
         * Aliases
         */
        public static final short Aliases = 3;

        /**
         * Alternative identifiers
         */
        public static final short AlternativeIdentifiers = 4;

        /**
         * Angle
         */
        public static final short Angle = 5;

        /**
         * Degrees
         */
        public static final short AngularDegrees = 6;

        /**
         * Minutes
         */
        public static final short AngularMinutes = 7;

        /**
         * Seconds
         */
        public static final short AngularSeconds = 8;

        /**
         * Attributes
         */
        public static final short Attributes = 9;

        /**
         * Axis changes
         */
        public static final short AxisChanges = 10;

        /**
         * Azimuth
         */
        public static final short Azimuth = 11;

        /**
         * Band {0}
         */
        public static final short Band_1 = 12;

        /**
         * Barometric altitude
         */
        public static final short BarometricAltitude = 13;

        /**
         * Black
         */
        public static final short Black = 175;

        /**
         * Blue
         */
        public static final short Blue = 176;

        /**
         * Cardinality
         */
        public static final short Cardinality = 14;

        /**
         * Caused by {0}
         */
        public static final short CausedBy_1 = 15;

        /**
         * {0} cells
         */
        public static final short CellCount_1 = 16;

        /**
         * Cells
         */
        public static final short Cells = 193;

        /**
         * Character encoding
         */
        public static final short CharacterEncoding = 17;

        /**
         * Characteristics
         */
        public static final short Characteristics = 18;

        /**
         * Classpath
         */
        public static final short Classpath = 19;

        /**
         * Code
         */
        public static final short Code = 20;

        /**
         * {0} code
         */
        public static final short Code_1 = 21;

        /**
         * Color index
         */
        public static final short ColorIndex = 184;

        /**
         * Commands
         */
        public static final short Commands = 22;

        /**
         * Constant pressure surface
         */
        public static final short ConstantPressureSurface = 23;

        /**
         * Container
         */
        public static final short Container = 24;

        /**
         * Conversion
         */
        public static final short Conversion = 25;

        /**
         * Coordinate
         */
        public static final short Coordinate = 26;

        /**
         * Coordinate reference system
         */
        public static final short CoordinateRefSys = 27;

        /**
         * Correlation
         */
        public static final short Correlation = 28;

        /**
         * Coverage
         */
        public static final short Coverage = 190;

        /**
         * Coverage domain
         */
        public static final short CoverageDomain = 29;

        /**
         * Create
         */
        public static final short Create = 30;

        /**
         * Current date and time
         */
        public static final short CurrentDateTime = 31;

        /**
         * Current directory
         */
        public static final short CurrentDirectory = 32;

        /**
         * Cyan
         */
        public static final short Cyan = 177;

        /**
         * Cycle omitted
         */
        public static final short CycleOmitted = 33;

        /**
         * Database
         */
        public static final short DataBase = 34;

        /**
         * Data directory
         */
        public static final short DataDirectory = 35;

        /**
         * Data formats
         */
        public static final short DataFormats = 36;

        /**
         * Data type
         */
        public static final short DataType = 185;

        /**
         * Datum
         */
        public static final short Datum = 37;

        /**
         * Datum shift
         */
        public static final short DatumShift = 38;

        /**
         * Daylight time
         */
        public static final short DaylightTime = 39;

        /**
         * Default value
         */
        public static final short DefaultValue = 40;

        /**
         * Deprecated
         */
        public static final short Deprecated = 41;

        /**
         * Derived from {0}
         */
        public static final short DerivedFrom_1 = 42;

        /**
         * Description
         */
        public static final short Description = 43;

        /**
         * Designation
         */
        public static final short Designation = 44;

        /**
         * Destination
         */
        public static final short Destination = 45;

        /**
         * Details
         */
        public static final short Details = 46;

        /**
         * Digital elevation model
         */
        public static final short DigitalElevationModel = 47;

        /**
         * Dimension {0}
         */
        public static final short Dimension_1 = 48;

        /**
         * Dimensions
         */
        public static final short Dimensions = 49;

        /**
         * Directory
         */
        public static final short Directory = 50;

        /**
         * Display
         */
        public static final short Display = 194;

        /**
         * ″
         */
        public static final short DittoMark = 51;

        /**
         * Domain
         */
        public static final short Domain = 52;

        /**
         * Dublin Julian
         */
        public static final short DublinJulian = 53;

        /**
         * East bound
         */
        public static final short EastBound = 54;

        /**
         * Ellipsoid
         */
        public static final short Ellipsoid = 55;

        /**
         * Ellipsoid change
         */
        public static final short EllipsoidChange = 56;

        /**
         * Ellipsoidal height
         */
        public static final short EllipsoidalHeight = 57;

        /**
         * End date
         */
        public static final short EndDate = 58;

        /**
         * End point
         */
        public static final short EndPoint = 59;

        /**
         * {0} entr{0,choice,0#y|2#ies}
         */
        public static final short EntryCount_1 = 60;

        /**
         * Envelope
         */
        public static final short Envelope = 61;

        /**
         * Errors
         */
        public static final short Errors = 62;

        /**
         * Fill value
         */
        public static final short FillValue = 63;

        /**
         * Format
         */
        public static final short Format = 196;

        /**
         * Geocentric
         */
        public static final short Geocentric = 64;

        /**
         * Geocentric conversion
         */
        public static final short GeocentricConversion = 65;

        /**
         * Geocentric radius
         */
        public static final short GeocentricRadius = 66;

        /**
         * Geodesic distance
         */
        public static final short GeodesicDistance = 67;

        /**
         * Geodetic dataset
         */
        public static final short GeodeticDataset = 68;

        /**
         * Geographic extent
         */
        public static final short GeographicExtent = 69;

        /**
         * Geographic identifier
         */
        public static final short GeographicIdentifier = 70;

        /**
         * Gray
         */
        public static final short Gray = 178;

        /**
         * Green
         */
        public static final short Green = 179;

        /**
         * Grid extent
         */
        public static final short GridExtent = 71;

        /**
         * Height
         */
        public static final short Height = 72;

        /**
         * Identifier
         */
        public static final short Identifier = 73;

        /**
         * Identity
         */
        public static final short Identity = 74;

        /**
         * Image layout
         */
        public static final short ImageLayout = 186;

        /**
         * Implementation
         */
        public static final short Implementation = 75;

        /**
         *  in 
         */
        public static final short InBetweenWords = 76;

        /**
         * Index
         */
        public static final short Index = 77;

        /**
         * Invalid
         */
        public static final short Invalid = 78;

        /**
         * Inverse operation
         */
        public static final short InverseOperation = 79;

        /**
         * Java extensions
         */
        public static final short JavaExtensions = 80;

        /**
         * Java home directory
         */
        public static final short JavaHome = 81;

        /**
         * Julian
         */
        public static final short Julian = 82;

        /**
         * Latitude
         */
        public static final short Latitude = 83;

        /**
         * Legend
         */
        public static final short Legend = 84;

        /**
         * Level
         */
        public static final short Level = 85;

        /**
         * Libraries
         */
        public static final short Libraries = 86;

        /**
         * Linear transformation
         */
        public static final short LinearTransformation = 87;

        /**
         * Local configuration
         */
        public static final short LocalConfiguration = 88;

        /**
         * Locale
         */
        public static final short Locale = 89;

        /**
         * Localization
         */
        public static final short Localization = 90;

        /**
         * Location type
         */
        public static final short LocationType = 91;

        /**
         * Logging
         */
        public static final short Logging = 92;

        /**
         * Longitude
         */
        public static final short Longitude = 93;

        /**
         * Lower bound
         */
        public static final short LowerBound = 94;

        /**
         * Magenta
         */
        public static final short Magenta = 180;

        /**
         * Mandatory
         */
        public static final short Mandatory = 95;

        /**
         * Mapping
         */
        public static final short Mapping = 96;

        /**
         * Maximum
         */
        public static final short Maximum = 191;

        /**
         * Maximum value
         */
        public static final short MaximumValue = 97;

        /**
         * Mean value
         */
        public static final short MeanValue = 98;

        /**
         * Measures
         */
        public static final short Measures = 99;

        /**
         * Methods
         */
        public static final short Methods = 100;

        /**
         * Minimum
         */
        public static final short Minimum = 192;

        /**
         * Minimum value
         */
        public static final short MinimumValue = 101;

        /**
         * Missing value
         */
        public static final short MissingValue = 102;

        /**
         * Modified Julian
         */
        public static final short ModifiedJulian = 103;

        /**
         * … {0} more…
         */
        public static final short More_1 = 197;

        /**
         * Multiplicity
         */
        public static final short Multiplicity = 104;

        /**
         * Name
         */
        public static final short Name = 105;

        /**
         * No data
         */
        public static final short Nodata = 106;

        /**
         * None
         */
        public static final short None = 107;

        /**
         * North bound
         */
        public static final short NorthBound = 108;

        /**
         * Note
         */
        public static final short Note = 109;

        /**
         * Number of ‘NaN’
         */
        public static final short NumberOfNaN = 110;

        /**
         * Number of values
         */
        public static final short NumberOfValues = 111;

        /**
         * Obligation
         */
        public static final short Obligation = 112;

        /**
         * {0} ({1} of {2})
         */
        public static final short Of_3 = 113;

        /**
         * Offset
         */
        public static final short Offset = 114;

        /**
         * Operating system
         */
        public static final short OperatingSystem = 115;

        /**
         * Operations
         */
        public static final short Operations = 116;

        /**
         * Optional
         */
        public static final short Optional = 117;

        /**
         * Options
         */
        public static final short Options = 118;

        /**
         * Origin
         */
        public static final short Origin = 188;

        /**
         * Origin in a cell center
         */
        public static final short OriginInCellCenter = 119;

        /**
         * Other surface
         */
        public static final short OtherSurface = 120;

        /**
         * Others
         */
        public static final short Others = 121;

        /**
         * {0} ({1})
         */
        public static final short Parenthesis_2 = 122;

        /**
         * Paths
         */
        public static final short Paths = 123;

        /**
         * Plug-ins
         */
        public static final short Plugins = 124;

        /**
         * Preprocessing
         */
        public static final short Preprocessing = 125;

        /**
         * “{0}”
         */
        public static final short Quoted_1 = 126;

        /**
         * Read
         */
        public static final short Read = 127;

        /**
         * Red
         */
        public static final short Red = 181;

        /**
         * Remarks
         */
        public static final short Remarks = 128;

        /**
         * Remote configuration
         */
        public static final short RemoteConfiguration = 129;

        /**
         * Representative value
         */
        public static final short RepresentativeValue = 130;

        /**
         * Resolution
         */
        public static final short Resolution = 131;

        /**
         * Result
         */
        public static final short Result = 132;

        /**
         * Retry
         */
        public static final short Retry = 189;

        /**
         * Root
         */
        public static final short Root = 133;

        /**
         * Root Mean Square
         */
        public static final short RootMeanSquare = 134;

        /**
         * Sample dimensions
         */
        public static final short SampleDimensions = 135;

        /**
         * Scale
         */
        public static final short Scale = 136;

        /**
         * Simplified
         */
        public static final short Simplified = 174;

        /**
         * {0}/{1}
         */
        public static final short SlashSeparatedList_2 = 137;

        /**
         * Source
         */
        public static final short Source = 138;

        /**
         * South bound
         */
        public static final short SouthBound = 139;

        /**
         * Standard deviation
         */
        public static final short StandardDeviation = 140;

        /**
         * Start date
         */
        public static final short StartDate = 141;

        /**
         * Start point
         */
        public static final short StartPoint = 142;

        /**
         * Subset of {0}
         */
        public static final short SubsetOf_1 = 143;

        /**
         * Superseded by {0}.
         */
        public static final short SupersededBy_1 = 144;

        /**
         * Temporal
         */
        public static final short Temporal = 145;

        /**
         * Temporal extent
         */
        public static final short TemporalExtent = 146;

        /**
         * Temporary files
         */
        public static final short TemporaryFiles = 147;

        /**
         * Tile size
         */
        public static final short TileSize = 187;

        /**
         * Time
         */
        public static final short Time = 148;

        /**
         * {0} time
         */
        public static final short Time_1 = 149;

        /**
         * Timezone
         */
        public static final short Timezone = 150;

        /**
         * Transformation
         */
        public static final short Transformation = 151;

        /**
         * Transformation accuracy
         */
        public static final short TransformationAccuracy = 152;

        /**
         * Transparency
         */
        public static final short Transparency = 183;

        /**
         * Truncated Julian
         */
        public static final short TruncatedJulian = 153;

        /**
         * Type
         */
        public static final short Type = 154;

        /**
         * Unavailable content.
         */
        public static final short UnavailableContent = 155;

        /**
         * Units
         */
        public static final short Units = 156;

        /**
         * Unknown
         */
        public static final short Unknown = 157;

        /**
         * Unnamed
         */
        public static final short Unnamed = 158;

        /**
         * Unspecified
         */
        public static final short Unspecified = 159;

        /**
         * Untitled
         */
        public static final short Untitled = 160;

        /**
         * Upper bound
         */
        public static final short UpperBound = 161;

        /**
         * User home directory
         */
        public static final short UserHome = 162;

        /**
         * Value
         */
        public static final short Value = 163;

        /**
         * Value domain
         */
        public static final short ValueDomain = 164;

        /**
         * Values
         */
        public static final short Values = 165;

        /**
         * Variables
         */
        public static final short Variables = 166;

        /**
         * {0} version {1}
         */
        public static final short Version_2 = 167;

        /**
         * Versions
         */
        public static final short Versions = 168;

        /**
         * Vertical
         */
        public static final short Vertical = 169;

        /**
         * Warnings
         */
        public static final short Warnings = 170;

        /**
         * West bound
         */
        public static final short WestBound = 171;

        /**
         * Width
         */
        public static final short Width = 195;

        /**
         * World
         */
        public static final short World = 172;

        /**
         * Write
         */
        public static final short Write = 173;

        /**
         * Yellow
         */
        public static final short Yellow = 182;
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
