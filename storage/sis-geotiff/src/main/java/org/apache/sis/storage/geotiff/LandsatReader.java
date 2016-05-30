/*
 * Copyright 2016 haonguyen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.extent.AbstractGeographicExtent;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.spatial.DefaultDimension;
import org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.metadata.spatial.SpatialRepresentation;

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
     * @preformat text DATE_ACQUIRED = 2014-03-12 SCENE_CENTER_TIME =
     * 03:02:01.5339408Z CORNER_UL_LAT_PRODUCT = 12.61111 CORNER_UL_LON_PRODUCT
     * = 108.33624 CORNER_UR_LAT_PRODUCT = 12.62381 CORNER_UR_LON_PRODUCT =
     * 110.44017 }
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
     * Gets the data bounding box in degrees of longitude and latitude, or
     * {@code null} if none.
     *
     * @return the data domain in degrees of longitude and latitude, or
     * {@code null} if none.
     * @throws DataStoreException if a longitude or a latitude can not be read.
     */
    private Date getAcquisitionDate() throws ParseException {
        //-- year month day
        final String dateAcquired = getValue("DATE_ACQUIRED");
        if (dateAcquired == null) {
            return null;
        }
        //-- hh mm ss:ms
        final String sceneCenterTime = getValue("SCENE_CENTER_TIME");
        String strDate = dateAcquired;
        if (sceneCenterTime != null) {
            strDate = dateAcquired + "T" + sceneCenterTime;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssssss'Z'");
        final Date date = formatter.parse(strDate);
        return date;
    }

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

    /*
     * Gets the data bouding box uper - lower corner 
     */
    private GeographicBoundingBox getGeographicBoundingBox1() throws DataStoreException {
        final DefaultGeographicBoundingBox bbox;
        try {
            bbox = new DefaultGeographicBoundingBox(
                    getExtremumValue("CORNER_LL_PROJECTION_X_PRODUCT", "CORNER_UL_PROJECTION_X_PRODUCT", false), // westBoundLongitude
                    getExtremumValue("CORNER_UR_PROJECTION_X_PRODUCT", "CORNER_LR_PROJECTION_X_PRODUCT", true), // eastBoundLongitude
                    getExtremumValue("CORNER_LL_PROJECTION_Y_PRODUCT", "CORNER_LR_PROJECTION_Y_PRODUCT", false), // southBoundLatitude
                    getExtremumValue("CORNER_UR_PROJECTION_Y_PRODUCT", "CORNER_UL_PROJECTION_Y_PRODUCT", true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        return bbox.isEmpty() ? null : bbox;
    }

    /*
     *Gets GeographicExtent
     */
    private Extent getExtent() throws DataStoreException {

        final DefaultExtent ex = new DefaultExtent();
        final GeographicBoundingBox box = getGeographicBoundingBox();
        final GeographicBoundingBox box1 = getGeographicBoundingBox1();
        ex.getGeographicElements().add(box);
        ex.getGeographicElements().add(box1);
        return ex;
    }

    /*
     *Get metadata AcquisitionInformation
     */
    private AcquisitionInformation getAcquisitionInformation() {
        final DefaultAcquisitionInformation dAi = new DefaultAcquisitionInformation();
        final DefaultPlatform platF = new DefaultPlatform();
        final String space = getValue("SPACECRAFT_ID");
        if (space == null) {
            return null;
        }
        platF.setCitation(new DefaultCitation(space));
        final DefaultInstrument instru = new DefaultInstrument();
        final String instrum = getValue("SENSOR_ID");
        instru.setType(new DefaultInternationalString(instrum));
        final String nadir = getValue("NADIR_OFFNADIR");
        instru.setDescription(new DefaultInternationalString(nadir));
        platF.setInstruments(Collections.singleton(instru));
        dAi.setPlatforms(Collections.singleton(platF));
        return dAi;

    }

    /*
     *Get metadata file info
     */
    private Identifier getFileIdentifier() throws IOException, ParseException {
        final DefaultIdentifier iden = new DefaultIdentifier();
        final DefaultCitation citation = new DefaultCitation();
        final String namespace = getValue("ORIGIN");
        citation.setTitle(new DefaultInternationalString(namespace));
        final Date date = getDates();
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.CREATION)));
        final String identifier = getValue("REQUEST_ID");
        final String codespace = getValue("LANDSAT_SCENE_ID");
        final String version = getValue("PROCESSING_SOFTWARE_VERSION");
        iden.setCodeSpace(codespace);
        iden.setAuthority(citation);
        iden.setCode(identifier);
        iden.setVersion(version);
        return iden;
    }

    /*
      * Creates a {@code <gmd:spatialRepresentationInfo>} element from the given grid geometries.
     */
    private GridSpatialRepresentation createSpatialRepresentationInfo() throws IOException {
        final DefaultGridSpatialRepresentation grid = new DefaultGridSpatialRepresentation();
        final DefaultDimension dimen = new DefaultDimension();
        final int path = Integer.parseInt(getValue("WRS_PATH"));
        dimen.setDimensionName(DimensionNameType.COLUMN);
        dimen.setDimensionSize(path);
        final DefaultDimension dimen1 = new DefaultDimension();
        final int row = Integer.parseInt(getValue("WRS_ROW"));
        dimen1.setDimensionName(DimensionNameType.ROW);
        dimen1.setDimensionSize(row);
        final DefaultDimension dimen3 = new DefaultDimension();
        final int panchromaticline = Integer.parseInt(getValue("PANCHROMATIC_LINES"));
        dimen3.setDimensionDescription(new DefaultInternationalString("Panchromatic lines"));
        dimen3.setDimensionName(DimensionNameType.LINE);
        dimen3.setDimensionSize(panchromaticline);
        final DefaultDimension dimen4 = new DefaultDimension();
        final int panchromaticsample = Integer.parseInt(getValue("PANCHROMATIC_SAMPLES"));
        dimen4.setDimensionDescription(new DefaultInternationalString("Panchromatic samples"));
        dimen4.setDimensionName(DimensionNameType.SAMPLE);
        dimen4.setDimensionSize(panchromaticsample);

        final DefaultDimension dimen5 = new DefaultDimension();
        final int reflecline = Integer.parseInt(getValue("REFLECTIVE_LINES"));
        dimen5.setDimensionDescription(new DefaultInternationalString("Reflective lines"));
        dimen5.setDimensionName(DimensionNameType.LINE);
        dimen5.setDimensionSize(reflecline);
        final DefaultDimension dimen6 = new DefaultDimension();
        final int reflecsample = Integer.parseInt(getValue("REFLECTIVE_SAMPLES"));
        dimen6.setDimensionDescription(new DefaultInternationalString("Reflective samples"));
        dimen6.setDimensionName(DimensionNameType.SAMPLE);
        dimen6.setDimensionSize(reflecsample);

        final DefaultDimension dimen7 = new DefaultDimension();
        final int thermalline = Integer.parseInt(getValue("THERMAL_LINES"));
        dimen7.setDimensionDescription(new DefaultInternationalString("Thermal lines"));
        dimen7.setDimensionName(DimensionNameType.LINE);
        dimen7.setDimensionSize(thermalline);
        final DefaultDimension dimen8 = new DefaultDimension();
        final int thermalsample = Integer.parseInt(getValue("THERMAL_SAMPLES"));
        dimen8.setDimensionDescription(new DefaultInternationalString("Thermal samples"));
        dimen8.setDimensionName(DimensionNameType.SAMPLE);
        dimen8.setDimensionSize(thermalline);

        grid.getAxisDimensionProperties().add(dimen);
        grid.getAxisDimensionProperties().add(dimen1);
        grid.getAxisDimensionProperties().add(dimen3);
        grid.getAxisDimensionProperties().add(dimen4);
        grid.getAxisDimensionProperties().add(dimen5);
        grid.getAxisDimensionProperties().add(dimen6);

        return grid;
    }

    /*
     *Get Metadata Content Infor
     */
    Identification getIdentification() throws ParseException, DataStoreException {
        final AbstractIdentification abtract = new AbstractIdentification();
        final DefaultCitation citation = new DefaultCitation();
        final String datatype = getValue("DATA_TYPE");
        citation.setTitle(new DefaultInternationalString(datatype));
        final Date date = getAcquisitionDate();
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
        final String part = getValue("ELEVATION_SOURCE");
        citation.setEdition(new DefaultInternationalString(part));
//     abtract.setPointOfContacts(newValues);
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
        return abtract;
    }

    public Metadata read() throws IOException, ParseException, DataStoreException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandards(Citations.ISO_19115);
        final Identifier identifier = getFileIdentifier();
        metadata.setMetadataIdentifier(identifier);
        final Identification identification = getIdentification();
        metadata.setIdentificationInfo(Collections.singleton(identification));
        final AcquisitionInformation Ai = getAcquisitionInformation();
        metadata.setAcquisitionInformation(Collections.singleton(Ai));
        final GridSpatialRepresentation grid = createSpatialRepresentationInfo();
        metadata.setSpatialRepresentationInfo(Collections.singleton(grid));
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
