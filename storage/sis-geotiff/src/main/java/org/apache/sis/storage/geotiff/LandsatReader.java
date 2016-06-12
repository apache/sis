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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Collections.singleton;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.measure.unit.Unit;
import javax.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultRequirement;
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
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.content.AttributeGroup;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.opengis.util.FactoryException;

/**
 *
 * @author haonguyen
 */
public class LandsatReader {

    /**
     * All properties found in the Landsat metadata file, except {@code GROUP}
     * and {@code END_GROUP}. Example:
     *
     * {
     *
     * @preformat text
     * DATE_ACQUIRED = 2014-03-12
     * SCENE_CENTER_TIME =03:02:01.5339408Z
     * CORNER_UL_LAT_PRODUCT = 12.61111
     * CORNER_UL_LON_PRODUCT= 108.33624
     * CORNER_UR_LAT_PRODUCT = 12.62381
     * CORNER_UR_LON_PRODUCT =110.44017 }
     */
    /**
     * Stores all properties found in the Landsat file read from the the given
     * reader, except {@code GROUP} and {@code END_GROUP}.
     *
     * @param reader a reader opened on the Landsat file. It is caller's
     * responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given
     * stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    private final Map<String, String> properties;

    /**
     * Stores all properties found in the Landsat file read from the the given
     * reader, except {@code GROUP} and {@code END_GROUP}.
     *
     * @param reader a reader opened on the Landsat file. It is caller's
     * responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given
     * stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    LandsatReader(final BufferedReader reader) throws IOException, DataStoreException {
        properties = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
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
     * @return the floating-point value associated to the given key,
     * {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to the given key
     * can not be parsed as a floating-point number.
     */
    private double getNumericValue(String key) throws NumberFormatException {
        String value = getValue(key);
        return (value != null) ? Double.parseDouble(value) : Double.NaN;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys.
     *
     * @param key1 the key for which to get the first floating-point value.
     * @param key2 the key for which to get the second floating-point value.
     * @param max {@code true} for the maximal value, or {@code false} for the
     * minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max}
     * is true) floating-point value associated to the given keys.
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
     * Creat the attributeGroup the band
     *
     * @return Information about the band identification
     */
    private AttributeGroup coverage() {
        final DefaultAttributeGroup attribute = new DefaultAttributeGroup();
        double[] wavelengths = {433.0, 482.0, 562.0,655.0,865.0,1610.0,2200.0,590.0,1375.0,10800.0,12000.0};
        String[] nameband = {"Coastal Aerosol (Operational Land Imager (OLI))","Blue (OLI)",
                             "Green (OLI)","Red (OLI)","Near-Infrared (NIR) (OLI)",
                             "Short Wavelength Infrared (SWIR) 1 (OLI)","SWIR 2 (OLI)",
                             "Panchromatic (OLI)","Cirrus (OLI)","Thermal Infrared Sensor (TIRS) 1","TIRS 2"};
            for (int i=0; i<wavelengths.length; i++) {
                final DefaultBand band = new DefaultBand();
                band.setPeakResponse(wavelengths[i]);
                band.setDescription(new DefaultInternationalString(nameband[i]));
                band.setUnits(Unit.valueOf("nm"));
                attribute.getAttributes().add(band);
            }
        return attribute;
    }

    /**
     * Gets Information about an image's suitability for use.
     *
     * @return the Information about an image's suitability for use.
     */
    private ImageDescription createContentInfo() {
        final DefaultImageDescription content = new DefaultImageDescription();
        final double cloudcover = Double.valueOf(getValue("CLOUD_COVER"));
        content.setCloudCoverPercentage(cloudcover);
        final double azimuth = Double.valueOf(getValue("SUN_AZIMUTH"));
        content.setIlluminationAzimuthAngle(azimuth);
        final double elevation = Double.valueOf(getValue("SUN_ELEVATION"));
        content.setIlluminationElevationAngle(elevation);
        content.setAttributeGroups(Collections.singleton(coverage()));
        return content;
    }

   /**
     * Returns the data acquisition {@link Date}.<br>
     *
     * May returns {@code null} if no date are stipulate from metadata file.
     *
     * @return the data acquisition {@link Date}.
     * @throws ParseException if problem during Date parsing.
     */
    private Date getAcquisitionDate() throws ParseException {
        final String dateAcquired  = getValue("DATE_ACQUIRED");
        if (dateAcquired == null)
            return null;
        //-- hh mm ss:ms
        final String sceneCenterTime = getValue("SCENE_CENTER_TIME");
        String strDate = dateAcquired;
        if (sceneCenterTime != null)
            strDate = dateAcquired+"T"+sceneCenterTime;
       SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssssss'Z'");
       final Date date = formatter.parse(strDate);
       return date;
    }

    /**
     * Gets the date when the metadata file for the L1G product set was created.
     *
     * @return the date the image was acquired.
     * @throws ParseException returns the position where the error was found. if
     * the error was found..
     */
    private Date getDates() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        final String dateInString = getValue("FILE_DATE");
        final Date date = formatter.parse(dateInString);
        if (dateInString == null) {
            return null;
        }
        return date;
    }

    /**
     * Gets the data bounding box in degrees of longitude and latitude, or
     * {@code null} if none.
     *
     * @return the data domain in degrees of longitude and latitude, or
     * {@code null} if none.
     * @throws DataStoreException if a longitude or a latitude can not be read.
     */
    private GeographicBoundingBox getGeographicBoundingBox() throws DataStoreException {
        final DefaultGeographicBoundingBox bbox;
        try {
            bbox = new DefaultGeographicBoundingBox(
                    getExtremumValue("CORNER_UL_LON_PRODUCT", "CORNER_LL_LON_PRODUCT", false), // westBoundLongitude
                    getExtremumValue("CORNER_UR_LON_PRODUCT", "CORNER_LR_LON_PRODUCT", true), // eastBoundLongitude
                    getExtremumValue("CORNER_LL_LAT_PRODUCT", "CORNER_LR_LAT_PRODUCT", false), // southBoundLatitude
                    getExtremumValue("CORNER_UL_LAT_PRODUCT", "CORNER_UR_LAT_PRODUCT", true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        return bbox.isEmpty() ? null : bbox;
    }

    /**
     * Gets the extent for Identification infor {@code null} if none.
     *
     * @return the data extent in Indentification infor {@code null} if none.
     * @throws DataStoreException if data can not be read.
     */

    private Extent getExtent() throws DataStoreException, ParseException, UnsupportedOperationException {
        DefaultExtent ex = new DefaultExtent();
        final GeographicBoundingBox box = getGeographicBoundingBox();
        ex.getGeographicElements().add(box);
        final DefaultTemporalExtent tex = new DefaultTemporalExtent();
        final Date startTime  = getAcquisitionDate();
        if(startTime !=null ){
            final DefaultTemporalExtent t = new DefaultTemporalExtent();
            t.setBounds(startTime, startTime);
            ex.setTemporalElements(singleton(t));
       }

        return ex;
    }

    /**
     * Gets the Information for the Acquisition Information {@code null} if
     * none.
     *
     * @return the data for the Acquisition Information {@code null} if none.
     */
    private AcquisitionInformation getAcquisitionInformation() throws ParseException {
        final DefaultAcquisitionInformation dAi = new DefaultAcquisitionInformation();
        final DefaultCitation citation = new DefaultCitation();
        final DefaultRequirement requirement = new DefaultRequirement();
        final Date date = getAcquisitionDate();
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
        requirement.setCitation(citation);
        dAi.setAcquisitionRequirements(Collections.singleton(requirement));
        final DefaultPlatform platF = new DefaultPlatform();
        final String space = getValue("SPACECRAFT_ID");
        if (space != null) {
            platF.setCitation(new DefaultCitation(space));
        }
        final DefaultInstrument instru = new DefaultInstrument();
        final String instrum = getValue("SENSOR_ID");
        instru.setType(new DefaultInternationalString(instrum));;
        platF.setInstruments(Collections.singleton(instru));
        dAi.setPlatforms(Collections.singleton(platF));
        return dAi;

    }

    /**
     * Gets Basic information required to uniquely identify a resource or
     * resources. {@code null} if none.
     *
     * @return the data for the File Identifier {@code null} if none.
     * @throws DataStoreException Thrown when a {@link DataStore} can not
     * complete a read or write operation.
     * @throws ParseException Signals that an error has been reached
     * unexpectedly
     */
    Identification getIdentification() throws ParseException, DataStoreException {
        final AbstractIdentification abtract = new AbstractIdentification();
        final DefaultCitation citation = new DefaultCitation();
        final Date date = getDates();
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
        final String identifier = getValue("LANDSAT_SCENE_ID");
        citation.setIdentifiers(Collections.singleton(new DefaultIdentifier(identifier)));
        final DefaultFormat format = new DefaultFormat();
        final String name = getValue("OUTPUT_FORMAT");
        final String version = getValue("DATA_TYPE");
        final String elevation = getValue("ELEVATION_SOURCE");
        format.setName(new DefaultInternationalString(name));
        format.setVersion(new DefaultInternationalString(version));
        format.setAmendmentNumber(new DefaultInternationalString(elevation));
        abtract.setResourceFormats(Collections.singleton(format));
        abtract.setCitation(citation);
        final Extent ex = getExtent();
        abtract.setExtents(Arrays.asList(ex));
        final String credit = getValue("ORIGIN");
        abtract.setCredits(Collections.singleton(new DefaultInternationalString(credit)));

        return abtract;
    }

    /**
     * Read which defines metadata about a resource or resources. {@code null}
     * if none.
     *
     * @return the data which defines metadata about a resource or resources.
     * {@code null} if none.
     * @throws FactoryException Thrown when a {@linkplain Factory factory} can't
     * create an instance of the requested object.
     * @throws DataStoreException Thrown when a {@link DataStore} can not
     * complete a read or write operation.
     * @throws ParseException Signals that an error has been reached
     * unexpectedly
     * @throws IOException Signals that an I/O exception of some sort has
     * occurred.
     */
    public Metadata read() throws IOException, ParseException, DataStoreException, FactoryException, JAXBException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandards(Citations.ISO_19115);
        metadata.setDateInfo(Collections.singleton(new DefaultCitationDate(getDates(), DateType.CREATION)));
        final Identification identification = getIdentification();
        metadata.setIdentificationInfo(Collections.singleton(identification));
        final ImageDescription creat = createContentInfo();
        metadata.getContentInfo().add(creat);
        final AcquisitionInformation Ai = getAcquisitionInformation();
        metadata.setAcquisitionInformation(Collections.singleton(Ai));
        return metadata;

    }

    public static void main(String[] args) throws Exception {
        LandsatReader reader;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/haonguyen/data/LC81230522014071LGN00_MTL.txt"))) {
            reader = new LandsatReader(in);
        }
        System.out.println("The Metadata of LC81230522014071LGN00_MTL.txt is:");
        System.out.println(reader.read());

    }
}
