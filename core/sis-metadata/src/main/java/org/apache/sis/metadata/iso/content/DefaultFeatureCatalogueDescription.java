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
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.FeatureCatalogueDescription;

import static org.apache.sis.internal.metadata.MetadataUtilities.getBoolean;
import static org.apache.sis.internal.metadata.MetadataUtilities.setBoolean;


/**
 * Information identifying the feature catalogue.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
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
    private static final long serialVersionUID = -3626075463499626813L;

    /**
     * Mask for the {@code compliant} {@link Boolean} value.
     * Needs 2 bits since the values can be {@code true}, {@code false} or {@code null}.
     *
     * @see #booleans
     */
    private static final byte COMPLIANT_MASK = 0b011;

    /**
     * Mask for the {@code includedWithDataset} {@code boolean} value.
     * Needs only 1 bit because the value can not be {@code null}.
     *
     * @see #booleans
     */
    private static final byte INCLUDED_MASK = 0b100;

    /**
     * Language(s) used within the catalogue
     */
    private Collection<Locale> languages;

    /**
     * Subset of feature types from cited feature catalogue occurring in dataset.
     */
    private Collection<GenericName> featureTypes;

    /**
     * Complete bibliographic reference to one or more external feature catalogues.
     */
    private Collection<Citation> featureCatalogueCitations;

    /**
     * The set of {@code boolean} and {@link Boolean} values.
     * Bits are read and written using the {@code *_MASK} constants.
     *
     * @see #COMPLIANT_MASK
     * @see #INCLUDED_MASK
     */
    private byte booleans;

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
            booleans                  = object.isIncludedWithDataset() ? INCLUDED_MASK : 0;
            booleans                  = (byte) setBoolean(booleans, COMPLIANT_MASK, object.isCompliant());
            languages                 = copyCollection(object.getLanguages(), Locale.class);
            featureTypes              = copyCollection(object.getFeatureTypes(), GenericName.class);
            featureCatalogueCitations = copyCollection(object.getFeatureCatalogueCitations(), Citation.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     */
    @Override
    @XmlElement(name = "complianceCode")
    public Boolean isCompliant() {
        return getBoolean(booleans, COMPLIANT_MASK);
    }

    /**
     * Sets whether or not the cited feature catalogue complies with ISO 19110.
     *
     * @param newValue The new compliance value.
     */
    public void setCompliant(final Boolean newValue) {
        checkWritePermission();
        booleans = (byte) setBoolean(booleans, COMPLIANT_MASK, newValue);
    }

    /**
     * Returns the language(s) used within the catalogue
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
     * Returns whether or not the feature catalogue is included with the dataset.
     */
    @Override
    @XmlElement(name = "includedWithDataset", required = true)
    public boolean isIncludedWithDataset() {
        return (booleans & INCLUDED_MASK) != 0;
    }

    /**
     * Sets whether or not the feature catalogue is included with the dataset.
     *
     * @param newValue {@code true} if the feature catalogue is included.
     */
    public void setIncludedWithDataset(final boolean newValue) {
        checkWritePermission();
        if (newValue) {
            booleans |= INCLUDED_MASK;
        } else {
            booleans &= ~INCLUDED_MASK;
        }
    }

    /**
     * Returns the subset of feature types from cited feature catalogue occurring in dataset.
     */
    @Override
    @XmlElement(name = "featureTypes")
    public Collection<GenericName> getFeatureTypes() {
        return featureTypes = nonNullCollection(featureTypes, GenericName.class);
    }

    /**
     * Sets the subset of feature types from cited feature catalogue occurring in dataset.
     *
     * @param newValues The new feature types.
     */
    public void setFeatureTypes(final Collection<? extends GenericName> newValues) {
        featureTypes = writeCollection(newValues, featureTypes, GenericName.class);
    }

    /**
     * Returns the complete bibliographic reference to one or more external feature catalogues.
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
