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
 * All imports below except "CF" and "ACDD" are for javadoc only.
 * The "CF" and "ACDD" imports are used only for its static final String constants,
 * which are inlined by javac. Consequently, the compiled file of this class should
 * have no dependency to the UCAR packages.
 */
import java.io.Serializable;
import ucar.nc2.NetcdfFile;             // For Javadoc only.
import ucar.nc2.VariableSimpleIF;       // For Javadoc only.
import ucar.nc2.constants.CF;           // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.ACDD;         // idem
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.acquisition.*;
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
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.BoundingPolygon;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;


/**
 * Name of attributes used in the mapping from/to netCDF metadata to ISO 19115 metadata.
 * The attributes recognized by SIS are listed below:
 *
 * <blockquote><table class="compact">
 * <caption>List of all netCDF attributes mapped by Apache SIS to ISO 19115 metadata</caption>
 * <tr style="vertical-align:top"><td style="width: 25%">
 * {@value     #ACCESS_CONSTRAINT}<br>
 * {@value     #ACKNOWLEDGEMENT}<br>
 * {@value     #COMMENT}<br>
 * {@linkplain #CONTRIBUTOR "contributor_email"}<br>
 * {@linkplain #CONTRIBUTOR "contributor_name"}<br>
 * {@linkplain #CONTRIBUTOR "contributor_role"}<br>
 * {@linkplain #CONTRIBUTOR "contributor_url"}<br>
 * {@linkplain #CREATOR     "creator_email"}<br>
 * {@linkplain #CREATOR     "creator_name"}<br>
 * {@linkplain #CREATOR     "creator_type"}<br>
 * {@linkplain #CREATOR     "creator_url"}<br>
 * {@value     #DATA_TYPE}<br>
 * {@value     #DATE_CREATED}<br>
 * {@value     #DATE_ISSUED}<br>
 * {@value     #METADATA_MODIFIED}<br>
 * {@value     #DATE_MODIFIED}<br>
 * {@value     #FLAG_MASKS}<br>
 * {@value     #FLAG_MEANINGS}<br>
 * {@value     #FLAG_NAMES}<br>
 * {@value     #FLAG_VALUES}<br>
 * {@linkplain #TITLE "full_name"}<br>
 * </td><td style="width: 25%">
 * {@linkplain #GEOGRAPHIC_IDENTIFIER "geographic_identifier"}<br>
 * {@value     #GEOSPATIAL_BOUNDS}<br>
 * {@linkplain #GEOSPATIAL_BOUNDS "geospatial_bounds_crs"}<br>
 * {@linkplain #GEOSPATIAL_BOUNDS "geospatial_bounds_vertical_crs"}<br>
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
 * {@value     #HISTORY}<br>
 * {@linkplain #IDENTIFIER "id"}<br>
 * {@linkplain #CREATOR    "institution"}<br>
 * </td><td style="width: 25%">
 * {@linkplain #KEYWORDS "keywords"}<br>
 * {@linkplain #KEYWORDS "keywords_vocabulary"}<br>
 * {@value     #LICENSE}<br>
 * {@value     #METADATA_CREATION}<br>
 * {@value     #METADATA_LINK}<br>
 * {@linkplain #TITLE "name"}<br>
 * {@value     #PROCESSING_LEVEL}<br>
 * {@value     #PRODUCT_VERSION}<br>
 * {@linkplain #PROGRAM   "program"}<br>
 * {@value     #PROJECT}<br>
 * {@linkplain #PUBLISHER "publisher_email"}<br>
 * {@linkplain #PUBLISHER "publisher_name"}<br>
 * {@linkplain #PUBLISHER "publisher_type"}<br>
 * {@linkplain #PUBLISHER "publisher_url"}<br>
 * {@value     #PURPOSE}<br>
 * {@value     #REFERENCES}<br>
 * {@value     #SOURCE}<br>
 * {@linkplain #STANDARD_NAME "standard_name"}<br>
 * {@linkplain #STANDARD_NAME "standard_name_vocabulary"}<br>
 * {@value     #SUMMARY}<br>
 * </td><td style="width: 25%">
 * {@linkplain #TIME "time_coverage_duration"}<br>
 * {@linkplain #TIME "time_coverage_end"}<br>
 * {@linkplain #TIME "time_coverage_resolution"}<br>
 * {@linkplain #TIME "time_coverage_start"}<br>
 * {@linkplain #TIME "time_coverage_units"}<br>
 * {@value     #TITLE}<br>
 * {@value     #TOPIC_CATEGORY}<br>
 * </td></tr></table></blockquote>
 *
 * <h2>References</h2>
 * <ul>
 *   <li><a href="https://wiki.esipfed.org/Category:Attribute_Conventions_Dataset_Discovery">NetCDF
 *       Attribute Convention for Dataset Discovery</a> wiki</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#title">ESIP reference</a>
     */
    public static final String TITLE = ACDD.title;

    /**
     * The {@value} attribute name for a paragraph describing the dataset
     * (<em>Highly Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getAbstract() abstract}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#summary">ESIP reference</a>
     */
    public static final String SUMMARY = ACDD.summary;

    /**
     * Holds the attribute names describing a term together with a vocabulary (or naming authority).
     * A term is a word or expression having a precise meaning in a domain identified by the vocabulary.
     * In the following table, the header lists the constants defined in the {@link AttributeNames}
     * class and the other cells give the values assigned in this class fields for those constants.
     *
     * <table class="sis">
     * <caption>Names of netCDF attributes describing a keyword</caption>
     * <tr><th>{@link AttributeNames}</th>                             <th>{@link #TEXT}</th>           <th>{@link #VOCABULARY}</th></tr>
     * <tr><td>{@link AttributeNames#IDENTIFIER    IDENTIFIER}</td>    <td>{@code "id"}</td>            <td>{@code "naming_authority"}</td></tr>
     * <tr><td>{@link AttributeNames#STANDARD_NAME STANDARD_NAME}</td> <td>{@code "standard_name"}</td> <td>{@code "standard_name_vocabulary"}</td></tr>
     * <tr><td>{@link AttributeNames#KEYWORDS      KEYWORDS}</td>      <td>{@code "keywords"}</td>      <td>{@code "keywords_vocabulary"}</td></tr>
     * <tr><td>{@link AttributeNames#PROGRAM       PROGRAM}</td>       <td>{@code "program"}</td>       <td></td></tr>
     * <tr><td>{@link AttributeNames#PLATFORM      PLATFORM}</td>      <td>{@code "platform"}</td>      <td>{@code "platform_vocabulary"}</td></tr>
     * <tr><td>{@link AttributeNames#INSTRUMENT    INSTRUMENT}</td>    <td>{@code "instrument"}</td>    <td>{@code "instrument_vocabulary"}</td></tr>
     * </table>
     *
     * <h2>Departure from conventions</h2>
     * The member names in this class are upper-cases because they should be considered as constants.
     * For example, {@code AttributeNames.KEYWORD.TEXT} maps exactly to the {@code "keywords"} string
     * and nothing else. A lower-case {@code text} member name could be misleading since it would
     * suggest that the field contains the actual text value rather than the key by which the value
     * is identified in a netCDF file.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.8
     * @since   0.8
     */
    public static class Term implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2625777878209548741L;

        /**
         * The keyword or the identifier code. Possible values for this field are
         * {@code "id"}, {@code "standard_name"}, {@code "keywords"}, {@code "program"},
         * {@code "platform"} or {@code "instrument"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
         * {@link Metadata#getIdentificationInfo() identificationInfo} /
         * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
         * {@link Keywords#getKeywords() keyword}</li>
         * <li>or {@link Identifier} / {@link Identifier#getCode() code}</li></ul>
         */
        public final String TEXT;

        /**
         * The vocabulary or identifier namespace, or {@code null} if none. Possible values for this field are
         * {@code "naming_authority"}, {@code "standard_name_vocabulary"}, {@code "keywords_vocabulary"},
         * {@code "platform_vocabulary"} or {@code "instrument_vocabulary"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
         * {@link Metadata#getIdentificationInfo() identificationInfo} /
         * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
         * {@link Keywords#getThesaurusName() thesaurusName} /
         * {@link Citation#getTitle() title}</li>
         * <li>or {@link Identifier} / {@link Identifier#getAuthority() authority} /
         * {@link Citation#getTitle() title}</li></ul>
         */
        public final String VOCABULARY;

        /**
         * Creates a new set of attribute names. Any argument can be {@code null} if not applicable.
         *
         * @param text        the keyword or the identifier code.
         * @param vocabulary  the vocabulary or identifier namespace.
         *
         * @since 0.8
         */
        public Term(final String text, final String vocabulary) {
            TEXT       = text;
            VOCABULARY = vocabulary;
        }
    }

    /**
     * The set of attribute names for an identifier (<em>Recommended</em>).
     * The combination of the {@code "naming_authority"} and the {@code "id"}
     * should be a globally unique identifier for the dataset.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getFileIdentifier() fileIdentifier}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getIdentifiers() identifier} /
     * {@link Identifier#getCode() code}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getIdentifiers() identifier} /
     * {@link Identifier#getAuthority() authority} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @see NetcdfFile#getId()
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#id">ESIP reference</a>
     */
    public static final Term IDENTIFIER = new Term(ACDD.id, ACDD.naming_authority);

    /**
     * The set of attribute names for a long descriptive name for the variable taken from a controlled
     * vocabulary of variable names. This is actually a {@linkplain VariableSimpleIF variable} attribute,
     * but sometimes appears also in {@linkplain NetcdfFile#findGlobalAttribute(String) global attributes}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with {@link KeywordType#THEME}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getThesaurusName() thesaurusName} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @see #KEYWORDS
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#standard_name">ESIP reference</a>
     */
    public static final Term STANDARD_NAME = new Term(CF.STANDARD_NAME, ACDD.standard_name_vocabulary);

    /**
     * The set of attribute names for a comma separated list of key words and phrases
     * (<em>Highly Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with {@link KeywordType#THEME}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getThesaurusName() thesaurusName} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @see #STANDARD_NAME
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#keywords">ESIP reference</a>
     */
    public static final Term KEYWORDS = new Term(ACDD.keywords, ACDD.keywords_vocabulary);

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#cdm_data_type">ESIP reference</a>
     */
    public static final String DATA_TYPE = ACDD.cdm_data_type;

    /**
     * The {@value} attribute name for providing an audit trail for modifications to the
     * original data (<em>Recommended</em>).
     * This is a character array with a line for each invocation of a program that has modified the dataset.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getResourceLineages() resourceLineage} /
     * {@link Lineage#getStatement() statement}</li></ul>
     *
     * <h4>Departure from convention</h4>
     * Located in "{@link Metadata#getDataQualityInfo() dataQualityInfo} /
     * {@link org.opengis.metadata.quality.DataQuality#getLineage() lineage}" instead of "{@code resourceLineage}"
     * in {@code UnidataDD2MI.xsl} file (retrieved in 2017).
     * See <a href="https://issues.apache.org/jira/browse/SIS-361">SIS-361</a>.
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#history">ESIP reference</a>
     */
    public static final String HISTORY = ACDD.history;

    /**
     * The {@value} attribute name for the method of production of the original data (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getResourceLineages() resourceLineage} /
     * {@link Lineage#getSources() source} /
     * {@link Source#getDescription() description}</li></ul>
     *
     * <h4>Departure from convention</h4>
     * Located in "{@link Metadata#getDataQualityInfo() dataQualityInfo} /
     * {@link org.opengis.metadata.quality.DataQuality#getLineage() lineage}" instead of "{@code resourceLineage}"
     * in {@code UnidataDD2MI.xsl} file (retrieved in 2017).
     * See <a href="https://issues.apache.org/jira/browse/SIS-361">SIS-361</a>.
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#source">ESIP reference</a>
     *
     * @since 0.8
     */
    public static final String SOURCE = "source";

    /**
     * The {@value} attribute name for miscellaneous information about the data
     * (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getSupplementalInformation() supplementalInformation}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#comment">ESIP reference</a>
     */
    public static final String COMMENT = ACDD.comment;

    /**
     * The {@value} attribute name for the date on which the metadata was created
     * (<em>Suggested</em>). This is actually defined in the "{@code NCISOMetadata}"
     * subgroup.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@code dateInfo}
     * {@link CitationDate#getDate() date} with {@link DateType#CREATION}</li></ul>
     */
    public static final String METADATA_CREATION = "metadata_creation";

    /**
     * The {@value} attribute name for the date on which the metadata has been modified
     * (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@code dateInfo}
     * {@link CitationDate#getDate() date} with {@link DateType#REVISION}</li></ul>
     *
     * @since 0.8
     */
    public static final String METADATA_MODIFIED = "date_metadata_modified";

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#date_created">ESIP reference</a>
     */
    public static final String DATE_CREATED = ACDD.date_created;

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#date_modified">ESIP reference</a>
     */
    public static final String DATE_MODIFIED = ACDD.date_modified;

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#date_issued">ESIP reference</a>
     */
    public static final String DATE_ISSUED = "date_issued";

    /**
     * The {@value} attribute for version identifier of the data file or product as assigned by the data creator
     * (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getEdition() edition}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#product_version">ESIP reference</a>
     */
    public static final String PRODUCT_VERSION = "product_version";

    /**
     * The set of attribute names for the overarching program(s) of which the dataset is a part (<em>Suggested</em>).
     * Examples: "GHRSST", "NOAA CDR", "NASA EOS", "JPSS", "GOES-R".
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getAcquisitionInformation() acquisitionInformation} /
     * {@link AcquisitionInformation#getOperations() operation} /
     * {@link Operation#getIdentifier() identifier} /
     * {@link Identifier#getCode() code}</li></ul>
     *
     * <p><b>This attribute is not yet read by {@link NetcdfStore}</b>,
     * because we are not sure what would be the most appropriate ISO 19115 location.
     * The above-cited location is experimental and may change in any future Apache SIS version.</p>
     *
     * @see #PROJECT
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#program">ESIP reference</a>
     *
     * @since 0.8
     */
    public static final Term PROGRAM = new Term("program", null);

    /**
     * The set of attribute names for the platform(s) that supported the sensors used to create the resource(s).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getAcquisitionInformation() acquisitionInformation} /
     * {@link AcquisitionInformation#getPlatforms() platform} /
     * {@link Platform#getIdentifier() identifier} /
     * {@link Identifier#getCode() code}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getAcquisitionInformation() acquisitionInformation} /
     * {@link AcquisitionInformation#getPlatforms() platform} /
     * {@link Platform#getIdentifier() identifier} /
     * {@link Identifier#getAuthority() authority} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @since 0.8
     */
    public static final Term PLATFORM = new Term("platform", "platform_vocabulary");

    /**
     * The set of attribute names for the contributing instrument(s) or sensor(s) used to create the resource(s).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getAcquisitionInformation() acquisitionInformation} /
     * {@link AcquisitionInformation#getPlatforms() platform} /
     * {@link Platform#getInstruments() instrument} /
     * {@link Instrument#getIdentifier() identifier} /
     * {@link Identifier#getCode() code}</li>
     * <li>{@link Metadata} /
     * {@link Metadata#getAcquisitionInformation() acquisitionInformation} /
     * {@link AcquisitionInformation#getPlatforms() platform} /
     * {@link Platform#getInstruments() instrument} /
     * {@link Instrument#getIdentifier() identifier} /
     * {@link Identifier#getAuthority() authority} /
     * {@link Citation#getTitle() title}</li></ul>
     *
     * @since 0.8
     */
    public static final Term INSTRUMENT = new Term("instrument", "instrument_vocabulary");

    /**
     * Holds the attribute names describing a responsible party.
     * In the following table, the header lists the constants defined in the {@link AttributeNames}
     * class and the other cells give the values assigned in this class fields for those constants.
     *
     * <table class="sis">
     * <caption>Names of netCDF attributes describing a responsible party</caption>
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
     *   <td            >{@link #TYPE}</td>
     *   <td class="sep">{@code "creator_type"}</td>
     *   <td            ></td>
     *   <td            >{@code "publisher_type"}</td>
     * </tr><tr>
     *   <td            >{@link #INSTITUTION}</td>
     *   <td class="sep">{@code "creator_institution"}</td>
     *   <td            ></td>
     *   <td            >{@code "publisher_institution"}</td>
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
     * <h2>Departure from conventions</h2>
     * The member names in this class are upper-cases because they should be considered as constants.
     * For example, {@code AttributeNames.CREATOR.EMAIL} maps exactly to the {@code "creator_email"} string
     * and nothing else. A lower-case {@code email} member name could be misleading since it would suggest
     * that the field contains the actual name value rather than the key by which the value is identified
     * in a netCDF file.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.8
     *
     * @see org.apache.sis.storage.netcdf.AttributeNames.Dimension
     *
     * @since 0.3
     */
    public static class Responsible implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2680152633273321012L;

        /**
         * The attribute name for the responsible's name. Possible values for this field are
         * {@code "creator_name"}, {@code "contributor_name"} or {@code "publisher_name"}.
         *
         * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link ResponsibleParty} /
         * {@link ResponsibleParty#getIndividualName() individualName}</li></ul>
         */
        public final String NAME;

        /**
         * The attribute name for the responsible's type. Possible values for this field are
         * {@code "creator_type"} or {@code "publisher_type"}. Possible values in a netCDF file
         * are {@code "person"}, {@code "group"}, {@code "institution"} or {@code "position"}.
         */
        public final String TYPE;

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
         * @param name         the attribute name for the responsible's name.
         * @param type         the attribute name for the responsible party type.
         * @param institution  the attribute name for the responsible's institution.
         * @param url          the attribute name for the responsible's URL.
         * @param email        the attribute name for the responsible's email address.
         * @param role         the attribute name for the responsible party role.
         * @param defaultRole  the role to use as a fallback if no attribute value is associated to the {@code role} key.
         *
         * @since 0.8
         */
        public Responsible(final String name, final String type, final String institution, final String url,
                final String email, final String role, final Role defaultRole)
        {
            NAME         = name;
            TYPE         = type;
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
     * {@link DataIdentification#getCitation() citation} /
     * {@link Citation#getCitedResponsibleParties() citedResponsibleParty} with {@link Role#ORIGINATOR}</li></ul>
     *
     * @see #CONTRIBUTOR
     * @see #PUBLISHER
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#creator_name">ESIP reference</a>
     */
    public static final Responsible CREATOR = new Responsible(ACDD.creator_name, "creator_type",
            "creator_institution", ACDD.creator_url, ACDD.creator_email, null, Role.ORIGINATOR);

    /**
     * The set of attribute names for the contributor (<em>Suggested</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCitation() citation}</li></ul>
     *
     * @see #CREATOR
     * @see #PUBLISHER
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#contributor_name">ESIP reference</a>
     */
    public static final Responsible CONTRIBUTOR = new Responsible("contributor_name", null,
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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#publisher_name">ESIP reference</a>
     */
    public static final Responsible PUBLISHER = new Responsible(ACDD.publisher_name, "publisher_type",
            ACDD.publisher_institution, ACDD.publisher_url, ACDD.publisher_email, null, Role.PUBLISHER);

    /**
     * The {@value} attribute name for the scientific project that produced the data
     * (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getDescriptiveKeywords() descriptiveKeywords} /
     * {@link Keywords#getKeywords() keyword} with the {@code "project"} {@link KeywordType}</li></ul>
     *
     * @see #PROGRAM
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#project">ESIP reference</a>
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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#processing_level">ESIP reference</a>
     */
    public static final String PROCESSING_LEVEL = ACDD.processing_level;

    /**
     * The {@value} attribute name for a place to acknowledge various type of support for
     * the project that produced this data (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getCredits() credit}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#acknowledgement">ESIP reference</a>
     *
     * @since 0.8
     */
    public static final String ACKNOWLEDGEMENT = ACDD.acknowledgement;

    /**
     * The {@value} attribute name for a description of the restrictions to data access
     * and distribution (<em>Recommended</em>).
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getResourceConstraints() resourceConstraints} /
     * {@link LegalConstraints#getUseLimitations() useLimitation}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#license">ESIP reference</a>
     */
    public static final String LICENSE = ACDD.license;

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
     * Data's 2D or 3D geospatial extent in OGC's Well-Known Text (WKT) geometry format.
     * The Coordinate Reference System is given by {@code "geospatial_bounds_crs"},
     * possibly completed by {@code "geospatial_bounds_vertical_crs"}.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@link Metadata#getIdentificationInfo() identificationInfo} /
     * {@link DataIdentification#getExtents() extent} /
     * {@link Extent#getGeographicElements() geographicElement} /
     * {@link BoundingPolygon#getPolygons() polygon}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#geospatial_bounds">ESIP reference</a>
     *
     * @since 0.8
     */
    public static final String GEOSPATIAL_BOUNDS = "geospatial_bounds";

    /**
     * Holds the attribute names describing a simple latitude, longitude, and vertical bounding box.
     * In the following table, the header lists the constants defined in the {@link AttributeNames}
     * class and the other cells give the values assigned in this class fields for those constants.
     *
     * <table class="sis">
     * <caption>Names of netCDF attributes describing an extent</caption>
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
     * <h2>Departure from conventions</h2>
     * The member names in this class are upper-cases because they should be considered as constants.
     * For example, {@code AttributeNames.LATITUDE.MINIMUM} maps exactly to the {@code "geospatial_lat_min"}
     * string and nothing else. A lower-case {@code minimum} member name could be misleading since it would
     * suggest that the field contains the actual latitude value rather than the key by which the value is
     * identified in a netCDF file.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.3
     *
     * @see org.apache.sis.storage.netcdf.AttributeNames.Responsible
     *
     * @since 0.3
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
         * The default ISO 19115 dimension name type, or {@code null} if none.
         * By default, {@link DimensionNameType#COLUMN} is associated to longitudes and {@link DimensionNameType#ROW}
         * to latitudes since geographic maps in netCDF files are typically shown horizontally.
         *
         * <p>The default associations may not be always correct since the columns and rows can be anything.
         * Strictly speaking, the dimension name types shall be associated to the <em>grid axes</em> rather
         * than the <em>coordinate system axes</em>. However, the default association is correct in the common case
         * (for netCDF files) where there is no axis swapping in the <i>grid to CRS</i> conversion.</p>
         */
        public final DimensionNameType DEFAULT_NAME_TYPE;

        /**
         * Creates a new set of attribute names.
         *
         * @param type        the default ISO 19115 dimension name type, or {@code null} if none.
         * @param min         the attribute name for the minimal value of the bounding box.
         * @param max         the attribute name for the maximal value of the bounding box.
         * @param span        the attribute name for the difference between the minimal and maximal values.
         * @param resolution  the attribute name for a further refinement of the geospatial bounding box.
         * @param units       the attribute name for the bounding box units of measurement.
         * @param positive    the attribute name for indicating which direction is positive.
         */
        public Dimension(final DimensionNameType type, final String min, final String max, final String span,
                final String resolution, final String units, final String positive)
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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#geospatial_lat_min">ESIP reference</a>
     */
    public static final Dimension LATITUDE = new Dimension(DimensionNameType.ROW,
            ACDD.LAT_MIN, ACDD.LAT_MAX, null, ACDD.LAT_RESOLUTION, ACDD.LAT_UNITS, null);

    /**
     * The standard attribute names for latitudes, followed by alternatives that are sometime observed.
     * The alternatives are tried only if no latitude or longitude <abbr>CR</abbr> attributes was found.
     * Examples of sources of alternatives: Mercator data from Copernicus.
     */
    static final Dimension[] LATITUDE_ALTERNATIVES = {
        LATITUDE,
        new Dimension(DimensionNameType.ROW, "latitude_min", "latitude_max", null, null, null, null)
    };

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#geospatial_lon_min">ESIP reference</a>
     */
    public static final Dimension LONGITUDE = new Dimension(DimensionNameType.COLUMN,
            ACDD.LON_MIN, ACDD.LON_MAX, null, ACDD.LON_RESOLUTION, ACDD.LON_UNITS, null);

    /**
     * The standard attribute names for longitudes, followed by alternatives that are sometime observed.
     * The alternatives are tried only if no latitude or longitude <abbr>CR</abbr> attributes was found.
     * Examples of sources of alternatives: Mercator data from Copernicus.
     */
    static final Dimension[] LONGITUDE_ALTERNATIVES = {
        LONGITUDE,
        new Dimension(DimensionNameType.COLUMN, "longitude_min", "longitude_max", null, null, null, null)
    };

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#geospatial_vertical_min">ESIP reference</a>
     */
    public static final Dimension VERTICAL = new Dimension(DimensionNameType.VERTICAL,
            ACDD.VERT_MIN, ACDD.VERT_MAX, null, ACDD.VERT_RESOLUTION, ACDD.VERT_UNITS, ACDD.VERT_IS_POSITIVE);

    /**
     * The standard attribute names for heights, followed by alternatives that are sometime observed.
     * The alternatives are tried only if no latitude, longitude or heights <abbr>CR</abbr> attributes was found.
     * Examples of sources of alternatives: Mercator data from Copernicus.
     */
    static final Dimension[] VERTICAL_ALTERNATIVES = {
        VERTICAL,
        new Dimension(DimensionNameType.VERTICAL, "z_min", "z_max", null, null, null, null)
    };

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
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#time_coverage_start">ESIP reference</a>
     */
    public static final Dimension TIME = new Dimension(DimensionNameType.TIME,
            ACDD.TIME_START, ACDD.TIME_END, ACDD.TIME_DURATION, ACDD.TIME_RESOLUTION, "time_coverage_units", null);

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
     * The {@value} attribute name for sample values to be flagged. The {@linkplain #FLAG_MASKS flag masks},
     * flag values and {@linkplain #FLAG_MEANINGS flag meaning} attributes, used together, describe a blend
     * of independent boolean conditions and enumerated status codes.
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
     * The {@value} attribute name for a URL that gives the location of more complete metadata.
     * For example, it may be the URL to an ISO 19115 metadata in XML format.
     *
     * <p><b>Path in ISO 19115:</b></p> <ul><li>{@link Metadata} /
     * {@code metadataLinkage} /
     * {@link OnlineResource#getLinkage() linkage}</li></ul>
     *
     * @see <a href="http://wiki.esipfed.org/index.php/Attribute_Convention_for_Data_Discovery#metadata_link">ESIP reference</a>
     *
     * @since 0.8
     */
    public static final String METADATA_LINK = "metadata_link";

    /**
     * For subclass constructors only. {@code AttributeNames} may be sub-classed by communities
     * defining domain-specific attributes in addition to the ones defined by the CF convention.
     */
    protected AttributeNames() {
    }
}
