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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collection;
import java.io.IOException;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.converter.UnitConverter;
import javax.measure.converter.ConversionException;

import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.spatial.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.referencing.crs.VerticalCRS;

import org.opengis.util.CodeList;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.extent.*;
import org.apache.sis.metadata.iso.spatial.*;
import org.apache.sis.metadata.iso.content.*;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.distribution.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.internal.netcdf.Axis;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.GridGeometry;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Units;

// The following dependency is used only for static final String constants.
// Consequently the compiled class files should not have this dependency.
import ucar.nc2.constants.CF;

import static java.util.Collections.singleton;
import static org.apache.sis.storage.netcdf.AttributeNames.*;
import static org.apache.sis.internal.util.CollectionsExt.first;


/**
 * Mapping from NetCDF metadata to ISO 19115-2 metadata. The {@link String} constants declared in
 * the {@linkplain AttributeNames parent class} are the name of attributes examined by this class.
 * The current implementation searches the attribute values in the following places, in that order:
 *
 * <ol>
 *   <li>{@code "NCISOMetadata"} group</li>
 *   <li>{@code "CFMetadata"} group</li>
 *   <li>Global attributes</li>
 *   <li>{@code "THREDDSMetadata"} group</li>
 * </ol>
 *
 * The {@code "CFMetadata"} group has precedence over the global attributes because the
 * {@linkplain #LONGITUDE longitude} and {@linkplain #LATITUDE latitude} resolutions are
 * often more accurate in that group.
 *
 * <div class="section">Known limitations</div>
 * <ul>
 *   <li>{@code "degrees_west"} and {@code "degrees_south"} units not correctly handled.</li>
 *   <li>Units of measurement not yet declared in the {@link Band} elements.</li>
 *   <li>{@link #FLAG_VALUES} and {@link #FLAG_MASKS} not yet included in the
 *       {@link RangeElementDescription} elements.</li>
 *   <li>Services (WMS, WCS, OPeNDAP, THREDDS) <i>etc.</i>) and transfer options not yet declared.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
final class MetadataReader {
    /**
     * Names of groups where to search for metadata, in precedence order.
     * The {@code null} value stands for global attributes.
     *
     * <p>REMINDER: if modified, update class javadoc too.</p>
     */
    private static final String[] SEARCH_PATH = {"NCISOMetadata", "CFMetadata", null, "THREDDSMetadata"};

    /**
     * Names of global attributes identifying services.
     */
    private static final String[] SERVICES = {"wms_service", "wcs_service"};

    /**
     * The string to use as a keyword separator. This separator is used for parsing the
     * {@value org.apache.sis.metadata.netcdf.AttributeNames#KEYWORDS} attribute value.
     * This is a regular expression.
     */
    private static final String KEYWORD_SEPARATOR = ",";

    /**
     * The vertical coordinate reference system to be given to the object created by {@link #createExtent()}.
     *
     * @todo Should be set to {@link org.apache.sis.referencing.crs.DefaultVerticalCRS#GEOIDAL_HEIGHT}
     *       after we ported the {@code sis-referencing} module.
     */
    private static final VerticalCRS VERTICAL_CRS = null;

    /**
     * The source of NetCDF attributes from which to infer ISO metadata.
     * This source is set at construction time.
     *
     * <p>This {@code MetadataReader} class does <strong>not</strong> close this source.
     * Closing this source after usage is the user responsibility.</p>
     */
    private final Decoder decoder;

    /**
     * The actual search path, as a subset of {@link #SEARCH_PATH} with only the name of the groups
     * which have been found in the NeCDF file.
     */
    private final String[] searchPath;

    /**
     * The name factory, created when first needed.
     *
     * The type is {@link NameFactory} on the JDK6 branch. However we have to force the SIS
     * implementation on the GeoAPI 3.0 branch because the older interface is missing a method.
     */
    private transient DefaultNameFactory nameFactory;

    /**
     * The contact, used at metadata creation time for avoiding to construct identical objects
     * more than once.
     *
     * <p>The point of contact is stored in the two following places. The semantic of those two
     * contacts is not strictly identical, but the distinction is not used in NetCDF file:</p>
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getContacts()}</li>
     *   <li>{@link DefaultDataIdentification#getPointOfContacts()}</li>
     * </ul>
     *
     * An object very similar is used as the creator. The point of contact and the creator
     * are often identical except for their role attribute.
     */
    private transient ResponsibleParty pointOfContact;

    /**
     * Creates a new <cite>NetCDF to ISO</cite> mapper for the given source.
     *
     * @param  decoder The source of NetCDF attributes.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    MetadataReader(final Decoder decoder) throws IOException {
        this.decoder = decoder;
        decoder.setSearchPath(SEARCH_PATH);
        searchPath = decoder.getSearchPath();
    }

    /**
     * Invoked when a non-fatal exception occurred while reading metadata.
     * This method will send a record to the registered listeners if any,
     * or will log the record otherwise.
     */
    private void warning(final Exception e) {
        decoder.listeners.warning(null, e);
    }

    /**
     * Returns the localized error resource bundle for the locale given by {@link #getLocale()}.
     *
     * @return The localized error resource bundle.
     */
    private Errors errors() {
        return Errors.getResources(decoder.listeners.getLocale());
    }

    /**
     * Reads the attribute value for the given name, then trims the leading and trailing spaces.
     * If the value is null, empty or contains only spaces, then this method returns {@code null}.
     */
    private String stringValue(final String name) throws IOException {
        String value = decoder.stringValue(name);
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            }
        }
        return value;
    }

    /**
     * Returns the given string as an {@code InternationalString} if non-null, or {@code null} otherwise.
     * This method does not trim leading or trailing spaces, since this is often already done by the caller.
     */
    private static InternationalString toInternationalString(final String value) {
        return (value != null) ? new SimpleInternationalString(value) : null;
    }

    /**
     * Returns the enumeration constant for the given name, or {@code null} if the given name is not recognized.
     * In the later case, this method emits a warning.
     */
    private <T extends Enum<T>> T forEnumName(final Class<T> enumType, final String name) {
        final T code = Types.forEnumName(enumType, name);
        if (code == null && name != null) {
            decoder.listeners.warning(errors().getString(Errors.Keys.UnknownEnumValue_2, enumType, name), null);
        }
        return code;
    }

    /**
     * Returns the code value for the given name, or {@code null} if the given name is not recognized.
     * In the later case, this method emits a warning.
     */
    private <T extends CodeList<T>> T forCodeName(final Class<T> codeType, final String name) {
        final T code = Types.forCodeName(codeType, name, false);
        if (code == null && name != null) {
            /*
             * CodeLists are not enums, but using the error message for enums is not completly wrong since
             * if we did not allowed CodeList to create new elements, then we are using it like an enum.
             */
            decoder.listeners.warning(errors().getString(Errors.Keys.UnknownEnumValue_2, codeType, name), null);
        }
        return code;
    }

    /**
     * Adds the given element in the given collection if the element is not already present in the collection.
     * We define this method because the metadata API uses collections while the SIS implementation uses lists.
     * The lists are usually very short (typically 0 or 1 element), so the call to {@link List#contains(Object)}
     * should be cheap.
     */
    private static <T> void addIfAbsent(final Collection<T> collection, final T element) {
        if (!collection.contains(element)) {
            collection.add(element);
        }
    }

    /**
     * Adds the given element in the given collection if the element is non-null.
     * If the element is non-null and the collection is null, a new collection is
     * created. The given collection, or the new collection if it has been created,
     * is returned.
     */
    private static <T> Set<T> addIfNonNull(Set<T> collection, final T element) {
        if (element != null) {
            if (collection == null) {
                collection = new LinkedHashSet<T>(4);
            }
            collection.add(element);
        }
        return collection;
    }

    /**
     * Returns {@code true} if the given NetCDF attribute is either null or equals to the
     * string value of the given metadata value.
     *
     * @param metadata  The value stored in the metadata object.
     * @param attribute The value parsed from the NetCDF file.
     */
    private static boolean canShare(final CharSequence metadata, final String attribute) {
        return (attribute == null) || (metadata != null && metadata.toString().equals(attribute));
    }

    /**
     * Returns {@code true} if the given NetCDF attribute is either null or equals to one
     * of the values in the given collection.
     *
     * @param metadata  The value stored in the metadata object.
     * @param attribute The value parsed from the NetCDF file.
     */
    private static boolean canShare(final Collection<String> metadata, final String attribute) {
        return (attribute == null) || metadata.contains(attribute);
    }

    /**
     * Returns {@code true} if the given URL is null, or if the given resource contains that URL.
     *
     * @param resource  The value stored in the metadata object.
     * @param url       The value parsed from the NetCDF file.
     */
    private static boolean canShare(final OnlineResource resource, final String url) {
        return (url == null) || (resource != null && canShare(resource.getLinkage().toString(), url));
    }

    /**
     * Returns {@code true} if the given email is null, or if the given address contains that email.
     *
     * @param address  The value stored in the metadata object.
     * @param email    The value parsed from the NetCDF file.
     */
    private static boolean canShare(final Address address, final String email) {
        return (email == null) || (address != null && canShare(address.getElectronicMailAddresses(), email));
    }

    /**
     * Creates an {@code OnlineResource} element if the given URL is not null. Since ISO 19115
     * declares the URL as a mandatory attribute, this method will ignore all other attributes
     * if the given URL is null.
     *
     * @param  url The URL (mandatory - if {@code null}, no resource will be created).
     * @return The online resource, or {@code null} if the URL was null.
     */
    private OnlineResource createOnlineResource(final String url) {
        if (url != null) try {
            final DefaultOnlineResource resource = new DefaultOnlineResource(new URI(url));
            resource.setProtocol("http");
            resource.setApplicationProfile("web browser");
            resource.setFunction(OnLineFunction.INFORMATION);
            return resource;
        } catch (URISyntaxException e) {
            warning(e);
        }
        return null;
    }

    /**
     * Creates an {@code Address} element if at least one of the given attributes is non-null.
     */
    private static Address createAddress(final String email) {
        if (email != null) {
            final DefaultAddress address = new DefaultAddress();
            address.setElectronicMailAddresses(singleton(email));
            return address;
        }
        return null;
    }

    /**
     * Creates a {@code Contact} element if at least one of the given attributes is non-null.
     */
    private static Contact createContact(final Address address, final OnlineResource url) {
        if (address != null || url != null) {
            final DefaultContact contact = new DefaultContact();
            if (address != null) contact.setAddresses(singleton(address));
            if (url     != null) contact.setOnlineResources(singleton(url));
            return contact;
        }
        return null;
    }

    /**
     * Creates a {@code Responsibility} element if at least one of the name, email or URL attributes is defined.
     * For more consistent results, the caller should restrict the {@linkplain Decoder#setSearchPath search path}
     * to a single group before invoking this method.
     *
     * <p>Implementation note: this method tries to reuse the existing {@link #pointOfContact} instance,
     * or part of it, if it is suitable.</p>
     *
     * @param  keys The group of attribute names to use for fetching the values.
     * @param  isPointOfContact {@code true} for forcing the role to {@link Role#POINT_OF_CONTACT}.
     * @return The responsible party, or {@code null} if none.
     * @throws IOException If an I/O operation was necessary but failed.
     *
     * @see AttributeNames#CREATOR
     * @see AttributeNames#CONTRIBUTOR
     * @see AttributeNames#PUBLISHER
     */
    private ResponsibleParty createResponsibleParty(final Responsible keys, final boolean isPointOfContact)
            throws IOException
    {
        final String individualName   = stringValue(keys.NAME);
        final String organisationName = stringValue(keys.INSTITUTION);
        final String email            = stringValue(keys.EMAIL);
        final String url              = stringValue(keys.URL);
        if (individualName == null && organisationName == null && email == null && url == null) {
            return null;
        }
        Role role = forCodeName(Role.class, stringValue(keys.ROLE));
        if (role == null) {
            role = isPointOfContact ? Role.POINT_OF_CONTACT : keys.DEFAULT_ROLE;
        }
        /*
         * Verify if we can share the existing 'pointOfContact' instance. This is often the case in practice.
         * If we can not share the whole existing instance, we usually can share parts of it like the address.
         */
        ResponsibleParty responsibility = pointOfContact;
        Contact          contact        = null;
        Address          address        = null;
        OnlineResource   resource       = null;
        if (responsibility != null) {
            { // Additional indentation for having the same level than SIS branches for GeoAPI snapshots (makes merges easier).
                contact = responsibility.getContactInfo();
                if (contact != null) {
                    address  = contact.getAddress();
                    resource = contact.getOnlineResource();
                }
                if (!canShare(resource, url)) {
                    resource       = null;
                    contact        = null; // Clear the parents all the way up to the root.
                    responsibility = null;
                }
                if (!canShare(address, email)) {
                    address        = null;
                    contact        = null; // Clear the parents all the way up to the root.
                    responsibility = null;
                }
                if (responsibility != null) {
                    if (!canShare(responsibility.getOrganisationName(), organisationName) ||
                        !canShare(responsibility.getIndividualName(),   individualName))
                    {
                        responsibility = null;
                    }
                }
            }
        }
        /*
         * If we can not share the exiting instance, we have to build a new one. If there is both
         * an individual and organisation name, then the individual is considered a member of the
         * organisation. This structure shall be kept consistent with the check in the above block.
         */
        if (responsibility == null) {
            if (contact == null) {
                if (address  == null) address  = createAddress(email);
                if (resource == null) resource = createOnlineResource(url);
                contact = createContact(address, resource);
            }
            if (individualName != null || organisationName != null || contact != null) { // Do not test role.
                AbstractParty party = null;
                if (individualName   != null) party = new DefaultIndividual(individualName, null, null);
                if (organisationName != null) party = new DefaultOrganisation(organisationName, null, (DefaultIndividual) party, null);
                if (party            == null) party = new AbstractParty(); // We don't know if this is an individual or an organisation.
                if (contact          != null) party.setContactInfo(singleton(contact));
                responsibility = new DefaultResponsibleParty(role);
                ((DefaultResponsibleParty) responsibility).setParties(singleton(party));
            }
        }
        return responsibility;
    }

    /**
     * Creates a {@code Citation} element if at least one of the required attributes is non-null.
     * This method will reuse the {@link #pointOfContact} field, if non-null and suitable.
     *
     * @param  identifier The citation {@code <gmd:identifier>} attribute.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private Citation createCitation(final Identifier identifier) throws IOException {
        String title = stringValue(TITLE);
        if (title == null) {
            title = stringValue("full_name"); // THREDDS attribute documented in TITLE javadoc.
            if (title == null) {
                title = stringValue("name"); // THREDDS attribute documented in TITLE javadoc.
                if (title == null) {
                    title = decoder.getTitle();
                }
            }
        }
        final Date   creation   = decoder.dateValue(DATE_CREATED);
        final Date   modified   = decoder.dateValue(DATE_MODIFIED);
        final Date   issued     = decoder.dateValue(DATE_ISSUED);
        final String references =       stringValue(REFERENCES);
        final DefaultCitation citation = new DefaultCitation(title);
        if (identifier != null) {
            citation.setIdentifiers(singleton(identifier));
        }
        if (creation != null) citation.setDates(singleton(new DefaultCitationDate(creation, DateType.CREATION)));
        if (modified != null) citation.getDates()  .add  (new DefaultCitationDate(modified, DateType.REVISION));
        if (issued   != null) citation.getDates()  .add  (new DefaultCitationDate(issued,   DateType.PUBLICATION));
        if (pointOfContact != null) {
            // Same responsible party than the contact, except for the role.
            final DefaultResponsibleParty np = new DefaultResponsibleParty(pointOfContact);
            np.setRole(Role.ORIGINATOR);
            citation.setCitedResponsibleParties(singleton(np));
        }
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final ResponsibleParty contributor = createResponsibleParty(CONTRIBUTOR, false);
            if (contributor != null && contributor != pointOfContact) {
                addIfAbsent(citation.getCitedResponsibleParties(), contributor);
            }
        }
        decoder.setSearchPath(searchPath);
        if (references != null) {
            citation.setOtherCitationDetails(new SimpleInternationalString(references));
        }
        return citation.isEmpty() ? null : citation;
    }

    /**
     * Creates a {@code DataIdentification} element if at least one of the required attributes is non-null.
     * This method will reuse the {@link #pointOfContact} value, if non-null and suitable.
     *
     * @param  identifier The citation {@code <gmd:identifier>} attribute.
     * @param  publisher  The publisher names, built by the caller in an opportunist way.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private DataIdentification createIdentificationInfo(final Identifier identifier,
            final Set<InternationalString> publisher) throws IOException
    {
        DefaultDataIdentification identification = null;
        Set<InternationalString>  project        = null;
        DefaultLegalConstraints   constraints    = null;
        boolean hasExtent = false;
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final Keywords standard = createKeywords(KeywordType.THEME, true);
            final Keywords keywords = createKeywords(KeywordType.THEME, false);
            final String   topic    = stringValue(TOPIC_CATEGORY);
            final String   type     = stringValue(DATA_TYPE);
            final String   credits  = stringValue(ACKNOWLEDGMENT);
            final String   license  = stringValue(LICENSE);
            final String   access   = stringValue(ACCESS_CONSTRAINT);
            final Extent   extent   = hasExtent ? null : createExtent();
            if (standard!=null || keywords!=null || topic != null || type!=null || credits!=null || license!=null || access!= null || extent!=null) {
                if (identification == null) {
                    identification = new DefaultDataIdentification();
                }
                if (topic    != null) addIfAbsent(identification.getTopicCategories(), forCodeName(TopicCategory.class, topic));
                if (type     != null) addIfAbsent(identification.getSpatialRepresentationTypes(), forCodeName(SpatialRepresentationType.class, type));
                if (standard != null) addIfAbsent(identification.getDescriptiveKeywords(), standard);
                if (keywords != null) addIfAbsent(identification.getDescriptiveKeywords(), keywords);
                if (credits  != null) addIfAbsent(identification.getCredits(), credits);
                if (license  != null) addIfAbsent(identification.getResourceConstraints(), constraints = new DefaultLegalConstraints(license));
                if (access   != null) {
                    for (String keyword : access.split(KEYWORD_SEPARATOR)) {
                        keyword = keyword.trim();
                        if (!keyword.isEmpty()) {
                            if (constraints == null) {
                                identification.getResourceConstraints().add(constraints = new DefaultLegalConstraints());
                            }
                            addIfAbsent(constraints.getAccessConstraints(), forCodeName(Restriction.class, keyword));
                        }
                    }
                }
                if (extent != null) {
                    // Takes only ONE extent, because a NetCDF file may declare many time the same
                    // extent with different precision. The groups are ordered in such a way that
                    // the first extent should be the most accurate one.
                    identification.setExtents(singleton(extent));
                    hasExtent = true;
                }
            }
            project = addIfNonNull(project, toInternationalString(stringValue(PROJECT)));
        }
        decoder.setSearchPath(searchPath);
        final Citation citation = createCitation(identifier);
        final String   summary  = stringValue(SUMMARY);
        final String   purpose  = stringValue(PURPOSE);
        if (identification == null) {
            if (citation==null && summary==null && purpose==null && project==null && publisher==null && pointOfContact==null) {
                return null;
            }
            identification = new DefaultDataIdentification();
        }
        identification.setCitation(citation);
        identification.setAbstract(toInternationalString(summary));
        identification.setPurpose (toInternationalString(purpose));
        if (pointOfContact != null) {
            identification.setPointOfContacts(singleton(pointOfContact));
        }
        addKeywords(identification, project,   KeywordType.valueOf("project"));
        addKeywords(identification, publisher, KeywordType.valueOf("dataCentre"));
        identification.setSupplementalInformation(toInternationalString(stringValue(COMMENT)));
        return identification;
    }

    /**
     * Adds the given keywords to the given identification info if the given set is non-null.
     */
    private void addKeywords(final DefaultDataIdentification addTo,
            final Set<InternationalString> words, final KeywordType type)
    {
        if (words != null) {
            final DefaultKeywords keywords = new DefaultKeywords();
            keywords.setKeywords(words);
            keywords.setType(type);
            addTo.getDescriptiveKeywords().add(keywords);
        }
    }

    /**
     * Returns the keywords if at least one required attribute is found, or {@code null} otherwise.
     * For more consistent results, the caller should restrict the {@linkplain Decoder#setSearchPath
     * search path} to a single group before invoking this method.
     *
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private Keywords createKeywords(final KeywordType type, final boolean standard) throws IOException {
        final String list = stringValue(standard ? STANDARD_NAME : KEYWORDS);
        DefaultKeywords keywords = null;
        if (list != null) {
            final Set<InternationalString> words = new LinkedHashSet<InternationalString>();
            for (String keyword : list.split(KEYWORD_SEPARATOR)) {
                keyword = keyword.trim();
                if (!keyword.isEmpty()) {
                    words.add(new SimpleInternationalString(keyword));
                }
            }
            if (!words.isEmpty()) {
                keywords = new DefaultKeywords();
                keywords.setKeywords(words);
                keywords.setType(type);
                final String vocabulary = stringValue(standard ? STANDARD_NAME_VOCABULARY : VOCABULARY);
                if (vocabulary != null) {
                    keywords.setThesaurusName(new DefaultCitation(vocabulary));
                }
            }
        }
        return keywords;
    }

    /**
     * Creates a {@code <gmd:spatialRepresentationInfo>} element from the given grid geometries.
     *
     * @param  cs The grid geometry (related to the NetCDF coordinate system).
     * @return The grid spatial representation info.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private GridSpatialRepresentation createSpatialRepresentationInfo(final GridGeometry cs) throws IOException {
        final DefaultGridSpatialRepresentation grid = new DefaultGridSpatialRepresentation();
        grid.setNumberOfDimensions(cs.getTargetDimensions());
        final Axis[] axes = cs.getAxes();
        for (int i=axes.length; --i>=0;) {
            final Axis axis = axes[i];
            if (axis.sourceDimensions.length != 0) {
                final DefaultDimension dimension = new DefaultDimension();
                dimension.setDimensionSize(axis.sourceSizes[0]);
                final AttributeNames.Dimension attributeNames = axis.attributeNames;
                if (attributeNames != null) {
                    dimension.setDimensionName(attributeNames.DEFAULT_NAME_TYPE);
                    final Number value = decoder.numericValue(attributeNames.RESOLUTION);
                    if (value != null) {
                        dimension.setResolution((value instanceof Double) ? (Double) value : value.doubleValue());
                    }
                }
                grid.getAxisDimensionProperties().add(dimension);
            }
        }
        grid.setCellGeometry(CellGeometry.AREA);
        return grid;
    }

    /**
     * Returns the extent declared in the given group, or {@code null} if none. For more consistent results,
     * the caller should restrict the {@linkplain Decoder#setSearchPath search path} to a single group before
     * invoking this method.
     */
    private Extent createExtent() throws IOException {
        DefaultExtent extent = null;
        final Number xmin = decoder.numericValue(LONGITUDE.MINIMUM);
        final Number xmax = decoder.numericValue(LONGITUDE.MAXIMUM);
        final Number ymin = decoder.numericValue(LATITUDE .MINIMUM);
        final Number ymax = decoder.numericValue(LATITUDE .MAXIMUM);
        final Number zmin = decoder.numericValue(VERTICAL .MINIMUM);
        final Number zmax = decoder.numericValue(VERTICAL .MAXIMUM);
        /*
         * If at least one geographic ordinates above is available, add a GeographicBoundingBox.
         */
        if (xmin != null || xmax != null || ymin != null || ymax != null) {
            final UnitConverter xConv = getConverterTo(decoder.unitValue(LONGITUDE.UNITS), NonSI.DEGREE_ANGLE);
            final UnitConverter yConv = getConverterTo(decoder.unitValue(LATITUDE .UNITS), NonSI.DEGREE_ANGLE);
            extent = new DefaultExtent(null, new DefaultGeographicBoundingBox(
                    valueOf(xmin, xConv), valueOf(xmax, xConv),
                    valueOf(ymin, yConv), valueOf(ymax, yConv)), null, null);
        }
        /*
         * If at least one vertical ordinates above is available, add a VerticalExtent.
         */
        if (zmin != null || zmax != null) {
            final UnitConverter c = getConverterTo(decoder.unitValue(VERTICAL.UNITS), SI.METRE);
            double min = valueOf(zmin, c);
            double max = valueOf(zmax, c);
            if (CF.POSITIVE_DOWN.equals(stringValue(VERTICAL.POSITIVE))) {
                final double tmp = min;
                min = -max;
                max = -tmp;
            }
            if (extent == null) {
                extent = new DefaultExtent();
            }
            extent.setVerticalElements(singleton(new DefaultVerticalExtent(min, max, VERTICAL_CRS)));
        }
        /*
         * Get the start and end times as Date objects if available, or as numeric values otherwise.
         * In the later case, the unit symbol tells how to convert to Date objects.
         */
        Date startTime = decoder.dateValue(TIME.MINIMUM);
        Date endTime   = decoder.dateValue(TIME.MAXIMUM);
        if (startTime == null && endTime == null) {
            final Number tmin = decoder.numericValue(TIME.MINIMUM);
            final Number tmax = decoder.numericValue(TIME.MAXIMUM);
            if (tmin != null || tmax != null) {
                final String symbol = stringValue(TIME.UNITS);
                if (symbol != null) {
                    final Date[] dates = decoder.numberToDate(symbol, tmin, tmax);
                    startTime = dates[0];
                    endTime   = dates[1];
                }
            }
        }
        /*
         * If at least one time values above is available, add a temporal extent.
         * This operation requires the the sis-temporal module. If not available,
         * we will report a warning and leave the temporal extent missing.
         */
        if (startTime != null || endTime != null) try {
            final DefaultTemporalExtent t = new DefaultTemporalExtent();
            t.setBounds(startTime, endTime);
            if (extent == null) {
                extent = new DefaultExtent();
            }
            extent.setTemporalElements(singleton(t));
        } catch (UnsupportedOperationException e) {
            warning(e);
        }
        /*
         * Add the geographic identifier, if present.
         */
        final String identifier = stringValue(GEOGRAPHIC_IDENTIFIER);
        if (identifier != null) {
            if (extent == null) {
                extent = new DefaultExtent();
            }
            extent.setGeographicElements(singleton(new DefaultGeographicDescription(identifier)));
        }
        return extent;
    }

    /**
     * Returns the converter from the given source unit (which may be {@code null}) to the
     * given target unit, or {@code null} if none or incompatible.
     */
    private UnitConverter getConverterTo(final Unit<?> source, final Unit<?> target) {
        if (source != null) try {
            return source.getConverterToAny(target);
        } catch (ConversionException e) {
            warning(e);
        }
        return null;
    }

    /**
     * Returns the values of the given number if non-null, or NaN if null. If the given
     * converter is non-null, it is applied.
     */
    private static double valueOf(final Number value, final UnitConverter converter) {
        double n = Double.NaN;
        if (value != null) {
            n = value.doubleValue();
            if (converter != null) {
                n = converter.convert(n);
            }
        }
        return n;
    }

    /**
     * Creates a {@code <gmd:contentInfo>} elements from all applicable NetCDF attributes.
     *
     * @return The content information.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private Collection<DefaultCoverageDescription> createContentInfo() throws IOException {
        final Map<List<String>, DefaultCoverageDescription> contents =
                new HashMap<List<String>, DefaultCoverageDescription>(4);
        final String processingLevel = stringValue(PROCESSING_LEVEL);
        for (final Variable variable : decoder.getVariables()) {
            if (!variable.isCoverage(2)) {
                continue;
            }
            DefaultAttributeGroup group = null;
            /*
             * Instantiate a CoverageDescription for each distinct set of NetCDF dimensions
             * (e.g. longitude,latitude,time). This separation is based on the fact that a
             * coverage has only one domain for every range of values.
             */
            final List<String> dimensions = Arrays.asList(variable.getGridDimensionNames());
            DefaultCoverageDescription content = contents.get(dimensions);
            if (content == null) {
                /*
                 * If there is some NetCDF attributes that can be stored only in the ImageDescription
                 * subclass, instantiate that subclass. Otherwise instantiate the more generic class.
                 */
                if (processingLevel != null) {
                    content = new DefaultImageDescription();
                    content.setProcessingLevelCode(new DefaultIdentifier(processingLevel));
                } else {
                    content = new DefaultCoverageDescription();
                }
                contents.put(dimensions, content);
            } else {
                group = first(content.getAttributeGroups());
            }
            if (group == null) {
                group = new DefaultAttributeGroup();
                content.setAttributeGroups(singleton(group));
            }
            group.getAttributes().add(createSampleDimension(variable));
            final Object[] names    = variable.getAttributeValues(FLAG_NAMES,    false);
            final Object[] meanings = variable.getAttributeValues(FLAG_MEANINGS, false);
            final Object[] masks    = variable.getAttributeValues(FLAG_MASKS,    true);
            final Object[] values   = variable.getAttributeValues(FLAG_VALUES,   true);
            final int length = Math.max(masks.length, Math.max(values.length, Math.max(names.length, meanings.length)));
            for (int i=0; i<length; i++) {
                final RangeElementDescription element = createRangeElementDescription(variable,
                        (i < names   .length) ? (String) names   [i] : null,
                        (i < meanings.length) ? (String) meanings[i] : null,
                        (i < masks   .length) ? (Number) masks   [i] : null,
                        (i < values  .length) ? (Number) values  [i] : null);
                if (element != null) {
                    content.getRangeElementDescriptions().add(element);
                }
            }
        }
        return contents.values();
    }

    /**
     * Creates a {@code <gmd:dimension>} element from the given variable.
     *
     * @param  variable The NetCDF variable.
     * @return The sample dimension information.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private Band createSampleDimension(final Variable variable) throws IOException {
        final DefaultBand band = new DefaultBand();
        String name = variable.getName();
        if (name != null && !(name = name.trim()).isEmpty()) {
            if (nameFactory == null) {
                nameFactory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class); // Real dependency injection to be used in a future version.
            }
            band.setSequenceIdentifier(nameFactory.createMemberName(null, name,
                    nameFactory.createTypeName(null, variable.getDataTypeName())));
        }
        String description = variable.getDescription();
        if (description != null && !(description = description.trim()).isEmpty() && !description.equals(name)) {
            band.setDescription(new SimpleInternationalString(description));
        }
        final String units = variable.getUnitsString();
        if (units != null) try {
            band.setUnits(Units.valueOf(units));
        } catch (RuntimeException e) { // IllegalArgumentException or ClassCastException (specific to this branch).
            decoder.listeners.warning(errors().getString(Errors.Keys.CanNotAssignUnitToDimension_2, name, units), e);
        }
        return band;
    }

    /**
     * Creates a {@code <gmd:rangeElementDescription>} elements from the given information.
     *
     * <p><b>Note:</b> ISO 19115 range elements are approximatively equivalent to
     * {@link org.apache.sis.coverage.Category} in the {@code sis-coverage} module.</p>
     *
     * @param  variable The NetCDF variable.
     * @param  name     One of the elements in the {@link AttributeNames#FLAG_NAMES} attribute, or {@code null}.
     * @param  meaning  One of the elements in the {@link AttributeNames#FLAG_MEANINGS} attribute or {@code null}.
     * @param  mask     One of the elements in the {@link AttributeNames#FLAG_MASKS} attribute or {@code null}.
     * @param  value    One of the elements in the {@link AttributeNames#FLAG_VALUES} attribute or {@code null}.
     * @return The sample dimension information or {@code null} if none.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private RangeElementDescription createRangeElementDescription(final Variable variable,
            final String name, final String meaning, final Number mask, final Number value) throws IOException
    {
        if (name != null && meaning != null) {
            final DefaultRangeElementDescription element = new DefaultRangeElementDescription();
            element.setName(new SimpleInternationalString(name));
            element.setDefinition(new SimpleInternationalString(meaning));
            // TODO: create a record from values (and possibly from the masks).
            //       if (pixel & mask == value) then we have that range element.
            return element;
        }
        return null;
    }

    /**
     * Returns a globally unique identifier for the current NetCDF {@linkplain #decoder}.
     * The default implementation builds the identifier from the following attributes:
     *
     * <ul>
     *   <li>{@value #NAMING_AUTHORITY} used as the {@linkplain Identifier#getAuthority() authority}.</li>
     *   <li>{@value #IDENTIFIER}, or {@link ucar.nc2.NetcdfFile#getId()} if no identifier attribute was found.</li>
     * </ul>
     *
     * @return The globally unique identifier, or {@code null} if none.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private Identifier getFileIdentifier() throws IOException {
        String identifier = stringValue(IDENTIFIER);
        if (identifier == null) {
            identifier = decoder.getId();
            if (identifier == null) {
                return null;
            }
        }
        final String namespace = stringValue(NAMING_AUTHORITY);
        return new DefaultIdentifier((namespace != null) ? new DefaultCitation(namespace) : null, identifier);
    }

    /**
     * Creates an ISO {@code Metadata} object from the information found in the NetCDF file.
     *
     * @return The ISO metadata object.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public Metadata read() throws IOException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandards(Citations.ISO_19115);
        final Identifier identifier = getFileIdentifier();
        metadata.setMetadataIdentifier(identifier);
        final Date creation = decoder.dateValue(METADATA_CREATION);
        if (creation != null) {
            metadata.setDateInfo(singleton(new DefaultCitationDate(creation, DateType.CREATION)));
        }
        metadata.setMetadataScopes(singleton(new DefaultMetadataScope(ScopeCode.DATASET, null)));
        for (final String service : SERVICES) {
            final String name = stringValue(service);
            if (name != null) {
                addIfAbsent(metadata.getMetadataScopes(), new DefaultMetadataScope(ScopeCode.SERVICE, name));
            }
        }
        /*
         * Add the responsible party which is declared in global attributes, or in
         * the THREDDS attributes if no information was found in global attributes.
         */
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final ResponsibleParty party = createResponsibleParty(CREATOR, true);
            if (party != null && party != pointOfContact) {
                addIfAbsent(metadata.getContacts(), party);
                if (pointOfContact == null) {
                    pointOfContact = party;
                }
            }
        }
        /*
         * Add the publisher AFTER the creator, because this method may
         * reuse the 'creator' field (if non-null and if applicable).
         */
        Set<InternationalString> publisher = null;
        DefaultDistribution distribution   = null;
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final ResponsibleParty r = createResponsibleParty(PUBLISHER, false);
            if (r != null) {
                if (distribution == null) {
                    distribution = new DefaultDistribution();
                    metadata.setDistributionInfo(distribution);
                }
                final DefaultDistributor distributor = new DefaultDistributor(r);
                // TODO: There is some transfert option, etc. that we could set there.
                // See UnidataDD2MI.xsl for options for OPeNDAP, THREDDS, etc.
                addIfAbsent(distribution.getDistributors(), distributor);
                publisher = addIfNonNull(publisher, r.getOrganisationName());
                publisher = addIfNonNull(publisher, toInternationalString(r.getIndividualName()));
            }
            // Also add history.
            final String history = stringValue(HISTORY);
            if (history != null) {
                final DefaultDataQuality quality = new DefaultDataQuality();
                final DefaultLineage lineage = new DefaultLineage();
                lineage.setStatement(new SimpleInternationalString(history));
                quality.setLineage(lineage);
                addIfAbsent(metadata.getDataQualityInfo(), quality);
            }
        }
        /*
         * Add the identification info AFTER the responsible parties (both creator and publisher),
         * because this method will reuse the 'creator' and 'publisher' information (if non-null).
         */
        final DataIdentification identification = createIdentificationInfo(identifier, publisher);
        if (identification != null) {
            metadata.setIdentificationInfo(singleton(identification));
        }
        metadata.setContentInfo(createContentInfo());
        /*
         * Add the dimension information, if any. This metadata node
         * is built from the NetCDF CoordinateSystem objects.
         */
        for (final GridGeometry cs : decoder.getGridGeometries()) {
            if (cs.getSourceDimensions() >= Variable.MIN_DIMENSION && cs.getTargetDimensions() >= Variable.MIN_DIMENSION) {
                metadata.getSpatialRepresentationInfo().add(createSpatialRepresentationInfo(cs));
            }
        }
        return metadata;
    }
}
