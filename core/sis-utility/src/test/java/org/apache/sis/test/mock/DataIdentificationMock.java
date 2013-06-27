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
package org.apache.sis.test.mock;

import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.AggregateInformation;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.Usage;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gmd.LocaleAdapter;


/**
 * A dummy implementation of {@link DataIdentification} with minimal XML (un)marshalling capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_DataIdentification_Type")
@XmlRootElement(name = "MD_DataIdentification", namespace = Namespaces.GMD)
public final strictfp class DataIdentificationMock implements DataIdentification {
    /**
     * The locale to (un)marshal as a language.
     */
    @XmlElement(namespace = Namespaces.GMD)
    @XmlJavaTypeAdapter(LocaleAdapter.class)
    public Locale language;

    /**
     * Creates an initially empty metadata.
     * This constructor is required by JAXB.
     */
    public DataIdentificationMock() {
    }

    /**
     * Creates an initially empty metadata with the given language.
     * Callers are free to assign new value to the {@link #language} field directly.
     *
     * @param language The initial {@link #language} value (can be {@code null}).
     */
    public DataIdentificationMock(final Locale language) {
        this.language = language;
    }

    /**
     * Returns {@link #language} in an unmodifiable collection.
     *
     * @return {@link #language} singleton.
     */
    @Override
    public Collection<Locale> getLanguages() {
        return (language != null) ? Collections.singleton(language) : Collections.<Locale>emptySet();
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Resolution> getSpatialResolutions() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<CharacterSet> getCharacterSets() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<TopicCategory> getTopicCategories() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public InternationalString getEnvironmentDescription() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Extent> getExtents() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public InternationalString getSupplementalInformation() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Citation getCitation() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public InternationalString getAbstract() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public InternationalString getPurpose() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<String> getCredits() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<Progress> getStatus() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends ResponsibleParty> getPointOfContacts() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends MaintenanceInformation> getResourceMaintenances() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends BrowseGraphic> getGraphicOverviews() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Format> getResourceFormats() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Keywords> getDescriptiveKeywords() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Usage> getResourceSpecificUsages() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Constraints> getResourceConstraints() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends AggregateInformation> getAggregationInfo() {
        return null;
    }
}
