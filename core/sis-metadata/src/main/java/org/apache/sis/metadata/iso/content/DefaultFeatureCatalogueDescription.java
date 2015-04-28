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

import java.util.Locale;
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information identifying the feature catalogue or the conceptual schema.
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
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_FeatureCatalogueDescription_Type", propOrder = {
    "compliant",
    "languages",
    "includedWithDataset",
    "featureTypes",
    "featureCatalogueCitations"
})
@XmlRootElement(name = "MD_FeatureCatalogueDescription")
public class DefaultFeatureCatalogueDescription extends AbstractContentInformation
        implements FeatureCatalogueDescription
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5731044701122380718L;

    /**
     * Whether or not the cited feature catalogue complies with ISO 19110.
     *
     * <p>Implementation note: we need to store the reference to the {@code Boolean} instance instead
     * than using bitmask because {@link org.apache.sis.internal.jaxb.PrimitiveTypeProperties} may
     * associate some properties to that particular instance.</p>
     */
    private Boolean compliant;

    /**
     * Language(s) used within the catalogue
     */
    private Collection<Locale> languages;

    /**
     * Whether or not the feature catalogue is included with the resource.
     */
    private boolean includedWithDataset;

    /**
     * Subset of feature types from cited feature catalogue occurring in resource.
     */
    private Collection<DefaultFeatureTypeInfo> featureTypes;

    /**
     * Complete bibliographic reference to one or more external feature catalogues.
     */
    private Collection<Citation> featureCatalogueCitations;

    /**
     * Constructs an initially empty feature catalogue description.
     */
    public DefaultFeatureCatalogueDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(FeatureCatalogueDescription)
     */
    public DefaultFeatureCatalogueDescription(final FeatureCatalogueDescription object) {
        super(object);
        if (object != null) {
            compliant                 = object.isCompliant();
            includedWithDataset       = object.isIncludedWithDataset();
            languages                 = copyCollection(object.getLanguages(), Locale.class);
            featureCatalogueCitations = copyCollection(object.getFeatureCatalogueCitations(), Citation.class);
            if (object instanceof DefaultFeatureCatalogueDescription) {
                featureTypes = copyCollection(((DefaultFeatureCatalogueDescription) object).getFeatureTypeInfo(), DefaultFeatureTypeInfo.class);
            } else {
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
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return Whether or not the cited feature catalogue complies with ISO 19110, or {@code null}.
     */
    @Override
    @XmlElement(name = "complianceCode")
    public Boolean isCompliant() {
        return compliant;
    }

    /**
     * Sets whether or not the cited feature catalogue complies with ISO 19110.
     *
     * @param newValue The new compliance value.
     */
    public void setCompliant(final Boolean newValue) {
        checkWritePermission();
        compliant = newValue;
    }

    /**
     * Returns the language(s) used within the catalogue
     *
     * @return Language(s) used within the catalogue.
     */
    @Override
    @XmlElement(name = "language")
    public Collection<Locale> getLanguages() {
        return languages = nonNullCollection(languages, Locale.class);
    }

    /**
     * Sets the language(s) used within the catalogue
     *
     * @param newValues The new languages.
     */
    public void setLanguages(final Collection<? extends Locale> newValues) {
        languages = writeCollection(newValues, languages, Locale.class);
    }

    /**
     * Returns whether or not the feature catalogue is included with the resource.
     *
     * @return Whether or not the feature catalogue is included with the resource.
     */
    @Override
    @XmlElement(name = "includedWithDataset", required = true)
    public boolean isIncludedWithDataset() {
        return includedWithDataset;
    }

    /**
     * Sets whether or not the feature catalogue is included with the resource.
     *
     * @param newValue {@code true} if the feature catalogue is included.
     */
    public void setIncludedWithDataset(final boolean newValue) {
        checkWritePermission();
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
     * @return Subset of feature types occurring in resource.
     *
     * @since 0.5
     */
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
     * @param newValues The new feature types.
     *
     * @since 0.5
     */
    public void setFeatureTypeInfo(final Collection<? extends DefaultFeatureTypeInfo> newValues) {
        featureTypes = writeCollection(newValues, featureTypes, DefaultFeatureTypeInfo.class);
    }

    /**
     * Returns the names of {@linkplain #getFeatureTypes() feature types}.
     *
     * @return The feature type names.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getFeatureTypeInfo()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "featureTypes")
    public final Collection<GenericName> getFeatureTypes() {
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
     * @param newValues The new feature type names.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setFeatureTypeInfo(Collection)}.
     */
    @Deprecated
    public void setFeatureTypes(final Collection<? extends GenericName> newValues) {
        checkWritePermission();
        ((LegacyPropertyAdapter<GenericName,?>) getFeatureTypes()).setValues(newValues);
    }

    /**
     * Returns the complete bibliographic reference to one or more external feature catalogues.
     *
     * @return Bibliographic reference to one or more external feature catalogues.
     */
    @Override
    @XmlElement(name = "featureCatalogueCitation", required = true)
    public Collection<Citation> getFeatureCatalogueCitations() {
        return featureCatalogueCitations = nonNullCollection(featureCatalogueCitations, Citation.class);
    }

    /**
     * Sets the complete bibliographic reference to one or more external feature catalogues.
     *
     * @param newValues The feature catalogue citations.
     */
    public void setFeatureCatalogueCitations(final Collection<? extends Citation> newValues) {
        featureCatalogueCitations = writeCollection(newValues, featureCatalogueCitations, Citation.class);
    }
}
