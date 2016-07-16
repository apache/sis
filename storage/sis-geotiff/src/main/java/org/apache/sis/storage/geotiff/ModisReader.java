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
import static java.util.Collections.singleton;
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
import static org.apache.sis.storage.geotiff.ModisPath.DataCenterId;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.ScopeCode;

/**
 *
 * @author haonguyen
 */
public class ModisReader {

    /**
     * All properties found in the Modis metadata file, except {@code GROUP} and
     * {@code END_GROUP}. Example:
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
     * Creates a new metadata parser from the given characters reader.
     *
     * @param reader a reader opened on the Modis file. It is caller's
     * responsibility to close this reader.
     * @throws IOException if an I/O error occurred while reading the given
     * stream.
     * @throws DataStoreException if the content is not a Modis file.
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
     *
     * @param sceneTime the data acquisition time, or {@code null} if none.
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
     * @param sceneTime the data acquisition time, or {@code null} if none.
     * @return the data identification information, or {@code null} if none.
     * @throws DataStoreException if a property value can not be parsed as a
     * number or a date.
     */
    private Identification createIdentification(final Date metadataTime) throws DataStoreException, Exception {
        final DefaultCitation citation = new DefaultCitation();
        final AbstractIdentification identification = new AbstractIdentification();
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
        final String value1 = getValue(ModisPath.ReprocessingActual);
        if (value1 != null) {
            final DefaultKeywords keyword = new DefaultKeywords(value1);
            identification.setDescriptiveKeywords(singleton(keyword));
            isEmpty = false;
        }
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
            metadata.setDateStamp(metadataTime);
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
//        final ImageDescription content = createImageDescription();
//        if (content != null) {
//            metadata.setContentInfo(singleton(content));
//        }
//        final AcquisitionInformation acquisition = createAcquisitionInformation(sceneTime);
//        if (acquisition != null) {
//            metadata.setAcquisitionInformation(singleton(acquisition));
//        }
        if (getValue(ModisPath.PlatformShortName) != null) {

            metadata.setHierarchyLevels(singleton(ScopeCode.valueOf(getValue(ModisPath.PlatformShortName))));
        }
        return metadata;
    }

    public static void main(String argv[]) throws Exception {

        File xml = new File("/home/haonguyen/data/MOD09Q1.A2010009.h08v07.005.2010027023253.hdf.xml");
        ModisReader a = new ModisReader(xml);

        System.out.println(a.read());
        System.out.println(a.getValue(ModisPath.ReprocessingActual));
    }
}
