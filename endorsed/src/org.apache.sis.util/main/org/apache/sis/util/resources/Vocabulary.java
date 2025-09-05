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

import java.io.InputStream;
import java.util.Map;
import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;


/**
 * Locale-dependent resources for single words or short sentences.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public class Vocabulary extends IndexedResourceBundle {
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
         * Abstract
         */
        public static final short Abstract = 1;

        /**
         * Accuracy
         */
        public static final short Accuracy = 2;

        /**
         * Administrator
         */
        public static final short Administrator = 3;

        /**
         * Aliases
         */
        public static final short Aliases = 4;

        /**
         * Alternative identifiers
         */
        public static final short AlternativeIdentifiers = 5;

        /**
         * Angle
         */
        public static final short Angle = 6;

        /**
         * Degrees
         */
        public static final short AngularDegrees = 7;

        /**
         * Minutes
         */
        public static final short AngularMinutes = 8;

        /**
         * Seconds
         */
        public static final short AngularSeconds = 9;

        /**
         * Attributes
         */
        public static final short Attributes = 10;

        /**
         * Automatic
         */
        public static final short Automatic = 11;

        /**
         * Axis changes
         */
        public static final short AxisChanges = 12;

        /**
         * Axis {0}
         */
        public static final short Axis_1 = 269;

        /**
         * Azimuth
         */
        public static final short Azimuth = 13;

        /**
         * Background
         */
        public static final short Background = 14;

        /**
         * Band {0}
         */
        public static final short Band_1 = 15;

        /**
         * Barometric altitude
         */
        public static final short BarometricAltitude = 16;

        /**
         * Bilinear
         */
        public static final short Bilinear = 230;

        /**
         * Black
         */
        public static final short Black = 17;

        /**
         * Blue
         */
        public static final short Blue = 18;

        /**
         * Coordinate Reference Systems
         */
        public static final short CRSs = 19;

        /**
         * Cardinality
         */
        public static final short Cardinality = 20;

        /**
         * Categories
         */
        public static final short Categories = 248;

        /**
         * Caused by {0}
         */
        public static final short CausedBy_1 = 21;

        /**
         * {0} cells
         */
        public static final short CellCount_1 = 22;

        /**
         * Cell geometry
         */
        public static final short CellGeometry = 23;

        /**
         * Cells
         */
        public static final short Cells = 24;

        /**
         * Character encoding
         */
        public static final short CharacterEncoding = 25;

        /**
         * Characteristics
         */
        public static final short Characteristics = 26;

        /**
         * Class
         */
        public static final short Class = 240;

        /**
         * Classpath
         */
        public static final short Classpath = 27;

        /**
         * Code
         */
        public static final short Code = 28;

        /**
         * {0} code
         */
        public static final short Code_1 = 29;

        /**
         * Color
         */
        public static final short Color = 251;

        /**
         * Color index
         */
        public static final short ColorIndex = 30;

        /**
         * Colors
         */
        public static final short Colors = 228;

        /**
         * Commands
         */
        public static final short Commands = 31;

        /**
         * Compression
         */
        public static final short Compression = 273;

        /**
         * Configuration
         */
        public static final short Configuration = 246;

        /**
         * Constant pressure surface
         */
        public static final short ConstantPressureSurface = 32;

        /**
         * Constants
         */
        public static final short Constants = 233;

        /**
         * Container
         */
        public static final short Container = 33;

        /**
         * Controls
         */
        public static final short Controls = 262;

        /**
         * Conversion
         */
        public static final short Conversion = 34;

        /**
         * Coordinate
         */
        public static final short Coordinate = 35;

        /**
         * Coordinate reference system
         */
        public static final short CoordinateRefSys = 36;

        /**
         * Correlation
         */
        public static final short Correlation = 37;

        /**
         * Coverage
         */
        public static final short Coverage = 38;

        /**
         * Coverage domain
         */
        public static final short CoverageDomain = 39;

        /**
         * Create
         */
        public static final short Create = 40;

        /**
         * Creation date
         */
        public static final short CreationDate = 41;

        /**
         * Credit
         */
        public static final short Credit = 42;

        /**
         * Current date and time
         */
        public static final short CurrentDateTime = 43;

        /**
         * Current directory
         */
        public static final short CurrentDirectory = 44;

        /**
         * Cyan
         */
        public static final short Cyan = 45;

        /**
         * Cycle omitted
         */
        public static final short CycleOmitted = 46;

        /**
         * Data
         */
        public static final short Data = 47;

        /**
         * Database
         */
        public static final short DataBase = 48;

        /**
         * Data directory
         */
        public static final short DataDirectory = 49;

        /**
         * Data formats
         */
        public static final short DataFormats = 50;

        /**
         * Data type
         */
        public static final short DataType = 51;

        /**
         * Date
         */
        public static final short Date = 52;

        /**
         * Date and time
         */
        public static final short DateAndTime = 243;

        /**
         * Datum
         */
        public static final short Datum = 53;

        /**
         * Datum shift
         */
        public static final short DatumShift = 54;

        /**
         * Daylight time
         */
        public static final short DaylightTime = 55;

        /**
         * Default value
         */
        public static final short DefaultValue = 56;

        /**
         * Deprecated
         */
        public static final short Deprecated = 57;

        /**
         * Derived from {0}
         */
        public static final short DerivedFrom_1 = 58;

        /**
         * Description
         */
        public static final short Description = 59;

        /**
         * Designation
         */
        public static final short Designation = 60;

        /**
         * Destination
         */
        public static final short Destination = 61;

        /**
         * Details
         */
        public static final short Details = 62;

        /**
         * Digital elevation model
         */
        public static final short DigitalElevationModel = 63;

        /**
         * Dimension {0}
         */
        public static final short Dimension_1 = 64;

        /**
         * Dimensions
         */
        public static final short Dimensions = 65;

        /**
         * Directory
         */
        public static final short Directory = 66;

        /**
         * Display
         */
        public static final short Display = 67;

        /**
         * ″
         */
        public static final short DittoMark = 68;

        /**
         * Domain
         */
        public static final short Domain = 69;

        /**
         * Dublin Julian
         */
        public static final short DublinJulian = 70;

        /**
         * East bound
         */
        public static final short EastBound = 71;

        /**
         * Ellipsoid
         */
        public static final short Ellipsoid = 72;

        /**
         * Ellipsoidal height
         */
        public static final short EllipsoidalHeight = 74;

        /**
         * End date
         */
        public static final short EndDate = 75;

        /**
         * End point
         */
        public static final short EndPoint = 76;

        /**
         * Engineering
         */
        public static final short Engineering = 77;

        /**
         * Ensemble accuracy
         */
        public static final short EnsembleAccuracy = 278;

        /**
         * {0} entr{0,choice,0#y|2#ies}
         */
        public static final short EntryCount_1 = 78;

        /**
         * Envelope
         */
        public static final short Envelope = 79;

        /**
         * Errors
         */
        public static final short Errors = 80;

        /**
         * Extent
         */
        public static final short Extent = 81;

        /**
         * False
         */
        public static final short False = 264;

        /**
         * File
         */
        public static final short File = 82;

        /**
         * Fill value
         */
        public static final short FillValue = 83;

        /**
         * Filter
         */
        public static final short Filter = 84;

        /**
         * Format
         */
        public static final short Format = 85;

        /**
         * Geocentric
         */
        public static final short Geocentric = 86;

        /**
         * Geocentric conversion
         */
        public static final short GeocentricConversion = 87;

        /**
         * Geocentric radius
         */
        public static final short GeocentricRadius = 88;

        /**
         * Geodesic distance
         */
        public static final short GeodesicDistance = 89;

        /**
         * Geodetic
         */
        public static final short Geodetic = 90;

        /**
         * Geodetic dataset
         */
        public static final short GeodeticDataset = 91;

        /**
         * Geographic
         */
        public static final short Geographic = 92;

        /**
         * Geographic extent
         */
        public static final short GeographicExtent = 93;

        /**
         * Geographic identifier
         */
        public static final short GeographicIdentifier = 94;

        /**
         * Gray
         */
        public static final short Gray = 95;

        /**
         * Grayscale
         */
        public static final short Grayscale = 250;

        /**
         * Green
         */
        public static final short Green = 96;

        /**
         * Grid extent
         */
        public static final short GridExtent = 97;

        /**
         * Height
         */
        public static final short Height = 98;

        /**
         * Identifier
         */
        public static final short Identifier = 99;

        /**
         * Identifiers
         */
        public static final short Identifiers = 100;

        /**
         * Identity
         */
        public static final short Identity = 101;

        /**
         * Image
         */
        public static final short Image = 102;

        /**
         * Image layout
         */
        public static final short ImageLayout = 103;

        /**
         * Image size
         */
        public static final short ImageSize = 234;

        /**
         * Image #{0}
         */
        public static final short Image_1 = 261;

        /**
         * Implementation
         */
        public static final short Implementation = 104;

        /**
         *  in 
         */
        public static final short InBetweenWords = 105;

        /**
         * Index
         */
        public static final short Index = 106;

        /**
         * Information
         */
        public static final short Information = 247;

        /**
         * Interpolation
         */
        public static final short Interpolation = 231;

        /**
         * Interval
         */
        public static final short Interval = 253;

        /**
         * Invalid
         */
        public static final short Invalid = 107;

        /**
         * Inverse operation
         */
        public static final short InverseOperation = 108;

        /**
         * Isolines
         */
        public static final short Isolines = 252;

        /**
         * Java home directory
         */
        public static final short JavaHome = 110;

        /**
         * Julian
         */
        public static final short Julian = 111;

        /**
         * Latitude
         */
        public static final short Latitude = 112;

        /**
         * Layout
         */
        public static final short Layout = 235;

        /**
         * Legend
         */
        public static final short Legend = 113;

        /**
         * Level
         */
        public static final short Level = 114;

        /**
         * Libraries
         */
        public static final short Libraries = 115;

        /**
         * Linear transformation
         */
        public static final short LinearTransformation = 116;

        /**
         * Local configuration
         */
        public static final short LocalConfiguration = 117;

        /**
         * Locale
         */
        public static final short Locale = 118;

        /**
         * Localization
         */
        public static final short Localization = 119;

        /**
         * Location type
         */
        public static final short LocationType = 120;

        /**
         * Logger
         */
        public static final short Logger = 241;

        /**
         * Logging
         */
        public static final short Logging = 121;

        /**
         * Logs
         */
        public static final short Logs = 244;

        /**
         * Longitude
         */
        public static final short Longitude = 122;

        /**
         * Lower bound
         */
        public static final short LowerBound = 123;

        /**
         * Magenta
         */
        public static final short Magenta = 124;

        /**
         * Mandatory
         */
        public static final short Mandatory = 125;

        /**
         * Mapping
         */
        public static final short Mapping = 126;

        /**
         * Maximum
         */
        public static final short Maximum = 127;

        /**
         * Maximum value
         */
        public static final short MaximumValue = 128;

        /**
         * Mean value
         */
        public static final short MeanValue = 129;

        /**
         * Measures
         */
        public static final short Measures = 130;

        /**
         * Message
         */
        public static final short Message = 239;

        /**
         * Metadata
         */
        public static final short Metadata = 131;

        /**
         * Method
         */
        public static final short Method = 242;

        /**
         * Methods
         */
        public static final short Methods = 132;

        /**
         * Minimum
         */
        public static final short Minimum = 133;

        /**
         * Minimum value
         */
        public static final short MinimumValue = 134;

        /**
         * Missing value
         */
        public static final short MissingValue = 135;

        /**
         * Modified Julian
         */
        public static final short ModifiedJulian = 136;

        /**
         * Module path
         */
        public static final short ModulePath = 109;

        /**
         * … {0} more…
         */
        public static final short More_1 = 137;

        /**
         * Multiplicity
         */
        public static final short Multiplicity = 138;

        /**
         * Name
         */
        public static final short Name = 139;

        /**
         * Nearest neighbor
         */
        public static final short NearestNeighbor = 232;

        /**
         * Nil reason
         */
        public static final short NilReason = 274;

        /**
         * No data
         */
        public static final short Nodata = 140;

        /**
         * None
         */
        public static final short None = 141;

        /**
         * North bound
         */
        public static final short NorthBound = 142;

        /**
         * Not known
         */
        public static final short NotKnown = 207;

        /**
         * Note
         */
        public static final short Note = 143;

        /**
         * Number of dimensions
         */
        public static final short NumberOfDimensions = 144;

        /**
         * Number of ‘NaN’
         */
        public static final short NumberOfNaN = 145;

        /**
         * Number of tiles
         */
        public static final short NumberOfTiles = 236;

        /**
         * Number of values
         */
        public static final short NumberOfValues = 146;

        /**
         * Obligation
         */
        public static final short Obligation = 147;

        /**
         * {0} ({1} of {2})
         */
        public static final short Of_3 = 148;

        /**
         * Offset
         */
        public static final short Offset = 149;

        /**
         * Operating system
         */
        public static final short OperatingSystem = 150;

        /**
         * Operations
         */
        public static final short Operations = 151;

        /**
         * Optional
         */
        public static final short Optional = 152;

        /**
         * Options
         */
        public static final short Options = 153;

        /**
         * Origin
         */
        public static final short Origin = 154;

        /**
         * Origin in a cell center
         */
        public static final short OriginInCellCenter = 155;

        /**
         * Original colors
         */
        public static final short OriginalColors = 272;

        /**
         * Other surface
         */
        public static final short OtherSurface = 156;

        /**
         * Others
         */
        public static final short Others = 157;

        /**
         * Page {0}
         */
        public static final short Page_1 = 254;

        /**
         * Page {0} of {1}
         */
        public static final short Page_2 = 255;

        /**
         * Panchromatic
         */
        public static final short Panchromatic = 258;

        /**
         * {0} ({1})
         */
        public static final short Parenthesis_2 = 158;

        /**
         * Paths
         */
        public static final short Paths = 159;

        /**
         * Plug-ins
         */
        public static final short Plugins = 160;

        /**
         * Preprocessing
         */
        public static final short Preprocessing = 161;

        /**
         * Projected
         */
        public static final short Projected = 162;

        /**
         * Properties
         */
        public static final short Properties = 237;

        /**
         * Property
         */
        public static final short Property = 238;

        /**
         * Publication date
         */
        public static final short PublicationDate = 163;

        /**
         * Purpose
         */
        public static final short Purpose = 164;

        /**
         * “{0}”
         */
        public static final short Quoted_1 = 165;

        /**
         * Radiance
         */
        public static final short Radiance = 256;

        /**
         * Read
         */
        public static final short Read = 166;

        /**
         * Red
         */
        public static final short Red = 167;

        /**
         * Reference system
         */
        public static final short ReferenceSystem = 168;

        /**
         * Reflectance
         */
        public static final short Reflectance = 257;

        /**
         * Reflective
         */
        public static final short Reflective = 259;

        /**
         * Remarks
         */
        public static final short Remarks = 169;

        /**
         * Remote configuration
         */
        public static final short RemoteConfiguration = 170;

        /**
         * Representative value
         */
        public static final short RepresentativeValue = 171;

        /**
         * Resolution
         */
        public static final short Resolution = 172;

        /**
         * Resource identification
         */
        public static final short ResourceIdentification = 173;

        /**
         * Resources
         */
        public static final short Resources = 263;

        /**
         * Result
         */
        public static final short Result = 174;

        /**
         * Retry
         */
        public static final short Retry = 175;

        /**
         * Root
         */
        public static final short Root = 176;

        /**
         * Root Mean Square
         */
        public static final short RootMeanSquare = 177;

        /**
         * Same datum ensemble
         */
        public static final short SameDatumEnsemble = 279;

        /**
         * Sample dimensions
         */
        public static final short SampleDimensions = 178;

        /**
         * Scale
         */
        public static final short Scale = 179;

        /**
         * Simplified
         */
        public static final short Simplified = 180;

        /**
         * {0}/{1}
         */
        public static final short SlashSeparatedList_2 = 181;

        /**
         * Slower
         */
        public static final short Slower = 268;

        /**
         * Slowness
         */
        public static final short Slowness = 267;

        /**
         * Source
         */
        public static final short Source = 182;

        /**
         * South bound
         */
        public static final short SouthBound = 183;

        /**
         * Spatial representation
         */
        public static final short SpatialRepresentation = 184;

        /**
         * Sphere
         */
        public static final short Sphere = 271;

        /**
         * Standard deviation
         */
        public static final short StandardDeviation = 185;

        /**
         * Start date
         */
        public static final short StartDate = 186;

        /**
         * Start point
         */
        public static final short StartPoint = 187;

        /**
         * Stretching
         */
        public static final short Stretching = 229;

        /**
         * Subset of {0}
         */
        public static final short SubsetOf_1 = 188;

        /**
         * Summary
         */
        public static final short Summary = 189;

        /**
         * Superseded by {0}.
         */
        public static final short SupersededBy_1 = 190;

        /**
         * Temporal
         */
        public static final short Temporal = 191;

        /**
         * Temporal extent
         */
        public static final short TemporalExtent = 192;

        /**
         * Temporal ({0})
         */
        public static final short Temporal_1 = 276;

        /**
         * Temporary files
         */
        public static final short TemporaryFiles = 193;

        /**
         * Thermal
         */
        public static final short Thermal = 260;

        /**
         * Tile size
         */
        public static final short TileSize = 194;

        /**
         * Time
         */
        public static final short Time = 195;

        /**
         * {0} time
         */
        public static final short Time_1 = 196;

        /**
         * Timezone
         */
        public static final short Timezone = 197;

        /**
         * Title
         */
        public static final short Title = 270;

        /**
         * Topic category
         */
        public static final short TopicCategory = 198;

        /**
         * Trace
         */
        public static final short Trace = 245;

        /**
         * Transformation
         */
        public static final short Transformation = 199;

        /**
         * Transformation accuracy
         */
        public static final short TransformationAccuracy = 200;

        /**
         * Transparency
         */
        public static final short Transparency = 201;

        /**
         * Transparent
         */
        public static final short Transparent = 249;

        /**
         * Tropical year
         */
        public static final short TropicalYear = 275;

        /**
         * True
         */
        public static final short True = 265;

        /**
         * Truncated Julian
         */
        public static final short TruncatedJulian = 202;

        /**
         * Type
         */
        public static final short Type = 203;

        /**
         * Type of resource
         */
        public static final short TypeOfResource = 204;

        /**
         * Unavailable content.
         */
        public static final short UnavailableContent = 205;

        /**
         * Units
         */
        public static final short Units = 206;

        /**
         * Unnamed
         */
        public static final short Unnamed = 208;

        /**
         * Unnamed #{0}
         */
        public static final short Unnamed_1 = 266;

        /**
         * Unspecified
         */
        public static final short Unspecified = 209;

        /**
         * Unspecified datum change
         */
        public static final short UnspecifiedDatumChange = 73;

        /**
         * Untitled
         */
        public static final short Untitled = 210;

        /**
         * Upper bound
         */
        public static final short UpperBound = 211;

        /**
         * User home directory
         */
        public static final short UserHome = 212;

        /**
         * Value
         */
        public static final short Value = 213;

        /**
         * Value domain
         */
        public static final short ValueDomain = 214;

        /**
         * Value range
         */
        public static final short ValueRange = 215;

        /**
         * Values
         */
        public static final short Values = 216;

        /**
         * Variables
         */
        public static final short Variables = 217;

        /**
         * {0} version {1}
         */
        public static final short Version_2 = 218;

        /**
         * Versions
         */
        public static final short Versions = 219;

        /**
         * Vertical
         */
        public static final short Vertical = 220;

        /**
         * Visual
         */
        public static final short Visual = 221;

        /**
         * Warnings
         */
        public static final short Warnings = 222;

        /**
         * West bound
         */
        public static final short WestBound = 223;

        /**
         * Width
         */
        public static final short Width = 224;

        /**
         * {0} with {1}.
         */
        public static final short With_2 = 277;

        /**
         * World
         */
        public static final short World = 225;

        /**
         * Write
         */
        public static final short Write = 226;

        /**
         * Yellow
         */
        public static final short Yellow = 227;
    }

    /**
     * Constructs a new resource bundle loading data from
     * the resource file of the same name as this class.
     */
    public Vocabulary() {
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
    public static Vocabulary forLocale(final Locale locale) {
        /*
         * We cannot factorize this method into the parent class, because we need to call
         * `ResourceBundle.getBundle(String)` from the module that provides the resources.
         * We do not cache the result because `ResourceBundle` already provides a cache.
         */
        return (Vocabulary) getBundle(Vocabulary.class.getName(), nonNull(locale));
    }

    /**
     * Returns resources in the locale specified in the given property map. This convenience method looks
     * for the {@link #LOCALE_KEY} entry. If the given map is null, or contains no entry for the locale key,
     * or the value is not an instance of {@link Locale}, then this method fallback on the default locale.
     *
     * @param  properties  the map of properties, or {@code null} if none.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources cannot be found.
     */
    public static Vocabulary forProperties(final Map<?,?> properties) throws MissingResourceException {
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
     * The international string to be returned by {@code formatInternational(…)} methods.
     * This implementation details is made public for allowing the creation of subclasses
     * implementing some additional interfaces.
     */
    public static class International extends ResourceInternationalString {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -5423999784169092823L;

        /**
         * Creates a new instance for the given vocabulary resource key.
         *
         * @param  key  one of the {@link Keys} values.
         */
        protected International(short key)                       {super(key);}
        International(short key, Object args)                    {super(key, args);}
        @Override protected final KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override protected final IndexedResourceBundle getBundle(final Locale locale) {
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
     * <h4>API note</h4>
     * This method is redundant with the one expecting {@code Object...}, but avoid the creation
     * of a temporary array. There is no risk of confusion since the two methods delegate their
     * work to the same {@code format} method anyway.
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
