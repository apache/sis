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

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultAggregateInformation;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.ScopeCode;

import static java.util.Collections.singleton;
import static org.apache.sis.storage.geotiff.ModisPath.DataCenterId;


/**
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class ModisReader {

    /**
     * All properties found in the Modis metadata file.
     */
    private final Map<String, String> properties;

    /**
     * Creates a new metadata parser from the given characters reader.
     *
     * @param xml a xml opened on the Modis file. It is caller's
     * responsibility to close this reader.
     * @throws Exception 
     */
    public ModisReader(File xml) throws Exception {
        properties = new HashMap();
        Xpath read = new Xpath(xml);
        ModisPath a = new ModisPath();
        for (String path : a.path()) {
            String value = read.getValue(path);
            properties.put(path, value);
        }

    }

    /**
     *  Returns the property value associated to the given key, or {@code null}
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
     * @param key the key for which to get the floating-point value.
     * @return
     * @throws NumberFormatException if the property associated to the given key
     * can not be parsed as a floating-point number.
     */
    private double getNumericValue(String key) throws NumberFormatException {
        String value = getValue(key);
        return (value != null) ? Double.parseDouble(value) : Double.NaN;
    }

    /**
     * Returns the minimal or maximal value associated to the given two keys
     * @param key1 the key for which to get the first floating-point value.
     * @param key2 the key for which to get the second floating-point value.
     * @param max {@code true} for the maximal value, or {@code false} for the
     * minimal value.
     * @return the minimal (if {@code max} is false) or maximal (if {@code max}
     * is true) floating-point value associated to the given keys, or
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
     *
     * @return the date associated to the given key, or {@code null} if none.
     * @throws DateTimeParseException if the date can not be parsed.
     */
    private Date getDate() throws DateTimeParseException, Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        final String dateInString = getValue(ModisPath.ProductionDateTime);
        final Date date = formatter.parse(dateInString);
        if (dateInString == null) {
            return null;
        }
        return date;
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
        String value = getValue(ModisPath.ShortName);
        format.setName(new DefaultInternationalString(value));
        distribution.setDistributionFormats(singleton(format));
        return distribution;
    }

    /**
     * Gets the geographic and temporal extent for identification info, or
     * {@code null} if none. This method expects the data acquisition time in
     * argument in order to avoid to compute it twice.
     * @return the data extent in Identification info, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    private Extent createExtent() throws DataStoreException, Exception {
        final DefaultGeographicBoundingBox box;
        try {
            box = new DefaultGeographicBoundingBox(
                    getExtremumValue(ModisPath.UL_LON, ModisPath.LL_LON, false), // westBoundLongitude
                    getExtremumValue(ModisPath.UR_LON, ModisPath.LR_LON, true), // eastBoundLongitude
                    getExtremumValue(ModisPath.LL_LAT, ModisPath.LR_LAT, false), // southBoundLatitude
                    getExtremumValue(ModisPath.UL_LAT, ModisPath.UR_LAT, true));      // northBoundLatitude
        } catch (NumberFormatException e) {
            throw new DataStoreException("Can not read the geographic bounding box.", e);
        }
        final DefaultExtent extent = new DefaultExtent();
        final boolean isEmpty = box.isEmpty();
        if (!isEmpty) {
            extent.setGeographicElements(singleton(box));
        }
        return extent;
    }

    /**
     * Gets basic information required to uniquely identify the data, or
     * {@code null} if none. This method expects the metadata and data
     * acquisition time in arguments in order to avoid to compute them twice.
     *
     * @param metadataTime the metadata file creation time, or {@code null} if
     * none.
     * @return the data identification information, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    private Identification createIdentification(final Date metadataTime) throws DataStoreException, Exception {
        
        final AbstractIdentification identification = new AbstractIdentification();
        final DefaultCitation citation = new DefaultCitation();
        boolean isEmpty = true;
        if (metadataTime != null) {
            citation.setDates(singleton(new DefaultCitationDate(metadataTime, DateType.PUBLICATION)));
            isEmpty = false;
        }
        String value = getValue(ModisPath.DistributedFileName);
        if (value != null) {
            citation.setTitle(new DefaultInternationalString(value + ".xml"));
            isEmpty = false;
        }
        if (!isEmpty) {
            identification.setCitation(citation);
        }
        
            final DefaultKeywords keyword = new DefaultKeywords("Land/Water Mask > Cloud Mask > Atmospheric > Land Cover > Snow Cover");
            identification.setDescriptiveKeywords(singleton(keyword));
        
        final Extent extent = createExtent();
        if (extent != null) {
            identification.setExtents(singleton(extent));
            isEmpty = false;
        }
        value = getValue(ModisPath.DataCenterId);
        if (value != null) {
            DefaultResponsibleParty responsible = new DefaultResponsibleParty();
            responsible.setOrganisationName(new DefaultInternationalString(value));
            responsible.setRole(Role.ORIGINATOR);
            DefaultResponsibleParty responsiblecontributor = new DefaultResponsibleParty();
            responsiblecontributor.setOrganisationName(new DefaultInternationalString(value));
            responsiblecontributor.setRole(Role.AUTHOR);
            identification.getPointOfContacts().add(responsible);
            identification.getPointOfContacts().add(responsiblecontributor);
            isEmpty = false;
        }
        value = getValue(DataCenterId);
        if (value != null) {
            DefaultCitation citation1 = new DefaultCitation();
            DefaultAggregateInformation aggregateInformation = new DefaultAggregateInformation();
            citation1.setTitle(new DefaultInternationalString(value));
            aggregateInformation.setAggregateDataSetName(citation1);
            identification.setAggregationInfo(singleton(aggregateInformation));
            isEmpty = false;
        }

        return isEmpty ? null : identification;
    }
 /**
     * Returns the metadata about the resources described in the Modis file.
     *
     * @return the metadata about Modis resources.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    public Metadata read() throws DataStoreException, Exception {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandards(Citations.ISO_19115);
        final Date metadataTime = getDate();
        if (metadataTime != null) {
            
            metadata.setDateInfo(singleton(new DefaultCitationDate(metadataTime,DateType.CREATION)));
        
        }
        metadata.setLanguage(Locale.ENGLISH);
        metadata.setFileIdentifier(getValue(ModisPath.LocalGranuleID));
        final Distribution metadataDistribution = createDistribution();
        metadata.setDistributionInfo(singleton(metadataDistribution));
//         final Date sceneTime = getDate();
        final Identification identification = createIdentification(metadataTime);
        if (identification != null) {
            metadata.setIdentificationInfo(singleton(identification));
        }
        if (getValue(ModisPath.PlatformShortName) != null) {

            metadata.setHierarchyLevels(singleton(ScopeCode.valueOf(getValue(ModisPath.PlatformShortName))));
        }
        return metadata;
    }
}
