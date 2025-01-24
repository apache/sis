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
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;
import java.time.temporal.Temporal;
import ucar.nc2.constants.CF;       // String constants are copied by the compiler with no UCAR reference left.
import ucar.nc2.constants.CDM;      // idem
import ucar.nc2.constants.ACDD;     // idem
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import javax.measure.format.MeasurementParseException;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.spatial.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.netcdf.base.Axis;
import org.apache.sis.storage.netcdf.base.Decoder;
import org.apache.sis.storage.netcdf.base.Variable;
import org.apache.sis.storage.netcdf.base.VariableRole;
import org.apache.sis.storage.netcdf.base.Dimension;
import org.apache.sis.storage.netcdf.base.Grid;
import org.apache.sis.storage.wkt.StoreFormat;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.CodeLists;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.Units;
import org.apache.sis.math.Vector;
import static org.apache.sis.storage.netcdf.AttributeNames.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.NameFactory;


/**
 * Mapping from netCDF metadata to ISO 19115-2 metadata. The {@link String} constants declared in
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
 * {@linkplain AttributeNames#LONGITUDE longitude} and {@linkplain AttributeNames#LATITUDE latitude}
 * resolutions are often more accurate in that group.
 *
 * <h2>Known limitations</h2>
 * <ul>
 *   <li>{@code "degrees_west"} and {@code "degrees_south"} units not correctly handled.</li>
 *   <li>Units of measurement not yet declared in the {@link Band} elements.</li>
 *   <li>{@link AttributeNames#FLAG_VALUES} and {@link AttributeNames#FLAG_MASKS}
 *       not yet included in the {@link RangeElementDescription} elements.</li>
 *   <li>Services (WMS, WCS, OPeNDAP, THREDDS) <i>etc.</i>) and transfer options not yet declared.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Alexis Manin (Geomatys)
 */
final class MetadataReader extends MetadataBuilder {
    /**
     * Whether the reader should include experimental fields.
     * They are fields for which we are unsure of the proper ISO 19115 location.
     */
    @Configuration
    private static final boolean EXPERIMENTAL = true;

    /**
     * Names of global attributes identifying services.
     */
    private static final String[] SERVICES = {"wms_service", "wcs_service"};

    /**
     * The character to use as a separator in comma-separated list. This separator is used for parsing the
     * {@link AttributeNames#KEYWORDS} attribute value for instance.
     */
    private static final char SEPARATOR = ',';

    /**
     * The character to use for quoting strings in a comma-separated list. Quoted strings may contain comma.
     *
     * <h4>Example</h4>
     * John Doe, Jane Lee, "L J Smith, Jr."
     */
    private static final char QUOTE = '"';

    /**
     * The source of netCDF attributes from which to infer ISO metadata.
     * This source is set at construction time.
     *
     * <p>This {@code MetadataReader} class does <strong>not</strong> close this source.
     * Closing this source after usage is the user responsibility.</p>
     */
    private final Decoder decoder;

    /**
     * The actual search path, as a subset of {@link org.apache.sis.storage.netcdf.base.Convention#SEARCH_PATH}
     * with only the name of the groups which have been found in the NeCDF file.
     */
    private final String[] searchPath;

    /**
     * The contact, used at metadata creation time for avoiding to construct identical objects
     * more than once.
     *
     * <p>The point of contact is stored in the two following places. The semantic of those two
     * contacts is not strictly identical, but the distinction is not used in netCDF file:</p>
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getContacts()}</li>
     *   <li>{@link DefaultDataIdentification#getPointOfContacts()}</li>
     * </ul>
     *
     * An object very similar is used as the creator. The point of contact and the creator
     * are often identical except for their role attribute.
     */
    private transient Responsibility pointOfContact;

    /**
     * The vertical coordinate reference system to be given to the object created by {@link #addExtent()}.
     * This is set to the first vertical CRS found.
     */
    private VerticalCRS verticalCRS;

    /**
     * Whether at least one grid coverage has been found during iteration over variables.
     */
    private boolean hasGridCoverages;

    /**
     * Creates a new <i>netCDF to ISO</i> mapper for the given source.
     *
     * @param  decoder  the source of netCDF attributes.
     */
    MetadataReader(final Decoder decoder) {
        this.decoder = decoder;
        decoder.setSearchPath(decoder.convention().getSearchPath());
        searchPath = decoder.getSearchPath();
    }

    /**
     * Invoked when a non-fatal exception occurred while reading metadata.
     * This method sends a record to the registered listeners if any,
     * or logs the record otherwise.
     */
    private void warning(final Exception e) {
        decoder.listeners.warning(e);
    }

    /**
     * Logs a warning using the localized error resource bundle for the locale given by
     * {@link StoreListeners#getLocale()}.
     *
     * @param  key  one of {@link Errors.Keys} values.
     */
    private void warning(final short key, final Object p1, final Object p2, final Exception e) {
        final StoreListeners listeners = decoder.listeners;
        listeners.warning(Errors.forLocale(listeners.getLocale()).getString(key, p1, p2), e);
    }

    /**
     * Splits comma-separated values. Leading and trailing spaces are removed for each item
     * unless the item is between double quotes. Empty strings are ignored unless between double quotes.
     * If a value begin with double quotes, all content will be copied verbatim until the closing double quote.
     * A double quote is considered as a closing double quote if just before a comma separator (ignoring spaces).
     */
    static List<String> split(final String value) {
        if (value == null) {
            return List.of();
        }
        final var items = new ArrayList<String>();
        int start = 0;      // Index of the first character of the next item to add in the list.
        int end;            // Index after the last character of the next item to add in the list.
        int next;           // Index of the next separator (comma) after `end`.
        final int length = CharSequences.skipTrailingWhitespaces(value, 0, value.length());
split:  while ((start = CharSequences.skipLeadingWhitespaces(value, start, length)) < length) {
            if (value.charAt(start) == QUOTE) {
                next = ++start;                                 // Skip the quote character.
                do {
                    end = value.indexOf(QUOTE, next);           // End of quoted text, may have comma separator before.
                    if (end < 0) break split;
                    next = CharSequences.skipLeadingWhitespaces(value, end+1, length);
                } while (next < length && value.charAt(next) != SEPARATOR);
            } else {
                next = value.indexOf(SEPARATOR, start);         // Unquoted text - comma is the item separator.
                if (next < 0) break;
                end = CharSequences.skipTrailingWhitespaces(value, start, next);
            }
            if (start != end) {
                items.add(value.substring(start, end));
            }
            start = next+1;
        }
        if (start < length) {
            items.add(value.substring(start, length));
        }
        return items;
    }

    /**
     * Reads the attribute value for the given name, then trims the leading and trailing spaces.
     * If the value is null, empty or contains only spaces, then this method returns {@code null}.
     */
    private String stringValue(final String name) {
        return Strings.trimOrNull(decoder.stringValue(name));
    }

    /**
     * Reads the numeric value for the given value, or returns {@code NaN} if none.
     */
    private double numericValue(final String name) {
        final Number v = decoder.numericValue(name);
        return (v != null) ? v.doubleValue() : Double.NaN;
    }

    /**
     * Returns the enumeration constant for the given name, or {@code null} if the given name is not recognized.
     * In the latter case, this method emits a warning.
     */
    private <T extends Enum<T>> T forEnumName(final Class<T> enumType, final String name) {
        try {
            return CodeLists.forEnumName(enumType, name);
        } catch (IllegalArgumentException e) {
            warning(Errors.Keys.UnknownEnumValue_2, enumType, name, e);
            return null;
        }
    }

    /**
     * Returns the code value for the given name, or {@code null} if the given name is not recognized.
     * In the latter case, this method emits a warning.
     */
    private <T extends CodeList<T>> T forCodeName(final Class<T> codeType, final String name) {
        final T code = Types.forCodeName(codeType, name, null);
        if (code == null && name != null) {
            /*
             * CodeLists are not enums, but using the error message for enums is not completly wrong since
             * if we did not allowed CodeList to create new elements, then we are using it like an enum.
             */
            warning(Errors.Keys.UnknownEnumValue_2, codeType, name, null);
        }
        return code;
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
                collection = new LinkedHashSet<>(4);
            }
            collection.add(element);
        }
        return collection;
    }

    /**
     * Returns {@code true} if the given netCDF attribute is either null or equals to the
     * string value of the given metadata value.
     *
     * @param metadata  The value stored in the metadata object.
     * @param attribute The value parsed from the netCDF file.
     */
    private static boolean canShare(final CharSequence metadata, final String attribute) {
        return (attribute == null) || (metadata != null && metadata.toString().equals(attribute));
    }

    /**
     * Returns {@code true} if the given netCDF attribute is either null or equals to one
     * of the values in the given collection.
     *
     * @param  metadata   the value stored in the metadata object.
     * @param  attribute  the value parsed from the netCDF file.
     */
    private static boolean canShare(final Collection<String> metadata, final String attribute) {
        return (attribute == null) || metadata.contains(attribute);
    }

    /**
     * Returns {@code true} if the given URL is null, or if the given resource contains that URL.
     *
     * @param  resource  the value stored in the metadata object.
     * @param  url       the value parsed from the netCDF file.
     */
    private static boolean canShare(final OnlineResource resource, final String url) {
        return (url == null) || (resource != null && canShare(resource.getLinkage().toString(), url));
    }

    /**
     * Returns {@code true} if the given email is null, or if the given address contains that email.
     *
     * @param  address  the value stored in the metadata object.
     * @param  email    the value parsed from the netCDF file.
     */
    private static boolean canShare(final Address address, final String email) {
        return (email == null) || (address != null && canShare(address.getElectronicMailAddresses(), email));
    }

    /**
     * Creates a URI form the given path, or returns {@code null} if the given URL is null or cannot be parsed.
     * In the latter case, a warning will be emitted.
     */
    private URI createURI(final String url) {
        if (url != null) try {
            return new URI(url);
        } catch (URISyntaxException e) {
            warning(e);
        }
        return null;
    }

    /**
     * Creates an {@code OnlineResource} element if the given URL is not null. Since ISO 19115
     * declares the URL as a mandatory attribute, this method will ignore all other attributes
     * if the given URL is null.
     *
     * @param  url   the URL (mandatory - if {@code null}, no resource will be created).
     * @return the online resource, or {@code null} if the URL was null.
     */
    private OnlineResource createOnlineResource(final String url) {
        final URI uri = createURI(url);
        if (uri == null) {
            return null;
        }
        final DefaultOnlineResource resource = new DefaultOnlineResource(uri);
        final String protocol = uri.getScheme();
        resource.setProtocol(protocol);
        if (IOUtilities.isHTTP(protocol)) {
            resource.setApplicationProfile("web browser");
        }
        resource.setFunction(OnLineFunction.INFORMATION);
        return resource;
    }

    /**
     * Creates an {@code Address} element if at least one of the given attributes is non-null.
     */
    private static Address createAddress(final String email) {
        if (email != null) {
            final DefaultAddress address = new DefaultAddress();
            address.setElectronicMailAddresses(Set.of(email));
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
            if (address != null) contact.setAddresses(Set.of(address));
            if (url     != null) contact.setOnlineResources(Set.of(url));
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
     * @param  keys              the group of attribute names to use for fetching the values.
     * @param  isPointOfContact  {@code true} if this responsible party is the "main" one. This will force the
     *         role to {@link Role#POINT_OF_CONTACT} and enable the use of {@code "institution"} attribute as
     *         a fallback if there is no value for {@link Responsible#INSTITUTION}.
     * @return the responsible party, or {@code null} if none.
     *
     * @see AttributeNames#CREATOR
     * @see AttributeNames#CONTRIBUTOR
     * @see AttributeNames#PUBLISHER
     */
    private Responsibility createResponsibleParty(final Responsible keys, final boolean isPointOfContact) {
        String individualName   = stringValue(keys.NAME);
        String organisationName = stringValue(keys.INSTITUTION);
        final String email      = stringValue(keys.EMAIL);
        final String url        = stringValue(keys.URL);
        if (organisationName == null && isPointOfContact) {
            organisationName = stringValue("institution");
        }
        if (individualName == null && organisationName == null && email == null && url == null) {
            return null;
        }
        /*
         * The "individual" name may actually be an institution name, either because a "*_type" attribute
         * said so or because the "individual" name is the same as the institution name. In such cases,
         * reorganize the names in order to avoid duplication.
         */
        if (organisationName == null) {
            if (isOrganisation(keys)) {
                organisationName = individualName;
                individualName = null;
            }
        } else if (organisationName.equalsIgnoreCase(individualName)) {
            individualName = null;
        }
        Role role = forCodeName(Role.class, stringValue(keys.ROLE));
        if (role == null) {
            role = isPointOfContact ? Role.POINT_OF_CONTACT : keys.DEFAULT_ROLE;
        }
        /*
         * Verify if we can share the existing `pointOfContact` instance. This is often the case in practice.
         * If we cannot share the whole existing instance, we usually can share parts of it like the address.
         */
        Responsibility responsibility = pointOfContact;
        Contact        contact        = null;
        Address        address        = null;
        OnlineResource resource       = null;
        if (responsibility != null) {
            final Party party = CollectionsExt.first(responsibility.getParties());
            if (party != null) {
                contact = CollectionsExt.first(party.getContactInfo());
                if (contact != null) {
                    address  = CollectionsExt.first(contact.getAddresses());
                    resource = CollectionsExt.first(contact.getOnlineResources());
                }
                if (!canShare(resource, url)) {
                    resource       = null;
                    contact        = null;                  // Clear the parents all the way up to the root.
                    responsibility = null;
                }
                if (!canShare(address, email)) {
                    address        = null;
                    contact        = null;                  // Clear the parents all the way up to the root.
                    responsibility = null;
                }
                if (responsibility != null) {
                    if (party instanceof Organisation) {
                        // Individual (if any) is considered an organisation member. See comment in next block.
                        if (!canShare(party.getName(), organisationName) ||
                            !canShare(CollectionsExt.first(((Organisation) party).getIndividual()).getName(), individualName))
                        {
                            responsibility = null;
                        }
                    } else if (!canShare(party.getName(), individualName)) {
                        responsibility = null;
                    }
                }
            }
        }
        /*
         * If we cannot share the exiting instance, we have to build a new one. If there is both
         * an individual and organisation name, then the individual is considered a member of the
         * organisation. This structure shall be kept consistent with the check in the above block.
         */
        if (responsibility == null) {
            if (contact == null) {
                if (address  == null) address  = createAddress(email);
                if (resource == null) resource = createOnlineResource(url);
                contact = createContact(address, resource);
            }
            if (individualName != null || organisationName != null || contact != null) {        // Do not test role.
                AbstractParty party = null;
                if (individualName   != null) party = new DefaultIndividual(individualName, null, null);
                if (organisationName != null) party = new DefaultOrganisation(organisationName, null, (Individual) party, null);
                if (party            == null) party = isOrganisation(keys) ? new DefaultOrganisation() : new DefaultIndividual();
                if (contact          != null) party.setContactInfo(Set.of(contact));
                responsibility = new DefaultResponsibility(role, null, party);
            }
        }
        return responsibility;
    }

    /**
     * Returns {@code true} if the responsible party described by the given keys is an organization.
     * In case of doubt, this method returns {@code false}. This is consistent with ACDD recommendation,
     * which set the default value to {@code "person"}.
     */
    private boolean isOrganisation(final Responsible keys) {
        final String type = stringValue(keys.TYPE);
        return "institution".equalsIgnoreCase(type) || "group".equalsIgnoreCase(type);
    }

    /**
     * Adds a {@code DataIdentification/Citation} element if at least one of the required attributes is non-null.
     * This method will initialize the {@link #pointOfContact} field, then reuses it if non-null and suitable.
     *
     * <p>This method opportunistically collects the name of all publishers.
     * Those names are useful to {@link #addIdentificationInfo(Set)}.</p>
     *
     * @return the name of all publishers, or {@code null} if none.
     */
    private Set<InternationalString> addCitation() {
        String title = stringValue(TITLE);
        if (title == null) {
            title = stringValue("full_name");   // THREDDS attribute documented in TITLE javadoc.
            if (title == null) {
                title = stringValue("name");    // THREDDS attribute documented in TITLE javadoc.
                if (title == null) {
                    title = decoder.getTitle();
                }
            }
        }
        addTitle(title);
        addEdition(stringValue(PRODUCT_VERSION));
        addOtherCitationDetails(stringValue(REFERENCES));
        addCitationDate(decoder.dateValue(METADATA_CREATION), DateType.CREATION,    Scope.ALL);
        addCitationDate(decoder.dateValue(METADATA_MODIFIED), DateType.REVISION,    Scope.ALL);
        addCitationDate(decoder.dateValue(DATE_CREATED),      DateType.CREATION,    Scope.RESOURCE);
        addCitationDate(decoder.dateValue(DATE_MODIFIED),     DateType.REVISION,    Scope.RESOURCE);
        addCitationDate(decoder.dateValue(DATE_ISSUED),       DateType.PUBLICATION, Scope.RESOURCE);
        /*
         * Add the responsible party which is declared in global attributes, or in
         * the THREDDS attributes if no information was found in global attributes.
         * This responsible party is taken as the point of contact.
         */
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final Responsibility party = createResponsibleParty(CREATOR, true);
            if (party != pointOfContact) {
                addPointOfContact(party, Scope.RESOURCE);
                if (pointOfContact == null) {
                    pointOfContact = party;
                }
            }
        }
        /*
         * There is no distinction in netCDF files between "point of contact" and "creator".
         * We take the first one as the data originator.
         */
        addCitedResponsibleParty(pointOfContact, Role.ORIGINATOR);
        /*
         * Add the contributors only after we did one full pass over the creators. We keep those two
         * loops separated in order to increase the chances that pointOfContact has been initialized
         * (it may not have been initialized on the first pass).
         */
        Set<InternationalString> publisher = null;
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final Responsibility contributor = createResponsibleParty(CONTRIBUTOR, false);
            if (contributor != pointOfContact) {
                addCitedResponsibleParty(contributor, null);
            }
            final Responsibility r = createResponsibleParty(PUBLISHER, false);
            if (r != null) {
                addDistributor(r);
                for (final Party party : r.getParties()) {
                    publisher = addIfNonNull(publisher, party.getName());
                }
            }
        }
        decoder.setSearchPath(searchPath);
        return publisher;
    }

    /**
     * Adds a {@code DataIdentification} element if at least one of the required attributes is non-null.
     *
     * @param  publisher   the publisher names, built by the caller in an opportunist way.
     */
    private void addIdentificationInfo(final Set<InternationalString> publisher) throws IOException, DataStoreException {
        boolean     hasExtent = false;
        Set<String> project   = null;
        Set<String> standard  = null;
        final Set<String> keywords = new LinkedHashSet<>();
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            keywords.addAll(split(stringValue(KEYWORDS.TEXT)));
            standard = addIfNonNull(standard, stringValue(STANDARD_NAME.TEXT));
            project  = addIfNonNull(project,  stringValue(PROJECT));
            for (final String keyword : split(stringValue(ACCESS_CONSTRAINT))) {
                addAccessConstraint(forCodeName(Restriction.class, keyword));
            }
            addTopicCategory(forEnumName(TopicCategory.class, stringValue(TOPIC_CATEGORY)));
            addSpatialRepresentation(forCodeName(SpatialRepresentationType.class, stringValue(DATA_TYPE)));
            if (!hasExtent) {
                /*
                 * Takes only ONE extent, because a netCDF file may declare many time the same
                 * extent with different precision. The groups are ordered in such a way that
                 * the first extent should be the most accurate one.
                 */
                hasExtent = addExtent();
            }
        }
        /*
         * For the following properties, use only the first non-empty attribute value found on the search path.
         */
        decoder.setSearchPath(searchPath);
        addAbstract               (stringValue(SUMMARY));
        addPurpose                (stringValue(PURPOSE));
        addSupplementalInformation(stringValue(COMMENT));
        addCredits                (stringValue(ACKNOWLEDGEMENT));
        addCredits                (stringValue("acknowledgment"));          // Legacy spelling.
        addUseLimitation          (stringValue(LICENSE));
        addKeywords(standard,  KeywordType.THEME,       stringValue(STANDARD_NAME.VOCABULARY));
        addKeywords(keywords,  KeywordType.THEME,       stringValue(KEYWORDS.VOCABULARY));
        addKeywords(project,   KeywordType.PROJECT,     null);
        addKeywords(publisher, KeywordType.DATA_CENTRE, null);
        /*
         * Add geospatial bounds as a geometric object. This optional operation requires
         * an external library (ESRI or JTS) to be present on the module path.
         */
        final String wkt = stringValue(GEOSPATIAL_BOUNDS);
        if (wkt != null) {
            addBoundingPolygon(new StoreFormat(null, null, decoder.geomlib, decoder.listeners).parseGeometry(wkt,
                    stringValue(GEOSPATIAL_BOUNDS + "_crs"), stringValue(GEOSPATIAL_BOUNDS + "_vertical_crs")));
        }
        /*
         * Add a description of the format. The description is determined by the decoder in use.
         * That decoder may itself infer that description from another library such as UCAR.
         */
        decoder.addFormatDescription(this);
    }

    /**
     * Adds the format description with a check about whether the given format identifier is recognized.
     * This is a helper method for {@link Decoder#addFormatDescription(MetadataBuilder)} implementations.
     *
     * @param  format     format identifier. Recognized value is {@value NetcdfStoreProvider#NAME}.
     * @param  listeners  ignored. Will be replaced by the listeners of the decoder.
     * @param  fallback   whether to use a fallback if the description was not found.
     * @return whether the format description has been added.
     */
    @Override
    public boolean setPredefinedFormat(String format, StoreListeners listeners, boolean fallback) {
        if (Constants.NETCDF.equalsIgnoreCase(format)) {
            return super.setPredefinedFormat(format, decoder.listeners, fallback);
        } else if (fallback) {
            addFormatName(format);
        }
        return false;
    }

    /**
     * Adds information about axes and cell geometry.
     * This is the {@code <mdb:spatialRepresentationInfo>} element in XML.
     * We work on grid axes instead of Coordinate Reference System axes because
     * {@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize} seems to imply that.
     *
     * @param  cs  the grid geometry (related to the netCDF coordinate system).
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    private void addSpatialRepresentationInfo(final Axis[] axes) throws IOException, DataStoreException {
        for (int i=0; i<axes.length; i++) {
            final Axis axis = axes[i];
            /*
             * Axes usually have exactly one dimension. However, some netCDF axes are backed by a two-dimensional
             * conversion grid. In such case, our Axis constructor should have ensured that the first element in
             * the `sourceDimensions` and `sourceSizes` arrays are for the grid dimension which is most closely
             * oriented toward the axis direction.
             */
            final int d = i;    // Because lambda expressions want final variable.
            axis.getMainSize().ifPresent((s) -> setAxisSize(d, s));
            final AttributeNames.Dimension attributeNames;
            switch (axis.abbreviation) {
                case 'λ': case 'θ':           attributeNames = AttributeNames.LONGITUDE; break;
                case 'φ': case 'Ω':           attributeNames = AttributeNames.LATITUDE;  break;
                case 'h': case 'H': case 'D': attributeNames = AttributeNames.VERTICAL;  break;
                case 't': case 'T':           attributeNames = AttributeNames.TIME;      break;
                default : continue;
            }
            final DimensionNameType name = attributeNames.DEFAULT_NAME_TYPE;
            setAxisName(i, name);
            final String res = stringValue(attributeNames.RESOLUTION);
            if (res != null) try {
                /*
                 * ACDD convention recommends to write units after the resolution.
                 * Examples: "100 meters", "0.1 degree".
                 */
                final int s = res.indexOf(' ');
                final double value;
                Unit<?> units = null;
                if (s < 0) {
                    value = numericValue(attributeNames.RESOLUTION);
                } else {
                    value = Double.parseDouble(res.substring(0, s).trim());
                    final String symbol = res.substring(s+1).trim();
                    if (!symbol.isEmpty()) try {
                        units = Units.valueOf(symbol);
                    } catch (MeasurementParseException e) {
                        warning(Errors.Keys.CanNotAssignUnitToDimension_2, name, units, e);
                    }
                }
                setAxisResolution(i, value, units);
            } catch (NumberFormatException e) {
                warning(e);
            }
        }
        setCellGeometry(CellGeometry.AREA);
    }

    /**
     * Adds the extent declared in the current group. For more consistent results, the caller should restrict
     * the {@linkplain Decoder#setSearchPath search path} to a single group before invoking this method.
     * The {@link #verticalCRS} field should have been set before to invoke this method.
     *
     * @return {@code true} if at least one numerical value has been added.
     */
    private boolean addExtent() {
        addExtent(stringValue(GEOGRAPHIC_IDENTIFIER));
        final double[] extent = new double[4];
        /*
         * If at least one geographic coordinate is available, add a GeographicBoundingBox.
         */
        boolean hasExtent;
        int convention = 0;
        do {
            hasExtent  = fillExtent(LONGITUDE_ALTERNATIVES[convention], Units.DEGREE, AxisDirection.EAST,  extent, 0);
            hasExtent |= fillExtent( LATITUDE_ALTERNATIVES[convention], Units.DEGREE, AxisDirection.NORTH, extent, 2);
            if (hasExtent) {
                addExtent(extent, 0);
                hasExtent = true;
                break;
            }
        } while (++convention < LONGITUDE_ALTERNATIVES.length);
        /*
         * If at least one vertical coordinate is available, add a VerticalExtent.
         */
        convention = Math.min(convention, VERTICAL_ALTERNATIVES.length - 1);
        for (int i=0; i <= convention; i++) {
            if (fillExtent(VERTICAL_ALTERNATIVES[i], Units.METRE, null, extent, 0)) {
                addVerticalExtent(extent[0], extent[1], verticalCRS);
                hasExtent = true;
                break;
            }
        }
        /*
         * Get the start and end times as temporal objects if available, or as numeric values otherwise.
         * In the latter case, the unit symbol tells how to convert to temporal objects.
         */
        Temporal startTime = decoder.dateValue(TIME.MINIMUM);
        Temporal endTime   = decoder.dateValue(TIME.MAXIMUM);
        if (startTime == null && endTime == null) {
            final Number tmin = decoder.numericValue(TIME.MINIMUM);
            final Number tmax = decoder.numericValue(TIME.MAXIMUM);
            if (tmin != null || tmax != null) {
                final String symbol = stringValue(TIME.UNITS);
                if (symbol != null) {
                    final Temporal[] dates = decoder.numberToDate(symbol, tmin, tmax);
                    startTime = dates[0];
                    endTime   = dates[1];
                }
            }
        }
        /*
         * If at least one time value above is available, add a temporal extent.
         */
        if (startTime != null || endTime != null) {
            addTemporalExtent(startTime, endTime);
            hasExtent = true;
        }
        return hasExtent;
    }

    /**
     * Fills one dimension of the geographic bounding box or vertical extent.
     * The extent values are written in the given {@code extent} array.
     *
     * @param  dim         the dimension for which to get the extent.
     * @param  targetUnit  the destination unit of the extent.
     * @param  positive    the direction considered positive, or {@code null} if the unit symbol is not expected to contain a direction.
     * @param  extent      where to store the minimum and maximum values.
     * @param  index       index where to store the minimum value in {@code extent}. The maximum value is stored at {@code index+1}.
     * @return {@code true} if a minimum or a maximum value has been found.
     */
    private boolean fillExtent(final AttributeNames.Dimension dim, final Unit<?> targetUnit, final AxisDirection positive,
                               final double[] extent, final int index)
    {
        double min = numericValue(dim.MINIMUM);
        double max = numericValue(dim.MAXIMUM);
        boolean hasExtent = !Double.isNaN(min) || !Double.isNaN(max);
        if (hasExtent && dim.UNITS != null) {
            final String symbol = stringValue(dim.UNITS);
            if (symbol != null) {
                try {
                    final UnitConverter c = Units.valueOf(symbol).getConverterToAny(targetUnit);
                    min = c.convert(min);
                    max = c.convert(max);
                } catch (MeasurementParseException | IncommensurableException e) {
                    warning(e);
                }
                boolean reverse = false;
                if (positive != null) {
                    reverse = AxisDirections.opposite(positive).equals(Axis.direction(symbol));
                } else if (dim.POSITIVE != null) {
                    // For now, only the vertical axis have a "positive" attribute.
                    reverse = CF.POSITIVE_DOWN.equals(stringValue(dim.POSITIVE));
                }
                if (reverse) {
                    final double tmp = min;
                    min = -max;
                    max = -tmp;
                }
            }
        }
        extent[index  ] = min;
        extent[index+1] = max;
        return hasExtent;
    }

    /**
     * Adds information about acquisition (program, platform).
     */
    private void addAcquisitionInfo() {
        final Term[] attributes = {
            AttributeNames.PROGRAM,
            AttributeNames.PLATFORM,
            AttributeNames.INSTRUMENT
        };
        for (int i=0; i<attributes.length; i++) {
            final Term at = attributes[i];
            final String authority = stringValue(at.VOCABULARY);
            for (final String keyword : split(stringValue(at.TEXT))) {
                switch (i) {
                    case 0: {
                        if (EXPERIMENTAL) {
                            addAcquisitionOperation(authority, keyword);
                        }
                        break;
                    }
                    case 1: addPlatform  (authority, keyword); break;
                    case 2: addInstrument(authority, keyword); break;
                }
            }
        }
    }

    /**
     * Adds information about all netCDF variables. This is the {@code <mdb:contentInfo>} element in <abbr>XML</abbr>.
     * This method groups variables by their domains, i.e. variables having the same set of axes, ignoring order,
     * are grouped together. Variables having only a subset of axes are also grouped together with the variables
     * having more dimension.
     *
     * <p><b>Example:</b> a netCDF file may contain variables for both static and dynamic phenomenons.
     * The dynamic phenomenons are associated to (<var>x</var>, <var>y</var>, <var>t</var>) axes,
     * while the static phenomenons have only the (<var>x</var>, <var>y</var>) axes.
     * But we still want to group them together. Not doing so appear to be confusing.</p>
     */
    private void addContentInfo() {
        /*
         * Prepare a list of features and coverages, but without writing metadata now.
         * We differ metadata writing for giving us a chance to group related contents.
         */
        final var features  = new LinkedHashSet<Dimension>();
        final var coverages = new LinkedHashMap<Set<Object>, List<Variable>>();
        for (final Variable variable : decoder.getVariables()) {
            if (VariableRole.isCoverage(variable)) {
                final var dimensions = variable.getGridDimensions();
                final var names = new Object[dimensions.size()];
                for (int i=0; i<names.length; i++) {
                    Object name = dimensions.get(i).getName();
                    if (name == null) name = i;
                    names[i] = name;
                }
                coverages.computeIfAbsent(Set.of(names), (key) -> {
                    for (Map.Entry<Set<Object>, List<Variable>> entry : coverages.entrySet()) {
                        final Set<Object> previous = entry.getKey();
                        if (previous.containsAll(key) || key.containsAll(previous)) {
                            // Share with all keys that are subset or superset.
                            return entry.getValue();
                        }
                    }
                    return new ArrayList<>();
                }).add(variable);
                hasGridCoverages = true;
            } else if (variable.getRole() == VariableRole.FEATURE_PROPERTY) {
                /*
                 * For feature property, we should take only the first dimension.
                 * If a second dimension exists, it is for character strings.
                 */
                features.add(variable.getGridDimensions().get(0));
            }
        }
        /*
         * Now write the metadata. Note that the spatial repersentation types added below are actually
         * parts of `DataIdentification` instead of `ContentInformation`, but we add them here because
         * we have the information here.
         */
        if (!features .isEmpty()) addSpatialRepresentation(SpatialRepresentationType.TEXT_TABLE);
        if (!coverages.isEmpty()) addSpatialRepresentation(SpatialRepresentationType.GRID);
        for (final Dimension feature : features) {
            final String name = feature.getName();
            if (name != null) {
                addFeatureType(decoder.nameFactory.createLocalName(decoder.namespace, name), feature.length());
            }
        }
        newFeatureTypes();   // See Javadoc about confusing ordering.
        final String processingLevel = stringValue(PROCESSING_LEVEL);
        for (final List<Variable> group : coverages.values()) {
            /*
             * Instantiate a CoverageDescription for each distinct set of netCDF dimensions
             * (e.g. longitude,latitude,time). This separation is based on the fact that a
             * coverage has only one domain for every range of values.
             */
            newCoverage(false);
            setProcessingLevelCode(null, processingLevel);
            for (final Variable variable : group) {
                addSampleDimension(variable);
                final CharSequence[] names    = variable.getAttributeAsStrings(FLAG_NAMES, ' ');
                final CharSequence[] meanings = variable.getAttributeAsStrings(FLAG_MEANINGS, ' ');
                final Vector         masks    = variable.getAttributeAsVector (FLAG_MASKS);
                final Vector         values   = variable.getAttributeAsVector (FLAG_VALUES);
                final int s1 = (names    != null) ? names.length    : 0;
                final int s2 = (meanings != null) ? meanings.length : 0;
                final int s3 = (masks    != null) ? masks .size()   : 0;
                final int s4 = (values   != null) ? values.size()   : 0;
                final int length = Math.max(s1, Math.max(s2, Math.max(s3, s4)));
                for (int i=0; i<length; i++) {
                    addSampleValueDescription(variable,
                            (i < s1) ? names     [i] : null,
                            (i < s2) ? meanings  [i] : null,
                            (i < s3) ? masks .get(i) : null,
                            (i < s4) ? values.get(i) : null);
                }
            }
        }
    }

    /**
     * Adds metadata about a sample dimension (or band) from the given variable.
     * This is the {@code <mrc:dimension>} element in XML.
     *
     * @param  variable  the netCDF variable.
     */
    private void addSampleDimension(final Variable variable) {
        newSampleDimension();
        final String name = Strings.trimOrNull(variable.getName());
        if (name != null) {
            final NameFactory f = decoder.nameFactory;
            final StringBuilder buffer = new StringBuilder(20);
            variable.writeDataTypeName(buffer);
            setBandIdentifier(f.createMemberName(null, name, f.createTypeName(null, buffer.toString())));
        }
        final String id = Strings.trimOrNull(variable.getStandardName());
        if (!id.equals(name)) {
            addBandName(variable.getAttributeAsString(ACDD.standard_name_vocabulary), id);
        }
        final String description = Strings.trimOrNull(variable.getDescription());
        if (description != null && !description.equals(name) && !description.equals(id)) {
            addBandDescription(description);
        }
        setSampleUnits(variable.getUnit());
        setTransferFunction(variable.getAttributeAsDouble(CDM.SCALE_FACTOR),
                            variable.getAttributeAsDouble(CDM.ADD_OFFSET));
        addContentType(forCodeName(CoverageContentType.class, stringValue(ACDD.coverage_content_type)));
    }

    /**
     * Adds metadata about the meaning of a sample value.
     * This is the {@code <mrc:rangeElementDescription>} element in XML.
     *
     * <p><b>Note:</b> ISO 19115 range elements are approximately equivalent to
     * {@code org.apache.sis.coverage.Category} in the {@code sis-coverage} module.</p>
     *
     * @param  variable  the netCDF variable.
     * @param  name      one of the elements in the {@link AttributeNames#FLAG_NAMES} attribute, or {@code null}.
     * @param  meaning   one of the elements in the {@link AttributeNames#FLAG_MEANINGS} attribute or {@code null}.
     * @param  mask      one of the elements in the {@link AttributeNames#FLAG_MASKS} attribute or {@code null}.
     * @param  value     one of the elements in the {@link AttributeNames#FLAG_VALUES} attribute or {@code null}.
     */
    private void addSampleValueDescription(final Variable variable,
            final CharSequence name, final CharSequence meaning, final Number mask, final Number value)
    {
        addSampleValueDescription(name, meaning);
        // TODO: create a record from values (and possibly from the masks).
        //       if (pixel & mask == value) then we have that range element.
    }

    /**
     * Adds a globally unique identifier for the current netCDF {@linkplain #decoder}.
     * The current implementation builds the identifier from the following attributes:
     *
     * <ul>
     *   <li>{@code AttributeNames.IDENTIFIER.VOCABULARY} used as the {@linkplain Identifier#getAuthority() authority}.</li>
     *   <li>{@code AttributeNames.IDENTIFIER.TEXT}, or {@link ucar.nc2.NetcdfFile#getId()} if no identifier attribute was found,
     *       or the filename without extension if {@code getId()} returned nothing.</li>
     * </ul>
     *
     * This method should be invoked last, after we made our best effort to set the title.
     */
    private void addFileIdentifier() {
        String identifier = stringValue(IDENTIFIER.TEXT);
        String authority;
        if (identifier != null) {
            authority = stringValue(IDENTIFIER.VOCABULARY);
        } else {
            identifier = decoder.getId();
            if (identifier == null) {
                identifier = IOUtilities.filenameWithoutExtension(decoder.getFilename());
                if (identifier == null) {
                    return;
                }
            }
            authority = null;
        }
        if (authority == null) {
            addTitleOrIdentifier(identifier, Scope.RESOURCE);
        } else {
            addIdentifier(authority, identifier, Scope.RESOURCE);
        }
    }

    /**
     * Creates an ISO {@code Metadata} object from the information found in the netCDF file.
     * The returned metadata is unmodifiable, for allowing the caller to share a unique instance.
     *
     * @return the ISO metadata object.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    public Metadata read() throws IOException, DataStoreException {
        for (final CoordinateReferenceSystem crs : decoder.getReferenceSystemInfo()) {
            addReferenceSystem(crs);
            if (verticalCRS == null) {
                verticalCRS = CRS.getVerticalComponent(crs, false);
            }
        }
        addResourceScope(ScopeCode.DATASET, null);
        addIdentificationInfo(addCitation());
        for (final String service : SERVICES) {
            final String name = stringValue(service);
            if (name != null) {
                addResourceScope(ScopeCode.SERVICE, name);
            }
        }
        addAcquisitionInfo();
        addContentInfo();
        /*
         * Add the dimension information, if any. This metadata node
         * is built from the netCDF CoordinateSystem objects.
         */
        for (final Grid cs : decoder.getGridCandidates()) {
            if (cs.getSourceDimensions() >= Grid.MIN_DIMENSION) {
                final Axis[] axes = cs.getAxes(decoder);
                if (axes.length >= Grid.MIN_DIMENSION) {
                    addSpatialRepresentationInfo(axes);
                }
            }
        }
        setISOStandards(hasGridCoverages);
        addFileIdentifier();
        /*
         * Departure: UnidataDD2MI.xsl puts the source in Metadata.dataQualityInfo.lineage.statement.
         * However, since ISO 19115:2014, Metadata.resourceLineage.statement seems a more appropriate place.
         * See https://issues.apache.org/jira/browse/SIS-361
         */
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            addLineage(stringValue(HISTORY));
            addSource(stringValue(SOURCE), null, null);
        }
        decoder.setSearchPath(searchPath);
        final DefaultMetadata metadata = build();
        addCompleteMetadata(createURI(stringValue(METADATA_LINK)));
        metadata.transitionTo(DefaultMetadata.State.FINAL);
        return metadata;
    }
}
