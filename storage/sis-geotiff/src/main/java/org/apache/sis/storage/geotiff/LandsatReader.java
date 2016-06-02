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
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.DefaultExtendedElementInformation;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultMetadataExtensionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultObjective;
import org.apache.sis.metadata.iso.acquisition.DefaultOperation;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultRequirement;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultSeries;
import org.apache.sis.metadata.iso.distribution.DefaultDataFile;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.quality.AbstractElement;
import org.apache.sis.metadata.iso.quality.AbstractResult;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.metadata.iso.quality.DefaultCoverageResult;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;

import org.apache.sis.metadata.iso.spatial.DefaultDimension;
import org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.acquisition.ObjectiveType;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;

/**
 *
 * @author haonguyen
 */
public class LandsatReader {

    /**
     * All properties found in the Landsat metadata file, except {@code GROUP} and {@code END_GROUP}.
     * Example:
     *
     * {@preformat text
     *   DATE_ACQUIRED = 2014-03-12
     *   SCENE_CENTER_TIME = 03:02:01.5339408Z
     *   CORNER_UL_LAT_PRODUCT = 12.61111
     *   CORNER_UL_LON_PRODUCT = 108.33624
     *   CORNER_UR_LAT_PRODUCT = 12.62381
     *   CORNER_UR_LON_PRODUCT = 110.44017
     * }
     *//**
     * Stores all properties found in the Landsat file read from the the given reader,
     * except {@code GROUP} and {@code END_GROUP}.
     *
     * @param  reader a reader opened on the Landsat file. It is caller's responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    private final Map<String, String> properties;

   /**
     * Stores all properties found in the Landsat file read from the the given reader,
     * except {@code GROUP} and {@code END_GROUP}.
     *
     * @param  reader a reader opened on the Landsat file. It is caller's responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given stream.
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
     * Gets the date the image was acquired.
     * @return the  date the image was acquired.
     * @throws ParseException returns the position where the error was found. if the error was found..
     */
    private Date getAcquisitionDate() throws ParseException {
        //-- year month day
        final String dateAcquired = getValue("DATE_ACQUIRED");
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
     /**
     * Gets the date when the metadata file for the L1G product set was created.
     * @return the  date the image was acquired.
     * @throws ParseException returns the position where the error was found. if the error was found..
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
     * Gets the extent for Identification infor 
     * {@code null} if none.
     *
     * @return the data extent in Indentification infor 
     * {@code null} if none.
     * @throws DataStoreException if data can not be read.
     */
    private Extent getExtent() throws DataStoreException {

        final DefaultExtent ex = new DefaultExtent();
        final GeographicBoundingBox box = getGeographicBoundingBox();
        ex.getGeographicElements().add(box);

        return ex;
    }
/**
     * Gets the Information for the Acquisition Information 
     * {@code null} if none.
     *
     * @return the data for the Acquisition Information 
     * {@code null} if none.
     */
    private AcquisitionInformation getAcquisitionInformation() throws ParseException {
        final DefaultAcquisitionInformation dAi = new DefaultAcquisitionInformation();
        final DefaultCitation citation = new DefaultCitation();
        final DefaultRequirement requirement = new DefaultRequirement();
        final Date date = getAcquisitionDate();
        final String title = getValue("DATA_TYPE");
        citation.setTitle(new DefaultInternationalString(title));
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
        requirement.setCitation(citation);
        dAi.setAcquisitionRequirements(Collections.singleton(requirement));
        final DefaultPlatform platF = new DefaultPlatform();
        /*
        The characteristics, spatial and temporal extent of the intended object to be observed.
        */
        final DefaultObjective object1 = new DefaultObjective();
        final DefaultObjective object2 = new DefaultObjective();
        final String orientatin = getValue("ORIENTATION");
        final String resampling = getValue("RESAMPLING_OPTION");
        object1.setTypes(Collections.singleton(ObjectiveType.valueOf("Orientation")));
        object1.setFunctions(Collections.singleton(new DefaultInternationalString(orientatin)));
        object2.setTypes(Collections.singleton(ObjectiveType.valueOf("Resampling")));
        object2.setFunctions(Collections.singleton(new DefaultInternationalString(resampling)));
        /*
        *The Platform, Instrument and the type of instrument used to observe the object 
        */
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
        dAi.getObjectives().add(object1);
        dAi.getObjectives().add(object2);
        return dAi;

    }

    /**
     * Gets the information for the File Identifier
     * {@code null} if none.
     *
     * @return the data for the File Identifier
     * {@code null} if none.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     * @throws ParseException Signals that an error has been reached unexpectedly
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

    /**
     * Gets the information for the SpatialRepresentationInfo
     * {@code null} if none.
     *
     * @return the data for the SpatialRepresentationInfo
     * {@code null} if none.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
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
        final DefaultDimension dimen9 = new DefaultDimension();
        final double panchromaticgrid = Double.valueOf(getValue("GRID_CELL_SIZE_PANCHROMATIC"));
        dimen9.setDimensionDescription(new DefaultInternationalString("Panchromatic grid cell size"));
        dimen9.setResolution(panchromaticgrid);

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
        final DefaultDimension dimen10 = new DefaultDimension();
        final double reflecgrid = Double.valueOf(getValue("GRID_CELL_SIZE_REFLECTIVE"));
        dimen10.setDimensionDescription(new DefaultInternationalString("Reflective grid cell size"));
        dimen10.setResolution(reflecgrid);

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
        final DefaultDimension dimen11 = new DefaultDimension();
        final double thermalgrid = Double.valueOf(getValue("GRID_CELL_SIZE_THERMAL"));
        dimen11.setDimensionDescription(new DefaultInternationalString("Thermal grid cell size"));
        dimen11.setResolution(thermalgrid);

        grid.getAxisDimensionProperties().add(dimen);
        grid.getAxisDimensionProperties().add(dimen1);
        grid.getAxisDimensionProperties().add(dimen3);
        grid.getAxisDimensionProperties().add(dimen4);
        grid.getAxisDimensionProperties().add(dimen5);
        grid.getAxisDimensionProperties().add(dimen6);
        grid.getAxisDimensionProperties().add(dimen7);
        grid.getAxisDimensionProperties().add(dimen8);
        grid.getAxisDimensionProperties().add(dimen9);
        grid.getAxisDimensionProperties().add(dimen10);
        grid.getAxisDimensionProperties().add(dimen11);

        return grid;
    }
/**
     * Gets Basic information required to uniquely identify a resource or resources.
     * {@code null} if none.
     *
     * @return the data for the File Identifier
     * {@code null} if none.
     * @throws DataStoreException Thrown when a {@link DataStore} can not complete a read or write operation.
     * @throws ParseException Signals that an error has been reached unexpectedly
     */
    Identification getIdentification() throws ParseException, DataStoreException {
        final AbstractIdentification abtract = new AbstractIdentification();
        final DefaultCitation citation = new DefaultCitation();
        final String datatype = getValue("ORIGIN");
        citation.setTitle(new DefaultInternationalString(datatype));
        final Date date = getDates();
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
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
    /**
     * Gets the description of the spatial and temporal reference systems used in the dataset.
     * {@code null} if none.
     *
     * @return the description of the spatial and temporal reference systems used in the dataset.
     * {@code null} if none.
     * @throws FactoryException Thrown when a {@linkplain Factory factory} can't create an instance
     * of the requested object.
     */

    private CoordinateReferenceSystem getReferenceSystem() throws FactoryException {
        final CoordinateReferenceSystem coordinate;
        final String orientation = getValue("ORIENTATION");
        final String resampling = getValue("RESAMPLING_OPTION");

        //-- Datum
        final String datum = getValue("DATUM");
        //-- Ellipsoid
        final String ellips = getValue("ELLIPSOID");
        if (!(("WGS84".equalsIgnoreCase(datum)) && ("WGS84".equalsIgnoreCase(ellips)))) {
            throw new IllegalStateException("Comportement not supported : expected Datum and Ellipsoid value WGS84, found Datum = " + datum + ", Ellipsoid : " + ellips);
        }
        final String projType = getValue("MAP_PROJECTION");

        switch (projType) {
            case "UTM": {
                /**
                 * From Landsat specification, normaly Datum and ellipsoid are
                 * always WGS84. UTM area is the only thing which change.
                 * Thereby we build a CRS from basic 32600 and we add read UTM
                 * area. For example if UTM area is 45 we decode 32645 CRS from
                 * EPSG database.
                 */
                final String utm_Zone = getValue("UTM_ZONE");
                final Integer utm = Integer.valueOf(utm_Zone);
                ArgumentChecks.ensureBetween(datum, 0, 60, utm);
                final NumberFormat nf = new DecimalFormat("##");
                final String utmFormat = nf.format(utm);
                coordinate = CRS.forCode("EPSG:326" + utmFormat);

                break;
            }
            case "PS": {
                final String originLongitude = getValue("VERTICAL_LON_FROM_POLE");
                final String trueLatitudeScale = getValue("TRUE_SCALE_LAT");
                final String falseEasting = getValue("FALSE_EASTING");
                final String falseNorthing = getValue("FALSE_NORTHING");
                final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
                        .getOperationMethod("Polar Stereographic (variant B)");
                final ParameterValueGroup psParameters = method.getParameters().createValue();
                psParameters.parameter(Constants.STANDARD_PARALLEL_1).setValue(Double.valueOf(trueLatitudeScale));
                psParameters.parameter(Constants.CENTRAL_MERIDIAN).setValue(Double.valueOf(originLongitude));
                psParameters.parameter(Constants.FALSE_EASTING).setValue(Double.valueOf(falseEasting));
                psParameters.parameter(Constants.FALSE_NORTHING).setValue(Double.valueOf(falseNorthing));

                final Map<String, String> properties = Collections.singletonMap("name", "Landsat 8 polar stereographic");
                //-- define mathematical formula to pass from Geographic Base CRS to projected Coordinate space.
                final DefaultConversion projection = new DefaultConversion(properties, method, null, psParameters);
                coordinate = new DefaultProjectedCRS(properties, CommonCRS.WGS84.normalizedGeographic(), projection, null);

                break;
            }
            default:
                throw new IllegalStateException("Comportement not supported : expected MAP_PROJECTION values are : PS or UTM, found : " + projType);
        }
        return coordinate;
    }
     /**
     * Gets the data quality 
     * {@code null} if none.
     * @return the data quality 
     * {@code null} if none.
     */
    private DataQuality getQuality(){
        final DefaultDataQuality quali = new DefaultDataQuality(); 
        final DefaultLineage lineage = new DefaultLineage();
        final String level = getValue("DATA_TYPE");
        final DefaultCitation citation = new DefaultCitation();
        citation.setTitle(new DefaultInternationalString(level) );
        final String version = getValue("GROUND_CONTROL_POINTS_VERSION");
        citation.setEdition(new DefaultInternationalString(version));
        final DefaultSeries seri = new DefaultSeries();
        final String gcpmodel = getValue("GROUND_CONTROL_POINTS_MODEL");
        seri.setIssueIdentification(new DefaultInternationalString("Number of GCPs used in the precision" +
"correction process is :"+gcpmodel));
        citation.setSeries(seri);
        lineage.setAdditionalDocumentation(Collections.singleton(citation));
       
        final AbstractElement abs = new AbstractElement();
        final String cloudcover = getValue("CLOUD_COVER");
        if(cloudcover !=null){
        abs.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Cloud cover ")));
        abs.setMeasureDescription(new DefaultInternationalString(cloudcover));
        quali.getReports().add(abs);
        }
        final AbstractElement abs2 = new AbstractElement();
        final String cloudcoland = getValue("CLOUD_COVER_LAND");
        if(cloudcoland !=null){
        abs2.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Cloud cover land")));
        abs2.setMeasureDescription(new DefaultInternationalString(cloudcoland));
        quali.getReports().add(abs2);
        }
        final AbstractElement abs3 = new AbstractElement();
        final String qualioli = getValue("IMAGE_QUALITY_OLI");
        if(qualioli !=null){
        abs3.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Image quality OLI")));
        abs3.setMeasureDescription(new DefaultInternationalString(qualioli));
        quali.getReports().add(abs3);
        }
        final AbstractElement abs4 = new AbstractElement();
        final String qualitirs = getValue("IMAGE_QUALITY_TIRS");
        if(qualitirs !=null){
        abs4.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Image quality TIRS")));
        abs4.setMeasureDescription(new DefaultInternationalString(qualitirs));
        quali.getReports().add(abs4);
        }
        final AbstractElement abs5 = new AbstractElement();
        final String tirsssm = getValue("TIRS_SSM_POSITION_STATUS");
        if(tirsssm !=null){
        abs5.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("TIRS SSM position status")));
        abs5.setMeasureDescription(new DefaultInternationalString(tirsssm));
        quali.getReports().add(abs5);
        }
        final AbstractElement abs6 = new AbstractElement();
        final String rollangle = getValue("ROLL_ANGLE");
        if(rollangle !=null){
        abs6.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Roll angle")));
        abs6.setMeasureDescription(new DefaultInternationalString(rollangle));
        quali.getReports().add(abs6);
        }
        final AbstractElement abs7 = new AbstractElement();
        final String sunazimuth = getValue("SUN_AZIMUTH");
        if(sunazimuth !=null){
        abs7.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Sun azimuth")));
        abs7.setMeasureDescription(new DefaultInternationalString(sunazimuth));
        quali.getReports().add(abs7);
        }
        final AbstractElement abs8 = new AbstractElement();
        final String sunelevation = getValue("SUN_ELEVATION");
        if(sunelevation !=null){
        abs8.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Sun elevation")));
        abs8.setMeasureDescription(new DefaultInternationalString(sunelevation));
        quali.getReports().add(abs8);
        }
        final AbstractElement abs9 = new AbstractElement();
        final String earthsun = getValue("EARTH_SUN_DISTANCE");
        if(earthsun !=null){
        abs9.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Earth sun distance")));
        abs9.setMeasureDescription(new DefaultInternationalString(earthsun));
        quali.getReports().add(abs9);
        }
        final AbstractElement abs10 = new AbstractElement();
        final String gcpverify = getValue("GROUND_CONTROL_POINTS_VERIFY");
        if(gcpverify !=null){
        abs10.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Ground control points verify")));
        abs10.setMeasureDescription(new DefaultInternationalString(gcpverify));
        quali.getReports().add(abs10);
        }
        final AbstractElement abs11 = new AbstractElement();
        final String rmsverify= getValue("GEOMETRIC_RMSE_VERIFY");
        if(rmsverify !=null){
        abs11.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Geometric RMSE verify")));
        abs11.setMeasureDescription(new DefaultInternationalString(rmsverify));
        quali.getReports().add(abs11);
        }
        final AbstractElement abs12 = new AbstractElement();
        final String rmsemodel = getValue("GEOMETRIC_RMSE_MODEL");
        if(rmsemodel !=null){
        abs12.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Geometric RMSE model")));
        abs12.setMeasureDescription(new DefaultInternationalString(rmsemodel));
        quali.getReports().add(abs12);
        }
        final AbstractElement abs13 = new AbstractElement();
        final String rmsemodely = getValue("GEOMETRIC_RMSE_MODEL_Y");
        if(rmsemodely !=null){
        abs13.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Geometric RMSE Model Y")));
        abs13.setMeasureDescription(new DefaultInternationalString(rmsemodely));
        quali.getReports().add(abs13);
        }
        final AbstractElement abs14 = new AbstractElement();
        final String rmsemodelx = getValue("GEOMETRIC_RMSE_MODEL_X");
        if(rmsemodelx !=null){
        abs14.setNamesOfMeasure(Collections.singleton(new DefaultInternationalString("Geometric RMSE Model X")));
        abs14.setMeasureDescription(new DefaultInternationalString(rmsemodelx));
        quali.getReports().add(abs14);
        }
        
        quali.setLineage(lineage);
        return quali;   
    }
     /**
     * Read which defines metadata about a resource or resources.
     * {@code null} if none.
     * @return the data which defines metadata about a resource or resources.
     * {@code null} if none.
     * @throws FactoryException Thrown when a {@linkplain Factory factory} can't create an instance
     * of the requested object.
     * @throws DataStoreException Thrown when a {@link DataStore} can not complete a read or write operation.
     * @throws ParseException Signals that an error has been reached unexpectedly
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    
    public Metadata read() throws IOException, ParseException, DataStoreException, FactoryException {
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
        final CoordinateReferenceSystem a = getReferenceSystem();
        metadata.setReferenceSystemInfo(Collections.singleton(a));
        final DataQuality quali = getQuality();
        metadata.setDataQualityInfo(Collections.singleton(quali));
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
