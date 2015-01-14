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

import java.util.Date;
import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import java.nio.charset.Charset;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.MetadataExtensionInformation;
import org.opengis.metadata.ApplicationSchemaInformation;
import org.opengis.metadata.PortrayalCatalogueReference;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.internal.jaxb.gmd.LocaleAdapter;
import org.apache.sis.xml.Namespaces;


/**
 * A dummy implementation of {@link Metadata} with minimal XML (un)marshalling capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
@XmlRootElement(name = "MD_Metadata", namespace = Namespaces.GMD)
public final strictfp class MetadataMock implements Metadata {
    /**
     * The language used for documenting metadata.
     */
    @XmlElement(namespace = Namespaces.GMD)
    @XmlJavaTypeAdapter(LocaleAdapter.class)
    public Locale language;

    /**
     * Creates an initially empty metadata.
     * This constructor is required by JAXB.
     */
    public MetadataMock() {
    }

    /**
     * Creates an initially empty metadata with the given language.
     * Callers are free to assign new value to the {@link #language} field directly.
     *
     * @param language The initial {@link #language} value (can be {@code null}).
     */
    public MetadataMock(final Locale language) {
        this.language = language;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Identifier getMetadataIdentifier() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public String getFileIdentifier() {
        return null;
    }

    /**
     * Returns {@link #language} in a singleton set or an empty set.
     *
     * @return {@link #language}
     */
    @Override
    public Collection<Locale> getLanguages() {
        return (language != null) ? Collections.<Locale>singleton(language) : Collections.<Locale>emptySet();
    }

    /**
     * Returns {@link #language}.
     *
     * @return {@link #language}
     */
    @Override
    @Deprecated
    public Locale getLanguage() {
        return language;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public Collection<Locale> getLocales() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<Charset> getCharacterSets() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public CharacterSet getCharacterSet() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Citation getParentMetadata() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public String getParentIdentifier() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends MetadataScope> getMetadataScopes() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public Collection<ScopeCode> getHierarchyLevels() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public Collection<String> getHierarchyLevelNames() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Responsibility> getContacts() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends CitationDate> getDateInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public Date getDateStamp() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Citation> getMetadataStandards() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public String getMetadataStandardName() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public String getMetadataStandardVersion() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Citation> getMetadataProfiles() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Citation> getAlternativeMetadataReferences() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends OnlineResource> getMetadataLinkages() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    @Deprecated
    public String getDataSetUri() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends SpatialRepresentation> getSpatialRepresentationInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends ReferenceSystem> getReferenceSystemInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends MetadataExtensionInformation> getMetadataExtensionInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Identification> getIdentificationInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends ContentInformation> getContentInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Distribution> getDistributionInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends DataQuality> getDataQualityInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends PortrayalCatalogueReference> getPortrayalCatalogueInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Constraints> getMetadataConstraints() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends ApplicationSchemaInformation> getApplicationSchemaInfo() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public MaintenanceInformation getMetadataMaintenance() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends AcquisitionInformation> getAcquisitionInformation() {
        return null;
    }

    /**
     * Undefined property.
     * @return {@code null}.
     */
    @Override
    public Collection<? extends Lineage> getResourceLineages() {
        return null;
    }
}
