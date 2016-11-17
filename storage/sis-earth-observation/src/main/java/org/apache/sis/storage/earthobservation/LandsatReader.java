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
package org.apache.sis.storage.earthobservation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.LineNumberReader;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.util.FactoryException;

import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultBand;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.referencing.provider.PolarStereographicB;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Utilities;

import static java.util.Collections.singleton;
import static org.apache.sis.internal.util.CollectionsExt.singletonOrNull;

// Branch-dependent imports
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.DateTimeException;
import java.time.temporal.Temporal;


/**
 * Parses Landsat metadata as {@linkplain DefaultMetadata ISO-19115 Metadata} object.
 * This class reads the content of a given {@link BufferedReader} from buffer position
 * until the first occurrence of the {@code END} keyword. Lines beginning with the
 * {@code #} character (ignoring spaces) are treated as comment lines and ignored.
 *
 * <p>This class will parse properties found in the Landsat metadata file,
 * except {@code GROUP} and {@code END_GROUP}. Example:
 *
 * {@preformat text
 *   DATE_ACQUIRED         = 2014-03-12
 *   SCENE_CENTER_TIME     = 03:02:01.5339408Z
 *   CORNER_UL_LAT_PRODUCT = 12.61111
 *   CORNER_UL_LON_PRODUCT = 108.33624
 *   CORNER_UR_LAT_PRODUCT = 12.62381
 *   CORNER_UR_LON_PRODUCT = 110.44017
 * }
 *
 * <p><b>NOTE FOR MAINTAINER:</b> if the work performed by this class is modified, consider updating
 * <a href="./doc-files/LandsatMetadata.html">./doc-files/LandsatMetadata.html</a> accordingly.</p>
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class LandsatReader {
    /**
     * Names of Landsat bands.
     *
     * @see #bands
     * @see #band(int)
     */
    private static final String[] BAND_NAMES = {
        "Coastal Aerosol",                      //   433 nm
        "Blue",                                 //   482 nm
        "Green",                                //   562 nm
        "Red",                                  //   655 nm
        "Near-Infrared",                        //   865 nm
        "Short Wavelength Infrared (SWIR) 1",   //  1610 nm
        "Short Wavelength Infrared (SWIR) 2",   //  2200 nm
        "Panchromatic",                         //   590 nm
        "Cirrus",                               //  1375 nm
        "Thermal Infrared Sensor (TIRS) 1",     // 10800 nm
        "Thermal Infrared Sensor (TIRS) 2"      // 12000 nm
    };

    /**
     * Peak response wavelength for the Landsat bands, in nanometres.
     *
     * @see #bands
     * @see #band(int)
     */
    private static final short[] WAVELENGTHS = {
        433, 482, 562, 655, 865, 1610, 2200, 590, 1375, 10800, 12000
    };

    /**
     * The pattern determining if the value of {@code ORIGIN} key is of the form
     * “Image courtesy of the U.S. Geological Survey”.
     */
    static final Pattern CREDIT = Pattern.compile("\\bcourtesy\\h+of\\h+(the)?\\b\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Number of spatial dimensions. This is the number of ordinate values to be stored
     * in the {@link #gridSizes} and {@link #corners} arrays for each tuple.
     */
    static final int DIM = 2;

    /**
     * The {@value} suffix added to attribute names that are followed by a band number.
     * This band suffix is itself followed by the {@code '_'} character, then the band number.
     * Example: {@code "REFLECTANCE_ADD_BAND_1"}.
     */
    private static final String BAND_SUFFIX = "_BAND";

    /**
     * A bit mask of the group in which to classify a given band.
     * There is three groups: panchromatic, reflective or thermal bands:
     *
     * <ul>
     *   <li><b>Panchromatic:</b> band  8.</li>
     *   <li><b>Reflective:</b>   bands 1, 2, 3, 4, 5, 6, 7, 9.</li>
     *   <li><b>Thermal:</b>      bands 10, 11.</li>
     * </ul>
     *
     * For a band numbered from 1 to 11 inclusive, the group is computed by
     * (constants 2 and 3 in that formula depends on the {@link #NUM_GROUPS} value):
     *
     * {@preformat java
     *   group = (BAND_GROUPS >>> 2*(band - 1)) & 3;
     * }
     *
     * The result is one of the {@link #PANCHROMATIC}, {@link #REFLECTIVE} or {@link #THERMAL} constant values
     * divided by {@value #DIM}.
     */
    static final int BAND_GROUPS = 2692437;     // Value computed by LandsatReaderTest.verifyBandGroupsMask()

    /**
     * Maximum number of band groups that a metadata may contains.
     * See {@link #BAND_GROUPS} javadoc for the list of groups.
     */
    private static final int NUM_GROUPS = 3;

    /**
     * Index of panchromatic, reflective or thermal groups in the {@link #gridSize} array.
     * The image size is each group is given by {@value #DIM} integers: the width and the height.
     */
    static final int PANCHROMATIC = 0*DIM,
                     REFLECTIVE   = 1*DIM,
                     THERMAL      = 2*DIM;

    /**
     * Index of projected and geographic coordinates in the {@link #corners} array.
     * Each kind of coordinates are stored as 4 corners of {@value #DIM} ordinate values.
     */
    private static final int PROJECTED  = 0*DIM,
                             GEOGRAPHIC = 4*DIM;

    /**
     * The keyword for end of metadata file.
     */
    private static final String END = "END";

    /**
     * An identifier of the file being read, or {@code null} if unknown.
     * This is used mostly for formatting error messages.
     *
     * @see #getFilename()
     */
    private String filename;

    /**
     * Helper class for building the ISO 19115 metadata instance.
     */
    private final MetadataBuilder metadata;

    /**
     * Where to send the warnings.
     */
    private final WarningListeners<?> listeners;

    /**
     * Group in process of being parsed, or {@code null} if none.
     */
    private String group;

    /**
     * The acquisition time, or {@code null} if not yet known. This needs to be parsed in two steps:
     * first by parsing the {@code "DATE_ACQUIRED"} attribute, then {@code "SCENE_CENTER_TIME"}.
     *
     * @see #flushSceneTime()
     */
    private Temporal sceneTime;

    /**
     * Projected and geographic coordinate values, stocked temporarily before to be saved in the extent.
     * Values are in (<var>x</var>,<var>y</var>) or (<var>lon</var>,<var>lat</var>) order.
     * The first 8 values are the projected ones. The next 8 values are the geographic ones.
     * Corner order is UL, UR, LL, LR.
     */
    private final double[] corners;

    /**
     * Image width and hight, in pixels. Values are (<var>width</var>,<var>height</var>) tuples.
     * Tuples in this array are for {@link #PANCHROMATIC}, {@link #REFLECTIVE} or {@link #THERMAL}
     * bands, in that order.
     */
    private final int[] gridSizes;

    /**
     * The bands description. Any element can be null if the corresponding band is not defined.
     * The bands can be, in this exact order:
     *
     * <ol>
     *   <li>Coastal Aerosol</li>
     *   <li>Blue</li>
     *   <li>Green</li>
     *   <li>Red</li>
     *   <li>Near-Infrared</li>
     *   <li>Short Wavelength Infrared (SWIR) 1</li>
     *   <li>Short Wavelength Infrared (SWIR) 2</li>
     *   <li>Panchromatic</li>
     *   <li>Cirrus</li>
     *   <li>Thermal Infrared Sensor (TIRS) 1</li>
     *   <li>Thermal Infrared Sensor (TIRS) 2</li>
     * </ol>
     *
     * @see #BAND_NAMES
     * @see #WAVELENGTHS
     * @see #band(int)
     */
    private final DefaultBand[] bands;

    /**
     * The enumeration for the {@code "DATUM"} element, to be used for creating the Coordinate Reference System.
     */
    private CommonCRS datum;

    /**
     * The Universal Transverse Mercator (UTM) zone as a number from 1 to 60 inclusive, or 0 if the zone has not
     * yet been determined. If the parser determined that the projection is Polar Stereographic, then this field
     * is set to -1.
     */
    private short utmZone;

    /**
     * The map projection parameters. This is used only for the polar stereographic case.
     */
    private ParameterValueGroup projection;

    /**
     * Creates a new metadata parser.
     *
     * @param  filename   an identifier of the file being read, or {@code null} if unknown.
     * @param  listeners  where to sent warnings that may occur during the parsing process.
     */
    LandsatReader(final String filename, final WarningListeners<?> listeners) {
        this.filename  = filename;
        this.listeners = listeners;
        this.metadata  = new MetadataBuilder();
        this.bands     = new DefaultBand[BAND_NAMES.length];
        this.gridSizes = new int[NUM_GROUPS * DIM];
        this.corners   = new double[GEOGRAPHIC + (4*DIM)];      // GEOGRAPHIC is the last group of corners to store.
        Arrays.fill(corners, Double.NaN);
    }

    /**
     * Parses the metadata from the given characters reader.
     * The parsing stop after the first {@code "END"} keyword.
     * See class javadoc for more information on the expected format.
     *
     * @param  reader  a reader opened on the Landsat file.
     *         It is caller's responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    void read(final BufferedReader reader) throws IOException, DataStoreException {
        metadata.newCoverage(true);   // Starts the description of a new image.
        String line;
        while ((line = reader.readLine()) != null) {
            int end  = CharSequences.skipTrailingWhitespaces(line, 0, line.length());
            int start = CharSequences.skipLeadingWhitespaces(line, 0, end);
            if (start < end && line.charAt(start) != '#') {
                /*
                 * Separate the line into its key and value. For example in CORNER_UL_LAT_PRODUCT = 12.61111,
                 * the key will be CORNER_UL_LAT_PRODUCT and the value will be 12.61111.
                 */
                final int separator = line.indexOf('=', start);
                if (separator < 0) {
                    /*
                     * Landsat metadata ends with the END keyword, without value after that keyword.
                     * If we find it, stop reading. All remaining lines (if any) will be ignored.
                     */
                    if (end - start != END.length() || !line.regionMatches(true, start, END, 0, END.length())) {
                        throw new DataStoreException(errors().getString(Errors.Keys.NotAKeyValuePair_1, line));
                    }
                    return;
                }
                /*
                 * If the key ends with "_BAND_" followed by a number, remove the band number from the
                 * key and parse that number as an integer value. Exemple: "REFLECTANCE_ADD_BAND_1".
                 * We keep the "_BAND_" suffix in the key for avoiding ambiguity.
                 */
                String key = line.substring(start, CharSequences.skipTrailingWhitespaces(line, start, separator)).toUpperCase(Locale.US);
                int band = 0;
                for (int i=key.length(); --i >= 0;) {
                    final char c = key.charAt(i);
                    if (c < '0' || c > '9') {
                        if (c == '_') {
                            if (key.regionMatches(i - BAND_SUFFIX.length(), BAND_SUFFIX, 0, BAND_SUFFIX.length())) try {
                                band = Integer.parseInt(key.substring(++i));
                                key = key.substring(0, i);
                            } catch (NumberFormatException e) {
                                warning(key, reader, e);
                            }
                        }
                        break;
                    }
                }
                /*
                 * In a Landsat file, String values are between quotes. Example: STATION_ID = "LGN".
                 * If such quotes are found, remove them.
                 */
                start = CharSequences.skipLeadingWhitespaces(line, separator + 1, end);
                if (end - start >= 2 && line.charAt(start) == '"' && line.charAt(end - 1) == '"') {
                    start = CharSequences.skipLeadingWhitespaces(line, start + 1, --end);
                    end = CharSequences.skipTrailingWhitespaces(line, start, end);
                }
                try {
                    parseKeyValuePair(key, band, line.substring(start, end));
                } catch (IllegalArgumentException | DateTimeException e) {
                    warning(key, reader, e);
                }
            }
        }
        listeners.warning(errors().getString(Errors.Keys.UnexpectedEndOfFile_1, getFilename()), null);
    }

    /**
     * Parses the given string as a {@code double} value, returning a shared instance if possible.
     *
     * @param   value  the string value to parse.
     * @return  the parsed value.
     * @throws  NumberFormatException if the given value can not be parsed.
     */
    private Double parseDouble(final String value) throws NumberFormatException {
        return metadata.shared(Double.valueOf(value));
    }

    /**
     * Parses the given value and stores it at the given index in the {@link #corners} array.
     * The given index must be one of the {@link #PROJECTED} or {@link #GEOGRAPHIC} constants
     * plus the ordinate index.
     */
    private void parseCorner(final int index, final String value) throws NumberFormatException {
        corners[index] = Double.parseDouble(value);
    }

    /**
     * Parses the given value and stores it at the given index in the {@link #gridSizes} array.
     *
     * @param  index  {@link #PANCHROMATIC}, {@link #REFLECTIVE} or {@link #THERMAL},
     *                +1 if parsing the height instead than the width.
     * @param  value  the value to parse.
     */
    private void parseGridSize(final int index, final String value) throws NumberFormatException {
        gridSizes[index] = Integer.parseInt(value);
    }

    /**
     * Invoked for every key-value pairs found in the file.
     * Leading and trailing spaces, if any, have been removed.
     *
     * @param  key    the key in upper cases.
     * @param  band   the band number, or 0 if none.
     * @param  value  the value, without quotes if those quotes existed.
     * @throws NumberFormatException if the value was expected to be a string but the parsing failed.
     * @throws DateTimeException if the value was expected to be a date but the parsing failed,
     *         or if the result of the parsing was not of the expected type.
     * @throws IllegalArgumentException if the value is out of range.
     */
    private void parseKeyValuePair(final String key, final int band, String value)
            throws IllegalArgumentException, DateTimeException, DataStoreException
    {
        switch (key) {
            case "GROUP": {
                group = value;
                break;
            }
            case "END_GROUP": {
                group = null;
                break;
            }

            ////
            //// GROUP = METADATA_FILE_INFO
            ////

            /*
             * Origin of the product.
             * Value is "Image courtesy of the U.S. Geological Survey".
             */
            case "ORIGIN": {
                final Matcher m = CREDIT.matcher(value);
                if (m.find()) {
                    metadata.newParty(MetadataBuilder.ORGANISATION);
                    metadata.addAuthor(value.substring(m.end()));
                }
                metadata.addCredits(value);
                break;
            }
            /*
             * Product Request ID. NNNNNNNNNNNNN_UUUUU, where NNNNNNNNNNNNN = 13-digit Tracking,
             * Routing, and Metrics (TRAM) order number and UUUUU = 5-digit TRAM unit number.
             * Example: "0501403126384_00011"
             */
            case "REQUEST_ID": {
                metadata.addAcquisitionRequirement(value);
                break;
            }
            /*
             * The unique Landsat scene identifier.
             * Format is {@code Ls8ppprrrYYYYDDDGGGVV}.
             * Example: "LC81230522014071LGN00".
             */
            case "LANDSAT_SCENE_ID": {
                metadata.addIdentifier(value);
                break;
            }
            /*
             * The date when the metadata file for the L1G product set was created.
             * The date is based on Universal Time Coordinated (UTC).
             * Date format is {@code YYYY-MM-DDTHH:MM:SSZ}.
             * Example: "2014-03-12T06:06:35Z".
             */
            case "FILE_DATE": {
                metadata.add(StandardDateFormat.toDate(OffsetDateTime.parse(value)), DateType.CREATION);
                break;
            }
            /*
             * The Ground Station that received the data. Grounds station identifiers are specified in LSDS-547.
             * Example: "LGN" = Landsat Ground Network.
             */
// TODO     case "STATION_ID":
            /*
             * The processing software version that created the product. Can be "IAS_X.Y.Z" or "LPGS_X.Y.Z"
             * where X, Y and Z are major, minor and patch version numbers.
             * Example: "LPGS_2.3.0".
             */
// TODO     case "PROCESSING_SOFTWARE_VERSION":

            ////
            //// GROUP = PRODUCT_METADATA
            ////

            /*
             * The identifier to inform the user of the product type.
             * Value can be "L1T" or "L1GT".
             */
// TODO     case "DATA_TYPE":
            /*
             * Indicates the source of the DEM used in the correction process.
             * Value can be "GLS2000", "RAMP" or "GTOPO30".
             */
// TODO     case "ELEVATION_SOURCE":
            /*
             * The output format of the image.
             * Value is "GEOTIFF".
             */
            case "OUTPUT_FORMAT": {
                if ("GeoTIFF".equalsIgnoreCase(value)) {
                    value = "GeoTIFF";                      // Because 'metadata.setFormat(…)' is case-sensitive.
                }
                try {
                    metadata.setFormat(value);
                } catch (MetadataStoreException e) {
                    warning(key, null, e);
                }
                break;
            }
            /*
             * Spacecraft from which the data were captured.
             * Example: "LANDSAT_8".
             */
            case "SPACECRAFT_ID": {
                metadata.addPlatform(value);
                break;
            }
            /*
             * Sensor(s) used to capture this scene.
             * Example: "OLI", "TIRS" or "OLI_TIRS".
             */
            case "SENSOR_ID": {
                metadata.addInstrument(value);
                break;
            }
            /*
             * The date the image was acquired.
             * Date format is {@code YYYY-MM-DD}.
             * Example: "2014-03-12".
             */
            case "DATE_ACQUIRED": {
                final LocalDate date = LocalDate.parse(value);
                if (sceneTime instanceof OffsetTime) {
                    sceneTime = date.atTime((OffsetTime) sceneTime);
                } else if (!date.equals(sceneTime)) {
                    flushSceneTime();
                    sceneTime = date;
                }
                break;
            }
            /*
             * Scene center time of the date the image was acquired.
             * Time format is {@code HH:MI:SS.SSSSSSSZ}.
             * Example: "03:02:01.5339408Z".
             */
            case "SCENE_CENTER_TIME": {
                final OffsetTime time = OffsetTime.parse(value);
                if (sceneTime instanceof LocalDate) {
                    sceneTime = ((LocalDate) sceneTime).atTime(time);
                } else {
                    sceneTime = time;
                }
                break;
            }
            /*
             * The longitude and latitude values for the upper-left (UL), upper-right (UR), lower-left (LL)
             * and lower-right (LR) corners of the product, measured at the center of the pixel.
             * Positive longitude value indicates east longitude; negative value indicates west longitude.
             * Positive latitude value indicates north latitude; negative value indicates south latitude.
             * Units are in degrees.
             */
            case "CORNER_UL_LON_PRODUCT": parseCorner(GEOGRAPHIC + 0, value); break;
            case "CORNER_UL_LAT_PRODUCT": parseCorner(GEOGRAPHIC + 1, value); break;
            case "CORNER_UR_LON_PRODUCT": parseCorner(GEOGRAPHIC + 2, value); break;
            case "CORNER_UR_LAT_PRODUCT": parseCorner(GEOGRAPHIC + 3, value); break;
            case "CORNER_LL_LON_PRODUCT": parseCorner(GEOGRAPHIC + 4, value); break;
            case "CORNER_LL_LAT_PRODUCT": parseCorner(GEOGRAPHIC + 5, value); break;
            case "CORNER_LR_LON_PRODUCT": parseCorner(GEOGRAPHIC + 6, value); break;
            case "CORNER_LR_LAT_PRODUCT": parseCorner(GEOGRAPHIC + 7, value); break;
            /*
             * The upper-left (UL), upper-right (UR), lower-left (LL) and lower-right (LR) corner map
             * projection X and Y coordinate, measured at the center of the pixel. Units are in meters.
             */
            case "CORNER_UL_PROJECTION_X_PRODUCT": parseCorner(PROJECTED + 0, value); break;
            case "CORNER_UL_PROJECTION_Y_PRODUCT": parseCorner(PROJECTED + 1, value); break;
            case "CORNER_UR_PROJECTION_X_PRODUCT": parseCorner(PROJECTED + 2, value); break;
            case "CORNER_UR_PROJECTION_Y_PRODUCT": parseCorner(PROJECTED + 3, value); break;
            case "CORNER_LL_PROJECTION_X_PRODUCT": parseCorner(PROJECTED + 4, value); break;
            case "CORNER_LL_PROJECTION_Y_PRODUCT": parseCorner(PROJECTED + 5, value); break;
            case "CORNER_LR_PROJECTION_X_PRODUCT": parseCorner(PROJECTED + 6, value); break;
            case "CORNER_LR_PROJECTION_Y_PRODUCT": parseCorner(PROJECTED + 7, value); break;
            /*
             * The number of product lines and samples for the panchromatic, reflective and thermal bands.
             * Those parameters are only present if the corresponding band is present in the product.
             */
            case "PANCHROMATIC_LINES":   parseGridSize(PANCHROMATIC + 1, value); break;
            case "PANCHROMATIC_SAMPLES": parseGridSize(PANCHROMATIC,     value); break;
            case "REFLECTIVE_LINES":     parseGridSize(REFLECTIVE + 1,   value); break;
            case "REFLECTIVE_SAMPLES":   parseGridSize(REFLECTIVE,       value); break;
            case "THERMAL_LINES":        parseGridSize(THERMAL + 1,      value); break;
            case "THERMAL_SAMPLES":      parseGridSize(THERMAL,          value); break;
            /*
             * The grid cell size in meters used in creating the image for the band, if part of the product.
             * This parameter is only included if the corresponding band is included in the product.
             */
            case "GRID_CELL_SIZE_PANCHROMATIC":
            case "GRID_CELL_SIZE_REFLECTIVE":
            case "GRID_CELL_SIZE_THERMAL": {
                metadata.addResolution(Double.parseDouble(value));
                break;
            }
            /*
             * The file name of the TIFF image that contains the pixel values for a band.
             * This parameter is only present if the band is included in the product.
             */
            case "FILE_NAME_BAND_": {
                final DefaultBand db = band(key, band);
                if (db != null) {
                    db.getNames().add(new DefaultIdentifier(value));
                }
                break;
            }
            /*
             * The file name for L1 metadata.
             * Exemple: "LC81230522014071LGN00_MTL.txt".
             */
            case "METADATA_FILE_NAME": {
                if (filename == null) {
                    filename = value;
                }
                break;
            }

            ////
            //// GROUP = IMAGE_ATTRIBUTES
            ////

            /*
             * The overall cloud coverage (percent) of the WRS-2 scene as a value between 0 and 100 inclusive.
             * -1 indicates that the score was not calculated.
             */
            case "CLOUD_COVER": {
                final double v = Double.parseDouble(value);
                if (v >= 0) metadata.setCloudCoverPercentage(v);
                break;
            }
            /*
             * The Sun azimuth angle in degrees for the image center location at the image center acquisition time.
             * Values are from -180 to 180 degrees inclusive.
             * A positive value indicates angles to the east or clockwise from the north.
             * A negative value indicates angles to the west or counterclockwise from the north.
             */
            case "SUN_AZIMUTH": {
                metadata.setIlluminationAzimuthAngle(Double.parseDouble(value));
                break;
            }
            /*
             * The Sun elevation angle in degrees for the image center location at the image center acquisition time.
             * Values are from -90 to 90 degrees inclusive.
             * A positive value indicates a daytime scene. A negative value indicates a nighttime scene.
             * Note: for reflectance calculation, the sun zenith angle is needed, which is 90 - sun elevation angle.
             */
            case "SUN_ELEVATION": {
                metadata.setIlluminationElevationAngle(Double.parseDouble(value));
                break;
            }

            ////
            //// GROUP = MIN_MAX_PIXEL_VALUE
            ////

            /*
             * Minimum achievable spectral radiance value for a band 1.
             * This parameter is only present if this band is included in the product.
             */
            case "QUANTIZE_CAL_MIN_BAND_": {
                final Double v = parseDouble(value);        // Done first in case an exception is thrown.
                final DefaultBand db = band(key, band);
                if (db != null) {
                    db.setMinValue(v);
                }
                break;
            }
            /*
             * Maximum achievable spectral radiance value for a band 1.
             * This parameter is only present if this band is included in the product.
             */
            case "QUANTIZE_CAL_MAX_BAND_": {
                final Double v = parseDouble(value);        // Done first in case an exception is thrown.
                final DefaultBand db = band(key, band);
                if (db != null) {
                    db.setMaxValue(v);
                }
                break;
            }

            ////
            //// GROUP = RADIOMETRIC_RESCALING
            ////

            /*
             * The multiplicative rescaling factor used to convert calibrated DN to Radiance units for a band.
             * Unit is W/(m² sr um)/DN.
             */
            case "RADIANCE_MULT_BAND_": {
                setTransferFunction(key, band, true, value);
                break;
            }
            /*
             * The additive rescaling factor used to convert calibrated DN to Radiance units for a band.
             * Unit is W/(m² sr um)/DN.
             */
            case "RADIANCE_ADD_BAND_": {
                setTransferFunction(key, band, false, value);
                break;
            }

            ////
            //// GROUP = PROJECTION_PARAMETERS
            ////

            /*
             * The map projection used in creating the image.
             * Universal Transverse Mercator (UTM) or Polar Stereographic (PS).
             */
            case "MAP_PROJECTION": {
                if ("UTM".equalsIgnoreCase(value)) {
                    projection = null;
                } else if ("PS".equalsIgnoreCase(value)) try {
                    projection = DefaultFactories.forBuildin(MathTransformFactory.class)
                                    .getDefaultParameters(Constants.EPSG + ':' + PolarStereographicB.IDENTIFIER);
                    utmZone = -1;
                } catch (NoSuchIdentifierException e) {
                    // Should never happen with Apache SIS implementation of MathTransformFactory.
                    throw new DataStoreReferencingException(e);
                }
                break;
            }
            /*
             * The datum used in creating the image. This is usually "WGS84".
             * We ignore the "ELLIPSOID" attribute because it is implied by the datum.
             */
            case "DATUM": {
                datum = CommonCRS.valueOf(Utilities.toUpperCase(value, Characters.Filter.LETTERS_AND_DIGITS));
                break;
            }
            /*
             * The value used to indicate the zone number. This parameter is only included for the UTM projection.
             * If this parameter is defined more than once (which should be illegal), only the first occurrence is
             * retained. If the projection is polar stereographic, the parameter is ignored.
             */
            case "UTM_ZONE": {
                if (utmZone == 0) {
                    utmZone = Short.parseShort(value);
                }
                break;
            }
            /*
             * Polar Stereographic projection parameters. Most parameters do not vary, except the latitude of
             * true scale which is -71 for scenes over Antarctica and 71 for off-nadir scenes at the North Pole.
             * If the datum is WGS84, then this is equivalent to EPSG:3031 and EPSG:3995 respectively.
             */
            case "VERTICAL_LON_FROM_POLE": setProjectionParameter(key, Constants.CENTRAL_MERIDIAN,    value, false); break;
            case "TRUE_SCALE_LAT":         setProjectionParameter(key, Constants.STANDARD_PARALLEL_1, value, false); break;
            case "FALSE_EASTING":          setProjectionParameter(key, Constants.FALSE_EASTING,       value, true);  break;
            case "FALSE_NORTHING":         setProjectionParameter(key, Constants.FALSE_NORTHING,      value, true);  break;
        }
    }

    /**
     * Sets a component of the linear transfer function.
     *
     * @param  key      the key without its band number. Used only for formatting warning messages.
     * @param  band     index of the band to set.
     * @param  isScale  {@code true} for setting the scale factor, or {@code false} for setting the offset.
     * @param  value    the value to set.
     */
    private void setTransferFunction(final String key, final int band, final boolean isScale, final String value) {
        final Double v = parseDouble(value);            // Done first in case an exception is thrown.
        final DefaultBand db = band(key, band);
        if (db != null) {
            db.setTransferFunctionType(TransferFunctionType.LINEAR);
            if (isScale) {
                db.setScaleFactor(v);
            } else {
                db.setOffset(v);
            }
        }
    }

    /**
     * Returns the band at the given index, creating it if needed.
     * If the given index is out of range, then this method logs a warning and returns {@code null}.
     *
     * @param  key    the key without its band number. Used only for formatting warning messages.
     * @param  index  the band index.
     */
    private DefaultBand band(final String key, int index) {
        if (index < 1 || index > BAND_NAMES.length) {
            listeners.warning(errors().getString(Errors.Keys.UnexpectedValueInElement_2, key + index, index), null);
            return null;
        }
        DefaultBand band = bands[--index];
        if (band == null) {
            band = new DefaultBand();
            band.setDescription(new SimpleInternationalString(BAND_NAMES[index]));
            band.setPeakResponse((double) WAVELENGTHS[index]);
            band.setBoundUnits(Units.NANOMETRE);
            bands[index] = band;
        }
        return band;
    }

    /**
     * Sets a map projection parameter. The parameter is ignored if the projection has not been set.
     *
     * @param key       the Landsat key, for formatting error message if needed.
     * @param name      the projection parameter name.
     * @param value     the parameter value.
     * @param isLinear  {@code true} for value in metres, or {@code false} for value in degrees.
     */
    private void setProjectionParameter(final String key, final String name, final String value, final boolean isLinear) {
        if (projection != null) {
            projection.parameter(name).setValue(Double.parseDouble(value), isLinear ? Units.METRE : Units.DEGREE);
        } else {
            listeners.warning(errors().getString(Errors.Keys.UnexpectedProperty_2, filename, key), null);
        }
    }

    /**
     * Writes the value of {@link #sceneTime} into the metadata object as a temporal extent.
     *
     * @throws DateTimeException if {@link #sceneTime} is an instance of {@link OffsetTime}. This may
     *         happen if {@code SCENE_CENTER_TIME} attribute was found without {@code DATE_ACQUIRED}.
     */
    private void flushSceneTime() {
        final Temporal st = sceneTime;
        if (st != null) {
            sceneTime = null;                   // Clear now in case an exception it thrown below.
            final Date t = StandardDateFormat.toDate(st);
            metadata.addAcquisitionTime(t);
            try {
                metadata.addExtent(t, t);
            } catch (UnsupportedOperationException e) {
                // May happen if the temporal module (which is optional) is not on the classpath.
                warning(null, null, e);
            }
        }
    }

    /**
     * Computes the bounding box for the 8 {@link #corners} values starting at the given index.
     * Valid indices are 0 for the projected envelope or 8 for the geographic bounding box.
     * Result is stored in the 4 values starting the given {@code base} index.
     *
     * @return {@code true} of success, or {@code false} if there is no bounding box.
     */
    private boolean toBoundingBox(int base) {
        double xmin = Double.POSITIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        for (int i = base + (4*DIM); --i >= base;) {
            double v = corners[i];
            if (v < ymin) ymin = v;
            if (v > ymax) ymax = v;
            v = corners[--i];
            if (v < xmin) xmin = v;
            if (v > xmax) xmax = v;
        }
        if (xmin < xmax && ymin < ymax) {
            corners[  base] = xmin;
            corners[++base] = xmax;
            corners[++base] = ymin;
            corners[++base] = ymax;
            return true;
        }
        return false;
    }

    /**
     * Returns the metadata about the resources described in the Landsat file.
     * The {@link #read(BufferedReader)} method must be invoked at least once before.
     *
     * @throws FactoryException if an error occurred while creating the Coordinate Reference System.
     */
    final Metadata getMetadata() throws FactoryException {
        metadata.add(Locale.ENGLISH);
        metadata.add(ScopeCode.COVERAGE);
        try {
            flushSceneTime();
        } catch (DateTimeException e) {
            // May happen if the SCENE_CENTER_TIME attribute was found without DATE_ACQUIRED.
            warning(null, null, e);
        }
        /*
         * Create the Coordinate Reference System. We normally have only one of UTM or Polar Stereographic,
         * but this block is nevertheless capable to take both (such metadata are likely to be invalid, but
         * we can not guess which of the two CRS is correct).
         */
        if (datum != null) {
            if (utmZone > 0) {
                metadata.add(datum.UTM(1, TransverseMercator.centralMeridian(utmZone)));
            }
            if (projection != null) {
                final double sp = projection.parameter(Constants.STANDARD_PARALLEL_1).doubleValue();
                ProjectedCRS crs = (ProjectedCRS) CRS.forCode(Constants.EPSG + ":" +
                        (sp >= 0 ? Constants.EPSG_ARCTIC_POLAR_STEREOGRAPHIC         // Standard parallel = 71°N
                                 : Constants.EPSG_ANTARCTIC_POLAR_STEREOGRAPHIC));   // Standard parallel = 71°S
                if (datum != CommonCRS.WGS84 || Math.abs(sp) != 71
                        || projection.parameter(Constants.FALSE_EASTING)   .doubleValue() != 0
                        || projection.parameter(Constants.FALSE_NORTHING)  .doubleValue() != 0
                        || projection.parameter(Constants.CENTRAL_MERIDIAN).doubleValue() != 0)
                {
                    crs = ReferencingUtilities.createProjectedCRS(
                            Collections.singletonMap(ProjectedCRS.NAME_KEY, "Polar stereographic"),
                            datum.geographic(), projection, crs.getCoordinateSystem());
                }
                metadata.add(crs);
            }
        }
        /*
         * Set information about envelope (or geographic area) and grid size.
         */
        if (toBoundingBox(GEOGRAPHIC)) {
            metadata.addExtent(corners, GEOGRAPHIC);
        }
        for (int i = 0; i < gridSizes.length; i += DIM) {
            final int width  = gridSizes[i  ];
            final int height = gridSizes[i+1];
            if (width != 0 || height != 0) {
                metadata.newGridRepresentation();
                metadata.setAxisName((short) 0, DimensionNameType.SAMPLE);
                metadata.setAxisName((short) 1, DimensionNameType.LINE);
                metadata.setAxisLength((short) 0, width);
                metadata.setAxisLength((short) 1, height);
            }
        }
        /*
         * At this point we are done configuring he metadata builder. Creates the ISO 19115 metadata instance,
         * then continue adding some more specific metadata elements by ourself. For example information about
         * bands are splitted in 3 different AttributeGroups based on their grid size.
         */
        final DefaultMetadata result = metadata.build(false);
        if (result != null) {
            /*
             * If there is exactly one data identification (which is usually the case, unless the user has invoked the
             * read(BufferedReader) method many times), use the same identifier and date for the metadata as a whole.
             */
            final Identification id = singletonOrNull(result.getIdentificationInfo());
            if (id != null) {
                final Citation citation = id.getCitation();
                if (citation != null) {
                    result.setMetadataIdentifier(singletonOrNull(citation.getIdentifiers()));
                    result.setDateInfo(singleton(singletonOrNull(citation.getDates())));
                }
            }
            /*
             * Set information about all non-null bands. The bands are categorized in three groups:
             * PANCHROMATIC, REFLECTIVE and THERMAL. The group in which each band belong is encoded
             * in the BAND_GROUPS bitmask.
             */
            final DefaultCoverageDescription content = (DefaultCoverageDescription) singletonOrNull(result.getContentInfo());
            if (content != null) {
                final DefaultAttributeGroup[] groups = new DefaultAttributeGroup[NUM_GROUPS];
                for (int i=0; i < bands.length; i++) {
                    final DefaultBand band = bands[i];
                    if (band != null) {
                        final int gi = (BAND_GROUPS >>> 2*i) & 3;
                        DefaultAttributeGroup group = groups[gi];
                        if (group == null) {
                            group = new DefaultAttributeGroup(CoverageContentType.PHYSICAL_MEASUREMENT, null);
                            content.getAttributeGroups().add(group);
                            groups[gi] = group;
                        }
                        group.getAttributes().add(band);
                    }
                }
            }
            result.setMetadataStandards(Citations.ISO_19115);
            result.freeze();
        }
        return result;
    }

    /**
     * Returns the filename to show in error messages, or a localized "unnamed" word if none.
     */
    private String getFilename() {
        return (filename != null) ? filename : Vocabulary.getResources(listeners.getLocale()).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * Prepends the group name before the given key, if a group name exists.
     * This is used only for formatting warning messages.
     */
    private String toLongName(String key) {
        if (group != null) {
            key = group + ':' + key;
        }
        return key;
    }

    /**
     * Invoked when a non-fatal exception occurred while reading metadata. This method
     * sends a record to the registered listeners if any, or logs the record otherwise.
     */
    private void warning(String key, final BufferedReader reader ,final Exception e) {
        if (key != null) {
            String file = getFilename();
            if (reader instanceof LineNumberReader) {
                file = file + ":" + ((LineNumberReader) reader).getLineNumber();
            }
            key = errors().getString(Errors.Keys.CanNotReadPropertyInFile_2, toLongName(key), file);
        }
        listeners.warning(key, e);
    }

    /**
     * Returns the resources to use for formatting error messages.
     */
    private Errors errors() {
        return Errors.getResources(listeners.getLocale());
    }
}
