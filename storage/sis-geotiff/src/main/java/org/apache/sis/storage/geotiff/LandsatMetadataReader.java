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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultSeries;
import org.apache.sis.metadata.iso.extent.DefaultExtent;

import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.acquisition.Platform;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;



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
    private String getValue(boolean par, String key) {
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
        String value = getValue(false, key);
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

    /**
     * Gets the data bounding box in degrees of longitude and latitude, or {@code null} if none.
     *
     * @return the data domain in degrees of longitude and latitude, or {@code null} if none.
     * @throws DataStoreException if a longitude or a latitude can not be read.
     */
 
   
   
   private Metadata getInfos() throws ParseException, DataStoreException{
       final DefaultMetadata filledMetadata = new DefaultMetadata();
      //contact
       final DefaultResponsibleParty cities = new DefaultResponsibleParty();
       final String part = getValue(false, "STATION_ID");
        cities.setOrganisationName(new DefaultInternationalString(part));
       
    
       cities.setRole(Role.POINT_OF_CONTACT);
        filledMetadata.setContacts(Arrays.asList(cities));
       //Identification info
       final AbstractIdentification inden = new AbstractIdentification();
            //citation
        final DefaultCitation citation = new DefaultCitation();
        final String identifier = getValue(false, "LANDSAT_SCENE_ID");
        if (identifier != null) 
        citation.setTitle(new DefaultInternationalString(identifier));
         final String id = getValue(false, "REQUEST_ID");
         citation.setSeries(new DefaultSeries(id));
        final Date date =  getDates();
        if (date != null)  
        citation.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.PUBLICATION)));
         final String identifiers = getValue(false, "DATA_TYPE");
        if (identifier != null)
            citation.setIdentifiers(Collections.singleton(new DefaultIdentifier(identifiers)));
        
       final String party = getValue(false, "ORIGIN");
       cities.setOrganisationName(new DefaultInternationalString(party));
       cities.setRole(Role.ORIGINATOR);
        citation.setCitedResponsibleParties(Arrays.asList(cities));
        
        inden.setCitation(new DefaultCitation(citation));
            //Abstrac
            
            inden.setAbstract(new DefaultInternationalString(part));
            
            //PointOfContacts
            final String point = getValue(false, "ORIGIN");
       cities.setOrganisationName(new DefaultInternationalString(point));
       cities.setRole(Role.POINT_OF_CONTACT);
        
       inden.setPointOfContacts(Arrays.asList(new DefaultResponsibleParty( cities)));
         
       filledMetadata.setIdentificationInfo(Collections.singleton(inden));
            //extend
                    //GeographicBoundingBox 
                    final DefaultExtent ex = new DefaultExtent();
             final GeographicBoundingBox box = getGeographicBoundingBox();
        ex.setGeographicElements(Arrays.asList(box));
                    //
           inden.setExtents(Collections.singleton(new DefaultExtent(ex)));
      //AcquisitionInformation
          final DefaultAcquisitionInformation dAI = new DefaultAcquisitionInformation();
       final DefaultPlatform platF = new DefaultPlatform();
       final String space = getValue(false, "SPACECRAFT_ID");
       platF.setCitation(new DefaultCitation(space));
       final DefaultInstrument instru = new DefaultInstrument();
        final String instrum = getValue(false, "SENSOR_ID");
        instru.setType(new DefaultInternationalString(instrum));
        final String nadir = getValue(false, "NADIR_OFFNADIR");
         instru.setDescription(new DefaultInternationalString(nadir));
        if (platF != null && instrum != null) {
            //-- set related founded instrument and platform
            //*****************************************************************//
            //-- a cycle is define here, platform -> instru and instru -> platform
            //-- like a dad know his son and a son know his dad.
            //-- during xml binding a cycle is not supported for the current Apach SIS version
            //-- decomment this row when upgrade SIS version
            //instru.setMountedOn(platform);
            //*****************************************************************//

            platF.setInstruments(Collections.singleton(instru));

            dAI.setPlatforms(Collections.singleton(platF));
            

            filledMetadata.setAcquisitionInformation(Collections.singleton(dAI));
        }
         
        

        return filledMetadata;
   }
   private Date getDates()throws ParseException {
       
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	final String dateInString = getValue(false, "FILE_DATE");		
	final Date date = formatter.parse(dateInString);
                if (dateInString == null) {
        return null;
            }	
        return date;
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

    /**
     * Temporary method for testing purpose only - will be removed in the final version.
     */
    public static void main(String[] args) throws Exception {
        LandsatMetadataReader reader;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/haonguyen/data/LC81230522014071LGN00_MTL.txt"))) {
            reader = new LandsatMetadataReader(in);
        }
        System.out.println("The geographic bounding box of LC81230522014071LGN00_MTL.txt is:");
//        System.out.println(reader.getGeographicBoundingBox());
       
        System.out.println(reader. getInfos());
  
    }

    
}
