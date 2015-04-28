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
package org.apache.sis.storage.netcdf;

/*
 * All imports below except "CF" are for javadoc only. The "CF" import is used only
 * for its static final String constants, which are inlined by javac. Consequently
 * the compiled file of this class should have no dependency to the UCAR packages.
 */
import java.io.Serializable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CF;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;


/**
 * Name of attributes used in the mapping from/to NetCDF metadata to ISO 19115 metadata.
 * The attributes recognized by SIS are listed below:
 *
 * <blockquote><table class="compact" summary="List of all NetCDF attributes.">
 * <tr valign="top"><td style="width: 25%">
 * {@value     #ACCESS_CONSTRAINT}<br>
 * {@value     #ACKNOWLEDGMENT}<br>
 * {@value     #COMMENT}<br>
 * {@linkplain #CONTRIBUTOR "contributor_email"}<br>
 * {@linkplain #CONTRIBUTOR "contributor_name"}<br>
 * {@linkplain #CONTRIBUTOR "contributor_role"}<br>
 * {@linkplain #CONTRIBUTOR "contributor_url"}<br>
 * {@linkplain #CREATOR     "creator_email"}<br>
 * {@linkplain #CREATOR     "creator_name"}<br>
 * {@linkplain #CREATOR     "creator_url"}<br>
 * {@value     #DATA_TYPE}<br>
 * {@value     #DATE_CREATED}<br>
 * {@value     #DATE_ISSUED}<br>
 * {@value     #DATE_MODIFIED}<br>
 * {@value     #FLAG_MASKS}<br>
 * {@value     #FLAG_MEANINGS}<br>
 * {@value     #FLAG_NAMES}<br>
 * {@value     #FLAG_VALUES}<br>
 * </td><td style="width: 25%">
 * {@linkplain #TITLE "full_name"}<br>
 * {@linkplain #GEOGRAPHIC_IDENTIFIER "geographic_identifier"}<br>
 * {@linkplain #LATITUDE  "geospatial_lat_max"}<br>
 * {@linkplain #LATITUDE  "geospatial_lat_min"}<br>
 * {@linkplain #LATITUDE  "geospatial_lat_resolution"}<br>
 * {@linkplain #LATITUDE  "geospatial_lat_units"}<br>
 * {@linkplain #LONGITUDE "geospatial_lon_max"}<br>
 * {@linkplain #LONGITUDE "geospatial_lon_min"}<br>
 * {@linkplain #LONGITUDE "geospatial_lon_resolution"}<br>
 * {@linkplain #LONGITUDE "geospatial_lon_units"}<br>
 * {@linkplain #VERTICAL  "geospatial_vertical_max"}<br>
 * {@linkplain #VERTICAL  "geospatial_vertical_min"}<br>
 * {@linkplain #VERTICAL  "geospatial_vertical_positive"}<br>
 * {@linkplain #VERTICAL  "geospatial_vertical_resolution"}<br>
 * {@linkplain #VERTICAL  "geospatial_vertical_units"}<br>
 * </td><td style="width: 25%">
 * {@value     #HISTORY}<br>
 * {@value     #IDENTIFIER}<br>
 * {@linkplain #CREATOR "institution"}<br>
 * {@value     #KEYWORDS}<br>
 * {@value     #VOCABULARY}<br>
 * {@value     #LICENSE}<br>
 * {@value     #METADATA_CREATION}<br>
 * {@linkplain #TITLE "name"}<br>
 * {@value     #NAMING_AUTHORITY}<br>
 * {@value     #PROCESSING_LEVEL}<br>
 * {@value     #PROJECT}<br>
 * {@linkplain #PUBLISHER "publisher_email"}<br>
 * {@linkplain #PUBLISHER "publisher_name"}<br>
 * {@linkplain #PUBLISHER "publisher_url"}<br>
 * {@value     #PURPOSE}<br>
 * {@value     #REFERENCES}<br>
 * </td><td style="width: 25%">
 * {@value     #STANDARD_NAME}<br>
 * {@value     #STANDARD_NAME_VOCABULARY}<br>
 * {@value     #SUMMARY}<br>
 * {@linkplain #TIME "time_coverage_duration"}<br>
 * {@linkplain #TIME "time_coverage_end"}<br>
 * {@linkplain #TIME "time_coverage_resolution"}<br>
 * {@linkplain #TIME "time_coverage_start"}<br>
 * {@linkplain #TIME "time_coverage_units"}<br>
 * {@value     #TITLE}<br>
 * {@value     #TOPIC_CATEGORY}<br>
 * </td></tr></table></blockquote>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li><a href="https://geo-ide.noaa.gov/wiki/index.php?title=NetCDF_Attribute_Convention_for_Dataset_Discovery">NetCDF
 *       Attribute Convention for Dataset Discovery</a> wiki</li>
 *   <li><a href="http://ngdc.noaa.gov/metadata/published/xsl/nciso2.0/UnidataDD2MI.xsl">UnidataDD2MI.xsl</a> file</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class AttributeNames {
    /**
     * The {@value} attribute name for a short description of the dataset
     * (<em>Highly Recommended</em>). If no {@value} attribute is provided,
     * then {@code AttributeNames} will look for "full_name" and "name".
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @see NetcdfFile#getTitle()
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#title_Attribute">UCAR reference</a>
     */
    public static final String TITLE = "title";

    /**
     * The {@value} attribute name for a paragraph describing the dataset
     * (<em>Highly Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getAbstract() abstract}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#summary_Attribute">UCAR reference</a>
     */
    public static final String SUMMARY = "summary";

    /**
     * The {@value} attribute name for an identifier (<em>Recommended</em>).
     * The combination of the {@value #NAMING_AUTHORITY} and the {@value}
     * should be a globally unique identifier for the dataset.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getFileIdentifier() fileIdentifier}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getIdentifiers() identifier} /
     * {@link Identifier#getCode() code}</li></ul>
     *
     * @see MetadataReader#getFileIdentifier()
     * @see NetcdfFile#getId()
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#id_Attribute">UCAR reference</a>
     */
    public static final String IDENTIFIER = "id";

    /**
     * The {@value} attribute name for the identifier authority (<em>Recommended</em>).
     * The combination of the {@value} and the {@value #IDENTIFIER} should be a globally
     * unique identifier for the dataset.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getFileIdentifier() fileIdentifier}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getIdentifiers() identifier} /
     * {@link Identifier#getAuthority() authority}</li></ul>
     *
     * @see #IDENTIFIER
     * @see MetadataReader#getFileIdentifier()
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#id_Attribute">UCAR reference</a>
     */
    public static final String NAMING_AUTHORITY = "naming_authority";

    /**
     * The {@value} attribute name for a long descriptive name for the variable taken from a controlled
     * vocabulary of variable names. This is actually a {@linkplain VariableSimpleIF variable} attribute,
     * but sometime appears also in {@linkplain NetcdfFile#findGlobalAttribute(String) global attributes}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with {@link KeywordType#THEME}</li></ul>
     *
     * @see #STANDARD_NAME_VOCABULARY
     * @see #KEYWORDS
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#standard_name_Attribute">UCAR reference</a>
     */
    public static final String STANDARD_NAME = CF.STANDARD_NAME;

    /**
     * The {@value} attribute name for indicating which controlled list of variable names has been
     * used in the {@value #STANDARD_NAME} attribute.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getThesaurusName() thesaurusName} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @see #STANDARD_NAME
     * @see #VOCABULARY
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#standard_name_vocabulary_Attribute">UCAR reference</a>
     */
    public static final String STANDARD_NAME_VOCABULARY = "standard_name_vocabulary";

    /**
     * The {@value} attribute name for a comma separated list of key words and phrases
     * (<em>Highly Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with {@link KeywordType#THEME}</li></ul>
     *
     * @see #VOCABULARY
     * @see #STANDARD_NAME
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#keywords_Attribute">UCAR reference</a>
     */
    public static final String KEYWORDS = "keywords";

    /**
     * The {@value} attribute name for the guideline for the words/phrases in the
     * {@value #KEYWORDS} attribute (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getThesaurusName() thesaurusName} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @see #KEYWORDS
     * @see #STANDARD_NAME_VOCABULARY
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#keywords_vocabulary_Attribute">UCAR reference</a>
     */
    public static final String VOCABULARY = "keywords_vocabulary";

    /**
     * The {@value} attribute name for a high-level geographic data thematic classification.
     * Typical values are {@code "farming"}, {@code "biota"}, {@code "boundaries"},
     * {@code "climatology meteorology atmosphere"}, {@code "economy"}, {@code "elevation"},
     * {@code "environment"}, {@code "geoscientific information"}, {@code "health"},
     * {@code "imagery base maps earth cover"}, {@code "intelligence military"},
     * {@code "inland waters"}, {@code "location"}, {@code "oceans"}, {@code "planning cadastre"},
     * {@code "society"}, {@code "structure"}, {@code "transportation"} and
     * {@code "utilitiesCommunication"}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getTopicCategories() topicCategory}</li></ul>
     *
     * @see TopicCategory
     */
    public static final String TOPIC_CATEGORY = "topic_category";

    /**
     * The {@value} attribute name for the THREDDS data type appropriate for this dataset
     * (<em>Recommended</em>). Examples: {@code "Vector"}, {@code "TextTable"}, {@code "Grid"},
     * {@code "Image"}, {@code "Video"}, {@code "Tin"}, {@code "StereoModel"}, {@code "Station"},
     * {@code "Swath"} or {@code "Trajectory"}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getSpatialRepresentationTypes() spatialRepresentationType}</li></ul>
     *
     * @see SpatialRepresentationType
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#cdm_data_type_Attribute">UCAR reference</a>
     */
    public static final String DATA_TYPE = "cdm_data_type";

    /**
     * The {@value} attribute name for providing an audit trail for modifications to the
     * original data (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getDataQualityInfo() dataQualityInfo} /
     * {@link DataQuality#getLineage() lineage} /
     * {@link Lineage#getStatement() statement}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#history_Attribute">UCAR reference</a>
     */
    public static final String HISTORY = "history";

    /**
     * The {@value} attribute name for miscellaneous information about the data
     * (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getSupplementalInformation() supplementalInformation}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#comment_Attribute">UCAR reference</a>
     */
    public static final String COMMENT = "comment";

    /**
     * The {@value} attribute name for the date on which the metadata was created
     * (<em>Suggested</em>). This is actually defined in the "{@code NCISOMetadata}"
     * subgroup.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getDateStamp() dateStamp}</li></ul>
     */
    public static final String METADATA_CREATION = "metadata_creation";

    /**
     * The {@value} attribute name for the date on which the data was created
     * (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getDates() date} /
     * {@link CitationDate#getDate() date} with {@link DateType#CREATION}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#date_created_Attribute">UCAR reference</a>
     */
    public static final String DATE_CREATED = "date_created";

    /**
     * The {@value} attribute name for the date on which this data was last modified
     * (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getDates() date} /
     * {@link CitationDate#getDate() date} with {@link DateType#REVISION}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#date_modified_Attribute">UCAR reference</a>
     */
    public static final String DATE_MODIFIED = "date_modified";

    /**
     * The {@value} attribute name for a date on which this data was formally issued
     * (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getDates() date} /
     * {@link CitationDate#getDate() date} with {@link DateType#PUBLICATION}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#date_issued_Attribute">UCAR reference</a>
     */
    public static final String DATE_ISSUED = "date_issued";

    /**
     * Holds the attribute names describing a responsible party.
     * In the following table, the header lists the constants defined in the {@link AttributeNames}
     * class and the other cells give the values assigned in this class fields for those constants.
     *
     * <table class="sis">
     * <caption>Names of NetCDF attributes describing a responsible party</caption>
     * <tr>
     *   <th            >Field in this class</th>
     *   <th class="sep">{@link AttributeNames#CREATOR     CREATOR}</th>
     *   <th            >{@link AttributeNames#CONTRIBUTOR CONTRIBUTOR}</th>
     *   <th            >{@link AttributeNames#PUBLISHER   PUBLISHER}</th>
     * </tr><tr>
     *   <td            >{@link #NAME}</td>
     *   <td class="sep">{@code "creator_name"}</td>
     *   <td            >{@code "contributor_name"}</td>
     *   <td            >{@code "publisher_name"}</td>
     * </tr><tr>
     *   <td            >{@link #INSTITUTION}</td>
     *   <td class="sep">{@code "institution"}</td>
     *   <td            ></td>
     *   <td            ></td>
     * </tr><tr>
     *   <td            >{@link #URL}</td>
     *   <td class="sep">{@code "creator_url"}</td>
     *   <td            >{@code "contributor_url"}</td>
     *   <td            >{@code "publisher_url"}</td>
     * </tr><tr>
     *   <td            >{@link #EMAIL}</td>
     *   <td class="sep">{@code "creator_email"}</td>
     *   <td            >{@code "contributor_email"}</td>
     *   <td            >{@code "publisher_email"}</td>
     * </tr><tr>
     *   <td            >{@link #ROLE}</td>
     *   <td class="sep"></td>
     *   <td            >{@code "contributor_role"}</td>
     *   <td            ></td>
     * </tr><tr>
     *   <td            >{@link #DEFAULT_ROLE}</td>
     *   <td class="sep">{@link Role#ORIGINATOR}</td>
     *   <td            ></td>
     *   <td>           {@link Role#PUBLISHER}</td>
     * </tr></table>
     *
     * <div class="note"><b>Note:</b>
     * The member names in this class are upper-cases because they should be considered as constants.
     * For example {@code AttributeNames.CREATOR.EMAIL} maps exactly to the {@code "creator_email"} string
     * and nothing else. A lower-case {@code email} member name could be misleading since it would suggest
     * that the field contains the actual name value rather than the key by which the value is identified
     * in a NetCDF file.</div>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see org.apache.sis.storage.netcdf.AttributeNames.Dimension
     */
    public static class Responsible implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2680152633273321012L;

        /**
         * The attribute name for the responsible's name. Possible values are
         * {@code "creator_name"}, {@code "contributor_name"} or {@code "publisher_name"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link ResponsibleParty} /
         * {@link ResponsibleParty#getIndividualName() individualName}</li></ul>
         */
        public final String NAME;

        /**
         * The attribute name for the responsible's institution, or {@code null} if none.
         * Possible value is {@code "institution"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link ResponsibleParty} /
         * {@link ResponsibleParty#getOrganisationName() organisationName}</li></ul>
         */
        public final String INSTITUTION;

        /**
         * The attribute name for the responsible's URL. Possible values are
         * {@code "creator_url"}, {@code "contributor_url"} or {@code "publisher_url"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link ResponsibleParty} /
         * {@link ResponsibleParty#getContactInfo() contactInfo} /
         * {@link Contact#getOnlineResource() onlineResource} /
         * {@link OnlineResource#getLinkage() linkage}</li></ul>
         */
        public final String URL;

        /**
         * The attribute name for the responsible's email address. Possible values are
         * {@code "creator_email"}, {@code "contributor_email"} or {@code "publisher_email"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link ResponsibleParty} /
         * {@link ResponsibleParty#getContactInfo() contactInfo} /
         * {@link Contact#getAddress() address} /
         * {@link Address#getElectronicMailAddresses() electronicMailAddress}</li></ul>
         */
        public final String EMAIL;

        /**
         * The attribute name for the responsible's role, or {@code null} if none.
         * Possible value is {@code "contributor_role"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link ResponsibleParty} /
         * {@link ResponsibleParty#getRole()}</li></ul>
         *
         * @see Role
         */
        public final String ROLE;

        /**
         * The role to use as a fallback if no attribute value is associated to the {@link #ROLE} key.
         */
        public final Role DEFAULT_ROLE;

        /**
         * Creates a new set of attribute names. Any argument can be {@code null} if not applicable.
         *
         * @param name        The attribute name for the responsible's name.
         * @param institution The attribute name for the responsible's institution.
         * @param url         The attribute name for the responsible's URL.
         * @param email       The attribute name for the responsible's email address.
         * @param role        The attribute name for the responsible's role.
         * @param defaultRole The role to use as a fallback if no attribute value is associated to the
         *                    {@code role} key.
         */
        public Responsible(final String name, final String institution, final String url, final String email,
                final String role, final Role defaultRole)
        {
            NAME         = name;
            INSTITUTION  = institution;
            URL          = url;
            EMAIL        = email;
            ROLE         = role;
            DEFAULT_ROLE = defaultRole;
        }
    }

    /**
     * The set of attribute names for the creator (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} with {@link Role#ORIGINATOR}</li></ul>
     *
     * @see #CONTRIBUTOR
     * @see #PUBLISHER
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#creator_name_Attribute">UCAR reference</a>
     */
    public static final Responsible CREATOR = new Responsible("creator_name",
            "institution", "creator_url", "creator_email", null, Role.ORIGINATOR);

    /**
     * The set of attribute names for the contributor (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation}</li></ul>
     *
     * @see #CREATOR
     * @see #PUBLISHER
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#contributor_name_Attribute">UCAR reference</a>
     */
    public static final Responsible CONTRIBUTOR = new Responsible("contributor_name",
            null, "contributor_url", "contributor_email", "contributor_role", null);

    /**
     * The set of attribute names for the publisher (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getDistributionInfo() distributionInfo} /
     * {@link Distribution#getDistributors() distributors} /
     * {@link Distributor#getDistributorContact() distributorContact} with {@link Role#PUBLISHER}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with the {@code "dataCenter"} {@link KeywordType}</li></ul>
     *
     * @see #CREATOR
     * @see #CONTRIBUTOR
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#publisher_name_Attribute">UCAR reference</a>
     */
    public static final Responsible PUBLISHER = new Responsible("publisher_name",
            null, "publisher_url", "publisher_email", null, Role.PUBLISHER);

    /**
     * The {@value} attribute name for the scientific project that produced the data
     * (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with the {@code "project"} {@link KeywordType}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#project_Attribute">UCAR reference</a>
     */
    public static final String PROJECT = "project";

    /**
     * The {@value} attribute name for the summary of the intentions with which the resource(s)
     * was developed.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getPurpose() purpose}</li></ul>
     */
    public static final String PURPOSE = "purpose";

    /**
     * The {@value} attribute name for bibliographical references.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getOtherCitationDetails() otherCitationDetails}</li></ul>
     */
    public static final String REFERENCES = "references";

    /**
     * The {@value} attribute name for a textual description of the processing (or quality control)
     * level of the data.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getContentInfo() contentInfo} /
     * {@link ImageDescription#getProcessingLevelCode() processingLevelCode}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#processing_level_Attribute">UCAR reference</a>
     */
    public static final String PROCESSING_LEVEL = "processing_level";

    /**
     * The {@value} attribute name for a place to acknowledge various type of support for
     * the project that produced this data (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCredits() credit}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#acknowledgement_Attribute">UCAR reference</a>
     */
    public static final String ACKNOWLEDGMENT = "acknowledgment";

    /**
     * The {@value} attribute name for a description of the restrictions to data access
     * and distribution (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getResourceConstraints() resourceConstraints} /
     * {@link LegalConstraints#getUseLimitations() useLimitation}</li></ul>
     *
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#license_Attribute">UCAR reference</a>
     */
    public static final String LICENSE = "license";

    /**
     * The {@value} attribute name for the access constraints applied to assure the protection of
     * privacy or intellectual property. Typical values are {@code "copyright"}, {@code "patent"},
     * {@code "patent pending"}, {@code "trademark"}, {@code "license"},
     * {@code "intellectual property rights"} or {@code "restricted"}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getResourceConstraints() resourceConstraints} /
     * {@link LegalConstraints#getAccessConstraints() accessConstraints}</li></ul>
     *
     * @see Restriction
     */
    public static final String ACCESS_CONSTRAINT = "acces_constraint";

    /**
     * The {@value} attribute name for an identifier of the geographic area.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getExtents() extent} /
     * {@link Extent#getGeographicElements() geographicElement} /
     * {@link GeographicDescription#getGeographicIdentifier() geographicIdentifier}</li></ul>
     */
    public static final String GEOGRAPHIC_IDENTIFIER = "geographic_identifier";

    /**
     * Holds the attribute names describing a simple latitude, longitude, and vertical bounding box.
     * In the following table, the header lists the constants defined in the {@link AttributeNames}
     * class and the other cells give the values assigned in this class fields for those constants.
     *
     * <table class="sis">
     * <caption>Names of NetCDF attributes describing an extent</caption>
     * <tr>
     *   <th            >Field in this class</th>
     *   <th class="sep">{@link AttributeNames#LATITUDE  LATITUDE}</th>
     *   <th            >{@link AttributeNames#LONGITUDE LONGITUDE}</th>
     *   <th            >{@link AttributeNames#VERTICAL  VERTICAL}</th>
     *   <th            >{@link AttributeNames#TIME      TIME}</th>
     * </tr><tr>
     *   <td            >{@link #MINIMUM}</td>
     *   <td class="sep">{@code "geospatial_lat_min"}</td>
     *   <td            >{@code "geospatial_lon_min"}</td>
     *   <td            >{@code "geospatial_vertical_min"}</td>
     *   <td            >{@code "time_coverage_start"}</td>
     * </tr><tr>
     *   <td            >{@link #MAXIMUM}</td>
     *   <td class="sep">{@code "geospatial_lat_max"}</td>
     *   <td            >{@code "geospatial_lon_max"}</td>
     *   <td            >{@code "geospatial_vertical_max"}</td>
     *   <td            >{@code "time_coverage_end"}</td>
     * </tr><tr>
     *   <td            >{@link #SPAN}</td>
     *   <td class="sep"></td>
     *   <td            ></td>
     *   <td            ></td>
     *   <td            >{@code "time_coverage_duration"}</td>
     * </tr><tr>
     *   <td            >{@link #RESOLUTION}</td>
     *   <td class="sep">{@code "geospatial_lat_resolution"}</td>
     *   <td            >{@code "geospatial_lon_resolution"}</td>
     *   <td            >{@code "geospatial_vertical_resolution"}</td>
     *   <td            >{@code "time_coverage_resolution"}</td>
     * </tr><tr>
     *   <td            >{@link #UNITS}</td>
     *   <td class="sep">{@code "geospatial_lat_units"}</td>
     *   <td            >{@code "geospatial_lon_units"}</td>
     *   <td            >{@code "geospatial_vertical_units"}</td>
     *   <td            >{@code "time_coverage_units"}</td>
     * </tr><tr>
     *   <td            >{@link #POSITIVE}</td>
     *   <td class="sep"></td>
     *   <td            ></td>
     *   <td            >{@code "geospatial_vertical_positive"}</td>
     *   <td></td>
     * </tr><tr>
     *   <td            >{@link #DEFAULT_NAME_TYPE}</td>
     *   <td class="sep">{@link DimensionNameType#ROW}</td>
     *   <td            >{@link DimensionNameType#COLUMN}</td>
     *   <td            >{@link DimensionNameType#VERTICAL}</td>
     *   <td            >{@link DimensionNameType#TIME}</td>
     * </tr></table>
     *
     * <div class="note"><b>Note:</b>
     * The member names in this class are upper-cases because they should be considered as constants.
     * For example {@code AttributeNames.LATITUDE.MINIMUM} maps exactly to the {@code "geospatial_lat_min"}
     * string and nothing else. A lower-case {@code minimum} member name could be misleading since it would
     * suggest that the field contains the actual name value rather than the key by which the value is
     * identified in a NetCDF file.</div>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see org.apache.sis.storage.netcdf.AttributeNames.Responsible
     */
    public static class Dimension implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 5063525623830032591L;

        /**
         * The attribute name for the minimal value of the bounding box (<em>Recommended</em>).
         * Possible values are {@code "geospatial_lat_min"}, {@code "geospatial_lon_min"},
         * {@code "geospatial_vertical_min"} and {@code "time_coverage_start"}.
         */
        public final String MINIMUM;

        /**
         * The attribute name for the maximal value of the bounding box (<em>Recommended</em>).
         * Possible values are {@code "geospatial_lat_max"}, {@code "geospatial_lon_max"},
         * {@code "geospatial_vertical_max"} and {@code "time_coverage_end"}.
         */
        public final String MAXIMUM;

        /**
         * The attribute name for the difference between the minimal and maximal values.
         * Possible value is {@code "time_coverage_duration"}.
         */
        public final String SPAN;

        /**
         * The attribute name for a further refinement of the geospatial bounding box
         * (<em>Suggested</em>). Possible values are {@code "geospatial_lat_resolution"},
         * {@code "geospatial_lon_resolution"}, {@code "geospatial_vertical_resolution"}
         * and {@code "time_coverage_resolution"}.
         */
        public final String RESOLUTION;

        /**
         * The attribute name for the bounding box units of measurement.
         * Possible values are {@code "geospatial_lat_units"}, {@code "geospatial_lon_units"},
         * {@code "geospatial_vertical_units"} and {@code "time_coverage_units"}.
         */
        public final String UNITS;

        /**
         * The attribute name for indicating which direction is positive (<em>Suggested</em>).
         * Possible value is {@code "geospatial_vertical_positive"}.
         */
        public final String POSITIVE;

        /**
         * The default ISO-19115 dimension name type, or {@code null} if none.
         * By default, {@link DimensionNameType#COLUMN} is associated to longitudes and {@link DimensionNameType#ROW}
         * to latitudes since geographic maps in NetCDF files are typically shown horizontally.
         *
         * <p>The default associations may not be always correct since the columns and rows can be anything.
         * Strictly speaking, the dimension name types shall be associated to the <em>grid axes</em> rather
         * than the <em>coordinate system axes</em>. However the default association is correct in the common case
         * (for NetCDF files) where there is no axis swapping in the <cite>grid to CRS</cite> conversion.</p>
         */
        public final DimensionNameType DEFAULT_NAME_TYPE;

        /**
         * Creates a new set of attribute names.
         *
         * @param type       The default ISO-19115 dimension name type, or {@code null} if none.
         * @param min        The attribute name for the minimal value of the bounding box.
         * @param max        The attribute name for the maximal value of the bounding box.
         * @param span       The attribute name for the difference between the minimal and maximal values.
         * @param resolution The attribute name for a further refinement of the geospatial bounding box.
         * @param units      The attribute name for the bounding box units of measurement.
         * @param positive   The attribute name for indicating which direction is positive.
         */
        public Dimension(final DimensionNameType type, final String min, final String max, final String span,
                final String resolution,final String units, final String positive)
        {
            DEFAULT_NAME_TYPE = type;
            MINIMUM           = min;
            MAXIMUM           = max;
            SPAN              = span;
            RESOLUTION        = resolution;
            UNITS             = units;
            POSITIVE          = positive;
        }
    }

    /**
     * The set of attribute names for the minimal and maximal latitudes of the bounding box,
     * resolution and units. Latitudes are assumed to be in decimal degrees north, unless a
     * units attribute is specified.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getExtents() extent} /
     * {@link Extent#getGeographicElements() geographicElement} /
     * {@link GeographicBoundingBox#getSouthBoundLatitude() southBoundLatitude} or
     * {@link GeographicBoundingBox#getNorthBoundLatitude() northBoundLatitude}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getSpatialRepresentationInfo() spatialRepresentationInfo} /
     * {@link GridSpatialRepresentation#getAxisDimensionProperties() axisDimensionProperties} /
     * {@link org.opengis.metadata.spatial.Dimension#getResolution() resolution}</li></ul>
     *
     * @see #LONGITUDE
     * @see #VERTICAL
     * @see #TIME
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lat_min_Attribute">UCAR reference</a>
     */
    public static final Dimension LATITUDE = new Dimension(DimensionNameType.ROW,
            "geospatial_lat_min", "geospatial_lat_max", null,
            "geospatial_lat_resolution", "geospatial_lat_units", null);

    /**
     * The set of attribute names for the minimal and maximal longitudes of the bounding box,
     * resolution and units. Longitudes are assumed to be in decimal degrees east, unless a
     * units attribute is specified.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getExtents() extent} /
     * {@link Extent#getGeographicElements() geographicElement} /
     * {@link GeographicBoundingBox#getWestBoundLongitude() westBoundLongitude} or
     * {@link GeographicBoundingBox#getEastBoundLongitude() eastBoundLongitude}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getSpatialRepresentationInfo() spatialRepresentationInfo} /
     * {@link GridSpatialRepresentation#getAxisDimensionProperties() axisDimensionProperties} /
     * {@link org.opengis.metadata.spatial.Dimension#getResolution() resolution}</li></ul>
     *
     * @see #LATITUDE
     * @see #VERTICAL
     * @see #TIME
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lon_min_Attribute">UCAR reference</a>
     */
    public static final Dimension LONGITUDE = new Dimension(DimensionNameType.COLUMN,
            "geospatial_lon_min", "geospatial_lon_max", null,
            "geospatial_lon_resolution", "geospatial_lon_units", null);

    /**
     * The set of attribute names for the minimal and maximal elevations of the bounding box,
     * resolution and units. Elevations are assumed to be in metres above the ground, unless a
     * units attribute is specified.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getExtents() extent} /
     * {@link Extent#getVerticalElements() verticalElement} /
     * {@link VerticalExtent#getMinimumValue() minimumValue} or
     * {@link VerticalExtent#getMaximumValue() maximumValue}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getSpatialRepresentationInfo() spatialRepresentationInfo} /
     * {@link GridSpatialRepresentation#getAxisDimensionProperties() axisDimensionProperties} /
     * {@link org.opengis.metadata.spatial.Dimension#getResolution() resolution}</li></ul>
     *
     * @see #LATITUDE
     * @see #LONGITUDE
     * @see #TIME
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_vertical_min_Attribute">UCAR reference</a>
     */
    public static final Dimension VERTICAL = new Dimension(DimensionNameType.VERTICAL,
            "geospatial_vertical_min", "geospatial_vertical_max", null,
            "geospatial_vertical_resolution", "geospatial_vertical_units", "geospatial_vertical_positive");

    /**
     * The set of attribute names for the start and end times of the bounding box, resolution and
     * units.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getExtents() extent} /
     * {@link Extent#getTemporalElements() temporalElement} /
     * {@link TemporalExtent#getExtent() extent}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getSpatialRepresentationInfo() spatialRepresentationInfo} /
     * {@link GridSpatialRepresentation#getAxisDimensionProperties() axisDimensionProperties} /
     * {@link org.opengis.metadata.spatial.Dimension#getResolution() resolution}</li></ul>
     *
     * @see #LATITUDE
     * @see #LONGITUDE
     * @see #VERTICAL
     * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#time_coverage_start_Attribute">UCAR reference</a>
     */
    public static final Dimension TIME = new Dimension(DimensionNameType.TIME,
            "time_coverage_start", "time_coverage_end", "time_coverage_duration",
            "time_coverage_resolution", "time_coverage_units", null);

    /**
     * The {@value} attribute name for the designation associated with a range element.
     * This attribute can be associated to {@linkplain VariableSimpleIF variables}.
     * If specified, they shall be one flag name for each {@linkplain #FLAG_MASKS flag mask},
     * {@linkplain #FLAG_VALUES flag value} and {@linkplain #FLAG_MEANINGS flag meaning}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getContentInfo() contentInfo} /
     * {@link CoverageDescription#getRangeElementDescriptions() rangeElementDescription} /
     * {@link RangeElementDescription#getName() name}</li></ul>
     */
    public static final String FLAG_NAMES = "flag_names";

    /**
     * The {@value} attribute name for bitmask to apply on sample values before to compare
     * them to the {@linkplain #FLAG_VALUES flag values}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getContentInfo() contentInfo} /
     * {@link CoverageDescription#getRangeElementDescriptions() rangeElementDescription} /
     * {@link RangeElementDescription#getRangeElements() rangeElement}</li></ul>
     */
    public static final String FLAG_MASKS = "flag_masks";

    /**
     * The {@value} attribute name for sample values to be flagged. The {@linkplain #FLAG_MASKS
     * flag masks}, flag values and {@linkplain #FLAG_MEANINGS flag meaning} attributes, used
     * together, describe a blend of independent boolean conditions and enumerated status codes.
     * A flagged condition is identified by a bitwise AND of the variable value and each flag masks
     * value; a result that matches the flag values value indicates a true condition.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getContentInfo() contentInfo} /
     * {@link CoverageDescription#getRangeElementDescriptions() rangeElementDescription} /
     * {@link RangeElementDescription#getRangeElements() rangeElement}</li></ul>
     */
    public static final String FLAG_VALUES = "flag_values";

    /**
     * The {@value} attribute name for the meaning of {@linkplain #FLAG_VALUES flag values}.
     * Each flag values and flag masks must coincide with a flag meanings.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getContentInfo() contentInfo} /
     * {@link CoverageDescription#getRangeElementDescriptions() rangeElementDescription} /
     * {@link RangeElementDescription#getDefinition() definition}</li></ul>
     */
    public static final String FLAG_MEANINGS = "flag_meanings";

    /**
     * For subclass constructors only. {@code AttributeNames} may be sub-classed by communities
     * defining domain-specific attributes in addition to the ones defined by the CF convention.
     */
    protected AttributeNames() {
    }
}
