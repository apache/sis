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

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultSeries;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.lineage.DefaultProcessStep;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Parses Landsat metadata as {@linkplain DefaultMetadata ISO-19115 Metadata} object.
 *
 * @author  Remi Marechal (Geomatys)
 * @author  Thi Phuong Hao NGUYEN
 * @author  Minh Chinh VU
 * @since   0.8
 * @version 0.8
 * @module
 */
class LandsatMetadataReader {
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
     */
    private final Map<String,String> properties;

     private CoordinateReferenceSystem projectedCRS2D;
     private CoordinateReferenceSystem projectedCRS;
    /**
     * Stores all properties found in the Landsat file read from the the given reader,
     * except {@code GROUP} and {@code END_GROUP}.
     *
     * @param  reader a reader opened on the Landsat file. It is caller's responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given stream.
     * @throws DataStoreException if the content is not a Landsat file.
     */
    LandsatMetadataReader(final BufferedReader reader) throws IOException, DataStoreException {
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
     * @param  key  the key for which to get the floating-point value.
     * @return the floating-point value associated to the given key, {@link Double#NaN} if none.
     * @throws NumberFormatException if the property associated to the given key can not be parsed
     *         as a floating-point number.
     */
    private double getNumericValue(String key) throws NumberFormatException {
        String value = getValue(key);
        return (value != null) ? Double.parseDouble(value) : Double.NaN;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys.
     *
     * @param  key1  the key for which to get the first floating-point value.
     * @param  key2  the key for which to get the second floating-point value.
     * @param  max   {@code true} for the maximal value, or {@code false} for the minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max} is true) floating-point value
     *         associated to the given keys.
     * @throws NumberFormatException if the property associated to one of the given keys can not be parsed
     *         as a floating-point number.
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
    private String getExtremumValue1(String key1, String key2, boolean max) throws NumberFormatException {
        double value1 = getNumericValue(key1);
        double value2 = getNumericValue(key2);
        if (max ? (value2 > value1) : (value2 < value1)) {
            return String.valueOf(value2);
        } else {
            return String.valueOf(value1);
        }
    }
    /**
     * Gets the data bounding box in degrees of longitude and latitude, or {@code null} if none.
     *
     * @return the data domain in degrees of longitude and latitude, or {@code null} if none.
     * @throws DataStoreException if a longitude or a latitude can not be read.
     */
   private Metadata getInfos() throws ParseException, DataStoreException, FactoryException, TransformException{
       final DefaultMetadata filledMetadata = new DefaultMetadata();
      //contact
       final DefaultResponsibleParty cities = new DefaultResponsibleParty();
       final String part = getValue("STATION_ID");
       if(part == null ){
           return null;
       }
       cities.setOrganisationName(new DefaultInternationalString(part));
       cities.setRole(Role.POINT_OF_CONTACT);
       filledMetadata.setContacts(Arrays.asList(cities));
       //Identification info
       final AbstractIdentification inden = new AbstractIdentification();
            //citation
       final DefaultCitation citation = new DefaultCitation();
       final String identifier = getValue("LANDSAT_SCENE_ID");
       if (identifier == null) {
           return null;
       }
       citation.setTitle(new DefaultInternationalString(identifier));
       final String id = getValue("REQUEST_ID");
       if (id == null){
           return null;
       }
       citation.setSeries(new DefaultSeries(id));
       final Date date =  getDates();
       if (date != null)
       citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
       final String identifiers = getValue("DATA_TYPE");
       if (identifier == null){
           return null;
       }
       citation.setIdentifiers(Collections.singleton(new DefaultIdentifier(identifiers)));
       final String party = getValue("ORIGIN");
       if(party == null){
           return null;
       }
       cities.setOrganisationName(new DefaultInternationalString(party));
       cities.setRole(Role.ORIGINATOR);
       citation.setCitedResponsibleParties(Arrays.asList(cities));
       final String version = getValue("PROCESSING_SOFTWARE_VERSION");
       if(version== null){
           return null;
       }
       citation.setEdition(new DefaultInternationalString(version));
       inden.setCitation(new DefaultCitation(citation));
            //Abstrac
       inden.setAbstract(new DefaultInternationalString(part));

            //PointOfContacts
       final String point = getValue("ORIGIN");
       if( point == null){
           return null;
       }
       cities.setOrganisationName(new DefaultInternationalString(point));
       cities.setRole(Role.POINT_OF_CONTACT);
       inden.setPointOfContacts(Arrays.asList(new DefaultResponsibleParty( cities)));
       filledMetadata.setIdentificationInfo(Collections.singleton(inden));
            //extend
                    //GeographicBoundingBox

       final DefaultExtent ex = new DefaultExtent();
       final GeographicBoundingBox box = getGeographicBoundingBox();
       ex.setGeographicElements(Arrays.asList(box));
            //TemporalExtent
//       final Envelope projectedenvelop = getProjectedEnvelope();
//       final Envelope en = getProjectedEnvelope();
//       final DefaultVerticalExtent uplow = new DefaultVerticalExtent();
//       uplow.setBounds(en);
//       ex.setVerticalElements(Collections.singleton(uplow));
                    //
       final String dateu = getAcquisitionDate();
       ex.setDescription(new DefaultInternationalString("Date : "+dateu));
       inden.setExtents(Collections.singleton(new DefaultExtent(ex)));
      //AcquisitionInformation
       final DefaultAcquisitionInformation dAI = new DefaultAcquisitionInformation();
       final DefaultPlatform platF = new DefaultPlatform();
       final String space = getValue("SPACECRAFT_ID");
       if (space == null ){
           return null;
       }
       platF.setCitation(new DefaultCitation(space));
       final DefaultInstrument instru = new DefaultInstrument();
       final String instrum = getValue("SENSOR_ID");
       if( instrum == null){
           return null;
       }
       instru.setType(new DefaultInternationalString(instrum));
       final String nadir = getValue("NADIR_OFFNADIR");
       if ( nadir == null){
           return null;
       }
       instru.setDescription(new DefaultInternationalString(nadir));
       if (platF == null && instrum == null) {
           return null;
        }
       platF.setInstruments(Collections.singleton(instru));
       dAI.setPlatforms(Collections.singleton(platF));
       filledMetadata.setAcquisitionInformation(Collections.singleton(dAI));
        //data quality info
       final DefaultDataQuality quali = new DefaultDataQuality();
        // Lineage
       filledMetadata.setDataQualityInfo(Collections.singleton(quali));
        //setResourceLineages
       final CoordinateReferenceSystem crs = getCRS();
       filledMetadata.setReferenceSystemInfo(Arrays.asList(crs));
       return filledMetadata;
   }
   private Date getAcquisitionDate1() throws ParseException {
     //-- year month day
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
   private String getAcquisitionDate() throws ParseException {
     //-- year month day
       final String dateAcquired  = getValue("DATE_ACQUIRED");
       if (dateAcquired == null)
            return null;
        //-- hh mm ss:ms
       final String sceneCenterTime = getValue("SCENE_CENTER_TIME");
       SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.sssssss'Z'");
       SimpleDateFormat formatter1 = new SimpleDateFormat("HH:mm:ss Z");
       final Date date = formatter.parse(sceneCenterTime);
       String str = formatter1.format(date);
       String strDate = dateAcquired;
       strDate = dateAcquired+"  "+str;
       return strDate;
    }
   private Date getDates()throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	final String dateInString = getValue("FILE_DATE");
	final Date date = formatter.parse(dateInString);
        if (dateInString == null) {
            return null;
            }
        return date;
   }
    private CoordinateReferenceSystem getCRS2D() throws FactoryException {
        if (projectedCRS2D != null)
            return projectedCRS2D;
        //-- Datum
        final String datum = getValue("DATUM");
        //-- Ellipsoid
        final String ellips = getValue("ELLIPSOID");
        if (!(("WGS84".equalsIgnoreCase(datum)) && ("WGS84".equalsIgnoreCase(ellips)))){
            throw new IllegalStateException("Comportement not supported : expected Datum and Ellipsoid value WGS84, found Datum = "+datum+", Ellipsoid : "+ellips);
        }
        final String projType = getValue("MAP_PROJECTION");

        switch (projType) {
            case "UTM" : {
                /**
                 * From Landsat specification, normaly Datum and ellipsoid are always WGS84.
                 * UTM area is the only thing which change.
                 * Thereby we build a CRS from basic 32600 and we add read UTM area.
                 * For example if UTM area is 45 we decode 32645 CRS from EPSG database.
                 */
                final String utm_Zone = getValue("UTM_ZONE");
                final Integer utm = Integer.valueOf(utm_Zone);
                ArgumentChecks.ensureBetween(datum, 0, 60, utm);
                final NumberFormat nf = new DecimalFormat("##");
                final String utmFormat = nf.format(utm);
                projectedCRS2D = CRS.forCode("EPSG:326"+utmFormat);
                break;
            }
            case "PS" : {
                final String originLongitude   = getValue("VERTICAL_LON_FROM_POLE");
                final String trueLatitudeScale = getValue("TRUE_SCALE_LAT");
                final String falseEasting      = getValue("FALSE_EASTING");
                final String falseNorthing     = getValue("FALSE_NORTHING");
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
                projectedCRS2D = new DefaultProjectedCRS(properties, CommonCRS.WGS84.normalizedGeographic(), projection, null);
                break;
            }
            default : throw new IllegalStateException("Comportement not supported : expected MAP_PROJECTION values are : PS or UTM, found : "+projType);
        }
        return projectedCRS2D;
    }

   CoordinateReferenceSystem getCRS() throws FactoryException {
        if (projectedCRS != null) {
            return projectedCRS;
        }
        final CoordinateReferenceSystem crs2D = getCRS2D();
        //-- add temporal part if Date exist
        final TemporalCRS temporalCRS = CommonCRS.Temporal.JAVA.crs();
        projectedCRS = new GeodeticObjectBuilder()
                .addName(crs2D.getName().getCode() + '/' + temporalCRS.getName().getCode())
                .createCompoundCRS(crs2D, temporalCRS);
        return projectedCRS;
    }
   Envelope getProjectedEnvelope() throws FactoryException, ParseException {
        final CoordinateReferenceSystem projCRS = getCRS();
        assert projCRS != null;
        //-- {west, est, south, north}
        final double[] projectedBound2D = getProjectedBound2D();
        final GeneralEnvelope projEnvelope = new GeneralEnvelope(projCRS);
        projEnvelope.setRange(0, projectedBound2D[0], projectedBound2D[1]);
        projEnvelope.setRange(1, projectedBound2D[2], projectedBound2D[3]);
        final Date dat = getAcquisitionDate1();
        if (dat != null)
        projEnvelope.setRange(2, dat.getTime(), dat.getTime());
        return projEnvelope;
    }

    private GeographicBoundingBox getGeographicBoundingBox() throws DataStoreException {
        final DefaultGeographicBoundingBox bbox;
        try {
            bbox = new DefaultGeographicBoundingBox(
                getExtremumValue("CORNER_UL_LON_PRODUCT", "CORNER_LL_LON_PRODUCT", false),      // westBoundLongitude
                getExtremumValue("CORNER_UR_LON_PRODUCT", "CORNER_LR_LON_PRODUCT", true),       // eastBoundLongitude
                getExtremumValue("CORNER_LL_LAT_PRODUCT", "CORNER_LR_LAT_PRODUCT", false),      // southBoundLatitude
                getExtremumValue("CORNER_UL_LAT_PRODUCT", "CORNER_UR_LAT_PRODUCT", true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        return bbox.isEmpty() ? null : bbox;
    }
private double[] getProjectedBound2D() {

        //-- longitude
        final String west = getExtremumValue1("CORNER_LL_PROJECTION_X_PRODUCT", "CORNER_UL_PROJECTION_X_PRODUCT",true);
        final String est  = getExtremumValue1("CORNER_UR_PROJECTION_X_PRODUCT", "CORNER_LR_PROJECTION_X_PRODUCT",true);
        //-- lattitude
        final String south = getExtremumValue1("CORNER_LL_PROJECTION_Y_PRODUCT", "CORNER_LR_PROJECTION_Y_PRODUCT",true);
        final String north = getExtremumValue1("CORNER_UR_PROJECTION_Y_PRODUCT", "CORNER_UL_PROJECTION_Y_PRODUCT",true);
        return new double[]{Double.valueOf(west), Double.valueOf(est), Double.valueOf(south), Double.valueOf(north)};
    }
  private Set<ProcessStep> getThermicInfos13(){
         final HashSet<ProcessStep> extendedInfos = new HashSet<>();

            final ProcessStep r1 = getband("ANGLE_COEFFICIENT_FILE_NAME");
            if (r1 != null) extendedInfos.add(r1);
            final ProcessStep r2 = getband("BPF_NAME_OLI");
            if (r2!= null) extendedInfos.add(r2);
            final ProcessStep r3 = getband("BPF_NAME_TIRS");
            if (r3!= null) extendedInfos.add(r3);
            final ProcessStep r4 = getband("CPF_NAME");
            if (r4!= null) extendedInfos.add(r4);
            final ProcessStep r5 = getband("RLUT_FILE_NAME");
            if (r5!= null) extendedInfos.add(r5);

           return extendedInfos;
         }
   private Set<ProcessStep> getThermicInfos15(){
         final HashSet<ProcessStep> extendedInfos = new HashSet<>();

            final ProcessStep r1 = getband("PANCHROMATIC_LINES");
            if (r1 != null) extendedInfos.add(r1);
            final ProcessStep r2 = getband("PANCHROMATIC_SAMPLES");
            if (r2!= null) extendedInfos.add(r2);
            final ProcessStep r3 = getband("REFLECTIVE_LINES");
            if (r3!= null) extendedInfos.add(r3);
            final ProcessStep r4 = getband("REFLECTIVE_SAMPLES");
            if (r4!= null) extendedInfos.add(r4);
            final ProcessStep r5 = getband("THERMAL_LINES");
            if (r5!= null) extendedInfos.add(r5);
            final ProcessStep r6 = getband("THERMAL_SAMPLES");
            if (r6!= null) extendedInfos.add(r6);
           return extendedInfos;
         }
  private Set<ProcessStep> getThermicInfos14(){
         final HashSet<ProcessStep> extendedInfos = new HashSet<>();

            final ProcessStep r1 = getband("CLOUD_COVER");
            if (r1 != null) extendedInfos.add(r1);
            final ProcessStep r2 = getband("CLOUD_COVER_LAND");
            if (r2!= null) extendedInfos.add(r2);
            final ProcessStep r3 = getband("IMAGE_QUALITY_OLI");
            if (r3!= null) extendedInfos.add(r3);
            final ProcessStep r4 = getband("IMAGE_QUALITY_TIRS");
            if (r4!= null) extendedInfos.add(r4);
            final ProcessStep r5 = getband("TIRS_SSM_POSITION_STATUS");
            if (r5!= null) extendedInfos.add(r5);
             final ProcessStep r6 = getband("ROLL_ANGLE");
            if (r6 != null) extendedInfos.add(r6);
            final ProcessStep r7 = getband("SUN_AZIMUTH");
            if (r7!= null) extendedInfos.add(r7);
            final ProcessStep r8 = getband("SUN_ELEVATION");
            if (r8!= null) extendedInfos.add(r8);
            final ProcessStep r9 = getband("EARTH_SUN_DISTANCE");
            if (r9!= null) extendedInfos.add(r9);
            final ProcessStep r10 = getband("GROUND_CONTROL_POINTS_VERSION");
            if (r10!= null) extendedInfos.add(r10);
            final ProcessStep r11 = getband("GROUND_CONTROL_POINTS_MODEL");
            if (r11!= null) extendedInfos.add(r11);
            final ProcessStep r12 = getband("GEOMETRIC_RMSE_MODEL");
            if (r12!= null) extendedInfos.add(r12);
            final ProcessStep r13 = getband("GEOMETRIC_RMSE_MODEL_Y");
            if (r13!= null) extendedInfos.add(r13);
            final ProcessStep r14 = getband("GEOMETRIC_RMSE_MODEL_X");
            if (r14!= null) extendedInfos.add(r14);
            final ProcessStep r15 = getband("GROUND_CONTROL_POINTS_VERIFYL");
            if (r15!= null) extendedInfos.add(r15);
            final ProcessStep r16 = getband("GEOMETRIC_RMSE_VERIFY");
            if (r16!= null) extendedInfos.add(r16);

           return extendedInfos;
         }
        //Minimum achievable spectral radiance value for Band
         private Set<ProcessStep> getThermicInfos2(){
         final HashSet<ProcessStep> extendedInfos = new HashSet<>();
         for(int x=1 ;x<=11 ; x++){
            final ProcessStep r1 = getRaminmax("RADIANCE_MINIMUM_BAND_", x);
            if (r1 != null) extendedInfos.add(r1);
         }
           return extendedInfos;
         }
         //Maximum achievable spectral radiance value for Band
          private Set<ProcessStep> getThermicInfos3(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("RADIANCE_MAXIMUM_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
        //Maximum achievable reflectance value for Band
          private Set<ProcessStep> getThermicInfos4(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("REFLECTANCE_MAXIMUM_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
//Minimum achievable reflectance value for Band
          private Set<ProcessStep> getThermicInfos5(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("REFLECTANCE_MINIMUM_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
          //Maximum possible pixel value for Band
           private Set<ProcessStep> getThermicInfos6(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("QUANTIZE_CAL_MAX_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
          //Minimum possible pixel value for Band
           private Set<ProcessStep> getThermicInfos7(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("QUANTIZE_CAL_MIN_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
          //The multiplicative rescaling factor used to convert calibrated DN to Radiance units for Band
          private Set<ProcessStep> getThermicInfos8(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("RADIANCE_MULT_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
          //The additive rescaling factor used to convert calibrated DN to Radiance units for Band
           private Set<ProcessStep> getThermicInfos9(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("RADIANCE_ADD_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
          //The multiplicative rescaling factor used to convert calibrated DN to Reflectance for Band
            private Set<ProcessStep> getThermicInfos10(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("REFLECTANCE_MULT_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
          //The additive rescaling factor used to convert calibrated DN to Reflectance for Band
          private Set<ProcessStep> getThermicInfos11(){
          final HashSet<ProcessStep> extendedInfos = new HashSet<>();
          for(int x=1 ;x<=11 ; x++){
               final ProcessStep r1 = getRaminmax("REFLECTANCE_ADD_BAND_", x);
               if (r1 != null) extendedInfos.add(r1);
           }
          return extendedInfos;
         }
           private Set<ProcessStep> getThermicInfos12(){
         final HashSet<ProcessStep> extendedInfos = new HashSet<>();
         for(int x=1 ;x<=11 ; x++){
            final ProcessStep rx = getRaminmax("FILE_NAME_BAND_", x);
            if (rx != null) extendedInfos.add(rx);
         }
         final ProcessStep r2 = getband("FILE_NAME_BAND_QUALITY");
         if (r2 != null) extendedInfos.add(r2);
           return extendedInfos;
           }
//return K infor
        private Set<ProcessStep> getThermicInfos() {
               final HashSet<ProcessStep> extendedInfos = new HashSet<>();
               final ProcessStep k110 = getKConstant(1, 10);
               if (k110 != null) extendedInfos.add(k110);
               final ProcessStep k210 = getKConstant(2, 10);
               if (k210 != null) extendedInfos.add(k210);
               final ProcessStep k111 = getKConstant(1, 11);
               if (k111 != null) extendedInfos.add(k111);
               final ProcessStep k211 = getKConstant(2, 11);
               if (k211 != null) extendedInfos.add(k211);
               return extendedInfos;
           }
        //radian min max band

        private ProcessStep getRaminmax(final String minmax, final int bandIndex) {

        final String RaLabel = minmax+bandIndex;
        final String k = getValue(RaLabel);
        if (k != null) {
            final DefaultProcessStep kProcessStep = new DefaultProcessStep();
            final String description = "Band"+bandIndex+" = "+k;
            kProcessStep.setDescription(new DefaultInternationalString(description));
            return kProcessStep;
        }
        return null;
    }
/**
     * Returns if exist K constant for one thermic band.
     *
     * @param kVal 1 or 2 for K1 or K2.
     * @param bandIndex thermic band index. (10 or 11 for Landsat)
     * @return
     */
     private ProcessStep getband(final String band) {

        final String kLabel = band;
        final String k = getValue(kLabel);
        if (k != null) {
            final DefaultProcessStep kProcessStep = new DefaultProcessStep();
            final String description = band+"is:  "+k;
            kProcessStep.setDescription(new DefaultInternationalString(description));
            return kProcessStep;
        }
        return null;
    }
    private ProcessStep getKConstant(final int kVal, final int bandIndex) {

        final String kLabel = "K"+kVal+"_CONSTANT_BAND_"+bandIndex;
        final String k = getValue(kLabel);
        if (k != null) {
            final DefaultProcessStep kProcessStep = new DefaultProcessStep();
            final String description = kLabel+" = "+k;
            kProcessStep.setDescription(new DefaultInternationalString(description));
            return kProcessStep;
        }
        return null;
    }
    /**
     * Temporary method for testing purpose only - will be removed in the final version.
     */
    public static void main(String[] args) throws Exception {
        LandsatMetadataReader reader;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/haonguyen/data/LC81230522014071LGN00_MTL.txt"))) {
            reader = new LandsatMetadataReader(in);
        }
        System.out.println("The Metadata of LC81230522014071LGN00_MTL.txt is:");
        System.out.println(reader.getCRS2D());

       System.out.println(reader. getInfos());
       System.out.println(reader.getProjectedEnvelope());

       System.out.println("The number of product ");
        System.out.println(reader.getThermicInfos15());
       System.out.println("The file name for Band");
        System.out.println(reader.getThermicInfos12());
        System.out.println("The file other");
        System.out.println(reader.getThermicInfos13());
         System.out.println("The Attributes image");
        System.out.println(reader.getThermicInfos14());
       System.out.println("Minimum achievable spectral radiance value for Band");
        System.out.println(reader.getThermicInfos2());
        System.out.println("Maximum achievable spectral radiance value for Band");
        System.out.println(reader.getThermicInfos3());
        System.out.println("Maximum achievable reflectance value for Band");
        System.out.println(reader.getThermicInfos4());
        System.out.println("Minimum achievable reflectance value for Band ");
        System.out.println(reader.getThermicInfos5());
        System.out.println("Maximum possible pixel value for Band");
        System.out.println(reader.getThermicInfos6());
        System.out.println("Minimum possible pixel value for Band ");
        System.out.println(reader.getThermicInfos7());
        System.out.println("The multiplicative rescaling factor used to convert calibrated DN to Radiance units for Band");
        System.out.println(reader.getThermicInfos8());
        System.out.println("The additive rescaling factor used to convert calibrated DN to Radiance units for Band");
        System.out.println(reader.getThermicInfos9());
        System.out.println("The multiplicative rescaling factor used to convert calibrated DN to Reflectance for Band");
        System.out.println(reader.getThermicInfos10());
        System.out.println("The additive rescaling factor used to convert calibrated DN to Reflectance for Band");
        System.out.println(reader.getThermicInfos11());
         System.out.println("K info is:");
       System.out.println(reader.getThermicInfos());
    }
}
