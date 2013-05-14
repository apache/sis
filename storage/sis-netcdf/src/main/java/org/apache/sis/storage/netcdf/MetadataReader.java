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

import org.apache.sis.util.iso.Types;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.util.DefaultFactories;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.extent.*;
import org.apache.sis.metadata.iso.content.*;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.distribution.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.Variable;
import org.apache.sis.internal.netcdf.WarningProducer;

// The following dependency is used only for static final String constants.
// Consequently the compiled class files should not have this dependency.
import ucar.nc2.constants.CF;

import static org.apache.sis.storage.netcdf.AttributeNames.*;


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
 * {@section Known limitations}
 * <ul>
 *   <li>{@code "degrees_west"} and {@code "degrees_south"} units not correctly handled.</li>
 *   <li>Units of measurement not yet declared in the {@link Band} elements.</li>
 *   <li>{@link #FLAG_VALUES} and {@link #FLAG_MASKS} not yet included in the
 *       {@link RangeElementDescription} elements.</li>
 *   <li>Services (WMS, WCS, OPeNDAP, THREDDS) <i>etc.</i>) and transfer options not yet declared.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
final class MetadataReader extends WarningProducer {
    /**
     * Names of groups where to search for metadata, in precedence order.
     * The {@code null} value stands for global attributes.
     *
     * <p>REMINDER: if modified, update class javadoc too.</p>
     */
    private static final String[] SEARCH_PATH = {"NCISOMetadata", "CFMetadata", null, "THREDDSMetadata"};

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
     */
    private transient NameFactory nameFactory;

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
     * @param  parent Where to send the warnings, or {@code null} if none.
     * @param  decoder The source of NetCDF attributes.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    MetadataReader(final WarningProducer parent, final Decoder decoder) throws IOException {
        super(parent);
        this.decoder = decoder;
        decoder.setSearchPath(SEARCH_PATH);
        searchPath = decoder.getSearchPath();
    }

    /**
     * Returns the given string as an {@code InternationalString} if non-null, or {@code null} otherwise.
     */
    private static InternationalString toInternationalString(final String value) {
        return (value != null) ? new SimpleInternationalString(value) : null;
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
                collection = new LinkedHashSet<>(4);
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
    private static boolean isDefined(final CharSequence metadata, final String attribute) {
        return (attribute == null) || (metadata != null && metadata.toString().equals(attribute));
    }

    /**
     * Returns {@code true} if the given NetCDF attribute is either null or equals to one
     * of the values in the given collection.
     *
     * @param metadata  The value stored in the metadata object.
     * @param attribute The value parsed from the NetCDF file.
     */
    private static boolean isDefined(final Collection<String> metadata, final String attribute) {
        return (attribute == null) || metadata.contains(attribute);
    }

    /**
     * Returns {@code true} if the given URL is null, or if the given resource contains that URL.
     *
     * @param resource  The value stored in the metadata object.
     * @param url       The value parsed from the NetCDF file.
     */
    private static boolean isDefined(final OnlineResource resource, final String url) {
        return (url == null) || (resource != null && isDefined(resource.getLinkage().toString(), url));
    }

    /**
     * Returns {@code true} if the given email is null, or if the given address contains that email.
     *
     * @param address  The value stored in the metadata object.
     * @param email    The value parsed from the NetCDF file.
     */
    private static boolean isDefined(final Address address, final String email) {
        return (email == null) || (address != null && isDefined(address.getElectronicMailAddresses(), email));
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
            warning("createOnlineResource", e);
        }
        return null;
    }

    /**
     * Creates an {@code Address} element if at least one of the given attributes is non-null.
     */
    private static Address createAddress(final String email) {
        if (email != null) {
            final DefaultAddress address = new DefaultAddress();
            address.getElectronicMailAddresses().add(email);
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
            contact.setAddress(address);
            contact.setOnlineResource(url);
            return contact;
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
        String identifier = decoder.stringValue(IDENTIFIER);
        if (identifier == null) {
            identifier = decoder.getId();
            if (identifier == null) {
                return null;
            }
        }
        final String namespace  = decoder.stringValue(NAMING_AUTHORITY);
        return new DefaultIdentifier((namespace != null) ? new DefaultCitation(namespace) : null, identifier);
    }

    /**
     * Creates a {@code ResponsibleParty} element if at least one of the name, email or URL attributes is defined.
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
        final String individualName   = decoder.stringValue(keys.NAME);
        final String organisationName = decoder.stringValue(keys.INSTITUTION);
        final String email            = decoder.stringValue(keys.EMAIL);
        final String url              = decoder.stringValue(keys.URL);
        if (individualName == null && organisationName == null && email == null && url == null) {
            return null;
        }
        Role role = Types.forCodeName(Role.class, decoder.stringValue(keys.ROLE), true);
        if (role == null) {
            role = isPointOfContact ? Role.POINT_OF_CONTACT : keys.DEFAULT_ROLE;
        }
        ResponsibleParty party    = pointOfContact;
        Contact          contact  = null;
        Address          address  = null;
        OnlineResource   resource = null;
        if (party != null) {
            contact = party.getContactInfo();
            if (contact != null) {
                address  = contact.getAddress();
                resource = contact.getOnlineResource();
            }
            if (!isDefined(resource, url)) {
                resource = null;
                contact  = null; // Clear the parents all the way up to the root.
                party    = null;
            }
            if (!isDefined(address, email)) {
                address = null;
                contact = null; // Clear the parents all the way up to the root.
                party   = null;
            }
            if (party != null) {
                if (!isDefined(party.getOrganisationName(), organisationName) ||
                    !isDefined(party.getIndividualName(),   individualName))
                {
                    party = null;
                }
            }
        }
        if (party == null) {
            if (contact == null) {
                if (address  == null) address  = createAddress(email);
                if (resource == null) resource = createOnlineResource(url);
                contact = createContact(address, resource);
            }
            if (individualName != null || organisationName != null || contact != null) { // Do not test role.
                final DefaultResponsibleParty np = new DefaultResponsibleParty(role);
                np.setIndividualName(individualName);
                np.setOrganisationName(toInternationalString(organisationName));
                np.setContactInfo(contact);
                party = np;
            }
        }
        return party;
    }

    /**
     * Creates a {@code Citation} element if at least one of the required attributes is non-null.
     * This method will reuse the {@link #pointOfContact} field, if non-null and suitable.
     *
     * @param  identifier The citation {@code <gmd:identifier> attribute.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    private Citation createCitation(final Identifier identifier) throws IOException {
        String title = decoder.stringValue(TITLE);
        if (title == null) {
            title = decoder.stringValue("full_name"); // THREDDS attribute documented in TITLE javadoc.
            if (title == null) {
                title = decoder.stringValue("name"); // THREDDS attribute documented in TITLE javadoc.
                if (title == null) {
                    title = decoder.getTitle();
                }
            }
        }
        final Date   creation   = decoder.dateValue(DATE_CREATED);
        final Date   modified   = decoder.dateValue(DATE_MODIFIED);
        final Date   issued     = decoder.dateValue(DATE_ISSUED);
        final String references = decoder.stringValue(REFERENCES);
        final DefaultCitation citation = new DefaultCitation(title);
        if (identifier != null) {
            citation.getIdentifiers().add(identifier);
        }
        if (creation != null) citation.getDates().add(new DefaultCitationDate(creation, DateType.CREATION));
        if (modified != null) citation.getDates().add(new DefaultCitationDate(modified, DateType.REVISION));
        if (issued   != null) citation.getDates().add(new DefaultCitationDate(issued,   DateType.PUBLICATION));
        if (pointOfContact != null) {
            // Same responsible party than the contact, except for the role.
            final DefaultResponsibleParty np = new DefaultResponsibleParty(Role.ORIGINATOR);
            np.setIndividualName  (pointOfContact.getIndividualName());
            np.setOrganisationName(pointOfContact.getOrganisationName());
            np.setContactInfo     (pointOfContact.getContactInfo());
            citation.getCitedResponsibleParties().add(np);
        }
        for (final String path : searchPath) {
            decoder.setSearchPath(path);
            final ResponsibleParty contributor = createResponsibleParty(CONTRIBUTOR, false);
            if (contributor != null && contributor != pointOfContact) {
                addIfAbsent(citation.getCitedResponsibleParties(), contributor);
            }
        }
        decoder.setSearchPath(searchPath);
        citation.setOtherCitationDetails(toInternationalString(references));
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
            final String   topic    = decoder.stringValue(TOPIC_CATEGORY);
            final String   type     = decoder.stringValue(DATA_TYPE);
            final String   credits  = decoder.stringValue(ACKNOWLEDGMENT);
            final String   license  = decoder.stringValue(LICENSE);
            final String   access   = decoder.stringValue(ACCESS_CONSTRAINT);
            final Extent   extent   = hasExtent ? null : createExtent();
            if (standard!=null || keywords!=null || topic != null || type!=null || credits!=null || license!=null || access!= null || extent!=null) {
                if (identification == null) {
                    identification = new DefaultDataIdentification();
                }
                if (topic    != null) addIfAbsent(identification.getTopicCategories(), Types.forCodeName(TopicCategory.class, topic, true));
                if (type     != null) addIfAbsent(identification.getSpatialRepresentationTypes(), Types.forCodeName(SpatialRepresentationType.class, type, true));
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
                            addIfAbsent(constraints.getAccessConstraints(), Types.forCodeName(Restriction.class, keyword, true));
                        }
                    }
                }
                if (extent != null) {
                    // Takes only ONE extent, because a NetCDF file may declare many time the same
                    // extent with different precision. The groups are ordered in such a way that
                    // the first extent should be the most accurate one.
                    identification.getExtents().add(extent);
                    hasExtent = true;
                }
            }
            project = addIfNonNull(project, toInternationalString(decoder.stringValue(PROJECT)));
        }
        decoder.setSearchPath(searchPath);
        final Citation citation = createCitation(identifier);
        final String   summary  = decoder.stringValue(SUMMARY);
        final String   purpose  = decoder.stringValue(PURPOSE);
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
            identification.getPointOfContacts().add(pointOfContact);
        }
        addKeywords(identification, project,   "project"); // Not necessarily the same string than PROJECT.
        addKeywords(identification, publisher, "dataCenter");
        identification.setSupplementalInformation(toInternationalString(decoder.stringValue(COMMENT)));
        return identification;
    }

    /**
     * Adds the given keywords to the given identification info if the given set is non-null.
     */
    private static void addKeywords(final DefaultDataIdentification addTo,
            final Set<InternationalString> words, final String type)
    {
        if (words != null) {
            final DefaultKeywords keywords = new DefaultKeywords();
            keywords.setKeywords(words);
            keywords.setType(Types.forCodeName(KeywordType.class, type, true));
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
        final String list = decoder.stringValue(standard ? STANDARD_NAME : KEYWORDS);
        DefaultKeywords keywords = null;
        if (list != null) {
            final Set<InternationalString> words = new LinkedHashSet<>();
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
                final String vocabulary = decoder.stringValue(standard ? STANDARD_NAME_VOCABULARY : VOCABULARY);
                if (vocabulary != null) {
                    keywords.setThesaurusName(new DefaultCitation(vocabulary));
                }
            }
        }
        return keywords;
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
            extent = new DefaultExtent();
            extent.getGeographicElements().add(new DefaultGeographicBoundingBox(
                    valueOf(xmin, xConv), valueOf(xmax, xConv),
                    valueOf(ymin, yConv), valueOf(ymax, yConv)));
        }
        /*
         * If at least one vertical ordinates above is available, add a VerticalExtent.
         */
        if (zmin != null || zmax != null) {
            final UnitConverter c = getConverterTo(decoder.unitValue(VERTICAL.UNITS), SI.METRE);
            double min = valueOf(zmin, c);
            double max = valueOf(zmax, c);
            if (CF.POSITIVE_DOWN.equals(decoder.stringValue(VERTICAL.POSITIVE))) {
                final double tmp = min;
                min = -max;
                max = -tmp;
            }
            if (extent == null) {
                extent = new DefaultExtent();
            }
            extent.getVerticalElements().add(new DefaultVerticalExtent(min, max, VERTICAL_CRS));
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
                final String symbol = decoder.stringValue(TIME.UNITS);
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
            extent.getTemporalElements().add(t);
        } catch (UnsupportedOperationException e) {
            warning("createExtent", e);
        }
        /*
         * Add the geographic identifier, if present.
         */
        final String identifier = decoder.stringValue(GEOGRAPHIC_IDENTIFIER);
        if (identifier != null) {
            if (extent == null) {
                extent = new DefaultExtent();
            }
            extent.getGeographicElements().add(new DefaultGeographicDescription(null, identifier));
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
            warning("getConverterTo", e);
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
        final Map<List<String>, DefaultCoverageDescription> contents = new HashMap<>(4);
        final String processingLevel = decoder.stringValue(PROCESSING_LEVEL);
        final List<? extends Variable> variables = decoder.getVariables();
        for (final Variable variable : variables) {
            if (!variable.isCoverage(2)) {
                continue;
            }
            /*
             * Instantiate a CoverageDescription for each distinct set of NetCDF dimensions
             * (e.g. longitude,latitude,time). This separation is based on the fact that a
             * coverage has only one domain for every range of values.
             */
            final List<String> dimensions = variable.getDimensions();
            DefaultCoverageDescription content = contents.get(dimensions);
            if (content == null) {
                /*
                 * If there is some NetCDF attributes that can be stored only in the ImageDescription
                 * subclass, instantiate that subclass. Otherwise instantiate the more generic class.
                 */
                if (processingLevel != null) {
                    content = new DefaultImageDescription();
                    ((DefaultImageDescription) content).setProcessingLevelCode(new DefaultIdentifier(processingLevel));
                } else {
                    content = new DefaultCoverageDescription();
                }
                contents.put(dimensions, content);
            }
            content.getDimensions().add(createSampleDimension(variable));
            final Object[] names    = variable.getAttributeValues(FLAG_NAMES,    false);
            final Object[] meanings = variable.getAttributeValues(FLAG_MEANINGS, false);
            final Object[] masks    = variable.getAttributeValues(FLAG_MASKS,    true);
            final Object[] values   = variable.getAttributeValues(FLAG_VALUES,   true);
            final int length = Math.max(masks.length, Math.max(values.length, Math.max(names.length, meanings.length)));
            for (int i=0; i<length; i++) {
                final RangeElementDescription element = createRangeElementDescription(variable,
                        i < names   .length ? (String) names   [i] : null,
                        i < meanings.length ? (String) meanings[i] : null,
                        i < masks   .length ? (Number) masks   [i] : null,
                        i < values  .length ? (Number) values  [i] : null);
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
                nameFactory = DefaultFactories.forClass(NameFactory.class);
            }
            // TODO: should be band.setName(...) with ISO 19115:2011.
            // Sequence identifiers are supposed to be numbers only.
            band.setSequenceIdentifier(nameFactory.createMemberName(null, name,
                    nameFactory.createTypeName(null, variable.getDataTypeName())));
        }
        String description = variable.getDescription();
        if (description != null && !(description = description.trim()).isEmpty() && !description.equals(name)) {
            band.setDescriptor(toInternationalString(description));
        }
//TODO: Can't store the units, because the Band interface restricts it to length.
//      We need the SampleDimension interface proposed in ISO 19115 revision draft.
//      band.setUnits(Units.valueOf(variable.getUnitsString()));
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
            element.setName(toInternationalString(name));
            element.setDefinition(toInternationalString(meaning));
            // TODO: create a record from values (and possibly from the masks).
            //       if (pixel & mask == value) then we have that range element.
            return element;
        }
        return null;
    }

    /**
     * Creates an ISO {@code Metadata} object from the information found in the NetCDF file.
     *
     * @return The ISO metadata object.
     * @throws IOException If an I/O operation was necessary but failed.
     */
    public Metadata read() throws IOException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataStandardName(MetadataUtilities.STANDARD_NAME_2);
        metadata.setMetadataStandardVersion(MetadataUtilities.STANDARD_VERSION_2);
        final Identifier identifier = getFileIdentifier();
        if (identifier != null) {
            String code = identifier.getCode();
            final Citation authority = identifier.getAuthority();
            if (authority != null) {
                final InternationalString title = authority.getTitle();
                if (title != null) {
                    code = title.toString() + DefaultNameSpace.DEFAULT_SEPARATOR + code;
                }
            }
            metadata.setFileIdentifier(code);
        }
        metadata.setDateStamp(decoder.dateValue(METADATA_CREATION));
        metadata.getHierarchyLevels().add(ScopeCode.DATASET);
        final String wms = decoder.stringValue("wms_service");
        final String wcs = decoder.stringValue("wcs_service");
        if (wms != null || wcs != null) {
            metadata.getHierarchyLevels().add(ScopeCode.SERVICE);
        }
        /*
         * Add the ResponsibleParty which is declared in global attributes, or in
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
            final ResponsibleParty party = createResponsibleParty(PUBLISHER, false);
            if (party != null) {
                if (distribution == null) {
                    distribution = new DefaultDistribution();
                    metadata.setDistributionInfo(distribution);
                }
                final DefaultDistributor distributor = new DefaultDistributor(party);
                // TODO: There is some transfert option, etc. that we could set there.
                // See UnidataDD2MI.xsl for options for OPeNDAP, THREDDS, etc.
                addIfAbsent(distribution.getDistributors(), distributor);
                publisher = addIfNonNull(publisher, toInternationalString(party.getIndividualName()));
            }
            // Also add history.
            final String history = decoder.stringValue(HISTORY);
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
            metadata.getIdentificationInfo().add(identification);
        }
        metadata.setContentInfo(createContentInfo());
        /*
         * Add the dimension information, if any. This metadata node
         * is built from the NetCDF CoordinateSystem objects.
         */
        decoder.addSpatialRepresentationInfo(metadata);
        return metadata;
    }
}
