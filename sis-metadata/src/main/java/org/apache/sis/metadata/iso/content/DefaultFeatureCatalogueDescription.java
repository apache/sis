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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
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
    private static final long serialVersionUID = 1984922846251567908L;

    /**
     * Indication of whether or not the cited feature catalogue complies with ISO 19110.
     */
    private Boolean compliant;

    /**
     * Language(s) used within the catalogue
     */
    private Collection<Locale> languages;

    /**
     * Indication of whether or not the feature catalogue is included with the dataset.
     */
    private boolean includedWithDataset;

    /**
     * Subset of feature types from cited feature catalogue occurring in dataset.
     */
    private Collection<GenericName> featureTypes;

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
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFeatureCatalogueDescription castOrCopy(final FeatureCatalogueDescription object) {
        if (object == null || object instanceof DefaultFeatureCatalogueDescription) {
            return (DefaultFeatureCatalogueDescription) object;
        }
        final DefaultFeatureCatalogueDescription copy = new DefaultFeatureCatalogueDescription();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns whether or not the cited feature catalogue complies with ISO 19110.
     */
    @Override
    @XmlElement(name = "complianceCode")
    public synchronized Boolean isCompliant() {
        return compliant;
    }

    /**
     * Sets whether or not the cited feature catalogue complies with ISO 19110.
     *
     * @param newValue The new compliance value.
     */
    public synchronized void setCompliant(final Boolean newValue) {
        checkWritePermission();
        compliant = newValue;
    }

    /**
     * Returns the language(s) used within the catalogue
     */
    @Override
    @XmlElement(name = "language")
    public synchronized Collection<Locale> getLanguages() {
        return languages = nonNullCollection(languages, Locale.class);
    }

    /**
     * Sets the language(s) used within the catalogue
     *
     * @param newValues The new languages.
     */
    public synchronized void setLanguages(final Collection<? extends Locale> newValues) {
        languages = copyCollection(newValues, languages, Locale.class);
    }

    /**
     * Returns whether or not the feature catalogue is included with the dataset.
     */
    @Override
    @XmlElement(name = "includedWithDataset", required = true)
    public synchronized boolean isIncludedWithDataset() {
        return includedWithDataset;
    }

    /**
     * Sets whether or not the feature catalogue is included with the dataset.
     *
     * @param newValue {@code true} if the feature catalogue is included.
     */
    public synchronized void setIncludedWithDataset(final boolean newValue) {
        checkWritePermission();
        includedWithDataset = newValue;
    }

    /**
     * Returns the subset of feature types from cited feature catalogue occurring in dataset.
     */
    @Override
    @XmlElement(name = "featureTypes")
    public synchronized Collection<GenericName> getFeatureTypes() {
        return featureTypes = nonNullCollection(featureTypes, GenericName.class);
    }

    /**
     * Sets the subset of feature types from cited feature catalogue occurring in dataset.
     *
     * @param newValues The new feature types.
     */
    public synchronized void setFeatureTypes(final Collection<? extends GenericName> newValues) {
        featureTypes = copyCollection(newValues, featureTypes, GenericName.class);
    }

    /**
     * Returns the complete bibliographic reference to one or more external feature catalogues.
     */
    @Override
    @XmlElement(name = "featureCatalogueCitation", required = true)
    public synchronized Collection<Citation> getFeatureCatalogueCitations() {
        return featureCatalogueCitations = nonNullCollection(featureCatalogueCitations, Citation.class);
    }

    /**
     * Sets the complete bibliographic reference to one or more external feature catalogues.
     *
     * @param newValues The feature catalogue citations.
     */
    public synchronized void setFeatureCatalogueCitations(final Collection<? extends Citation> newValues) {
        featureCatalogueCitations = copyCollection(newValues, featureCatalogueCitations, Citation.class);
    }
}
