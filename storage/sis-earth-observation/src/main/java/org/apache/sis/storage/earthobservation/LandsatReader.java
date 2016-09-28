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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.maintenance.ScopeCode;

import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultEvent;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultOperation;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultBand;
import org.apache.sis.metadata.iso.content.DefaultImageDescription;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.logging.WarningListeners;

import static java.util.Collections.singleton;
import static org.apache.sis.storage.earthobservation.LandsatKeys.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.LocalDate;
import org.apache.sis.internal.jdk8.OffsetDateTime;
import org.apache.sis.internal.jdk8.OffsetTime;
import org.apache.sis.internal.jdk8.DateTimeParseException;
import org.apache.sis.internal.jdk8.ChronoField;
import org.opengis.metadata.acquisition.Context;


/**
 * Parses Landsat metadata as {@linkplain DefaultMetadata ISO-19115 Metadata} object.
 * This class reads the content of a given {@link BufferedReader} from buffer position
 * until the first occurrence of the {@code END} keyword. Lines beginning with the
 * {@code #} character (ignoring spaces) are treated as comment lines and ignored.
 *
 * <p><b>NOTE FOR MAINTAINER:</b> if the work performed by this class is modified, consider updating
 * <a href="./doc-files/LandsatMetadata.html">./doc-files/LandsatMetadata.html</a> accordingly.</p>
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Rémi Maréchal (Geomatys)
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
    private static final DefaultAttributeGroup BANDS;
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
     * All properties found in the Landsat metadata file, except {@code GROUP} and {@code END_GROUP}. Example:
     *
     * {@preformat text
     *   DATE_ACQUIRED         = 2014-03-12
     *   SCENE_CENTER_TIME     = 03:02:01.5339408Z
     *   CORNER_UL_LAT_PRODUCT = 12.61111
     *   CORNER_UL_LON_PRODUCT = 108.33624
     *   CORNER_UR_LAT_PRODUCT = 12.62381
     *   CORNER_UR_LON_PRODUCT = 110.44017
     * }
     */
    private final Map<String, String> properties;

    /**
     * Where to send the warnings.
     */
    private final WarningListeners<?> listeners;

    /**
     * Creates a new metadata parser from the given characters reader.
     * See class javadoc for more information on the expected format.
     *
     * @param  reader  a reader opened on the Landsat file.
     *         It is caller's responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    LandsatReader(final BufferedReader reader, final WarningListeners<?> listeners)
            throws IOException, DataStoreException
    {
        this.listeners = listeners;
        properties = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && line.charAt(0) != '#') {
                /*
                 * Landsat metadata ends with the END keyword. If we find that keyword, stop reading.
                 * All remaining lines (if any) will be ignored.
                 */
                if (line.equals("END")) {
                    break;
                }
                /*
                 * Separate the line into its key and value. For example in CORNER_UL_LAT_PRODUCT = 12.61111,
                 * the key will be CORNER_UL_LAT_PRODUCT and the value will be 12.61111.
                 */
                int separator = line.indexOf('=');
                if (separator < 0) {
                    throw new DataStoreException("Not a key-value pair.");
                }
                String key = line.substring(0, separator).trim().toUpperCase(Locale.US);
                if (!key.equals("GROUP") && !key.equals("END_GROUP")) {
                    String value = line.substring(separator + 1).trim();
                    if (key.isEmpty()) {
                        throw new DataStoreException("Key shall not be empty.");
                    }
                    /*
                     * In a Landsat file, String values are between quotes. Example: STATION_ID = "LGN".
                     * If such quotes are found, remove them.
                     */
                    int length = value.length();
                    if (length >= 2 && value.charAt(0) == '"' && value.charAt(length - 1) == '"') {
                        value = value.substring(1, length - 1).trim();
                        length = value.length();
                    }
                    /*
                     * Store only non-empty values. If a different value was already specified for the same key,
                     * this is considered as an error.
                     */
                    if (length != 0) {
                        String previous = properties.put(key, value);
                        if (previous != null && !value.equals(previous)) {
                            throw new DataStoreException("Duplicated values for \"" + key + "\".");
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the property value associated to the given key, or {@code null} if none.
     *
     * @param  key  the key for which to get the property value.
     * @return the property value associated to the given key, {@code null} if none.
     */
    private String getValue(String key) {
        return properties.get(key);
    }

    /**
     * Returns the floating-point value associated to the given key, or {@code NaN} if none.
     *
     * @param  key the key for which to get the floating-point value.
     * @return the floating-point value associated to the given key, or {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to the given key can not be parsed
     *         as a floating-point number.
     */
    private double getNumericValue(String key) throws NumberFormatException {
        String value = getValue(key);
        return (value != null) ? Double.parseDouble(value) : Double.NaN;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys, or {@code NaN} if none.
     *
     * @param  key1  the key for which to get the first floating-point value.
     * @param  key2  the key for which to get the second floating-point value.
     * @param  max   {@code true} for the maximal value, or {@code false} for the minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max} is true) floating-point value
     *         associated to the given keys, or {@link Double#NaN} if none.
     * @throws NumberFormatException if the properties associated to the given keys can not be parsed
     *         as floating-point numbers.
     */
    private double getExtremumValue(String key1, String key2, boolean max) throws NumberFormatException {
        double value1 = getNumericValue(key1);
        double value2 = getNumericValue(key2);
        if (max ? (value2 > value1) : (value2 < value1)) {
            return value2;
        } else {
            return value1;
        }
    }

    /**
     * Returns the date associated to the given key, or {@code null} if none.
     * The date is expected to be formatted in ISO 8601 format.
     *
     * @param  key the key for which to get the date value.
     * @return the date associated to the given key, or {@code null} if none.
     * @throws DateTimeParseException if the date can not be parsed.
     */
    private Date getDate(final String key) throws DateTimeParseException {
        final String value = getValue(key);
        if (value == null) {
            return null;
        }
        final OffsetDateTime time = OffsetDateTime.parse(value);
        return new Date(time.toEpochSecond() * 1000 + time.getNano() / 1000000);
    }

    /**
     * Returns the date and time associated to the given key, or {@code null} if none.
     * The date and time are expected to be in two separated fields, with each field
     * formatted in ISO 8601 format.
     *
     * @param  dateKey  the key for which to get the date value.
     * @param  timeKey  the key for which to get the time value.
     * @return the date and time associated to the given keys, or {@code null} if none.
     * @throws DateTimeParseException if the date can not be parsed.
     */
    private Date getDate(final String dateKey, final String timeKey) throws DateTimeParseException {
        String value = getValue(dateKey);
        if (value == null) {
            return null;
        }
        final LocalDate date = LocalDate.parse(value);
        value = getValue(timeKey);
        final long millis;
        if (value == null) {
            millis = date.getLong(ChronoField.INSTANT_SECONDS) * 1000;
        } else {
            final OffsetDateTime time = date.atTime(OffsetTime.parse(value));
            millis = time.toEpochSecond() * 1000 + time.getNano() / 1000000;
        }
        return new Date(millis);
    }

    /**
     * Gets information about an image's suitability for use.
     *
     * @throws DataStoreException if a property value can not be parsed as a number or a date.
     */
    private ImageDescription createImageDescription() throws DataStoreException {
        final DefaultImageDescription content = new DefaultImageDescription();
        try {
            double value;
            if (0 <= (value = getNumericValue(CLOUD_COVER))) {
                content.setCloudCoverPercentage(value);
            }
            if (!Double.isNaN(value = getNumericValue(SUN_AZIMUTH))) {
                content.setIlluminationAzimuthAngle(value);
            }
            if (!Double.isNaN(value = getNumericValue(SUN_ELEVATION))) {
                content.setIlluminationElevationAngle(value);
            }
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read content information.", e);
        }
        content.setAttributeGroups(singleton(BANDS));
        return content;
    }

    /**
     * Gets the geographic and temporal extent for identification info, or {@code null} if none.
     * This method expects the data acquisition time in argument in order to avoid to compute it twice.
     *
     * @param  sceneTime the data acquisition time, or {@code null} if none.
     * @return the data extent in Identification info, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a number or a date.
     */
    private Extent createExtent(final Date sceneTime) throws DataStoreException {
        final DefaultGeographicBoundingBox box;
        try {
            box = new DefaultGeographicBoundingBox(
                    getExtremumValue(CORNER_UL_LON_PRODUCT, CORNER_LL_LON_PRODUCT, false),      // westBoundLongitude
                    getExtremumValue(CORNER_UR_LON_PRODUCT, CORNER_LR_LON_PRODUCT, true),       // eastBoundLongitude
                    getExtremumValue(CORNER_LL_LAT_PRODUCT, CORNER_LR_LAT_PRODUCT, false),      // southBoundLatitude
                    getExtremumValue(CORNER_UL_LAT_PRODUCT, CORNER_UR_LAT_PRODUCT, true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        final DefaultExtent extent = new DefaultExtent();
        boolean isEmpty = box.isEmpty();
        if (!isEmpty) {
            extent.setGeographicElements(singleton(box));
        }
        if (sceneTime != null) {
            try {
                final DefaultTemporalExtent t = new DefaultTemporalExtent();
                t.setBounds(sceneTime, sceneTime);
                extent.setTemporalElements(singleton(t));
                isEmpty = false;                            // Set only after the above succeed.
            } catch (UnsupportedOperationException e) {
                // May happen if the temporal module (which is optional) is not on the classpath.
                warning(e);
            }
        }
        return isEmpty ? null : extent;
    }

    /**
     * Gets the acquisition information, or {@code null} if none. This method expects
     * the data acquisition time in argument in order to avoid to compute it twice.
     *
     * @param  sceneTime  the data acquisition time, or {@code null} if none.
     * @return the data for the Acquisition Information, or {@code null} if none.
     */
    private AcquisitionInformation createAcquisitionInformation(final Date sceneTime) {
        final DefaultAcquisitionInformation acquisition = new DefaultAcquisitionInformation();
        final DefaultPlatform platform = new DefaultPlatform();
        String value = getValue(SPACECRAFT_ID);
        boolean isEmpty = true;
        if (value != null) {
            platform.setIdentifier(new DefaultIdentifier(value));
            isEmpty = false;
        }
        value = getValue(SENSOR_ID);
        if (value != null) {
            final DefaultInstrument instrument = new DefaultInstrument();
            instrument.setIdentifier(new DefaultIdentifier(value));
            platform.setInstruments(singleton(instrument));
            isEmpty = false;
        }
        if (!isEmpty) {
            acquisition.setPlatforms(singleton(platform));
        }
        if (sceneTime != null) {
            final DefaultEvent event = new DefaultEvent();
            event.setContext(Context.ACQUISITION);
            event.setTime(sceneTime);
            final DefaultOperation op = new DefaultOperation();
            op.setSignificantEvents(singleton(event));
            op.setType(OperationType.REAL);
            op.setStatus(Progress.COMPLETED);
            acquisition.setOperations(singleton(op));
            isEmpty = false;
        }
        return isEmpty ? null : acquisition;
    }

    /**
     * Gets basic information required to uniquely identify the data, or {@code null} if none.
     * This method expects the metadata and data acquisition time in argument in order to avoid
     * to compute them twice.
     *
     * @param  metadataTime  the metadata file creation time, or {@code null} if none.
     * @param  sceneTime     the data acquisition time, or {@code null} if none.
     * @return the data identification information, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a number or a date.
     */
    private Identification createIdentification(final DefaultCitationDate metadataTime, final Date sceneTime) throws DataStoreException {
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        final DefaultCitation citation = new DefaultCitation();
        boolean isEmpty = true;
        if (metadataTime != null) {
            citation.setDates(singleton(metadataTime));
            isEmpty = false;
        }
        String value = getValue(LANDSAT_SCENE_ID);
        if (value != null) {
            citation.setIdentifiers(singleton(new DefaultIdentifier(value)));
            isEmpty = false;
        }
        if (!isEmpty) {
            identification.setCitation(citation);
        }
        final Extent extent = createExtent(sceneTime);
        if (extent != null) {
            identification.setExtents(singleton(extent));
            isEmpty = false;
        }
        value = getValue(ORIGIN);
        if (value != null) {
            identification.setCredits(singleton(value));
            isEmpty = false;
        }
        value = getValue(OUTPUT_FORMAT);
        if (value != null) {
            identification.setResourceFormats(singleton(new DefaultFormat(value, null)));
            isEmpty = false;
        }
        return isEmpty ? null : identification;
    }

    /**
     * Returns the metadata about the resources described in the Landsat file.
     *
     * @return the metadata about Landsat resources.
     * @throws DataStoreException if a property value can not be parsed as a number or a date.
     */
    public Metadata read() throws DataStoreException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandards(Citations.ISO_19115);
        final Date fileDate = getDate(FILE_DATE);
        DefaultCitationDate metadataTime = null;
        if (fileDate != null) {
            metadataTime = new DefaultCitationDate(fileDate, DateType.CREATION);
            metadata.setDateInfo(singleton(metadataTime));
        }
        metadata.setLanguages(singleton(Locale.ENGLISH));
        metadata.setMetadataIdentifier(new DefaultIdentifier(getValue(LANDSAT_SCENE_ID)));
        final Date sceneTime = getDate(DATE_ACQUIRED, SCENE_CENTER_TIME);
        final Identification identification = createIdentification(metadataTime, sceneTime);
        if (identification != null) {
            metadata.setIdentificationInfo(singleton(identification));
        }
        final ImageDescription content = createImageDescription();
        if (content != null) {
            metadata.setContentInfo(singleton(content));
        }
        final AcquisitionInformation acquisition = createAcquisitionInformation(sceneTime);
        if (acquisition != null) {
            metadata.setAcquisitionInformation(singleton(acquisition));
        }
        metadata.setMetadataScopes(singleton(new DefaultMetadataScope(ScopeCode.DATASET, null)));
        return metadata;
    }

    /**
     * Invoked when a non-fatal exception occurred while reading metadata. This method
     * sends a record to the registered listeners if any, or logs the record otherwise.
     */
    private void warning(final Exception e) {
        listeners.warning(null, e);
    }
}
