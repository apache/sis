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
package org.apache.sis.metadata.iso.distribution;

import java.util.Collection;
import java.util.function.BiConsumer;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.Distributor;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.metadata.MD_Medium;
import org.apache.sis.xml.bind.metadata.CI_Citation;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.metadata.iso.citation.DefaultCitation;

// Specific to the main branch:
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Description of the computer language construct that specifies the representation
 * of data objects in a record, file, message, storage device or transmission channel.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Format}
 * {@code   └─formatSpecificationCitation……} Citation/URL of the specification format.
 * {@code       ├─title……………………………………………………} Name by which the cited resource is known.
 * {@code       └─date………………………………………………………} Reference date for the cited resource.</div>
 *
 * Each {@code Format} instance should contain a {@linkplain #getFormatSpecificationCitation() reference
 * to the format specification}, for example <q>PNG (Portable Network Graphics) Specification</q>.
 * The specification often has an abbreviation (for example "PNG") which can be stored as an
 * {@linkplain DefaultCitation#getAlternateTitles() alternate title}.
 *
 * <p>Apache SIS provides predefined metadata structures for some commonly-used formats.
 * A predefined format can be obtained by a call to
 * <code>{@linkplain org.apache.sis.metadata.sql.MetadataSource#lookup(Class, String) lookup}(Format.class,
 * <var>abbreviation</var>)</code> where <var>abbreviation</var> can be one of the values listed below:</p>
 *
 * <table class="sis">
 *   <caption>Specification titles for well-known format names</caption>
 *   <tr><th>Abbreviation</th> <th>Specification title</th></tr>
 *   <tr><td>CSV</td>          <td>Common Format and MIME Type for Comma-Separated Values (CSV) Files</td></tr>
 *   <tr><td>GeoTIFF</td>      <td>GeoTIFF Coverage Encoding Profile</td></tr>
 *   <tr><td>NetCDF</td>       <td>NetCDF Classic and 64-bit Offset Format</td></tr>
 *   <tr><td>PNG</td>          <td>PNG (Portable Network Graphics) Specification</td></tr>
 * </table>
 *
 * Above list may be expanded in any future SIS version.
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_Format_Type", propOrder = {
    // ISO 19115:2003 (legacy)
    "name",
    "version",
    "amendmentNumber",
    "specification",

    // ISO 19115:2014
    "formatSpecificationCitation",
    "fileDecompressionTechnique",
    "media",
    "formatDistributors"
})
@XmlRootElement(name = "MD_Format")
public class DefaultFormat extends ISOMetadata implements Format {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8346373589075887348L;

    /**
     * Citation / URL of the specification format.
     */
    @SuppressWarnings("serial")
    private Citation formatSpecificationCitation;

    /**
     * Amendment number of the format version.
     */
    @SuppressWarnings("serial")
    private InternationalString amendmentNumber;

    /**
     * Recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     */
    @SuppressWarnings("serial")
    private InternationalString fileDecompressionTechnique;

    /**
     * Media used by the format.
     */
    @SuppressWarnings("serial")
    private Collection<Medium> media;

    /**
     * Provides information about the distributor's format.
     */
    @SuppressWarnings("serial")
    private Collection<Distributor> formatDistributors;

    /**
     * Constructs an initially empty format.
     */
    public DefaultFormat() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Format)
     */
    public DefaultFormat(final Format object) {
        super(object);
        if (object != null) {
            amendmentNumber             = object.getAmendmentNumber();
            fileDecompressionTechnique  = object.getFileDecompressionTechnique();
            formatDistributors          = copyCollection(object.getFormatDistributors(), Distributor.class);
            if (object instanceof DefaultFormat) {
                formatSpecificationCitation = ((DefaultFormat) object).getFormatSpecificationCitation();
                media = copyCollection(((DefaultFormat) object).getMedia(), Medium.class);
            } else {
                setSpecification(object.getSpecification());
                setVersion(object.getVersion());
                setName(object.getName());
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultFormat}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultFormat} instance is created using the
     *       {@linkplain #DefaultFormat(Format) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFormat castOrCopy(final Format object) {
        if (object == null || object instanceof DefaultFormat) {
            return (DefaultFormat) object;
        }
        return new DefaultFormat(object);
    }

    /**
     * Returns the citation / URL of the specification format.
     *
     * @return citation / URL of the specification format.
     *
     * @since 0.5
     */
    @XmlElement(name = "formatSpecificationCitation", required = true)
    @XmlJavaTypeAdapter(CI_Citation.Since2014.class)
    @UML(identifier="formatSpecificationCitation", obligation=MANDATORY, specification=ISO_19115)
    public Citation getFormatSpecificationCitation() {
        return formatSpecificationCitation;
    }

    /**
     * Sets the citation / URL of the specification format.
     *
     * @param  newValue  the new specification format.
     *
     * @since 0.5
     */
    public void setFormatSpecificationCitation(final Citation newValue) {
        checkWritePermission(formatSpecificationCitation);
        formatSpecificationCitation = newValue;
    }

    /**
     * Sets the specification title or version.
     */
    private <T> void setFormatSpecificationCitation(final BiConsumer<DefaultCitation,T> setter, final T value) {
        Citation citation = formatSpecificationCitation;
        if (citation != null || value != null) {
            if (!(citation instanceof DefaultCitation)) {
                citation = new DefaultCitation(citation);
            }
            setter.accept((DefaultCitation) citation, value);
            if (value == null && ((DefaultCitation) citation).isEmpty()) {
                citation = null;
            }
        }
        // Invoke the non-deprecated setter method only if the reference changed,
        // for consistency with other deprecated setter methods in metadata module.
        if (citation != formatSpecificationCitation) {
            setFormatSpecificationCitation(citation);
        }
    }

    /**
     * Returns the name of a subset, profile, or product specification of the format.
     *
     * @return name of a subset, profile, or product specification of the format, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by
     * <code>{@linkplain #getFormatSpecificationCitation()}.{@linkplain DefaultCitation#getTitle() getTitle()}</code>.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getFormatSpecificationCitation")
    @XmlElement(name = "specification", namespace = LegacyNamespaces.GMD)
    public InternationalString getSpecification() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Citation citation = getFormatSpecificationCitation();
            if (citation != null) {
                return citation.getTitle();
            }
        }
        return null;
    }

    /**
     * Sets the name of a subset, profile, or product specification of the format.
     *
     * @param  newValue  the new specification.
     *
     * @deprecated As of ISO 19115:2014, replaced by
     * <code>{@linkplain #getFormatSpecificationCitation()}.{@linkplain DefaultCitation#setTitle(InternationalString)
     * setTitle(InternationalString)}</code>.
     */
    @Deprecated(since="1.0")
    public void setSpecification(final InternationalString newValue) {
        checkWritePermission(formatSpecificationCitation);
        setFormatSpecificationCitation((citation, value) -> citation.setTitle(value), newValue);
    }

    /**
     * Returns the name of the data transfer format(s).
     *
     * @return name of the data transfer format(s), or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by
     * <code>{@linkplain #getFormatSpecificationCitation()}.{@linkplain DefaultCitation#getAlternateTitles()
     * getAlternateTitles()}</code>. Note that citation alternate titles are often used for abbreviations.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getFormatSpecificationCitation")
    @XmlElement(name = "name", namespace = LegacyNamespaces.GMD)
    public InternationalString getName() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Citation citation = getFormatSpecificationCitation();
            if (citation != null) {
                return LegacyPropertyAdapter.getSingleton(citation.getAlternateTitles(),
                        InternationalString.class, null, DefaultFormat.class, "getName");
            }
        }
        return null;
    }

    /**
     * Sets the name of the data transfer format(s).
     *
     * @param  newValue  the new name.
     *
     * @deprecated As of ISO 19115:2014, replaced by
     * <code>{@linkplain #getFormatSpecificationCitation()}.{@linkplain DefaultCitation#setAlternateTitles(Collection)
     * setAlternateTitles(Collection)}</code>.
     */
    @Deprecated(since="1.0")
    public void setName(final InternationalString newValue) {
        checkWritePermission(formatSpecificationCitation);
        setFormatSpecificationCitation((citation, value) ->
                citation.setAlternateTitles(CollectionsExt.singletonOrEmpty(value)), newValue);
    }

    /**
     * Returns the version of the format (date, number, etc.).
     *
     * @return version of the format, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by
     * <code>{@linkplain #getFormatSpecificationCitation()}.{@linkplain DefaultCitation#getEdition()
     * getEdition()}</code>.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getFormatSpecificationCitation")
    @XmlElement(name = "version", namespace = LegacyNamespaces.GMD)
    public InternationalString getVersion() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Citation citation = getFormatSpecificationCitation();
            if (citation != null) {
                return citation.getEdition();
            }
        }
        return null;
    }

    /**
     * Sets the version of the format (date, number, etc.).
     *
     * @param  newValue  the new version.
     *
     * @deprecated As of ISO 19115:2014, replaced by
     * <code>{@linkplain #getFormatSpecificationCitation()}.{@linkplain DefaultCitation#setEdition(InternationalString)
     * setEdition(InternationalString)}</code>.
     */
    @Deprecated(since="1.0")
    public void setVersion(final InternationalString newValue) {
        checkWritePermission(formatSpecificationCitation);
        setFormatSpecificationCitation((citation, value) -> citation.setEdition(value), newValue);
    }

    /**
     * Returns the amendment number of the format version.
     *
     * @return amendment number of the format version, or {@code null}.
     */
    @Override
    @XmlElement(name = "amendmentNumber")
    public InternationalString getAmendmentNumber() {
        return amendmentNumber;
    }

    /**
     * Sets the amendment number of the format version.
     *
     * @param  newValue  the new amendment number.
     */
    public void setAmendmentNumber(final InternationalString newValue) {
        checkWritePermission(amendmentNumber);
        amendmentNumber = newValue;
    }

    /**
     * Returns recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     *
     * @return processes that can be applied to read resources to which compression techniques have
     *         been applied, or {@code null}.
     */
    @Override
    @XmlElement(name = "fileDecompressionTechnique")
    public InternationalString getFileDecompressionTechnique() {
        return fileDecompressionTechnique;
    }

    /**
     * Sets recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     *
     * @param  newValue  the new file decompression technique.
     */
    public void setFileDecompressionTechnique(final InternationalString newValue) {
        checkWritePermission(fileDecompressionTechnique);
        fileDecompressionTechnique = newValue;
    }

    /**
     * Returns the media used by the format.
     *
     * @return media used by the format.
     *
     * @since 0.5
     */
    @XmlElement(name = "medium")
    @XmlJavaTypeAdapter(MD_Medium.Since2014.class)
    @UML(identifier="medium", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Medium> getMedia() {
        return media = nonNullCollection(media, Medium.class);
    }

    /**
     * Sets the media used by the format.
     *
     * @param  newValues  the new media.
     *
     * @since 0.5
     */
    public void setMedia(final Collection<? extends Medium> newValues) {
        media = writeCollection(newValues, media, Medium.class);
    }

    /**
     * Provides information about the distributor's format.
     *
     * @return information about the distributor's format.
     */
    @Override
    @XmlElement(name = "formatDistributor")
    public Collection<Distributor> getFormatDistributors() {
        return formatDistributors = nonNullCollection(formatDistributors, Distributor.class);
    }

    /**
     * Sets information about the distributor's format.
     *
     * @param  newValues  the new format distributors.
     */
    public void setFormatDistributors(final Collection<? extends Distributor> newValues) {
        formatDistributors = writeCollection(newValues, formatDistributors, Distributor.class);
    }
}
