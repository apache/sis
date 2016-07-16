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
package org.apache.sis.storage.geotiff;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.content.AttributeGroup;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.Progress;

import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
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
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.util.logging.WarningListeners;

import static java.util.Collections.singleton;
import java.util.List;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultAggregateInformation;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import static org.apache.sis.storage.geotiff.LandsatKeys.*;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.util.InternationalString;

/**
 * Parses Landsat metadata as {@linkplain DefaultMetadata ISO-19115 Metadata}
 * object.
 *
 * @author Thi Phuong Hao Nguyen (VNSC)
 * @author Remi Marechal (Geomatys)
 * @since 0.8
 * @version 0.8
 * @module
 */
public class LandsatReader {

    /**
     * The description of all bands that can be included in a Landsat coverage.
     * This description is hard-coded and shared by all metadata instances.
     */
    private static final AttributeGroup BANDS;

    static {
        final double[] wavelengths = {433, 482, 562, 655, 865, 1610, 2200, 590, 1375, 10800, 12000};
        final String[] nameband = {
            "Coastal Aerosol", //   433 nm
            "Blue", //   482 nm
            "Green", //   562 nm
            "Red", //   655 nm
            "Near-Infrared", //   865 nm
            "Short Wavelength Infrared (SWIR) 1", //  1610 nm
            "Short Wavelength Infrared (SWIR) 2", //  2200 nm
            "Panchromatic", //   590 nm
            "Cirrus", //  1375 nm
            "Thermal Infrared Sensor (TIRS) 1", // 10800 nm
            "Thermal Infrared Sensor (TIRS) 2" // 12000 nm
        };
        final DefaultBand[] bands = new DefaultBand[wavelengths.length];
        final Unit<Length> nm = SI.MetricPrefix.NANO(SI.METRE);
        for (int i = 0; i < bands.length; i++) {
            final DefaultBand band = new DefaultBand();
            band.setDescription(new DefaultInternationalString(nameband[i]));
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
     * All properties found in the Landsat metadata file, except {@code GROUP}
     * and {@code END_GROUP}. Example:
     *
     * {
     *
     * @preformat text DATE_ACQUIRED = 2014-03-12 SCENE_CENTER_TIME =
     * 03:02:01.5339408Z CORNER_UL_LAT_PRODUCT = 12.61111 CORNER_UL_LON_PRODUCT
     * = 108.33624 CORNER_UR_LAT_PRODUCT = 12.62381 CORNER_UR_LON_PRODUCT =
     * 110.44017 }
     */
    private final Map<String, String> properties;

    /**
     * Where to sends the warnings.
     *
     * @todo Set a reference given by the data store.
     */
    private WarningListeners<?> listeners;

    /**
     * Creates a new metadata parser from the given characters reader.
     *
     * @param reader a reader opened on the Landsat file. It is caller's
     * responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given
     * stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    public LandsatReader(final BufferedReader reader) throws IOException, DataStoreException {
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
                     * In a Landsat file, String values are between quotes. Example: STATION_ID = "LGN"
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
     * Returns the property value associated to the given key, or {@code null}
     * if none.
     *
     * @param key the key for which to get the property value.
     * @return the property value associated to the given key, {@code null} if
     * none.
     */
    private String getValue(String key) {
        return properties.get(key);
    }

    /**
     * Returns the floating-point value associated to the given key, or
     * {@code NaN} if none.
     *
     * @param key the key for which to get the floating-point value.
     * @return the floating-point value associated to the given key, or
     * {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to the given key
     * can not be parsed as a floating-point number.
     */
    private double getNumericValue(String key) throws NumberFormatException {
        String value = getValue(key);
        return (value != null) ? Double.parseDouble(value) : Double.NaN;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys, or
     * {@code NaN} if none.
     *
     * @param key1 the key for which to get the first floating-point value.
     * @param key2 the key for which to get the second floating-point value.
     * @param max {@code true} for the maximal value, or {@code false} for the
     * minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max}
     * is true) floating-point value associated to the given keys, or
     * {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to one of the
     * given keys can not be parsed as a floating-point number.
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
     * @param key the key for which to get the date value.
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
     * Returns the date and time associated to the given key, or {@code null} if
     * none. The date and time are expected to be in two separated fields, with
     * each field formatted in ISO 8601 format.
     *
     * @param dateKey the key for which to get the date value.
     * @param timeKey the key for which to get the time value.
     * @return the date and time associated to the given keys, or {@code null}
     * if none.
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
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
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
     * Gets the geographic and temporal extent for identification info, or
     * {@code null} if none. This method expects the data acquisition time in
     * argument in order to avoid to compute it twice.
     *
     * @param sceneTime the data acquisition time, or {@code null} if none.
     * @return the data extent in Identification info, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    private Extent createExtent(final Date sceneTime) throws DataStoreException {
        final DefaultGeographicBoundingBox box;
        try {
            box = new DefaultGeographicBoundingBox(
                    getExtremumValue(CORNER_UL_LON_PRODUCT, CORNER_LL_LON_PRODUCT, false), // westBoundLongitude
                    getExtremumValue(CORNER_UR_LON_PRODUCT, CORNER_LR_LON_PRODUCT, true), // eastBoundLongitude
                    getExtremumValue(CORNER_LL_LAT_PRODUCT, CORNER_LR_LAT_PRODUCT, false), // southBoundLatitude
                    getExtremumValue(CORNER_UL_LAT_PRODUCT, CORNER_UR_LAT_PRODUCT, true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        final DefaultExtent extent = new DefaultExtent();
        final boolean isEmpty = box.isEmpty();
        if (!isEmpty) {
            extent.setGeographicElements(singleton(box));
        }
        if (sceneTime != null) {
            try {
                final DefaultTemporalExtent t = new DefaultTemporalExtent();
                t.setBounds(sceneTime, sceneTime);
                extent.setTemporalElements(singleton(t));
            } catch (UnsupportedOperationException e) {
                // May happen if the temporal module (which is optional) is not on the classpath.
                warning(e);
                if (isEmpty) {
                    return null;
                }
            }
        }
        return extent;
    }

    /**
     * Gets the acquisition information, or {@code null} if none. This method
     * expects the data acquisition time in argument in order to avoid to
     * compute it twice.
     *
     * @param sceneTime the data acquisition time, or {@code null} if none.
     * @return the data for the Acquisition Information, or {@code null} if
     * none.
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
     * Get basic Information about the distributor of and options for obtaining
     * the resource.
     *
     * @return the data distributor information, or {@code null} if none.
     */
    private Distribution createDistribution() {
        DefaultDistribution distribution = new DefaultDistribution();
        DefaultFormat format = new DefaultFormat();
        String value = getValue(OUTPUT_FORMAT);
        format.setName(new DefaultInternationalString(value));
        distribution.setDistributionFormats(singleton(format));
        return distribution;
    }

    /**
     * Gets basic information required to uniquely identify the data, or
     * {@code null} if none. This method expects the metadata and data
     * acquisition time in arguments in order to avoid to compute them twice.
     *
     * @param metadataTime the metadata file creation time, or {@code null} if
     * none.
     * @param sceneTime the data acquisition time, or {@code null} if none.
     * @return the data identification information, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    private Identification createIdentification(final Date metadataTime, final Date sceneTime) throws DataStoreException {
//        final DefaultDataIdentification identification = new DefaultDataIdentification();
        final DefaultCitation citation = new DefaultCitation();
        final AbstractIdentification identification = new AbstractIdentification();
        boolean isEmpty = true;
        if (metadataTime != null) {
            citation.setDates(singleton(new DefaultCitationDate(metadataTime, DateType.PUBLICATION)));
            isEmpty = false;
        }
        String value = getValue(METADATA_FILE_NAME);
        if (value != null) {
            citation.setTitle(new DefaultInternationalString(value));
            isEmpty = false;
        }

        if (!isEmpty) {
            identification.setCitation(citation);
        }
        value = getValue(ELEVATION_SOURCE);
        if (value != null) {
            final DefaultKeywords keyword = new DefaultKeywords(value);
            identification.setDescriptiveKeywords(singleton(keyword));
            isEmpty = false;
        }
        final Extent extent = createExtent(sceneTime);
        if (extent != null) {
            identification.setExtents(singleton(extent));
            isEmpty = false;
        }
        value = getValue(ORIGIN);
        if (value != null) {
            DefaultResponsibleParty responsible = new DefaultResponsibleParty();
            responsible.setOrganisationName(new DefaultInternationalString(value));
            responsible.setRole(Role.ORIGINATOR);
            DefaultResponsibleParty responsiblepublisher = new DefaultResponsibleParty();
            responsiblepublisher.setOrganisationName(new DefaultInternationalString(value));
            responsiblepublisher.setRole(Role.PUBLISHER);
            DefaultResponsibleParty responsiblecontributor = new DefaultResponsibleParty();
            responsiblecontributor.setOrganisationName(new DefaultInternationalString(value));
            responsiblecontributor.setRole(Role.AUTHOR);
            identification.getPointOfContacts().add(responsible);
            identification.getPointOfContacts().add(responsiblepublisher);
            identification.getPointOfContacts().add(responsiblecontributor);
            isEmpty = false;
        }
        value = getValue(ORIGIN);
        if (value != null) {
            DefaultCitation citation1 = new DefaultCitation();
            DefaultAggregateInformation aggregateInformation = new DefaultAggregateInformation();
            citation1.setTitle(new DefaultInternationalString(value));
            aggregateInformation.setAggregateDataSetName(citation1);
            isEmpty = false;
        }

        return isEmpty ? null : identification;
    }

    /**
     * Returns the metadata about the resources described in the Landsat file.
     *
     * @return the metadata about Landsat resources.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    public Metadata read() throws DataStoreException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandards(Citations.ISO_19115);
        final Date metadataTime = getDate(FILE_DATE);
        if (metadataTime != null) {
            metadata.setDateStamp(metadataTime);
        }
        metadata.setLanguage(Locale.ENGLISH);
        metadata.setFileIdentifier(getValue(LANDSAT_SCENE_ID));
        final Distribution metadataDistribution = createDistribution();
        metadata.setDistributionInfo(singleton(metadataDistribution));
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
        
        if(getValue(DATA_TYPE) != null){
        
        metadata.setHierarchyLevels(singleton(ScopeCode.valueOf(getValue(DATA_TYPE))));
        }
        return metadata;
    }

    /**
     * Invoked when a non-fatal exception occurred while reading metadata. This
     * method sends a record to the registered listeners if any, or logs the
     * record otherwise.
     */
    private void warning(final Exception e) {
        if (listeners != null) {
            listeners.warning(null, e);
        }
    }

    public static void main(String[] args) throws IOException, DataStoreException {
        LandsatReader read;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/haonguyen/data/LC81230522014071LGN00_MTL.txt"))) {
            read = new LandsatReader(in);
        }
        System.out.println("The Metadata of LC81230522014071LGN00_MTL.txt is:");
        System.out.println(read.read());

    }

}
