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
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.Distributor;
import org.apache.sis.internal.metadata.Dependencies;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.BiConsumer;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Description of the computer language construct that specifies the representation
 * of data objects in a record, file, message, storage device or transmission channel.
 * Each {@code Format} instance should contain a {@linkplain #getFormatSpecificationCitation() reference
 * to the format specification}, for example <cite>"PNG (Portable Network Graphics) Specification"</cite>.
 * The specification often has an abbreviation (for example "PNG") which can be stored as an
 * {@linkplain DefaultCitation#getAlternateTitles() alternate title}.
 *
 * <p>Apache SIS provides pre-defined metadata structures for some commonly-used formats.
 * A pre-defined format can be obtained by a call to
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
 * <p><b>Limitations:</b></p>
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
 * @version 0.8
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@XmlType(name = "MD_Format_Type", propOrder = {
    "name",
    "version",
    "amendmentNumber",
    "specification",
    "fileDecompressionTechnique",
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
    private Citation formatSpecificationCitation;

    /**
     * Amendment number of the format version.
     */
    private InternationalString amendmentNumber;

    /**
     * Recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     */
    private InternationalString fileDecompressionTechnique;

    /**
     * Media used by the format.
     */
    private Collection<Medium> media;

    /**
     * Provides information about the distributor's format.
     */
    private Collection<Distributor> formatDistributors;

    /**
     * Constructs an initially empty format.
     */
    public DefaultFormat() {
    }

    /**
     * Creates a format initialized to the given name and version.
     * The given name should be a short name or abbreviation, for example "JPEG" or "GeoTIFF".
     *
     * @param  name     the abbreviated name of the data transfer format, or {@code null}.
     * @param  version  the version of the format (date, number, <i>etc.</i>), or {@code null}.
     *
     * @deprecated This constructor had a straightforward meaning in ISO 19115:2003, but became confusing
     *             with the ISO 19115:2014 update because of differences in the {@code Format} model.
     *             Consider using {@link org.apache.sis.metadata.sql.MetadataSource#lookup(Class, String)} instead.
     */
    @Deprecated
    public DefaultFormat(CharSequence name, final CharSequence version) {
        final DefaultCitation citation = new DefaultCitation();
        if (name != null) {
            citation.setAlternateTitles(Collections.singleton(Types.toInternationalString(name)));
        }
        citation.setEdition(Types.toInternationalString(version));
        formatSpecificationCitation = citation;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
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
     *       {@linkplain #DefaultFormat(Format) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
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
/// @XmlElement(name = "formatSpecificationCitation", required = true)
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
        checkWritePermission();
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
    @Deprecated
    @XmlElement(name = "specification")
    @Dependencies("getFormatSpecificationCitation")
    public InternationalString getSpecification() {
        final Citation citation = getFormatSpecificationCitation();
        return (citation != null) ? citation.getTitle() : null;
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
    @Deprecated
    public void setSpecification(final InternationalString newValue) {
        checkWritePermission();
        setFormatSpecificationCitation(new BiConsumer<DefaultCitation,InternationalString>() {
            @Override public void accept(DefaultCitation citation, InternationalString value) {
                citation.setTitle(value);
            }
        }, newValue);
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
    @Deprecated
    @XmlElement(name = "name", required = true)
    @Dependencies("getFormatSpecificationCitation")
    public InternationalString getName() {
        final Citation citation = getFormatSpecificationCitation();
        if (citation != null) {
            return LegacyPropertyAdapter.getSingleton(citation.getAlternateTitles(),
                    InternationalString.class, null, DefaultFormat.class, "getName");
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
    @Deprecated
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        setFormatSpecificationCitation(new BiConsumer<DefaultCitation,InternationalString>() {
            @Override public void accept(DefaultCitation citation, InternationalString value) {
                citation.setAlternateTitles(LegacyPropertyAdapter.asCollection(value));
            }
        }, newValue);
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
    @Deprecated
    @XmlElement(name = "version", required = true)
    @Dependencies("getFormatSpecificationCitation")
    public InternationalString getVersion() {
        final Citation citation = getFormatSpecificationCitation();
        return (citation != null) ? citation.getEdition() : null;
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
    @Deprecated
    public void setVersion(final InternationalString newValue) {
        checkWritePermission();
        setFormatSpecificationCitation(new BiConsumer<DefaultCitation,InternationalString>() {
            @Override public void accept(DefaultCitation citation, InternationalString value) {
                citation.setEdition(value);
            }
        }, newValue);
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
        checkWritePermission();
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
        checkWritePermission();
        fileDecompressionTechnique = newValue;
    }

    /**
     * Returns the media used by the format.
     *
     * @return media used by the format.
     *
     * @since 0.5
     */
/// @XmlElement(name = "medium")
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
