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
package org.apache.sis.internal.storage;

import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.nio.charset.Charset;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.geometry.Geometry;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.spatial.GCP;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.acquisition.Context;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.quality.Element;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultBoundingPolygon;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.metadata.iso.spatial.DefaultGridSpatialRepresentation;
import org.apache.sis.metadata.iso.spatial.DefaultDimension;
import org.apache.sis.metadata.iso.spatial.DefaultGeorectified;
import org.apache.sis.metadata.iso.spatial.DefaultGeoreferenceable;
import org.apache.sis.metadata.iso.spatial.DefaultGCPCollection;
import org.apache.sis.metadata.iso.spatial.DefaultGCP;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultSampleDimension;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription;
import org.apache.sis.metadata.iso.content.DefaultRangeElementDescription;
import org.apache.sis.metadata.iso.content.DefaultImageDescription;
import org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.distribution.DefaultDistributor;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultEvent;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultOperation;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultRequirement;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.lineage.DefaultProcessStep;
import org.apache.sis.metadata.iso.lineage.DefaultProcessing;
import org.apache.sis.metadata.iso.lineage.DefaultSource;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.metadata.iso.maintenance.DefaultScopeDescription;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;

import static java.util.Collections.singleton;
import static org.apache.sis.internal.util.StandardDateFormat.MILLISECONDS_PER_DAY;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.metadata.citation.Responsibility;


/**
 * Helper methods for the metadata created by {@code DataStore} implementations.
 * This is not a general-purpose builder suitable for public API, since the
 * methods provided in this class are tailored for Apache SIS data store needs.
 * API of this class may change in any future SIS versions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @version 1.0
 * @since   0.8
 * @module
 */
public class MetadataBuilder {
    /**
     * Band numbers, created when first needed.
     */
    private static final MemberName[] BAND_NUMBERS = new MemberName[16];

    /**
     * Whether the next party to create should be an instance of {@link DefaultIndividual} or {@link DefaultOrganisation}.
     *
     * @see #party()
     * @see #newParty(PartyType)
     */
    private PartyType partyType = PartyType.UNKNOWN;

    /**
     * Whether the next grid should be an instance of {@link DefaultGeorectified} or {@link DefaultGeoreferenceable}.
     *
     * @see #gridRepresentation()
     * @see #newGridRepresentation(GridType)
     */
    private GridType gridType = GridType.UNSPECIFIED;

    /**
     * {@code true} if the next {@code CoverageDescription} to create will be a description of measurements
     * in the electromagnetic spectrum. In that case, the coverage description will actually be an instance
     * of {@code ImageDescription}.
     *
     * @see #coverageDescription()
     * @see #newCoverage(boolean)
     */
    private boolean electromagnetic;

    /**
     * For using the same instance of {@code Integer} or {@code Double} when the value is the same.
     * Also used for reusing {@link Citation} instances already created for a given title.
     * Keys and values can be:
     *
     * <table class="sis">
     *   <tr><th>Key</th>                          <th>Value</th>               <th>Method</th></tr>
     *   <tr><td>{@link Integer}</td>              <td>{@link Integer}</td>     <td>{@link #shared(Integer)}</td></tr>
     *   <tr><td>{@link Double}</td>               <td>{@link Double}</td>      <td>{@link #shared(Double)}</td></tr>
     *   <tr><td>{@link Identifier}</td>           <td>{@link Identifier}</td>  <td>{@link #sharedIdentifier(CharSequence, String)}</td></tr>
     *   <tr><td>{@link InternationalString}</td>  <td>{@link Citation}</td>    <td>{@link #sharedCitation(InternationalString)}</td></tr>
     * </table>
     */
    private final Map<Object,Object> sharedValues = new HashMap<>();

    // Other fields declared below together with closely related methods.

    /**
     * Creates a new metadata builder.
     */
    public MetadataBuilder() {
    }

    /**
     * The metadata created by this builder, or {@code null} if not yet created.
     */
    private DefaultMetadata metadata;

    /**
     * Creates the metadata object if it does not already exists, then returns it.
     *
     * @return the metadata (never {@code null}).
     * @see #build(boolean)
     */
    private DefaultMetadata metadata() {
        if (metadata == null) {
            metadata = new DefaultMetadata();
        }
        return metadata;
    }

    /**
     * The identification information that are part of {@linkplain #metadata}, or {@code null} if none.
     */
    private DefaultDataIdentification identification;

    /**
     * Creates the identification information object if it does not already exists, then returns it.
     *
     * @return the identification information (never {@code null}).
     * @see #newIdentification()
     */
    private DefaultDataIdentification identification() {
        if (identification == null) {
            identification = new DefaultDataIdentification();
        }
        return identification;
    }

    /**
     * The citation of data {@linkplain #identification}, or {@code null} if none.
     */
    private DefaultCitation citation;

    /**
     * Creates the citation object if it does not already exists, then returns it.
     *
     * @return the citation information (never {@code null}).
     */
    private DefaultCitation citation() {
        if (citation == null) {
            citation = new DefaultCitation();
        }
        return citation;
    }

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private DefaultResponsibility responsibility;

    /**
     * Creates the responsibility object if it does not already exists, then returns it.
     *
     * @return the responsibility party (never {@code null}).
     */
    private DefaultResponsibility responsibility() {
        if (responsibility == null) {
            responsibility = new DefaultResponsibility();
        }
        return responsibility;
    }

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private AbstractParty party;

    /**
     * Creates the individual or organization information object if it does not already exists, then returns it.
     *
     * <p><b>Limitation:</b> if the party type is unknown, then this method creates an {@code AbstractParty} instead
     * than one of the subtypes. This is not valid, but we currently have no way to guess if a party is an individual
     * or an organization. For now we prefer to let users know that the type is unknown rather than to pick a
     * potentially wrong type.</p>
     *
     * @return the individual or organization information (never {@code null}).
     * @see #newParty(MetadataBuilder.PartyType)
     */
    private AbstractParty party() {
        if (party == null) {
            switch (partyType) {
                case UNKNOWN:      party = new AbstractParty();       break;
                case INDIVIDUAL:   party = new DefaultIndividual();   break;
                case ORGANISATION: party = new DefaultOrganisation(); break;
                default:           throw new AssertionError(partyType);
            }
        }
        return party;
    }

    /**
     * Copyright information, or {@code null} if none.
     */
    private DefaultLegalConstraints constraints;

    /**
     * Creates the constraints information object if it does not already exists, then returns it.
     *
     * @return the constraints information (never {@code null}).
     */
    private DefaultLegalConstraints constraints() {
        if (constraints == null) {
            constraints = new DefaultLegalConstraints();
        }
        return constraints;
    }

    /**
     * The extent information that are part of {@linkplain #identification}, or {@code null} if none.
     */
    private DefaultExtent extent;

    /**
     * Creates the extent information object if it does not already exists, then returns it.
     *
     * @return the extent information (never {@code null}).
     */
    private DefaultExtent extent() {
        if (extent == null) {
            extent = new DefaultExtent();
        }
        return extent;
    }

    /**
     * Information about the platforms and sensors that collected the data, or {@code null} if none.
     */
    private DefaultAcquisitionInformation acquisition;

    /**
     * Creates the acquisition information object if it does not already exists, then returns it.
     *
     * @return the acquisition information (never {@code null}).
     * @see #newAcquisition()
     */
    private DefaultAcquisitionInformation acquisition() {
        if (acquisition == null) {
            acquisition = new DefaultAcquisitionInformation();
        }
        return acquisition;
    }

    /**
     * Platform where are installed the sensors that collected the data, or {@code null} if none.
     */
    private DefaultPlatform platform;

    /**
     * Creates a platform object if it does not already exists, then returns it.
     *
     * @return the platform information (never {@code null}).
     */
    private DefaultPlatform platform() {
        if (platform == null) {
            platform = new DefaultPlatform();
        }
        return platform;
    }

    /**
     * Information about the feature types, or {@code null} if none.
     */
    private DefaultFeatureCatalogueDescription featureDescription;

    /**
     * Creates the feature descriptions object if it does not already exists, then returns it.
     * This method sets the {@code includedWithDataset} property to {@code true} because the
     * metadata built by this helper class are typically encoded together with the data.
     *
     * @return the feature descriptions (never {@code null}).
     * @see #newFeatureTypes()
     */
    private DefaultFeatureCatalogueDescription featureDescription() {
        if (featureDescription == null) {
            featureDescription = new DefaultFeatureCatalogueDescription();
            featureDescription.setIncludedWithDataset(true);
        }
        return featureDescription;
    }

    /**
     * Information about the content of a grid data cell, or {@code null} if none.
     * May also be an instance of {@link DefaultImageDescription} if {@link #electromagnetic} is {@code true}.
     */
    private DefaultCoverageDescription coverageDescription;

    /**
     * Creates the coverage description object if it does not already exists, then returns it.
     *
     * @return the coverage description (never {@code null}).
     * @see #newCoverage(boolean)
     */
    private DefaultCoverageDescription coverageDescription() {
        if (coverageDescription == null) {
            coverageDescription = electromagnetic ? new DefaultImageDescription() : new DefaultCoverageDescription();
        }
        return coverageDescription;
    }

    /**
     * Information about content type for groups of attributes for a specific range dimension, or {@code null} if none.
     */
    private DefaultAttributeGroup attributeGroup;

    /**
     * Creates the attribute group object if it does not already exists, then returns it.
     *
     * @return the attribute group (never {@code null}).
     */
    private DefaultAttributeGroup attributeGroup() {
        if (attributeGroup == null) {
            attributeGroup = new DefaultAttributeGroup();
        }
        return attributeGroup;
    }

    /**
     * The characteristic of each dimension (layer) included in the resource, or {@code null} if none.
     */
    private DefaultSampleDimension sampleDimension;

    /**
     * Creates the sample dimension object if it does not already exists, then returns it.
     *
     * @return the sample dimension (never {@code null}).
     * @see #newSampleDimension()
     */
    private DefaultSampleDimension sampleDimension() {
        if (sampleDimension == null) {
            sampleDimension = new DefaultSampleDimension();
        }
        return sampleDimension;
    }

    /**
     * Information about the grid shape, or {@code null} if none.
     */
    private DefaultGridSpatialRepresentation gridRepresentation;

    /**
     * Creates a grid representation object if it does not already exists, then returns it.
     *
     * @return the grid representation object (never {@code null}).
     * @see #newGridRepresentation(GridType)
     */
    private DefaultGridSpatialRepresentation gridRepresentation() {
        if (gridRepresentation == null) {
            switch (gridType) {
                case GEORECTIFIED:     gridRepresentation = new DefaultGeorectified(); break;
                case GEOREFERENCEABLE: gridRepresentation = new DefaultGeoreferenceable(); break;
                default:               gridRepresentation = new DefaultGridSpatialRepresentation(); break;
            }
        }
        return gridRepresentation;
    }

    /**
     * Collection of ground control points.
     */
    private DefaultGCPCollection groundControlPoints;

    /**
     * Creates the collection of ground control points if it does not already exists, then returns it.
     *
     * @return the ground control points (never {@code null}).
     */
    private DefaultGCPCollection groundControlPoints() {
        if (groundControlPoints == null) {
            groundControlPoints = new DefaultGCPCollection();
        }
        return groundControlPoints;
    }

    /**
     * Information about the distributor of and options for obtaining the resource.
     */
    private DefaultDistribution distribution;

    /**
     * Creates the distribution information object if it does not already exists, then returns it.
     *
     * @return the distribution information (never {@code null}).
     * @see #newDistribution()
     */
    private DefaultDistribution distribution() {
        if (distribution == null) {
            distribution = new DefaultDistribution();
        }
        return distribution;
    }

    /**
     * The distribution format, or {@code null} if none.
     * This is part of the resource {@linkplain #identification}.
     */
    private Format format;

    /**
     * Creates the distribution format object if it does not already exists, then returns it.
     *
     * @return the distribution format (never {@code null}).
     */
    private DefaultFormat format() {
        DefaultFormat df = DefaultFormat.castOrCopy(format);
        if (df == null) {
            format = df = new DefaultFormat();
        }
        return df;
    }

    /**
     * Information about the events or source data used in constructing the data specified by the
     * {@linkplain DefaultLineage#getScope() scope}.
     */
    private DefaultLineage lineage;

    /**
     * Creates the lineage object if it does not already exists, then returns it.
     *
     * @return the lineage (never {@code null}).
     * @see #newLineage()
     */
    private DefaultLineage lineage() {
        if (lineage == null) {
            lineage = new DefaultLineage();
        }
        return lineage;
    }

    /**
     * Information about an event or transformation in the life of a resource.
     * This is part of {@link #lineage}.
     */
    private DefaultProcessStep processStep;

    /**
     * Creates the process step object if it does not already exists, then returns it.
     *
     * @return the process step (never {@code null}).
     */
    private DefaultProcessStep processStep() {
        if (processStep == null) {
            processStep = new DefaultProcessStep();
        }
        return processStep;
    }

    /**
     * Information about the procedures, processes and algorithms applied in the process step.
     * This is part of {@link #processStep}.
     */
    private DefaultProcessing processing;

    /**
     * Creates the processing object if it does not already exists, then returns it.
     *
     * @return the processing (never {@code null}).
     */
    private DefaultProcessing processing() {
        if (processing == null) {
            processing = new DefaultProcessing();
        }
        return processing;
    }

    /**
     * Adds the given element in the given collection if not already present. This method is used only for
     * properties that are usually stored in {@code List} rather than {@code Set} and for which we do not
     * keep a reference in this {@code MetadataBuilder} after the element has been added. This method is
     * intended for adding elements that despite being modifiable, are not going to be modified by this
     * {@code MetadataBuilder} class. Performance should not be a concern since the given list is usually
     * very short (0 or 1 element).
     *
     * <p>The given element should be non-null. The check for null value should be done by the caller instead
     * than by this method in order to avoid unneeded creation of collections. Such creation are implicitly
     * done by calls to {@code metadata.getFoos()} methods.</p>
     */
    private static <E> void addIfNotPresent(final Collection<E> collection, final E element) {
        if (!collection.contains(element)) {
            collection.add(element);
        }
    }

    /**
     * The type of party to create (individual, organization or unknown).
     */
    public enum PartyType {
        /**
         * Instructs {@link #newParty(PartyType)} that the next party to create should be an instance of
         * {@link DefaultIndividual}.
         */
        INDIVIDUAL,

        /**
         * Instructs {@link #newParty(PartyType)} that the next party to create should be an instance of
         * {@link DefaultOrganisation}.
         */
        ORGANISATION,

        /**
         * Instructs {@link #newParty(PartyType)} that the next party to create if of unknown type.
         */
        UNKNOWN
    }

    /**
     * Commits all pending information under the "responsible party" node (author, address, <i>etc</i>).
     * If there is no pending party information, then invoking this method has no effect
     * except setting the {@code type} flag.
     * If new party information are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "responsible party" node,
     * separated from the previous one, is desired.</p>
     *
     * @param  type  whether the party to create is an individual or an organization.
     */
    public final void newParty(final PartyType type) {
        ArgumentChecks.ensureNonNull("type", type);
        if (party != null) {
            addIfNotPresent(responsibility().getParties(), party);
            party = null;
        }
        partyType = type;
    }

    /**
     * Commits all pending information under the metadata "identification info" node (author, bounding box, <i>etc</i>).
     * If there is no pending identification information, then invoking this method has no effect.
     * If new identification info are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "identification info" node,
     * separated from the previous one, is desired.</p>
     */
    public final void newIdentification() {
        /*
         * Construction shall be ordered from children to parents.
         */
        newParty(PartyType.UNKNOWN);
        if (responsibility != null) {
            addIfNotPresent(citation().getCitedResponsibleParties(), responsibility);
            responsibility = null;
        }
        if (citation != null) {
            identification().setCitation(citation);
            citation = null;
        }
        if (extent != null) {
            addIfNotPresent(identification().getExtents(), extent);
            extent = null;
        }
        if (format != null) {
            addIfNotPresent(identification().getResourceFormats(), format);
            format = null;
        }
        if (constraints != null) {
            addIfNotPresent(identification().getResourceConstraints(), constraints);
            constraints = null;
        }
        if (identification != null) {
            addIfNotPresent(metadata().getIdentificationInfo(), identification);
            identification = null;
        }
    }

    /**
     * Commits all pending information under the metadata "acquisition" node (station, sensors, <i>etc</i>).
     * If there is no pending acquisition information, then invoking this method has no effect.
     * If new acquisition info are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "acquisition info" node,
     * separated from the previous one, is desired.</p>
     */
    public final void newAcquisition() {
        if (platform != null) {
            addIfNotPresent(acquisition().getPlatforms(), platform);
        }
        if (acquisition != null) {
            addIfNotPresent(metadata().getAcquisitionInformation(), acquisition);
            acquisition = null;
        }
    }

    /**
     * Commits all pending information under metadata "distribution" node.
     * If there is no pending distribution information, then invoking this method has no effect.
     * If new distribution info are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "distribution info" node,
     * separated from the previous one, is desired.</p>
     */
    public final void newDistribution() {
        if (distribution != null) {
            addIfNotPresent(metadata().getDistributionInfo(), distribution);
            distribution = null;
        }
    }

    /**
     * Commits all pending information under the metadata "feature catalog" node.
     * If there is no pending feature description, then invoking this method has no effect.
     * If new feature descriptions are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "feature catalog description" node is desired.</p>
     */
    public final void newFeatureTypes() {
        if (featureDescription != null) {
            addIfNotPresent(metadata().getContentInfo(), featureDescription);
            featureDescription = null;
        }
    }

    /**
     * Commits all pending information under the metadata "content info" node (bands, <i>etc</i>).
     * If there is no pending coverage description, then invoking this method has no effect
     * except setting the {@code electromagnetic} flag.
     * If new coverage descriptions are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "coverage description" node is desired,
     * or the {@code electromagnetic} flag needs to be set to {@code true}.</p>
     *
     * @param  electromagnetic  {@code true} if the next {@code CoverageDescription} to create
     *         will be a description of measurements in the electromagnetic spectrum.
     */
    public final void newCoverage(final boolean electromagnetic) {
        newSampleDimension();
        if (attributeGroup != null) {
            addIfNotPresent(coverageDescription().getAttributeGroups(), attributeGroup);
            attributeGroup = null;
        }
        if (coverageDescription != null) {
            addIfNotPresent(metadata().getContentInfo(), coverageDescription);
            coverageDescription = null;
        }
        this.electromagnetic = electromagnetic;
    }

    /**
     * Commits all pending information under the coverage "attribute group" node.
     * If there is no pending sample dimension description, then invoking this method has no effect.
     * If new sample dimensions are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "sample dimension" node is desired.</p>
     */
    public final void newSampleDimension() {
        if (sampleDimension != null) {
            addIfNotPresent(attributeGroup().getAttributes(), sampleDimension);
            sampleDimension = null;
        }
    }

    /**
     * The type of grid spatial representation (georectified, georeferenceable or unspecified).
     */
    public enum GridType {
        /**
         * Grid is an instance of {@link org.opengis.metadata.spatial.Georectified}.
         */
        GEORECTIFIED,

        /**
         * Grid is an instance of {@link org.opengis.metadata.spatial.Georeferenceable}.
         */
        GEOREFERENCEABLE,

        /**
         * Grid is neither georectified or georeferenceable.
         * A plain {@link org.opengis.metadata.spatial.GridSpatialRepresentation} instance will be used.
         */
        UNSPECIFIED
    }

    /**
     * Commits all pending information under the metadata "spatial representation" node (dimensions, <i>etc</i>).
     * If there is no pending spatial representation information, then invoking this method has no effect.
     * If new spatial representation info are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "spatial representation info" node,
     * separated from the previous one, is desired.</p>
     *
     * @param type  whether the next grid should be an instance of {@link DefaultGeorectified} or {@link DefaultGeoreferenceable}.
     */
    public final void newGridRepresentation(final GridType type) {
        ArgumentChecks.ensureNonNull("type", type);
        if (gridRepresentation != null) {
            final int n = gridRepresentation.getAxisDimensionProperties().size();
            if (n != 0) {
                gridRepresentation.setNumberOfDimensions(shared(n));
            }
            if (groundControlPoints != null && gridRepresentation instanceof DefaultGeoreferenceable) {
                addIfNotPresent(((DefaultGeoreferenceable) gridRepresentation).getGeolocationInformation(), groundControlPoints);
                groundControlPoints = null;
            }
            addIfNotPresent(metadata.getSpatialRepresentationInfo(), gridRepresentation);
            gridRepresentation = null;
        }
        gridType = type;
    }

    /**
     * Commits all pending information under the metadata "lineage" node (process steps, <i>etc</i>).
     * If there is no pending lineage information, then invoking this method has no effect.
     * If new lineage information are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "lineage" node,
     * separated from the previous one, is desired.</p>
     */
    public final void newLineage() {
        if (processing != null) {
            processStep().setProcessingInformation(processing);
            processing = null;
        }
        if (processStep != null) {
            addIfNotPresent(lineage().getProcessSteps(), processStep);
            processStep = null;
        }
        if (lineage != null) {
            addIfNotPresent(metadata().getResourceLineages(), lineage);
            lineage = null;
        }
    }

    /**
     * Creates or fetches a citation for the given title. The same citation may be shared by many metadata objects,
     * for example identifiers or groups of keywords. Current implementation creates a {@link DefaultCitation} for
     * the given title and caches the result. Future implementations may return predefined citation constants from
     * the SQL database when applicable.
     *
     * @param  title  the citation title, or {@code null} if none.
     * @return a (potentially shared) citation for the given title, or {@code null} if the given title was null.
     */
    private Citation sharedCitation(final InternationalString title) {
        if (title == null) return null;
        return (Citation) sharedValues.computeIfAbsent(title, k -> new DefaultCitation((CharSequence) k));
    }

    /**
     * Creates or fetches an identifier for the given authority and code. This method may query the metadata
     * database for fetching a more complete {@link Citation} for the given {@code authority}.
     * This method may return a shared {@code Identifier} instance.
     *
     * @param  authority  the authority tile, or {@code null} if none.
     * @param  code       the identifier code (mandatory).
     */
    private Identifier sharedIdentifier(final CharSequence authority, final String code) {
        final DefaultIdentifier id = new DefaultIdentifier(sharedCitation(trim(authority)), code);
        return (Identifier) sharedValues.getOrDefault(id, id);
    }

    /**
     * Specify if an information apply to data, to metadata or to both.
     * This is used for setting the locale or character encoding.
     */
    public enum Scope {
        /**
         * Information applies only to the resource (data).
         */
        RESOURCE,

        /**
         * Information applies only to metadata.
         */
        METADATA,

        /**
         * Information applies to both resource and metadata.
         */
        ALL
    }

    /**
     * Adds a data and/or metadata identifier. This method performs the same work than
     * {@link #addIdentifier(CharSequence, String, Scope)} for situations where the
     * identifier instance is already available.
     *
     * @param  id     the identifier, or {@code null} if none.
     * @param  scope  whether the date applies to data, to metadata or to both.
     *
     * @see #addIdentifier(CharSequence, String, Scope)
     */
    public final void addIdentifier(Identifier id, final Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (id != null) {
            id = (Identifier) sharedValues.getOrDefault(id, id);
            if (scope != Scope.RESOURCE) metadata().setMetadataIdentifier(id);
            if (scope != Scope.METADATA) addIfNotPresent(citation().getIdentifiers(), id);
        }
    }

    /**
     * Adds a resource (data) identifier, a metadata identifier, or both as they are often the same.
     * The identifier is added only if {@code code} is non-null, regardless other argument values.
     * Empty strings (ignoring spaces) are ignored.
     * Storages locations are:
     *
     * <ul>
     *   <li><b>Metadata:</b> {@code metadata/metadataIdentifier}</li>
     *   <li><b>Resource:</b> {@code metadata/identificationInfo/citation/identifier}</li>
     * </ul>
     *
     * @param  authority  the the person or party responsible for maintenance of the namespace, or {@code null} if none.
     * @param  code       the identifier code, or {@code null} for no-operation.
     * @param  scope      whether the date applies to data, to metadata or to both.
     *
     * @see #addTitle(CharSequence)
     * @see #addTitleOrIdentifier(String, Scope)
     * @see #addIdentifier(Identifier, Scope)
     */
    public final void addIdentifier(final CharSequence authority, String code, final Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (code != null && !(code = code.trim()).isEmpty()) {
            final Identifier id = sharedIdentifier(authority, code);
            if (scope != Scope.RESOURCE) metadata().setMetadataIdentifier(id);
            if (scope != Scope.METADATA) addIfNotPresent(citation().getIdentifiers(), id);
        }
    }

    /**
     * Sets the file format. The given name should be a short name like "GeoTIFF".
     * The long name will be inferred from the given short name, if possible.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceFormat}</li>
     * </ul>
     *
     * This method should be invoked <strong>before</strong> any other method writing in the
     * {@code identificationInfo/resourceFormat} node. If this exception throws an exception,
     * than that exception should be reported as a warning. Example:
     *
     * {@preformat java
     *     try {
     *         metadata.setFormat("MyFormat");
     *     } catch (MetadataStoreException e) {
     *         metadata.addFormatName("MyFormat");
     *         listeners.warning(null, e);
     *     }
     *     metadata.addCompression("decompression technique");
     * }
     *
     * @param  abbreviation  the format short name or abbreviation, or {@code null} for no-operation.
     * @throws MetadataStoreException  if this method can not connect to the {@code jdbc/SpatialMetadata} database.
     *         Callers should generally handle this exception as a recoverable one (i.e. log a warning and continue).
     *
     * @see #addCompression(CharSequence)
     * @see #addFormatName(CharSequence)
     */
    public final void setFormat(final String abbreviation) throws MetadataStoreException {
        if (abbreviation != null && abbreviation.length() != 0) {
            if (format == null) {
                format = MetadataSource.getProvided().lookup(Format.class, abbreviation);
            } else {
                addFormatName(abbreviation);
            }
        }
    }

    /**
     * Adds a language used for documenting data and/or metadata.
     * Storage locations are:
     *
     * <ul>
     *   <li><b>Metadata:</b> {@code metadata/language}</li>
     *   <li><b>Resource:</b> {@code metadata/identificationInfo/language}</li>
     * </ul>
     *
     * @param  language  a language used for documenting data and/or metadata, or {@code null} for no-operation.
     * @param  scope     whether the language applies to data, to metadata or to both.
     *
     * @see #addEncoding(Charset, MetadataBuilder.Scope)
     */
    public final void addLanguage(final Locale language, final Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (language != null) {
            // No need to use 'addIfNotPresent(…)' because Locale collection is a Set by default.
            if (scope != Scope.RESOURCE) metadata().getLanguages().add(language);
            if (scope != Scope.METADATA) identification().getLanguages().add(language);
        }
    }

    /**
     * Adds a character set used for encoding the data and/or metadata.
     * Storage locations are:
     *
     * <ul>
     *   <li><b>Metadata:</b> {@code metadata/characterSet}</li>
     *   <li><b>Resource:</b> {@code metadata/identificationInfo/characterSet}</li>
     * </ul>
     *
     * @param  encoding  the character set used for encoding data and/or metadata, or {@code null} for no-operation.
     * @param  scope     whether the encoding applies to data, to metadata or to both.
     *
     * @see #addLanguage(Locale, MetadataBuilder.Scope)
     */
    public final void addEncoding(final Charset encoding, final Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (encoding != null) {
            // No need to use 'addIfNotPresent(…)' because Charset collection is a Set by default.
            if (scope != Scope.RESOURCE) metadata().getCharacterSets().add(encoding);
            if (scope != Scope.METADATA) identification().getCharacterSets().add(encoding);
        }
    }

    /**
     * Adds information about the scope of the resource.
     * The scope is typically {@link ScopeCode#DATASET}.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/metadataScope/resourceScope}</li>
     *   <li>{@code metadata/metadataScope/name}</li>
     * </ul>
     *
     * @param  scope  the scope of the resource, or {@code null} if none.
     * @param  name   description of the scope, or {@code null} if none.
     */
    public final void addResourceScope(final ScopeCode scope, final CharSequence name) {
        if (scope != null || name != null) {
            addIfNotPresent(metadata().getMetadataScopes(), new DefaultMetadataScope(scope, name));
        }
    }

    /**
     * Adds a date of the given type. This is not the data acquisition time, but rather the metadata creation
     * or last update time. With {@link Scope#METADATA}, this is the creation of the metadata file as a whole.
     * With {@link Scope#RESOURCE}, this is the creation of the metadata for a particular "identification info".
     * They are often the same, since there is typically only one "identification info" per file.
     * Storage locations are:
     *
     * <ul>
     *   <li><b>Metadata:</b> {@code metadata/dateInfo/*}</li>
     *   <li><b>Resource:</b> {@code metadata/identificationInfo/citation/date/*}</li>
     * </ul>
     *
     * If a date already exists for the given type, then the earliest date is retained (oldest date are discarded)
     * except for {@link DateType#LAST_REVISION}, {@link DateType#LAST_UPDATE LAST_UPDATE} or any other date type
     * prefixed by {@code "LATE_"}, where only the latest date is kept.
     *
     * @param  date   the date to add, or {@code null} for no-operation..
     * @param  type   the type of the date to add, or {@code null} if none (not legal but tolerated).
     * @param  scope  whether the date applies to data, to metadata or to both.
     *
     * @see #addAcquisitionTime(Date)
     */
    public final void addCitationDate(final Date date, final DateType type, final Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (date != null) {
            final DefaultCitationDate cd = new DefaultCitationDate(date, type);
            if (scope != Scope.RESOURCE) addEarliest(metadata().getDateInfo(), cd, type);
            if (scope != Scope.METADATA) addEarliest(citation().getDates(),    cd, type);
        }
    }

    /**
     * Adds a date in the given collection, making sure that there is no two dates of the same type.
     * If two dates are of the same type, retains the latest one if the type name starts with {@code "LATE_"}
     * or retains the earliest date otherwise.
     */
    private static void addEarliest(final Collection<CitationDate> dates, final CitationDate cd, final DateType type) {
        for (final Iterator<CitationDate> it = dates.iterator(); it.hasNext();) {
            final CitationDate co = it.next();
            if (type.equals(co.getDateType())) {
                final Date oldDate = co.getDate();
                final Date newDate = cd.getDate();
                if (type.name().startsWith("LATE_") ? oldDate.before(newDate) : oldDate.after(newDate)) {
                    it.remove();
                    break;
                }
                return;
            }
        }
        dates.add(cd);
    }

    /**
     * Returns the given character sequence as a non-empty character string with leading and trailing spaces removed.
     * If the given character sequence is null, empty or blank, then this method returns {@code null}.
     */
    private static InternationalString trim(CharSequence value) {
        value = CharSequences.trimWhitespaces(value);
        if (value != null && value.length() != 0) {
            return Types.toInternationalString(value);
        } else {
            return null;
        }
    }

    /**
     * Returns the concatenation of the given strings. The previous string may be {@code null}.
     * This method does nothing if the previous string already contains the one to append.
     */
    private static InternationalString append(final InternationalString previous, final InternationalString toAdd) {
        if (previous == null) {
            return toAdd;
        }
        final String p = previous.toString();
        final String a = toAdd.toString();
        if (p.contains(a)) {
            return previous;
        }
        return Types.toInternationalString(p + System.lineSeparator() + a);
    }

    /**
     * Returns {@code true} if the given character sequences have equal content.
     */
    private static boolean equals(final CharSequence s1, final CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.toString().equals(s2.toString());
    }

    /**
     * Adds a title or alternate title of the resource.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/title} if not yet used</li>
     *   <li>{@code metadata/identificationInfo/citation/alternateTitle} otherwise</li>
     * </ul>
     *
     * @param title  the resource title or alternate title, or {@code null} for no-operation.
     *
     * @see #addAbstract(CharSequence)
     */
    public final void addTitle(final CharSequence title) {
        final InternationalString i18n = trim(title);
        if (i18n != null) {
            final DefaultCitation citation = citation();
            final InternationalString current = citation.getTitle();
            if (current == null) {
                citation.setTitle(i18n);
            } else if (!equals(current, i18n)) {
                addIfNotPresent(citation.getAlternateTitles(), i18n);
            }
        }
    }

    /**
     * Adds the given code as a title if the current citation has no title, or as an identifier otherwise.
     * This method is invoked when adding an identifier to a metadata that may have no title. Because the
     * title is mandatory, adding only an identifier would make an invalid metadata.
     *
     * @param  code   the identifier code, or {@code null} for no-operation.
     * @param  scope  whether the date applies to data, to metadata or to both.
     *
     * @see #addTitle(CharSequence)
     * @see #addIdentifier(CharSequence, String, Scope)
     */
    public final void addTitleOrIdentifier(final String code, Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (scope != Scope.METADATA) {
            if (citation == null || citation.getTitle() == null) {
                addTitle(code);
                if (scope == Scope.RESOURCE) {
                    return;
                }
                scope = Scope.METADATA;
            }
        }
        addIdentifier(null, code, scope);
    }

    /**
     * Adds a version of the resource.
     * If a version already exists, the new one will be appended after a new line.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/edition}</li>
     * </ul>
     *
     * @param version  the version of resource(s), or {@code null} for no-operation.
     */
    public final void addEdition(final CharSequence version) {
        final InternationalString i18n = trim(version);
        if (i18n != null) {
            final DefaultCitation citation = citation();
            citation.setEdition(append(citation.getEdition(), i18n));
        }
    }

    /**
     * Adds a brief narrative summary of the resource(s).
     * If a summary already exists, the new one will be appended after a new line.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/abstract}</li>
     * </ul>
     *
     * @param description  the summary of resource(s), or {@code null} for no-operation.
     *
     * @see #addTitle(CharSequence)
     * @see #addPurpose(CharSequence)
     */
    public final void addAbstract(final CharSequence description) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultDataIdentification identification = identification();
            identification.setAbstract(append(identification.getAbstract(), i18n));
        }
    }

    /**
     * Adds a summary of the intentions with which the resource(s) was developed.
     * If a purpose already exists, the new one will be appended after a new line.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/purpose}</li>
     * </ul>
     *
     * @param intention  the summary of intention(s), or {@code null} for no-operation.
     *
     * @see #addAbstract(CharSequence)
     */
    public final void addPurpose(final CharSequence intention) {
        final InternationalString i18n = trim(intention);
        if (i18n != null) {
            final DefaultDataIdentification identification = identification();
            identification.setPurpose(append(identification.getPurpose(), i18n));
        }
    }

    /**
     * Adds other information required to complete the citation that is not recorded elsewhere.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/otherCitationDetails}</li>
     * </ul>
     *
     * @param  details  other details, or {@code null} for no-operation.
     */
    public final void addOtherCitationDetails(final CharSequence details) {
        final InternationalString i18n = trim(details);
        if (i18n != null) {
            addIfNotPresent(citation().getOtherCitationDetails(), i18n);
        }
    }

    /**
     * Adds any other descriptive information about the resource.
     * If information already exists, the new one will be appended after a new line.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/supplementalInformation}</li>
     * </ul>
     *
     * @param info  any other descriptive information about the resource, or {@code null} for no-operation.
     */
    public final void addSupplementalInformation(final CharSequence info) {
        final InternationalString i18n = trim(info);
        if (i18n != null) {
            final DefaultDataIdentification identification = identification();
            identification.setSupplementalInformation(append(identification.getSupplementalInformation(), i18n));
        }
    }

    /**
     * Adds a main theme of the resource.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/topicCategory}</li>
     * </ul>
     *
     * @param  topic  main theme of the resource, or {@code null} for no-operation.
     */
    public final void addTopicCategory(final TopicCategory topic) {
        if (topic != null) {
            // No need to use 'addIfNotPresent(…)' for enumerations.
            identification().getTopicCategories().add(topic);
        }
    }

    /**
     * Adds keywords if at least one non-empty element exists in the {@code keywords} array.
     * Other arguments have no impact on whether keywords are added or not because only the
     * {@code MD_Keywords.keyword} property is mandatory.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/descriptiveKeywords}</li>
     *   <li>{@code metadata/identificationInfo/thesaurusName/title}</li>
     *   <li>{@code metadata/identificationInfo/type}</li>
     * </ul>
     *
     * @param  keywords       word(s) used to describe the subject, or {@code null} for no-operation.
     * @param  type           subject matter used to group similar keywords, or {@code null} if none.
     * @param  thesaurusName  name of the formally registered thesaurus, or {@code null} if none.
     */
    public final void addKeywords(final Iterable<? extends CharSequence> keywords, final KeywordType type,
            final CharSequence thesaurusName)
    {
        if (keywords != null) {
            DefaultKeywords group = null;
            Collection<InternationalString> list = null;
            for (final CharSequence kw : keywords) {
                final InternationalString i18n = trim(kw);
                if (i18n != null) {
                    if (list == null) {
                        group = new DefaultKeywords();
                        group.setType(type);
                        group.setThesaurusName(sharedCitation(trim(thesaurusName)));
                        list = group.getKeywords();
                    }
                    list.add(i18n);
                }
            }
            if (group != null) {
                addIfNotPresent(identification().getDescriptiveKeywords(), group);
            }
        }
    }

    /**
     * Adds an author name. If an author was already defined with a different name,
     * then a new party instance is created.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/party/name}</li>
     * </ul>
     *
     * @param  name  the name of the author or publisher, or {@code null} for no-operation.
     */
    public final void addAuthor(final CharSequence name) {
        final InternationalString i18n = trim(name);
        if (i18n != null) {
            if (party != null) {
                final InternationalString current = party.getName();
                if (current != null) {
                    if (equals(current, name)) {
                        return;
                    }
                    newParty(partyType);
                }
            }
            party().setName(i18n);
        }
    }

    /**
     * Adds role, name, contact and position information for an individual or organization that is responsible
     * for the resource. This method can be used as an alternative to {@link #addAuthor(CharSequence)} when the
     * caller needs to create the responsibly party itself.
     *
     * <p>If the given {@code role} is non-null, then this method will ensure that the added party has the given
     * role. A copy of the given party will be created if needed (the given party will never be modified).</p>
     *
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/citedResponsibleParty}</li>
     *   <li>{@code metadata/identificationInfo/citation/citedResponsibleParty/role}</li>
     * </ul>
     *
     * @param  party  the individual or organization that is responsible, or {@code null} for no-operation.
     * @param  role   the role to set, or {@code null} for leaving it unchanged.
     */
    public final void addCitedResponsibleParty(Responsibility party, final Role role) {
        if (party != null) {
            if (role != null && !role.equals(party.getRole())) {
                party = new DefaultResponsibility(party);
                ((DefaultResponsibility) party).setRole(role);
            }
            addIfNotPresent(citation().getCitedResponsibleParties(), party);
        }
    }

    /**
     * Adds a means of communication with person(s) and organizations(s) associated with the resource(s).
     * This is often the same party than the above cited responsibly party, with only the role changed.
     * Storage locations are:
     *
     * <ul>
     *   <li><b>Metadata</b> {@code metadata/contact}</li>
     *   <li><b>Resource</b> {@code metadata/identificationInfo/pointOfContact}</li>
     * </ul>
     *
     * @param  contact  means of communication with party associated with the resource, or {@code null} for no-operation.
     * @param  scope    whether the contact applies to data, to metadata or to both.
     */
    public final void addPointOfContact(final Responsibility contact, final Scope scope) {
        ArgumentChecks.ensureNonNull("scope", scope);
        if (contact != null) {
            if (scope != Scope.RESOURCE)     addIfNotPresent(metadata().getContacts(), contact);
            if (scope != Scope.METADATA) addIfNotPresent(identification().getPointOfContacts(), contact);
        }
    }

    /**
     * Adds a distributor. This is often the same than the above responsible party.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/distributionInfo/distributor/distributorContact}</li>
     * </ul>
     *
     * @param  distributor  the distributor, or {@code null} for no-operation.
     */
    public final void addDistributor(final Responsibility distributor) {
        if (distributor != null) {
            addIfNotPresent(distribution().getDistributors(), new DefaultDistributor(distributor));
        }
    }

    /**
     * Adds recognition of those who contributed to the resource(s).
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/credit}</li>
     * </ul>
     *
     * @param  credit  recognition of those who contributed to the resource, or {@code null} for no-operation.
     */
    public final void addCredits(final CharSequence credit) {
        final InternationalString i18n = trim(credit);
        if (i18n != null) {
            addIfNotPresent(identification().getCredits(), i18n);
        }
    }

    /**
     * Elements to omit in the legal notice to be parsed by {@link MetadataBuilder#parseLegalNotice(String)}.
     * Some of those elements are implied by the metadata were the legal notice will be stored.
     */
    private static final class LegalSymbols {
        /**
         * Symbols associated to restrictions.
         */
        private static final LegalSymbols[] VALUES = {
            new LegalSymbols(Restriction.COPYRIGHT, "COPYRIGHT", "(C)", "©", "All rights reserved"),
            new LegalSymbols(Restriction.TRADEMARK, "TRADEMARK", "(TM)", "™", "(R)", "®")
        };

        /**
         * The restriction to use if an item in the {@linkplain #symbols} list is found.
         */
        private final Restriction restriction;

        /**
         * Symbols to use as an indication that the {@linkplain #restriction} applies.
         */
        private final String[] symbols;

        /**
         * Creates a new enumeration value for the given symbol.
         */
        private LegalSymbols(final Restriction restriction, final String... symbols) {
            this.restriction = restriction;
            this.symbols = symbols;
        }

        /**
         * Returns {@code true} if the given character is a space or a punctuation of category "other".
         * The punctuation characters include coma, dot, semi-colon, <i>etc.</i> but do not include
         * parenthesis or connecting punctuation.
         *
         * @param c the Unicode code point of the character to test.
         */
        private static boolean isSpaceOrPunctuation(final int c) {
            switch (Character.getType(c)) {
                case Character.LINE_SEPARATOR:
                case Character.SPACE_SEPARATOR:
                case Character.PARAGRAPH_SEPARATOR:
                case Character.OTHER_PUNCTUATION: return true;
                default: return false;
            }
        }

        /**
         * Implementation of {@link MetadataBuilder#parseLegalNotice(String)}, provided here for reducing
         * the amount of class loading in the common case where there is no legal notice to parse.
         */
        static void parse(final String notice, final DefaultLegalConstraints constraints) {
            final int length = notice.length();
            final StringBuilder buffer = new StringBuilder(length);
            int     year           = 0;         // The copyright year, or 0 if none.
            int     quoteLevel     = 0;         // Incremented on ( [ « characters, decremented on ) ] » characters.
            boolean isCopyright    = false;     // Whether the word parsed by previous iteration was "Copyright" or "(C)".
            boolean wasSeparator   = true;      // Whether the caracter parsed by the previous iteration was a word separator.
            boolean wasPunctuation = true;      // Whether the previous character was a punctuation of Unicode category "other".
            boolean skipNextChars  = true;      // Whether the next spaces and some punction characters should be ignored.
parse:      for (int i = 0; i < length;) {
                final int c = notice.codePointAt(i);
                final int n = Character.charCount(c);
                int     quoteChange   = 0;
                boolean isSeparator   = false;
                boolean isPunctuation;
                switch (Character.getType(c)) {
                    case Character.INITIAL_QUOTE_PUNCTUATION:
                    case Character.START_PUNCTUATION: {
                        quoteChange   = +1;                     //  ( [ «  etc.
                        skipNextChars = false;
                        isPunctuation = false;
                        break;
                    }
                    case Character.FINAL_QUOTE_PUNCTUATION:
                    case Character.END_PUNCTUATION: {
                        quoteChange   = -1;                     //  ) ] »  etc.
                        skipNextChars = false;
                        isPunctuation = false;
                        break;
                    }
                    default: {                                  // Letter, digit, hyphen, etc.
                        skipNextChars = false;
                        isPunctuation = false;
                        break;
                    }
                    case Character.OTHER_PUNCTUATION: {         //  , . : ; / " etc. but not -.
                        isPunctuation = true;
                        isSeparator   = true;
                        break;
                    }
                    case Character.LINE_SEPARATOR:
                    case Character.SPACE_SEPARATOR:
                    case Character.PARAGRAPH_SEPARATOR: {
                        isPunctuation = wasPunctuation;
                        isSeparator   = true;
                        break;
                    }
                }
                if (wasSeparator && !isSeparator && quoteLevel == 0) {
                    /*
                     * Found the beginning of a new word. Ignore textes like "(C)" or "All rights reserved".
                     * Some of those textes are implied by the metadata where the legal notice will be stored.
                     */
                    for (final LegalSymbols r : VALUES) {
                        for (final String symbol : r.symbols) {
                            if (notice.regionMatches(true, i, symbol, 0, symbol.length())) {
                                final int after = i + symbol.length();
                                if (after >= length || isSpaceOrPunctuation(notice.codePointAt(after))) {
                                    isCopyright |= Restriction.COPYRIGHT.equals(r.restriction);
                                    constraints.getUseConstraints().add(r.restriction);
                                    wasPunctuation = true;      // Pretend that "Copyright" was followed by a coma.
                                    skipNextChars  = true;      // Ignore spaces and punctuations until the next word.
                                    i = after;                  // Skip the "Copyright" (or other) word.
                                    continue parse;
                                }
                            }
                        }
                    }
                    /*
                     * If a copyright notice is followed by digits, assume that those digits are the copyright year.
                     * We require the year is followed by punctuations or non-breaking space in order to reduce the
                     * risk of confusion with postal addresses. So this block should accept "John, 1992." but not
                     * "1992-1 Nowhere road".
                     */
                    if (isCopyright && wasPunctuation && year == 0 && c >= '0' && c <= '9') {
                        int endOfDigits = i + n;            // After the last digit in sequence.
                        while (endOfDigits < length) {
                            final int d = notice.codePointAt(endOfDigits);
                            if (d < '0' || d > '9') break;
                            endOfDigits++;              // No need to use Character.charCount(s) here.
                        }
                        // Verify if the digits are followed by a punctuation.
                        final int endOfToken = CharSequences.skipLeadingWhitespaces(notice, endOfDigits, length);
                        if (endOfToken > endOfDigits || isSpaceOrPunctuation(notice.codePointAt(endOfToken))) try {
                            year = Integer.parseInt(notice.substring(i, endOfDigits));
                            if (year >= 1800 && year <= 9999) {                     // Those limits are arbitrary.
                                skipNextChars = true;
                                i = endOfToken;
                                continue;
                            }
                            year = 0;                                               // Reject as not a copyright year.
                        } catch (NumberFormatException e) {
                            // Not an integer - ignore, will be handled as text.
                        }
                    }
                }
                /*
                 * End of the block that was executed at the beginning of each new word.
                 * Following is executed for every characters, except if the above block
                 * skipped a portion of the input string.
                 */
                wasPunctuation = isPunctuation;
                wasSeparator   = isSeparator;
                quoteLevel    += quoteChange;
                if (!skipNextChars && !Character.isIdentifierIgnorable(c)) {
                    buffer.appendCodePoint(c);
                }
                i += n;
            }
            /*
             * End of parsing. Omit trailing spaces and some punctuations if any, then store the result.
             */
            int i = buffer.length();
            while (i > 0) {
                final int c = buffer.codePointBefore(i);
                if (!isSpaceOrPunctuation(c)) break;
                i -= Character.charCount(c);
            }
            final DefaultCitation c = new DefaultCitation(notice);
            if (year != 0) {
                final Date date = new Date(LocalDate.of(year, 1, 1).toEpochDay() * MILLISECONDS_PER_DAY);
                c.setDates(Collections.singleton(new DefaultCitationDate(date, DateType.IN_FORCE)));
            }
            if (i != 0) {
                buffer.setLength(i);
                // Same limitation than MetadataBuilder.party().
                final AbstractParty party = new AbstractParty(buffer, null);
                final DefaultResponsibility r = new DefaultResponsibility(Role.OWNER, null, party);
                c.setCitedResponsibleParties(Collections.singleton(r));
            }
            constraints.getReferences().add(c);
        }
    }

    /**
     * Parses the legal notice. The method expects a string of the form
     * “Copyright, John Smith, 1992. All rights reserved.”
     * The result of above example will be:
     *
     * {@preformat text
     *   Metadata
     *     └─Identification info
     *         └─Resource constraints
     *             ├─Use constraints……………………………… Copyright
     *             └─Reference
     *                 ├─Title……………………………………………… Copyright (C), John Smith, 1992. All rights reserved.
     *                 ├─Date
     *                 │   ├─Date……………………………………… 1992-01-01
     *                 │   └─Date type………………………… In force
     *                 └─Cited responsible party
     *                     ├─Party
     *                     │   └─Name…………………………… John Smith
     *                     └─Role……………………………………… Owner
     * }
     *
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceConstraint}</li>
     * </ul>
     *
     * @param  notice  the legal notice, or {@code null} for no-operation.
     */
    public final void parseLegalNotice(final String notice) {
        if (notice != null) {
            LegalSymbols.parse(notice, constraints());
        }
    }

    /**
     * Adds an access constraint applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations on obtaining the resource.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceConstraint/accessConstraints}</li>
     * </ul>
     *
     * @param  restriction  access constraints applied, or {@code null} for no-operation.
     */
    public final void addAccessConstraint(final Restriction restriction) {
        if (restriction != null) {
            // No need to use 'addIfNotPresent(…)' for code lists.
            constraints().getAccessConstraints().add(restriction);
        }
    }

    /**
     * Adds a limitation affecting the fitness for use of the resource.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceConstraint/useLimitation}</li>
     * </ul>
     *
     * @param  limitation  limitation affecting the fitness for use, or {@code null} for no-operation.
     */
    public final void addUseLimitation(final CharSequence limitation) {
        final InternationalString i18n = trim(limitation);
        if (i18n != null) {
            addIfNotPresent(constraints().getUseLimitations(), i18n);
        }
    }

    /**
     * Adds the given coordinate reference system to metadata, if it does not already exists.
     * This method ensures that there is no duplicated values.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/referenceSystemInfo}</li>
     * </ul>
     *
     * @param  crs  the coordinate reference system to add to the metadata, or {@code null} for no-operation.
     */
    public final void addReferenceSystem(final ReferenceSystem crs) {
        if (crs != null) {
            addIfNotPresent(metadata().getReferenceSystemInfo(), crs);
        }
    }

    /**
     * Adds the given geometry as a bounding polygon.
     * Storage locations is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/extent/geographicElement/polygon}</li>
     * </ul>
     *
     * @param  bounds  the bounding polygon, or {@code null} if none.
     */
    public final void addBoundingPolygon(final Geometry bounds) {
        if (bounds != null) {
            addIfNotPresent(extent().getGeographicElements(), new DefaultBoundingPolygon(bounds));
        }
    }

    /**
     * Adds a geographic extent described by an identifier. The given identifier is stored as-is as
     * the natural language description, and possibly in a modified form as the geographic identifier.
     * See {@link DefaultGeographicDescription#DefaultGeographicDescription(CharSequence)} for details.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/extent/geographicElement/description}</li>
     *   <li>{@code metadata/identificationInfo/extent/geographicElement/identifier}</li>
     * </ul>
     *
     * @param  identifier  identifier or description of spatial and temporal extent, or {@code null} for no-operation.
     */
    public final void addExtent(final CharSequence identifier) {
        final InternationalString i18n = trim(identifier);
        if (i18n != null) {
            addIfNotPresent(extent().getGeographicElements(), new DefaultGeographicDescription(identifier));
        }
    }

    /**
     * Adds the given envelope, including its CRS, to the metadata. If the metadata already contains a geographic
     * bounding box, then a new bounding box is added; this method does not compute the union of the two boxes.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/extent/geographicElement}</li>
     * </ul>
     *
     * @param  envelope  the extent to add in the metadata, or {@code null} for no-operation.
     * @throws TransformException if an error occurred while converting the given envelope to extents.
     */
    public final void addExtent(final AbstractEnvelope envelope) throws TransformException {
        if (envelope != null) {
            addReferenceSystem(envelope.getCoordinateReferenceSystem());
            if (!envelope.isAllNaN()) {
                extent().addElements(envelope);
            }
        }
    }

    /**
     * Adds a geographic bounding box initialized to the values in the given array.
     * The array must contains at least 4 values starting at the given index in this exact order:
     *
     * <ul>
     *   <li>{@code westBoundLongitude} (the minimal λ value), or {@code NaN}</li>
     *   <li>{@code eastBoundLongitude} (the maximal λ value), or {@code NaN}</li>
     *   <li>{@code southBoundLatitude} (the minimal φ value), or {@code NaN}</li>
     *   <li>{@code northBoundLatitude} (the maximal φ value), or {@code NaN}</li>
     * </ul>
     *
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/extent/geographicElement}</li>
     * </ul>
     *
     * @param  ordinates  the geographic coordinates, or {@code null} for no-operation.
     * @param  index      index of the first value to use in the given array.
     */
    public final void addExtent(final double[] ordinates, int index) {
        if (ordinates != null) {
            final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(
                        ordinates[index], ordinates[++index], ordinates[++index], ordinates[++index]);
            if (!bbox.isEmpty()) {
                addIfNotPresent(extent().getGeographicElements(), bbox);
            }
        }
    }

    /**
     * Adds a vertical extent covered by the data.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/extent/verticalElement}</li>
     * </ul>
     *
     * @param  minimumValue  the lowest vertical extent contained in the dataset, or {@link Double#NaN} if none.
     * @param  maximumValue  the highest vertical extent contained in the dataset, or {@link Double#NaN} if none.
     * @param  verticalCRS   the information about the vertical coordinate reference system, or {@code null}.
     */
    public final void addVerticalExtent(final double minimumValue,
                                        final double maximumValue,
                                        final VerticalCRS verticalCRS)
    {
        if (!Double.isNaN(minimumValue) || !Double.isNaN(maximumValue) || verticalCRS != null) {
            addIfNotPresent(extent().getVerticalElements(),
                            new DefaultVerticalExtent(minimumValue, maximumValue, verticalCRS));
        }
    }

    /**
     * Adds a temporal extent covered by the data.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/extent/temporalElement}</li>
     * </ul>
     *
     * @param  startTime  when the data begins, or {@code null} if unbounded.
     * @param  endTime    when the data ends, or {@code null} if unbounded.
     * @throws UnsupportedOperationException if the temporal module is not on the classpath.
     *
     * @see #addAcquisitionTime(Date)
     */
    public final void addTemporalExtent(final Date startTime, final Date endTime) {
        if (startTime != null || endTime != null) {
            final DefaultTemporalExtent t = new DefaultTemporalExtent();
            t.setBounds(startTime, endTime);
            addIfNotPresent(extent().getTemporalElements(), t);
        }
    }

    /**
     * Adds descriptions for the given feature.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/featureTypes/featureTypeName}</li>
     *   <li>{@code metadata/contentInfo/featureTypes/featureInstanceCount}</li>
     * </ul>
     *
     * This method returns the feature name for more convenient chaining with
     * {@link org.apache.sis.storage.FeatureNaming#add FeatureNaming.add(…)}.
     * Note that the {@link FeatureCatalogBuilder} subclasses can also be used for that chaining.
     *
     * @param  type         the feature type to add, or {@code null} for no-operation.
     * @param  occurrences  number of instances of the given feature type, or {@code null} if unknown.
     * @return the name of the added feature, or {@code null} if none.
     *
     * @see FeatureCatalogBuilder#define(FeatureType)
     */
    public final GenericName addFeatureType(final FeatureType type, final Integer occurrences) {
        if (type != null) {
            final GenericName name = type.getName();
            if (name != null) {
                final DefaultFeatureTypeInfo info = new DefaultFeatureTypeInfo(name);
                if (occurrences != null) {
                    info.setFeatureInstanceCount(shared(occurrences));
                }
                addIfNotPresent(featureDescription().getFeatureTypeInfo(), info);
                return name;
            }
        }
        return null;
    }

    /**
     * Adds a method used to spatially represent geographic information.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/spatialRepresentationType}</li>
     * </ul>
     *
     * @param  type  method used to spatially represent geographic information, or {@code null} for no-operation.
     */
    public final void addSpatialRepresentation(final SpatialRepresentationType type) {
        if (type != null) {
            // No need to use 'addIfNotPresent(…)' for code lists.
            identification().getSpatialRepresentationTypes().add(type);
        }
    }

    /**
     * Adds and populates a "spatial representation info" node using the given grid geometry.
     * If this method invokes implicitly {@link #newGridRepresentation(GridType)}, unless this
     * method returns {@code false} in which case nothing has been done.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/transformationDimensionDescription}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/transformationParameterAvailability}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionName}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/resolution}</li>
     *   <li>{@code metadata/identificationInfo/spatialRepresentationType}</li>
     *   <li>{@code metadata/referenceSystemInfo}</li>
     * </ul>
     *
     * @param  description    a general description of the "grid to CRS" transformation, or {@code null} if none.
     * @param  grid           the grid extent, "grid to CRS" transform and target CRS, or {@code null} if none.
     * @param  addResolution  whether to declare the resolutions. Callers should set this argument to {@code false} if they intend
     *                        to provide the resolution themselves, or if grid axes are not in the same order than CRS axes.
     * @return whether a "spatial representation info" node has been added.
     */
    public final boolean addSpatialRepresentation(final String description, final GridGeometry grid, final boolean addResolution) {
        final GridType type;
        if (grid == null) {
            if (description == null) {
                return false;
            }
            type = GridType.UNSPECIFIED;
        } else {
            type = grid.isConversionLinear(0, 1) ? GridType.GEORECTIFIED : GridType.GEOREFERENCEABLE;
        }
        addSpatialRepresentation(SpatialRepresentationType.GRID);
        newGridRepresentation(type);
        setGridToCRS(description);
        if (grid != null) {
            setGeoreferencingAvailability(grid.isDefined(GridGeometry.GRID_TO_CRS), false, false);
            CoordinateSystem cs = null;
            if (grid.isDefined(GridGeometry.CRS)) {
                final CoordinateReferenceSystem crs = grid.getCoordinateReferenceSystem();
                cs = crs.getCoordinateSystem();
                addReferenceSystem(crs);
            }
            if (grid.isDefined(GridGeometry.EXTENT)) {
                final GridExtent extent = grid.getExtent();
                final int dimension = extent.getDimension();
                for (int i=0; i<dimension; i++) {
                    final Optional<DimensionNameType> axisType = extent.getAxisType(i);
                    if (axisType.isPresent()) {
                        setAxisName(i, axisType.get());
                    }
                    final long size = extent.getSize(i);
                    if (size >= 0 && size <= Integer.MAX_VALUE) {
                        setAxisLength(i, (int) size);
                    }
                }
            }
            if (addResolution && grid.isDefined(GridGeometry.RESOLUTION)) {
                final double[] resolution = grid.getResolution(false);
                for (int i=0; i<resolution.length; i++) {
                    setAxisResolution(i, resolution[i], (cs != null) ? cs.getAxis(i).getUnit() : null);
                }
            }
        }
        return true;
    }

    /**
     * Adds a linear resolution in metres.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/spatialResolution/distance}</li>
     * </ul>
     *
     * @param  distance  the resolution in metres, or {@code NaN} for no-operation.
     */
    public final void addResolution(final double distance) {
        if (!Double.isNaN(distance)) {
            final DefaultResolution r = new DefaultResolution();
            r.setDistance(shared(distance));
            addIfNotPresent(identification().getSpatialResolutions(), r);
        }
    }

    /**
     * Sets identification of grid data as point or cell.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/cellGeometry}</li>
     * </ul>
     *
     * @param  value   whether the data represent point or area, or {@code null} for no-operation.
     */
    public final void setCellGeometry(final CellGeometry value) {
        if (value != null) {
            gridRepresentation().setCellGeometry(value);
        }
    }

    /**
     * Sets the point in a pixel corresponding to the Earth location of the pixel.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/pointInPixel}</li>
     * </ul>
     *
     * @param  value   whether the data represent point or area, or {@code null} for no-operation.
     */
    public final void setPointInPixel(final PixelOrientation value) {
        if (value != null) {
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            if (gridRepresentation instanceof DefaultGeorectified) {
                ((DefaultGeorectified) gridRepresentation).setPointInPixel(value);
            }
        }
    }

    /**
     * Sets whether parameters for transformation, control/check point(s) or orientation parameters are available.
     * Storage location are:
     *
     * <ul>
     *   <li>If georeferenceable:<ul>
     *     <li>{@code metadata/spatialRepresentationInfo/transformationParameterAvailability}</li>
     *     <li>{@code metadata/spatialRepresentationInfo/controlPointAvailability}</li>
     *     <li>{@code metadata/spatialRepresentationInfo/orientationParameterAvailability}</li>
     *   </ul></li>
     *   <li>If georeferenced:<ul>
     *     <li>{@code metadata/spatialRepresentationInfo/transformationParameterAvailability}</li>
     *     <li>{@code metadata/spatialRepresentationInfo/checkPointAvailability}</li>
     *   </ul></li>
     * </ul>
     *
     * @param  transformationParameterAvailability  indication of whether or not parameters for transformation exists.
     * @param  controlPointAvailability             indication of whether or not control or check point(s) exists.
     * @param  orientationParameterAvailability     indication of whether or not orientation parameters are available.
     */
    public final void setGeoreferencingAvailability(final boolean transformationParameterAvailability,
                                                    final boolean controlPointAvailability,
                                                    final boolean orientationParameterAvailability)
    {
        final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
        gridRepresentation.setTransformationParameterAvailable(transformationParameterAvailability);
        if (gridRepresentation instanceof DefaultGeorectified) {
            ((DefaultGeorectified) gridRepresentation).setCheckPointAvailable(controlPointAvailability);
        } else if (gridRepresentation instanceof DefaultGeoreferenceable) {
            ((DefaultGeoreferenceable) gridRepresentation).setControlPointAvailable(controlPointAvailability);
            ((DefaultGeoreferenceable) gridRepresentation).setOrientationParameterAvailable(orientationParameterAvailability);
        }
    }

    /**
     * Adds information about the geolocation of an image.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/geolocationInformation}</li>
     * </ul>
     *
     * @param  info  the geolocation information to add, or {@code null} if none.
     */
    public final void addGeolocation(final GeolocationInformation info) {
        if (info != null) {
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            if (gridRepresentation instanceof DefaultGeoreferenceable) {
                addIfNotPresent(((DefaultGeoreferenceable) gridRepresentation).getGeolocationInformation(), info);
            }
        }
    }

    /**
     * Adds <cite>check points</cite> (if georectified) or <cite>ground control points</cite> (if georeferenceable).
     * Ground control points (GCP) are large marked targets on the ground. GCP should not be used for storing the
     * localization grid (e.g. "model tie points" in a GeoTIFF file).
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/checkPoint/geographicCoordinates} if georectified</li>
     *   <li>{@code metadata/spatialRepresentationInfo/geolocationInformation/gcp/geographicCoordinates} if georeferenceable</li>
     * </ul>
     *
     * @param  geographicCoordinates  the geographic or map position of the control point, in either two or three dimensions.
     * @param  accuracyReport         the accuracy of a ground control point, or {@code null} if none.
     *                                Ignored if {@code geographicCoordinates} is null.
     */
    public final void addControlPoints(final DirectPosition geographicCoordinates, final Element accuracyReport) {
        if (geographicCoordinates != null) {
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            final Collection<GCP> points;
            if (gridRepresentation instanceof DefaultGeorectified) {
                points = ((DefaultGeorectified) gridRepresentation).getCheckPoints();
            } else if (gridRepresentation instanceof DefaultGeoreferenceable) {
                points = groundControlPoints().getGCPs();
            } else {
                return;
            }
            final DefaultGCP gcp = new DefaultGCP();
            gcp.setGeographicCoordinates(geographicCoordinates);
            if (accuracyReport != null) {
                addIfNotPresent(gcp.getAccuracyReports(), accuracyReport);
            }
            addIfNotPresent(points, gcp);
        }
    }

    /**
     * Sets a general description of the transformation from grid coordinates to "real world" coordinates.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/transformationDimensionDescription}</li>
     * </ul>
     *
     * @param  value  a general description of the "grid to CRS" transformation, or {@code null} for no-operation.
     */
    public final void setGridToCRS(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            if (gridRepresentation instanceof DefaultGeorectified) {
                ((DefaultGeorectified) gridRepresentation).setTransformationDimensionDescription(i18n);
            }
        }
    }

    /**
     * Returns the axis at the given dimension index. All previous dimensions are created if needed.
     *
     * @param  index  index of the desired dimension.
     * @return dimension at the given index.
     */
    private DefaultDimension axis(final int index) {
        final List<Dimension> axes = gridRepresentation().getAxisDimensionProperties();
        for (int i=axes.size(); i <= index; i++) {
            axes.add(new DefaultDimension());
        }
        return (DefaultDimension) axes.get(index);
    }

    /**
     * Sets the number of cells along the given dimension.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionName}</li>
     * </ul>
     *
     * @param  dimension  the axis dimension.
     * @param  name       the name to set for the given dimension.
     */
    public final void setAxisName(final int dimension, final DimensionNameType name) {
        axis(dimension).setDimensionName(name);
    }

    /**
     * Sets the number of cells along the given dimension.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize}</li>
     * </ul>
     *
     * @param  dimension  the axis dimension.
     * @param  length     number of cell values along the given dimension.
     */
    public final void setAxisLength(final int dimension, final int length) {
        axis(dimension).setDimensionSize(shared(length));
    }

    /**
     * Sets the degree of detail in the given dimension.
     * This method does nothing if the given resolution if NaN or infinite.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/resolution}</li>
     * </ul>
     *
     * @param  dimension   the axis dimension.
     * @param  resolution  the degree of detail in the grid dataset, or NaN for no-operation.
     * @param  unit        the resolution unit, of {@code null} if unknown.
     */
    public final void setAxisResolution(final int dimension, double resolution, final Unit<?> unit) {
        if (Double.isFinite(resolution)) {
            /*
             * Value should be a Quantity<?>. Since GeoAPI does not yet allow that,
             * we convert to metres for now. Future version should store the value
             * as-is with its unit of measurement (TODO).
             */
            if (Units.isLinear(unit)) {
                resolution = unit.asType(Length.class).getConverterTo(Units.METRE).convert(resolution);
            }
            axis(dimension).setResolution(shared(resolution));
        }
    }

    /**
     * Adds type of information represented in the cell.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/contentType}</li>
     * </ul>
     *
     * @param  type  type of information represented in the cell, or {@code null} for no-operation.
     */
    public final void addContentType(final CoverageContentType type) {
        if (type != null) {
            attributeGroup().getContentTypes().add(type);
        }
    }

    /**
     * Sets the name or number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/sequenceIdentifier}</li>
     * </ul>
     *
     * @param  sequenceIdentifier  the band name or number, or {@code null} for no-operation.
     */
    public final void setBandIdentifier(final MemberName sequenceIdentifier) {
        if (sequenceIdentifier != null) {
            sampleDimension().setSequenceIdentifier(sequenceIdentifier);
        }
    }

    /**
     * Sets the number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     * This is a convenience method for {@link #setBandIdentifier(MemberName)} when the band is specified only
     * by a number.
     *
     * @param  sequenceIdentifier  the band number, or 0 or negative if none.
     */
    public final void setBandIdentifier(final int sequenceIdentifier) {
        if (sequenceIdentifier > 0) {
            final boolean cached = (sequenceIdentifier <= BAND_NUMBERS.length);
            MemberName name = null;
            if (cached) synchronized (BAND_NUMBERS) {
                name = BAND_NUMBERS[sequenceIdentifier - 1];
            }
            if (name == null) {
                name = Names.createMemberName(null, null, String.valueOf(sequenceIdentifier), Integer.class);
                if (cached) synchronized (BAND_NUMBERS) {
                    /*
                     * No need to check if a value has been set concurrently because Names.createMemberName(…)
                     * already checked if an equal instance exists in the current JVM.
                     */
                    BAND_NUMBERS[sequenceIdentifier - 1] = name;
                }
            }
            setBandIdentifier(name);
        }
    }

    /**
     * Adds an identifier for the current band.
     * These identifiers can be use to provide names for the attribute from a standard set of names.
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/name}</li>
     * </ul>
     *
     * @param  authority  identifies which controlled list of name is used, or {@code null} if none.
     * @param  name       the band name, or {@code null} for no-operation.
     */
    public final void addBandName(final CharSequence authority, String name) {
        if (name != null && !(name = name.trim()).isEmpty()) {
            addIfNotPresent(sampleDimension().getNames(), sharedIdentifier(authority, name));
        }
    }

    /**
     * Adds a description of the current band.
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/description}</li>
     * </ul>
     *
     * @param  description  the band description, or {@code null} for no-operation.
     */
    public final void addBandDescription(final CharSequence description) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultSampleDimension sampleDimension = sampleDimension();
            sampleDimension.setDescription(append(sampleDimension.getDescription(), i18n));
        }
    }

    /**
     * Adds a description of a particular sample value.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/rangeElementDescription}</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * ISO 19115 range elements are approximately equivalent to
     * {@code org.apache.sis.coverage.Category} in the {@code sis-coverage} module.</div>
     *
     * @param  name        designation associated with a set of range elements, or {@code null} if none.
     * @param  definition  description of a set of specific range elements, or {@code null} if none.
     */
    public void addSampleValueDescription(final CharSequence name, final CharSequence definition) {
        final InternationalString i18n = trim(name);
        final InternationalString def  = trim(definition);
        if (i18n != null && def != null) {
            final DefaultRangeElementDescription element = new DefaultRangeElementDescription();
            element.setName(i18n);
            element.setDefinition(def);
            addIfNotPresent(coverageDescription().getRangeElementDescriptions(), element);
        }
    }

    /**
     * Adds a minimal value for the current sample dimension. If a minimal value was already defined, then
     * the new value will be set only if it is smaller than the existing one. {@code NaN} values are ignored.
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/minValue}</li>
     * </ul>
     *
     * @param value  the minimal value to add to the existing range of sample values, or {@code NaN} for no-operation.
     */
    public final void addMinimumSampleValue(final double value) {
        if (!Double.isNaN(value)) {
            final DefaultSampleDimension sampleDimension = sampleDimension();
            final Double current = sampleDimension.getMinValue();
            if (current == null || value < current) {
                sampleDimension.setMinValue(shared(value));
            }
        }
    }

    /**
     * Adds a maximal value for the current sample dimension. If a maximal value was already defined, then
     * the new value will be set only if it is greater than the existing one. {@code NaN} values are ignored.
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/maxValue}</li>
     * </ul>
     *
     * @param value  the maximal value to add to the existing range of sample values, or {@code NaN} for no-operation.
     */
    public final void addMaximumSampleValue(final double value) {
        if (!Double.isNaN(value)) {
            final DefaultSampleDimension sampleDimension = sampleDimension();
            final Double current = sampleDimension.getMaxValue();
            if (current == null || value > current) {
                sampleDimension.setMaxValue(shared(value));
            }
        }
    }

    /**
     * Sets the units of data in the current band.
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/unit}</li>
     * </ul>
     *
     * @param  unit  units of measurement of sample values.
     */
    public final void setSampleUnits(final Unit<?> unit) {
        if (unit != null) {
            sampleDimension().setUnits(unit);
        }
    }

    /**
     * Sets the scale factor and offset which have been applied to the cell value.
     * The transfer function type is declared {@linkplain TransferFunctionType#LINEAR linear}
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/scale}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/offset}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/transferFunctionType}</li>
     * </ul>
     *
     * @param scale   the scale factor which has been applied to the cell value.
     * @param offset  the physical value corresponding to a cell value of zero.
     */
    public final void setTransferFunction(final double scale, final double offset) {
        if (!Double.isNaN(scale) || !Double.isNaN(offset)) {
            final DefaultSampleDimension sd = sampleDimension();
            if (!Double.isNaN(scale))  sd.setScaleFactor(scale);
            if (!Double.isNaN(offset)) sd.setOffset(offset);
            sd.setTransferFunctionType(TransferFunctionType.LINEAR);
        }
    }

    /**
     * Sets the maximum number of significant bits in the uncompressed representation for the value in current band.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/bitsPerValue}</li>
     * </ul>
     *
     * @param  bits  the new maximum number of significant bits.
     * @throws IllegalArgumentException if the given value is zero or negative.
     */
    public final void setBitPerSample(final int bits) {
        sampleDimension().setBitsPerValue(bits);
    }

    /**
     * Sets an identifier for the level of processing that has been applied to the coverage.
     * For image descriptions, this is the image distributor's code that identifies the level
     * of radiometric and geometric processing that has been applied.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/processingLevelCode}</li>
     * </ul>
     *
     * Note that another storage location exists at {@code metadata/identificationInfo/processingLevel}
     * but is currently not used.
     *
     * @param  authority        identifies which controlled list of code is used, or {@code null} if none.
     * @param  processingLevel  identifier for the level of processing that has been applied to the resource,
     *                          or {@code null} for no-operation.
     */
    public final void setProcessingLevelCode(final CharSequence authority, String processingLevel) {
        if (processingLevel != null) {
            processingLevel = processingLevel.trim();
            if (!processingLevel.isEmpty()) {
                coverageDescription().setProcessingLevelCode(sharedIdentifier(authority, processingLevel));
            }
        }
    }

    /**
     * Sets the area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     * This method does nothing if the given value is {@link Double#NaN}.
     *
     * <p>This method is available only if {@link #newCoverage(boolean)} has been invoked
     * with the {@code electromagnetic} parameter set to {@code true}. Storage location is:</p>
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/cloudCoverPercentage}</li>
     * </ul>
     *
     * @param  value  the new cloud percentage, or {@code NaN} for no-operation.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public final void setCloudCoverPercentage(final double value) {
        if (!Double.isNaN(value)) {
            ((DefaultImageDescription) coverageDescription()).setCloudCoverPercentage(shared(value));
        }
    }

    /**
     * Sets the illumination azimuth measured in degrees clockwise from true north at the time the image is taken.
     * For images from a scanning device, refer to the centre pixel of the image.
     * This method does nothing if the given value is {@link Double#NaN}.
     *
     * <p>This method is available only if {@link #newCoverage(boolean)} has been invoked
     * with the {@code electromagnetic} parameter set to {@code true}. Storage location is:</p>
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/illuminationAzimuthAngle}</li>
     * </ul>
     *
     * @param  value  the new illumination azimuth angle, or {@code NaN} for no-operation.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public final void setIlluminationAzimuthAngle(final double value) {
        if (!Double.isNaN(value)) {
            ((DefaultImageDescription) coverageDescription()).setIlluminationAzimuthAngle(shared(value));
        }
    }

    /**
     * Sets the illumination elevation measured in degrees clockwise from the target plane
     * at intersection of the optical line of sight with the Earth's surface.
     * For images from a canning device, refer to the centre pixel of the image.
     * This method does nothing if the given value is {@link Double#NaN}.
     *
     * <p>This method is available only if {@link #newCoverage(boolean)} has been invoked
     * with the {@code electromagnetic} parameter set to {@code true}. Storage location is:</p>
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/illuminationElevationAngle}</li>
     * </ul>
     *
     * @param  value  the new illumination azimuth angle, or {@code NaN} for no-operation.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public final void setIlluminationElevationAngle(final double value) {
        if (!Double.isNaN(value)) {
            ((DefaultImageDescription) coverageDescription()).setIlluminationElevationAngle(shared(value));
        }
    }

    /**
     * Adds a platform on which instrument are installed. If a platform was already defined
     * with a different identifier, then a new platform instance will be created.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/acquisitionInformation/platform/identifier}</li>
     * </ul>
     *
     * @param  authority   identifiers the authority that define platform codes, or {@code null} if none.
     * @param  identifier  identifier of the platform to add, or {@code null} for no-operation.
     */
    public final void addPlatform(final CharSequence authority, String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            if (platform != null) {
                final Identifier current = platform.getIdentifier();
                if (current != null) {
                    if (identifier.equals(current.getCode())) {
                        return;
                    }
                    acquisition().getPlatforms().add(platform);
                    platform = null;
                }
            }
            platform().setIdentifier(sharedIdentifier(authority, identifier));
        }
    }

    /**
     * Adds an instrument or sensor on the platform.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/acquisitionInformation/platform/instrument/identifier}</li>
     * </ul>
     *
     * @param  authority   identifiers the authority that define instrument codes, or {@code null} if none.
     * @param  identifier  identifier of the sensor to add, or {@code null} for no-operation.
     */
    public final void addInstrument(final CharSequence authority, String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            final DefaultInstrument instrument = new DefaultInstrument();
            instrument.setIdentifier(sharedIdentifier(authority, identifier));
            addIfNotPresent(platform().getInstruments(), instrument);
        }
    }

    /**
     * Adds an event that describe the time at which data were acquired.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/acquisitionInformation/operation/significantEvent/time}</li>
     * </ul>
     *
     * @param  time  the acquisition time, or {@code null} for no-operation.
     *
     * @see #addTemporalExtent(Date, Date)
     */
    public final void addAcquisitionTime(final Date time) {
        if (time != null) {
            final DefaultEvent event = new DefaultEvent();
            event.setContext(Context.ACQUISITION);
            event.setTime(time);
            final DefaultOperation op = new DefaultOperation();
            op.setSignificantEvents(singleton(event));
            op.setType(OperationType.REAL);
            op.setStatus(Progress.COMPLETED);
            addIfNotPresent(acquisition().getOperations(), op);
        }
    }

    /**
     * Adds the identifier of the operation used to acquire the dataset.
     * Examples: "GHRSST", "NOAA CDR", "NASA EOS", "JPSS", "GOES-R".
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/acquisitionInformation/operation/identifier}</li>
     * </ul>
     *
     * @param  program     identification of the mission, or {@code null} if none.
     * @param  identifier  unique identification of the operation, or {@code null} for no-operation.
     */
    public final void addAcquisitionOperation(final CharSequence program, String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            final DefaultOperation r = new DefaultOperation();
            r.setIdentifier(sharedIdentifier(program, identifier));
            addIfNotPresent(acquisition().getOperations(), r);
        }
    }

    /**
     * Adds the identifier of the requirement to be satisfied by data acquisition.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/acquisitionInformation/acquisitionRequirement/identifier}</li>
     * </ul>
     *
     * @param  authority   specifies the authority that define requirement codes, or {@code null} if none.
     * @param  identifier  unique name or code for the requirement, or {@code null} for no-operation.
     */
    public final void addAcquisitionRequirement(final CharSequence authority, String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            final DefaultRequirement r = new DefaultRequirement();
            r.setIdentifier(sharedIdentifier(authority, identifier));
            addIfNotPresent(acquisition().getAcquisitionRequirements(), r);
        }
    }

    /**
     * Adds a general explanation of the data producer's knowledge about the lineage of a dataset.
     * If a statement already exists, the new one will be appended after a new line.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/statement}</li>
     * </ul>
     *
     * @param statement  explanation of the data producer's knowledge about the lineage, or {@code null} for no-operation.
     *
     * @see #addProcessDescription(CharSequence)
     */
    public final void addLineage(final CharSequence statement) {
        final InternationalString i18n = trim(statement);
        if (i18n != null) {
            final DefaultLineage lineage = lineage();
            lineage.setStatement(append(lineage.getStatement(), i18n));
        }
    }

    /**
     * Adds information about a source of data used for producing the resource.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/source/description}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/level}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/levelDescription/features}</li>
     * </ul>
     *
     * <div class="note"><b>Example:</b>
     * if a Landsat image uses the "GTOPO30" digital elevation model, then it can declare the source
     * with "GTOPO30" description, {@link ScopeCode#MODEL} and feature "Digital Elevation Model".</div>
     *
     * @param  description  a detailed description of the level of the source data, or {@code null} if none.
     * @param  level        hierarchical level of the source (e.g. model), or {@code null} if unspecified.
     * @param  feature      more detailed name for {@code level}, or {@code null} if none.
     *
     * @see #addProcessing(CharSequence, String)
     * @see #addProcessDescription(CharSequence)
     */
    public final void addSource(final CharSequence description, final ScopeCode level, final CharSequence feature) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultSource source = new DefaultSource(description);
            if (level != null || feature != null) {
                DefaultScope scope = new DefaultScope(level);
                if (feature != null) {
                    final DefaultScopeDescription sd = new DefaultScopeDescription();
                    sd.getFeatures().add(feature);
                    scope.getLevelDescription().add(sd);
                }
            }
            addIfNotPresent(lineage().getSources(), source);
        }
    }

    /**
     * Adds information about the procedure, process and algorithm applied in a process step.
     * If a processing was already defined with a different identifier, then a new processing
     * instance will be created. Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/processStep/processingInformation/identifier}</li>
     * </ul>
     *
     * @param  authority   identifies the authority that defines processing code, or {@code null} if none.
     * @param  identifier  processing package that produced the data, or {@code null} for no-operation.
     *
     * @see #addSoftwareReference(CharSequence)
     * @see #addHostComputer(CharSequence)
     * @see #addProcessDescription(CharSequence)
     * @see #addSource(CharSequence, ScopeCode, CharSequence)
     */
    public final void addProcessing(final CharSequence authority, String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            if (processing != null) {
                final Identifier current = processing.getIdentifier();
                if (current != null) {
                    if (identifier.equals(current.getCode())) {
                        return;
                    }
                    processStep().setProcessingInformation(processing);
                    addIfNotPresent(lineage().getProcessSteps(), processStep);
                    processing  = null;
                    processStep = null;
                }
            }
            processing().setIdentifier(sharedIdentifier(authority, identifier));
        }
    }

    /**
     * Adds a reference to document describing processing software.
     * This is added to the processing identified by last call to {@link #addProcessing(CharSequence, String)}.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/processStep/processingInformation/softwareReference/title}</li>
     * </ul>
     *
     * @param  title  title of the document that describe the software, or {@code null} for no-operation.
     *
     * @see #addProcessing(CharSequence, String)
     * @see #addSource(CharSequence, ScopeCode, CharSequence)
     */
    public final void addSoftwareReference(final CharSequence title) {
        final InternationalString i18n = trim(title);
        if (i18n != null) {
            addIfNotPresent(processing().getSoftwareReferences(), sharedCitation(i18n));
        }
    }

    /**
     * Adds information about the computer and/or operating system in use at the processing time.
     * This is added to the processing identified by last call to {@link #addProcessing(CharSequence, String)}.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/processStep/processingInformation/procedureDescription}</li>
     * </ul>
     *
     * @param  platform  name of the system on which the processing has been executed, or {@code null} for no-operation.
     *
     * @see #addProcessing(CharSequence, String)
     * @see #addSource(CharSequence, ScopeCode, CharSequence)
     */
    public final void addHostComputer(final CharSequence platform) {
        InternationalString i18n = trim(platform);
        if (i18n != null) {
            i18n = Resources.formatInternational(Resources.Keys.ProcessingExecutedOn_1, i18n);
            final DefaultProcessing p = processing();
            p.setProcedureDescription(append(p.getProcedureDescription(), i18n));
        }
    }

    /**
     * Adds additional details about the process step.
     * If a description already exists, the new one will be added on a new line.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/processStep/description}</li>
     * </ul>
     *
     * @param  description  additional details about the process step, or {@code null} for no-operation.
     *
     * @see #addProcessing(CharSequence, String)
     * @see #addSource(CharSequence, ScopeCode, CharSequence)
     * @see #addLineage(CharSequence)
     */
    public final void addProcessDescription(final CharSequence description) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultProcessStep ps = processStep();
            ps.setDescription(append(ps.getDescription(), i18n));
        }
    }

    /**
     * Adds a name to the resource format. Note that this method does not add a new format,
     * but only an alternative name to current format. Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceFormat/formatSpecificationCitation/alternateTitle}</li>
     * </ul>
     *
     * If this method is used together with {@link #setFormat(String)},
     * then {@code setFormat} should be invoked <strong>before</strong> this method.
     *
     * @param value  the format name, or {@code null} for no-operation.
     *
     * @see #setFormat(String)
     * @see #addCompression(CharSequence)
     */
    public final void addFormatName(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            final DefaultFormat format = format();
            DefaultCitation citation = DefaultCitation.castOrCopy(format.getFormatSpecificationCitation());
            if (citation == null) {
                citation = new DefaultCitation(i18n);
            } else {
                addIfNotPresent(citation.getAlternateTitles(), i18n);
            }
            format.setFormatSpecificationCitation(citation);
        }
    }

    /**
     * Adds a compression name.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceFormat/fileDecompressionTechnique}</li>
     * </ul>
     *
     * If this method is used together with {@link #setFormat(String)},
     * then {@code setFormat} should be invoked <strong>before</strong> this method.
     *
     * @param value  the compression name, or {@code null} for no-operation.
     *
     * @see #setFormat(String)
     * @see #addFormatName(CharSequence)
     */
    public final void addCompression(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            final DefaultFormat format = format();
            format.setFileDecompressionTechnique(append(format.getFileDecompressionTechnique(), i18n));
        }
    }

    /**
     * Adds a URL to a more complete description of the metadata.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/metadataLinkage/linkage}
     *     with {@code function} set to {@link OnLineFunction#COMPLETE_METADATA}</li>
     * </ul>
     *
     * @param  link  URL to a more complete description of the metadata, or {@code null}.
     */
    public final void addCompleteMetadata(final URI link) {
        if (link != null) {
            final DefaultOnlineResource ln = new DefaultOnlineResource(link);
            ln.setFunction(OnLineFunction.COMPLETE_METADATA);
            ln.setProtocol(link.getScheme());
            addIfNotPresent(metadata().getMetadataLinkages(), ln);
        }
    }

    /**
     * Returns the metadata (optionally as an unmodifiable object), or {@code null} if none.
     * If {@code freeze} is {@code true}, then the returned metadata instance can not be modified.
     *
     * @param  freeze  {@code true} if this method should set the returned metadata to
     *                 {@link DefaultMetadata.State#FINAL}, or {@code false} for leaving the metadata editable.
     * @return the metadata, or {@code null} if none.
     */
    public final DefaultMetadata build(final boolean freeze) {
        newIdentification();
        newGridRepresentation(GridType.UNSPECIFIED);
        newFeatureTypes();
        newCoverage(false);
        newAcquisition();
        newDistribution();
        newLineage();
        final DefaultMetadata md = metadata;
        metadata = null;
        if (freeze && md != null) {
            md.transition(DefaultMetadata.State.FINAL);
        }
        return md;
    }

    /**
     * Returns a shared instance of the given value.
     * This is a helper method for callers who want to set themselves some additional
     * metadata values on the instance returned by {@link #build(boolean)}.
     *
     * @param   value  a double value.
     * @return  the same value, but as an existing instance if possible.
     */
    public final Double shared(final Double value) {
        final Object existing = sharedValues.putIfAbsent(value, value);
        return (existing != null) ? (Double) existing : value;
    }

    /**
     * Returns a shared instance of the given value.
     * This is a helper method for callers who want to set themselves some additional
     * metadata values on the instance returned by {@link #build(boolean)}.
     *
     * @param   value  an integer value.
     * @return  the same value, but as an existing instance if possible.
     */
    public final Integer shared(final Integer value) {
        final Object existing = sharedValues.putIfAbsent(value, value);
        return (existing != null) ? (Integer) existing : value;
    }
}
