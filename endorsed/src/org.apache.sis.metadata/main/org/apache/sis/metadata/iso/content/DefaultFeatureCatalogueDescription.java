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
package org.apache.sis.metadata.iso.content;

import java.util.Map;
import java.util.Collection;
import java.util.Locale;
import java.nio.charset.Charset;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.lan.PT_Locale;
import org.apache.sis.xml.bind.lan.LocaleAndCharset;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;

// Specific to the main branch:
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information identifying the feature catalogue or the conceptual schema.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_FeatureCatalogueDescription}
 * {@code   ├─includedWithDataset…………………} Indication of whether or not the feature catalogue is included with the dataset.
 * {@code   └─featureCatalogueCitation……} Complete bibliographic reference to one or more external feature catalogues.
 * {@code       ├─title……………………………………………} Name by which the cited resource is known.
 * {@code       └─date………………………………………………} Reference date for the cited resource.</div>
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
@XmlType(name = "MD_FeatureCatalogueDescription_Type", propOrder = {
    "compliant",
    "locale",                       // New in ISO 19115:2014
    "languages",                    // Legacy ISO 19115:2003
    "includedWithDataset",
    "featureTypesInfo",             // New in ISO 19115:2014. Actual name is "featureTypeInfo"
    "featureTypes",                 // Legacy ISO 19115:2003
    "featureCatalogueCitations"
})
@XmlRootElement(name = "MD_FeatureCatalogueDescription")
public class DefaultFeatureCatalogueDescription extends AbstractContentInformation
        implements FeatureCatalogueDescription
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4637544662644655274L;

    /**
     * Whether or not the cited feature catalogue complies with ISO 19110.
     * May be {@code null} is unspecified.
     */
    private Boolean compliant;

    /**
     * Language(s) and character set(s) used within the catalogue.
     */
    @SuppressWarnings("serial")
    private Map<Locale,Charset> locales;

    /**
     * Whether or not the feature catalogue is included with the resource.
     */
    private boolean includedWithDataset;

    /**
     * Subset of feature types from cited feature catalogue occurring in resource.
     */
    @SuppressWarnings("serial")
    private Collection<DefaultFeatureTypeInfo> featureTypes;

    /**
     * Complete bibliographic reference to one or more external feature catalogues.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> featureCatalogueCitations;

    /**
     * Constructs an initially empty feature catalogue description.
     */
    public DefaultFeatureCatalogueDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(FeatureCatalogueDescription)
     */
    public DefaultFeatureCatalogueDescription(final FeatureCatalogueDescription object) {
        super(object);
        if (object != null) {
            compliant                 = object.isCompliant();
            includedWithDataset       = object.isIncludedWithDataset();
            featureCatalogueCitations = copyCollection(object.getFeatureCatalogueCitations(), Citation.class);
            if (object instanceof DefaultFeatureCatalogueDescription) {
                locales = copyMap(((DefaultFeatureCatalogueDescription) object).getLocalesAndCharsets(), Locale.class);
                featureTypes = copyCollection(((DefaultFeatureCatalogueDescription) object).getFeatureTypeInfo(), DefaultFeatureTypeInfo.class);
            } else {
                setLanguages(copyCollection(object.getLanguages(), Locale.class));
                setFeatureTypes(object.getFeatureTypes());
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
     *       {@code DefaultFeatureCatalogueDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultFeatureCatalogueDescription} instance is created using the
     *       {@linkplain #DefaultFeatureCatalogueDescription(FeatureCatalogueDescription) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFeatureCatalogueDescription castOrCopy(final FeatureCatalogueDescription object) {
        if (object == null || object instanceof DefaultFeatureCatalogueDescription) {
            return (DefaultFeatureCatalogueDescription) object;
        }
        return new DefaultFeatureCatalogueDescription(object);
    }

    /**
     * Returns whether or not the cited feature catalogue complies with ISO 19110.
     *
     * @return whether or not the cited feature catalogue complies with ISO 19110, or {@code null}.
     */
    @Override
    @XmlElement(name = "complianceCode")
    public Boolean isCompliant() {
        return compliant;
    }

    /**
     * Sets whether or not the cited feature catalogue complies with ISO 19110.
     *
     * @param  newValue  the new compliance value.
     */
    public void setCompliant(final Boolean newValue) {
        checkWritePermission(compliant);
        compliant = newValue;
    }

    /**
     * Returns the language(s) and character set(s) used within the catalogue.
     *
     * @return language(s) and character set(s) used within the catalogue.
     *
     * @since 1.0
     */
    @UML(identifier="locale", obligation=CONDITIONAL, specification=ISO_19115)
    // @XmlElement at the end of this class.
    public Map<Locale,Charset> getLocalesAndCharsets() {
        return locales = nonNullMap(locales, Locale.class);
    }

    /**
     * Sets the language(s) and character set(s) used within the catalogue.
     *
     * @param  newValues  the new language(s) and character set(s) used within the catalogue.
     *
     * @since 1.0
     */
    public void setLocalesAndCharsets(final Map<? extends Locale, ? extends Charset> newValues) {
        locales = writeMap(newValues, locales, Locale.class);
    }

    /**
     * Returns the language(s) used within the catalogue.
     *
     * @return language(s) used within the catalogue.
     *
     * @deprecated Replaced by {@code getLocalesAndCharsets().keySet()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getLocalesAndCharsets")
    @XmlElement(name = "language", namespace = LegacyNamespaces.GMD)
    public Collection<Locale> getLanguages() {
        return FilterByVersion.LEGACY_METADATA.accept() ? LocaleAndCharset.getLanguages(getLocalesAndCharsets()) : null;
    }

    /**
     * Sets the language(s) used within the catalogue.
     *
     * @param  newValues  the new languages.
     *
     * @deprecated Replaced by putting keys in {@link #getLocalesAndCharsets()} map.
     */
    @Deprecated(since="1.0")
    public void setLanguages(final Collection<? extends Locale> newValues) {
        // TODO: delete after SIS 1.0 release (method not needed by JAXB).
        setLocalesAndCharsets(LocaleAndCharset.setLanguages(getLocalesAndCharsets(), newValues));
    }

    /**
     * Returns whether or not the feature catalogue is included with the resource.
     *
     * @return whether or not the feature catalogue is included with the resource.
     */
    @Override
    @XmlElement(name = "includedWithDataset")
    public boolean isIncludedWithDataset() {
        return includedWithDataset;
    }

    /**
     * Sets whether or not the feature catalogue is included with the resource.
     *
     * @param  newValue  {@code true} if the feature catalogue is included.
     */
    public void setIncludedWithDataset(final boolean newValue) {
        checkWritePermission(includedWithDataset ? Boolean.TRUE : null);
        includedWithDataset = newValue;
    }

    /**
     * Returns the subset of feature types from cited feature catalogue occurring in resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code FeatureTypeInfo} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @return subset of feature types occurring in resource.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="featureTypes", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<DefaultFeatureTypeInfo> getFeatureTypeInfo() {
        return featureTypes = nonNullCollection(featureTypes, DefaultFeatureTypeInfo.class);
    }

    /**
     * Sets the subset of feature types from cited feature catalogue occurring in resource.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * The element type will be changed to the {@code FeatureTypeInfo} interface
     * when GeoAPI will provide it (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param  newValues  the new feature types.
     *
     * @since 0.5
     */
    public void setFeatureTypeInfo(final Collection<? extends DefaultFeatureTypeInfo> newValues) {
        featureTypes = writeCollection(newValues, featureTypes, DefaultFeatureTypeInfo.class);
    }

    /**
     * Returns the names of {@linkplain #getFeatureTypes() feature types}.
     *
     * @return the feature type names.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getFeatureTypeInfo()}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getFeatureTypeInfo")
    @XmlElement(name = "featureTypes", namespace = LegacyNamespaces.GMD)
    public final Collection<GenericName> getFeatureTypes() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new LegacyPropertyAdapter<GenericName,DefaultFeatureTypeInfo>(getFeatureTypeInfo()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected DefaultFeatureTypeInfo wrap(final GenericName value) {
                return new DefaultFeatureTypeInfo(value);
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected GenericName unwrap(final DefaultFeatureTypeInfo container) {
                return container.getFeatureTypeName();
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final DefaultFeatureTypeInfo container, final GenericName value) {
                if (container instanceof DefaultFeatureTypeInfo) {
                    container.setFeatureTypeName(value);
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets the names of {@linkplain #getFeatureTypes() feature types}.
     *
     * @param  newValues  the new feature type names.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setFeatureTypeInfo(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setFeatureTypes(final Collection<? extends GenericName> newValues) {
        checkWritePermission(valueIfDefined(featureTypes));
        ((LegacyPropertyAdapter<GenericName,?>) getFeatureTypes()).setValues(newValues);
    }

    /**
     * Returns the complete bibliographic reference to one or more external feature catalogues.
     *
     * @return bibliographic reference to one or more external feature catalogues.
     */
    @Override
    @XmlElement(name = "featureCatalogueCitation")
    public Collection<Citation> getFeatureCatalogueCitations() {
        return featureCatalogueCitations = nonNullCollection(featureCatalogueCitations, Citation.class);
    }

    /**
     * Sets the complete bibliographic reference to one or more external feature catalogues.
     *
     * @param  newValues  the feature catalogue citations.
     */
    public void setFeatureCatalogueCitations(final Collection<? extends Citation> newValues) {
        featureCatalogueCitations = writeCollection(newValues, featureCatalogueCitations, Citation.class);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "featureTypes")
    private Collection<DefaultFeatureTypeInfo> getFeatureTypesInfo() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getFeatureTypeInfo() : null;
    }

    /**
     * Returns the locales and character sets to marshal if the XML document is to be written
     * according the new ISO 19115:2014 model.
     */
    @XmlElement(name = "locale")
    private Collection<PT_Locale> getLocale() {
        return FilterByVersion.CURRENT_METADATA.accept() ? PT_Locale.wrap(locales) : null;
    }
}
