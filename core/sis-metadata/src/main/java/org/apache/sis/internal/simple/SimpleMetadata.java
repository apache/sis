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
package org.apache.sis.internal.simple;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.util.InternationalString;


/**
 * An empty implementation of ISO 19115 metadata for dataset (not for services).
 * This simple implementation presumes that the metadata describes exactly one dataset,
 * which is described by implementing methods from the {@link DataIdentification} interface.
 * The identification information itself presumes that the dataset is referenced by exactly one citation.
 *
 * <p>Unless specified otherwise, all methods in this class returns {@code null} or an empty collection by default.
 * The exceptions to this rules are the following methods:</p>
 * <ul>
 *   <li>{@link #getMetadataScopes()} returns {@code this}</li>
 *   <li>{@link #getResourceScope()} returns {@link ScopeCode#DATASET}</li>
 *   <li>{@link #getIdentificationInfo()} returns {@code this}</li>
 *   <li>{@link #getCitation()} returns {@code this}</li>
 *   <li>{@link #getSpatialRepresentationTypes()} returns {@link SpatialRepresentationType#VECTOR}</li>
 *   <li>{@link #getTopicCategories()} returns {@link TopicCategory#LOCATION}</li>
 *   <li>{@link #getPresentationForms()} returns {@link PresentationForm#TABLE_DIGITAL}</li>
 * </ul>
 *
 * Subclasses are encouraged to override the following methods (typically with hard-coded values):
 *
 * <ul>
 *   <li>{@link #getSpatialRepresentationTypes()} if the metadata describe gridded data instead of vector data.</li>
 *   <li>{@link #getTopicCategories()} if the data represent something else than locations.</li>
 *   <li>{@link #getResourceFormats()} with a hard-coded value provided by the data store implementation.</li>
 *   <li>{@link #getPresentationForms()} if the data represent something else than tabular data.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public class SimpleMetadata implements Metadata, MetadataScope, DataIdentification, Citation {
    /**
     * Creates a new metadata object.
     */
    protected SimpleMetadata() {
    }

    /**
     * Language(s) used for documenting metadata.
     * Also the language(s) used within the data.
     */
    @Override
    public Map<Locale,Charset> getLocalesAndCharsets() {
        return Collections.emptyMap();
    }

    /**
     * The scope or type of resource for which metadata is provided.
     * This method returns {@code this} for allowing call to {@link #getResourceScope()}.
     *
     * @see #getResourceScope()
     * @see #getName()
     */
    @Override
    public Collection<MetadataScope> getMetadataScopes() {
        return Collections.singleton(this);
    }

    /**
     * Code for the metadata scope, fixed to {@link ScopeCode#DATASET} by default. This is part of the information
     * provided by {@link #getMetadataScopes()}. The {@code DATASET} default value is consistent with the fact that
     * {@code SimpleMetadata} implements {@link DataIdentification}.
     */
    @Override
    public ScopeCode getResourceScope() {
        return ScopeCode.DATASET;
    }

    /**
     * Parties responsible for the metadata information.
     */
    @Override
    public Collection<Responsibility> getContacts() {
        return Collections.emptyList();
    }

    /**
     * Date(s) associated with the metadata.
     */
    @Override
    public Collection<CitationDate> getDateInfo() {
        return Collections.emptyList();
    }

    /**
     * Basic information about the resource(s) to which the metadata applies.
     * This method returns {@code this} for allowing call to {@link #getCitation()}.
     * and other methods.
     *
     * @see #getCitation()
     * @see #getAbstract()
     * @see #getPointOfContacts()
     * @see #getSpatialRepresentationTypes()
     * @see #getSpatialResolutions()
     * @see #getTemporalResolutions()
     * @see #getTopicCategories()
     * @see #getExtents()
     * @see #getResourceFormats()
     * @see #getDescriptiveKeywords()
     */
    @Override
    public Collection<DataIdentification> getIdentificationInfo() {
        return Collections.singleton(this);
    }


    /* -------------------------------------------------------------------------------------------------
     * Implementation of the DataIdentification object returned by Metadata.getIdentificationInfo().
     * ------------------------------------------------------------------------------------------------- */

    /**
     * Citation for the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     * This method returns {@code this} for allowing call to {@link #getTitle()} and other methods.
     */
    @Override
    public Citation getCitation() {
        return this;
    }

    /**
     * Brief narrative summary of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public InternationalString getAbstract() {
        return null;
    }

    /**
     * Methods used to spatially represent geographic information.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     * Default implementation returns {@link SpatialRepresentationType#VECTOR}.
     * Subclasses should override this method if they represent gridded data instead of vector data.
     */
    @Override
    public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return Collections.singleton(SpatialRepresentationType.VECTOR);
    }

    /**
     * Main theme(s) of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     * Default implementation returns {@link TopicCategory#LOCATION}.
     * Subclasses should override this method if they represent other kind of data.
     */
    @Override
    public Collection<TopicCategory> getTopicCategories() {
        return Collections.singleton(TopicCategory.LOCATION);
    }

    /**
     * Spatial and temporal extent of the resource.
     * This is part of the information returned by {@link #getIdentificationInfo()}.
     */
    @Override
    public Collection<Extent> getExtents() {
        return Collections.emptyList();
    }


    /* -------------------------------------------------------------------------------------------------
     * Implementation of the Citation object returned by DataIdentification.getCitation().
     * ------------------------------------------------------------------------------------------------- */

    /**
     * Name by which the cited resource is known.
     * This is part of the information returned by {@link #getCitation()}.
     */
    @Override
    public InternationalString getTitle() {
        return null;
    }

    /**
     * Mode in which the resource is represented.
     * This is part of the information returned by {@link #getCitation()}.
     * Default implementation returns {@link PresentationForm#TABLE_DIGITAL}.
     * Subclasses should override this method if they represent other kind of data.
     */
    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.singleton(PresentationForm.TABLE_DIGITAL);
    }
}
