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
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.LineNumberReader;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.ScopeCode;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultBand;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.util.StandardDateFormat;

import static java.util.Collections.singleton;
import static org.apache.sis.internal.util.CollectionsExt.singletonOrNull;

// Branch-dependent imports
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.DateTimeException;
import java.time.temporal.Temporal;
import org.opengis.metadata.content.AttributeGroup;


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
     * The description of all bands that can be included in a Landsat coverage.
     * This description is hard-coded and shared by all metadata instances.
     *
     * @todo Move those information in a database after we implemented the {@code org.apache.sis.metadata.sql} package.
     */
    private static final AttributeGroup BANDS;
    static {
        final double[] wavelengths = {433, 482, 562, 655, 865, 1610, 2200, 590, 1375, 10800, 12000};
        final String[] nameband = {
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
        final DefaultBand[] bands = new DefaultBand[wavelengths.length];
        final Unit<Length> nm = SI.MetricPrefix.NANO(SI.METRE);
        for (int i = 0; i < bands.length; i++) {
            final DefaultBand band = new DefaultBand();
            band.setDescription(new SimpleInternationalString(nameband[i]));
            band.setPeakResponse(wavelengths[i]);
            band.setBoundUnits(nm);
            bands[i] = band;
        }
        final DefaultAttributeGroup attributes = new DefaultAttributeGroup(CoverageContentType.PHYSICAL_MEASUREMENT, null);
        attributes.setAttributes(Arrays.asList(bands));
        attributes.freeze();
        BANDS = attributes;
    }

    /**
     * The pattern determining if the value of {@code ORIGIN} key is of the form
     * “Image courtesy of the U.S. Geological Survey”.
     */
    static final Pattern CREDIT = Pattern.compile("\\bcourtesy\\h+of\\h+(the)?\\b\\s*", Pattern.CASE_INSENSITIVE);

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
     * The locale to use for formatting warning or error messages.
     */
    private final Locale locale;

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
     * Creates a new metadata parser.
     *
     * @param  filename   an identifier of the file being read, or {@code null} if unknown.
     * @param  locale     the locale to use for formatting warning or error messages.
     * @param  listeners  where to sent warnings that may occur during the parsing process.
     */
    LandsatReader(final String filename, final Locale locale, final WarningListeners<?> listeners) {
        this.filename  = filename;
        this.locale    = locale;
        this.listeners = listeners;
        this.metadata  = new MetadataBuilder();
        this.corners   = new double[16];
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
                     * If a group was opened but not closed, report a warning.
                     */
                    if (end - start != END.length() || !line.regionMatches(true, start, END, 0, END.length())) {
                        throw new DataStoreException(errors().getString(Errors.Keys.NotAKeyValuePair_1, line));
                    }
                    if (group != null) {
                        missingEndGroup(reader);
                    }
                    return;
                } else {
                    final String key = line.substring(start,
                            CharSequences.skipTrailingWhitespaces(line, start, separator)).toUpperCase(Locale.US);
                    start = CharSequences.skipLeadingWhitespaces(line, separator + 1, end);
                    /*
                     * In a Landsat file, String values are between quotes. Example: STATION_ID = "LGN".
                     * If such quotes are found, remove them.
                     */
                    if (end - start >= 2 && line.charAt(start) == '"' && line.charAt(end - 1) == '"') {
                        start = CharSequences.skipLeadingWhitespaces(line, start + 1, --end);
                        end = CharSequences.skipTrailingWhitespaces(line, start, end);
                    }
                    try {
                        parseKeyValuePair(key, line.substring(start, end), reader);
                    } catch (IllegalArgumentException | DateTimeException e) {
                        warning(e);
                    }
                }
            }
        }
        warning(Errors.Keys.UnexpectedEndOfFile_1, getFilename());
    }

    /**
     * Parses the given value and stores its value at the given index in the {@link #corners} array.
     */
    private void parseCorner(final int index, final String value) throws NumberFormatException {
        corners[index] = Double.parseDouble(value);
    }

    /**
     * Invoked for every key-value pairs found in the file.
     * Leading and trailing spaces, if any, have been removed.
     *
     * @param  key     the key in upper cases.
     * @param  value   the value, without quotes if those quotes existed.
     * @param  reader  used only for reporting line number of warnings, if any.
     * @throws NumberFormatException if the value was expected to be a string but the parsing failed.
     * @throws DateTimeException if the value was expected to be a date but the parsing failed,
     *         or if the result of the parsing was not of the expected type.
     * @throws IllegalArgumentException if the value is out of range.
     */
    private void parseKeyValuePair(final String key, final String value, final BufferedReader reader)
            throws IllegalArgumentException, DateTimeException
    {
        switch (key) {
            case "GROUP": {
                group = value;
                break;
            }
            case "END_GROUP": {
                if (!value.equals(group) && group != null) {
                    missingEndGroup(reader);
                }
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
// TODO     case "REQUEST_ID":
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
                metadata.addFormat(value);
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
            case "CORNER_UL_LON_PRODUCT": parseCorner( 8, value); break;
            case "CORNER_UL_LAT_PRODUCT": parseCorner( 9, value); break;
            case "CORNER_UR_LON_PRODUCT": parseCorner(10, value); break;
            case "CORNER_UR_LAT_PRODUCT": parseCorner(11, value); break;
            case "CORNER_LL_LON_PRODUCT": parseCorner(12, value); break;
            case "CORNER_LL_LAT_PRODUCT": parseCorner(13, value); break;
            case "CORNER_LR_LON_PRODUCT": parseCorner(14, value); break;
            case "CORNER_LR_LAT_PRODUCT": parseCorner(15, value); break;
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
                warning(e);
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
        for (int i = base+8; --i >= 0;) {
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
     */
    final Metadata getMetadata() {
        metadata.add(Locale.ENGLISH);
        metadata.add(ScopeCode.DATASET);
        try {
            flushSceneTime();
        } catch (DateTimeException e) {
            // May happen if the SCENE_CENTER_TIME attribute was found without DATE_ACQUIRED.
            warning(e);
        }
        if (toBoundingBox(8)) {
            metadata.addExtent(corners, 8);
        }
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
             * Set pre-defined information about all bands.
             */
            final ContentInformation content = singletonOrNull(result.getContentInfo());
            if (content instanceof DefaultCoverageDescription) {
                ((DefaultCoverageDescription) content).setAttributeGroups(singleton(BANDS));
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
        return (filename != null) ? filename : Vocabulary.getResources(locale).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * Invoked when a non-fatal exception occurred while reading metadata. This method
     * sends a record to the registered listeners if any, or logs the record otherwise.
     */
    private void warning(final Exception e) {
        listeners.warning(null, e);
    }

    /**
     * Invoked when a non-fatal error occurred while reading metadata. This method
     * sends a record to the registered listeners if any, or logs the record otherwise.
     *
     * @param  error     one of the {@link Errors.Keys} values.
     * @param  argument  the argument (or an array of arguments) to format.
     */
    private void warning(final short error, final Object argument) {
        listeners.warning(errors().getString(error, argument), null);
    }

    /**
     * Invoked when an expected {@code END_GROUP} statement is missing.
     */
    private void missingEndGroup(final BufferedReader reader) {
        final Object line = (reader instanceof LineNumberReader) ? ((LineNumberReader) reader).getLineNumber() : "?";
        listeners.warning(errors().getString(Errors.Keys.ExpectedStatementAtLine_3, "END_GROUP " + group, getFilename(), line), null);
    }

    /**
     * Returns the resources to use for formatting error messages.
     */
    private Errors errors() {
        return Errors.getResources(locale);
    }
}
