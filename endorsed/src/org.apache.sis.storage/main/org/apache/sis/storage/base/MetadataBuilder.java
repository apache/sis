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
package org.apache.sis.storage.base;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.net.URI;
import java.nio.charset.Charset;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.*;
import org.opengis.metadata.acquisition.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.constraint.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.distribution.*;
import org.opengis.metadata.extent.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.lineage.*;
import org.opengis.metadata.maintenance.*;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.spatial.*;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.AbstractInternationalString;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Version;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.iso.*;
import org.apache.sis.metadata.iso.acquisition.*;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.constraint.*;
import org.apache.sis.metadata.iso.content.*;
import org.apache.sis.metadata.iso.distribution.*;
import org.apache.sis.metadata.iso.extent.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.metadata.iso.lineage.*;
import org.apache.sis.metadata.iso.maintenance.*;
import org.apache.sis.metadata.iso.spatial.*;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.privy.Merger;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.pending.jdk.JDK21;
import org.apache.sis.measure.Units;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Helper methods for the metadata created by {@code DataStore} implementations.
 * This is not a general-purpose builder suitable for public API, since the
 * methods provided in this class are tailored for Apache SIS data store needs.
 * API of this class may change in any future SIS versions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Alexis Manin (Geomatys)
 */
public class MetadataBuilder {
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
     *   <tr><td>{@link Integer}</td>              <td>{@link Integer}</td>     <td>{@link #shared(int)}</td></tr>
     *   <tr><td>{@link Double}</td>               <td>{@link Double}</td>      <td>{@link #shared(double)}</td></tr>
     *   <tr><td>{@link Identifier}</td>           <td>{@link Identifier}</td>  <td>{@link #sharedIdentifier(CharSequence, String)}</td></tr>
     *   <tr><td>{@link InternationalString}</td>  <td>{@link Citation}</td>    <td>{@link #sharedCitation(InternationalString)}</td></tr>
     *   <tr><td>Other</td>                        <td>Same as key</td>         <td>{@link #shared(Class, Object)}</td></tr>
     * </table>
     */
    private final Map<Object,Object> sharedValues = new HashMap<>();

    /**
     * Whether to add ISO 19115-1 and ISO 19115-2 entries in "metadata standards" node.
     * Those entries will be added only if the metadata object would be otherwise non-empty.
     * A value of 1 will add ISO 19115-1. A value of 2 will add both ISO 19115-1 and ISO 19115-2.
     *
     * @see #setISOStandards(boolean)
     */
    private byte standardISO;

    // Other fields declared below together with closely related methods.

    /**
     * Creates a new metadata builder.
     */
    public MetadataBuilder() {
    }

    /**
     * Creates a new metadata builder for completing an existing metadata.
     * The given metadata shall may be modifiable. When a metadata element accepts many instances,
     * the instance which will be modified is the last one.
     *
     * @param  edit  the metadata to modify, or {@code null} if none.
     */
    public MetadataBuilder(final Metadata edit) {
        metadata = DefaultMetadata.castOrCopy(edit);
        useParentElements();
    }

    /**
     * The metadata created by this builder, or {@code null} if not yet created.
     */
    private DefaultMetadata metadata;

    /**
     * Creates the metadata object if it does not already exists, then returns it.
     *
     * @return the metadata (never {@code null}).
     * @see #build()
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
     * Returns the information about the series, or aggregate dataset, of which the dataset is a part.
     */
    private DefaultSeries series() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final DefaultCitation citation = citation();
        DefaultSeries series = DefaultSeries.castOrCopy(citation.getSeries());
        if (series == null) {
            series = new DefaultSeries();
            citation.setSeries(series);
        }
        return series;
    }

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private DefaultResponsibleParty responsibility;

    /**
     * Creates the responsibility object if it does not already exists, then returns it.
     *
     * @return the responsibility party (never {@code null}).
     */
    private DefaultResponsibleParty responsibility() {
        if (responsibility == null) {
            responsibility = new DefaultResponsibleParty();
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
     * <h4>Limitations</h4>
     * If the party type is unknown, then this method creates an {@code AbstractParty} instead of one of the subtypes.
     * This is not valid, but we currently have no way to guess if a party is an individual or an organization.
     * For now we prefer to let users know that the type is unknown rather than to pick a potentially wrong type.
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
            sampleDimension = electromagnetic ? new DefaultBand() : new DefaultSampleDimension();
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
            df = new DefaultFormat();
        }
        format = df;
        return df;
    }

    /**
     * Information about the events or source data used in constructing the data specified by the scope.
     *
     * @see DefaultLineage#getScope()
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
        if (party != null) {
            addIfNotPresent(responsibility().getParties(), party);
            party = null;
        }
        partyType = Objects.requireNonNull(type);
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
            metadata().setDistributionInfo(distribution);
            distribution = null;
        }
    }

    /**
     * Commits all pending information under the metadata "feature catalog" node.
     * If there is no pending feature description, then invoking this method has no effect.
     * If new feature descriptions are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "feature catalog description" node is desired.
     * It may also be useful when switching from writing feature types to writing coverage descriptions,
     * because both classes appear under the same "content information" node.
     * Invoking this method may avoid confusing ordering of those elements.</p>
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
     * or the {@code electromagnetic} flag needs to be set to {@code true}. It may also be useful when
     * switching from writing coverage descriptions to writing feature types,
     * because both classes appear under the same "content information" node.
     * Invoking this method may avoid confusing ordering of those elements.</p>
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
        gridType = Objects.requireNonNull(type);
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
     * Adds default metadata for the specified resource.
     * This is used for default implementation of {@link AbstractResource#createMetadata()}.
     *
     * @param  resource   the resource for which to add metadata.
     * @param  listeners  the listeners to notify in case of warning, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    public final void addDefaultMetadata(final AbstractResource resource, final StoreListeners listeners) throws DataStoreException {
        if (getTitle() == null) {
            // Note: title is mandatory in ISO metadata, contrarily to the identifier.
            resource.getIdentifier().ifPresent((name) -> addTitle(new Sentence(name)));
        }
        resource.getEnvelope().ifPresent((envelope) -> addExtent(envelope, listeners));
    }

    /**
     * Adds default metadata for the specified resource.
     * This is used for default implementation of {@link AbstractFeatureSet#createMetadata()}.
     *
     * @param  resource   the resource for which to add metadata.
     * @param  listeners  the listeners to notify in case of warning, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    public final void addDefaultMetadata(final AbstractFeatureSet resource, final StoreListeners listeners) throws DataStoreException {
        addDefaultMetadata((AbstractResource) resource, listeners);
        addFeatureType(resource.getType(), resource.getFeatureCount().orElse(-1));
    }

    /**
     * Adds default metadata for the specified resource.
     * This is used for default implementation of {@link AbstractGridCoverageResource#createMetadata()}.
     *
     * @param  resource   the resource for which to add metadata.
     * @param  listeners  the listeners to notify in case of warning, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    public final void addDefaultMetadata(final AbstractGridCoverageResource resource, final StoreListeners listeners) throws DataStoreException {
        addDefaultMetadata((AbstractResource) resource, listeners);
        addSpatialRepresentation(null, resource.getGridGeometry(), false);
        for (final SampleDimension band : resource.getSampleDimensions()) {
            addNewBand(band);
        }
    }

    /**
     * An international string where localized identifiers are formatted more like an English sentence.
     * This is used for wrapping {@link GenericName#toInternationalString()} representation for use as
     * a citation title.
     */
    private static final class Sentence extends AbstractInternationalString {
        /** The generic name localized representation. */
        private final InternationalString name;

        /** Returns a new wrapper for the given generic name. */
        Sentence(final GenericName name) {
            this.name = name.toInternationalString();
        }

        /** Returns the generic name as an English-like sentence. */
        @Override public String toString(final Locale locale) {
            return CharSequences.camelCaseToSentence(name.toString(locale)).toString();
        }

        /** Returns a hash code value for this sentence. */
        @Override public int hashCode() {
            return ~name.hashCode();
        }

        /** Compares the given object with this sentence for equality. */
        @Override public boolean equals(final Object other) {
            return (other instanceof Sentence) && name.equals(((Sentence) other).name);
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
        final var id = new DefaultIdentifier(sharedCitation(trim(authority)), code);
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
     * Adds a data and/or metadata identifier. This method performs the same work as
     * {@link #addIdentifier(CharSequence, String, Scope)} for situations where the
     * identifier instance is already available.
     *
     * @param  id     the identifier, or {@code null} if none.
     * @param  scope  whether the date applies to data, to metadata or to both.
     *
     * @see #addIdentifier(CharSequence, String, Scope)
     */
    public final void addIdentifier(Identifier id, final Scope scope) {
        if (id != null) {
            id = (Identifier) sharedValues.getOrDefault(id, id);
            if (scope != Scope.RESOURCE) metadata().setMetadataIdentifier(id);
            if (scope != Scope.METADATA) addIfNotPresent(citation().getIdentifiers(), id);
        }
    }

    /**
     * Adds a data and/or metadata identifier provided as a generic name.
     *
     * @param  id     the identifier, or {@code null} if none.
     * @param  scope  whether the date applies to data, to metadata or to both.
     *
     * @see #addIdentifier(CharSequence, String, Scope)
     */
    public final void addIdentifier(final GenericName id, final Scope scope) {
        if (id != null) {
            addIdentifier((id instanceof Identifier) ? (Identifier) id : new NamedIdentifier(id), scope);
        }
    }

    /**
     * Adds a resource (data) identifier, a metadata identifier, or both as they are often the same.
     * The identifier is added only if {@code code} is non-null, regardless other argument values.
     * Empty strings (ignoring spaces) are considered as null.
     * The identifier is not added if already presents.
     * Storage locations are:
     *
     * <ul>
     *   <li><b>Metadata:</b> {@code metadata/metadataIdentifier}</li>
     *   <li><b>Resource:</b> {@code metadata/identificationInfo/citation/identifier}</li>
     * </ul>
     *
     * @param  authority  the person or party responsible for maintenance of the namespace, or {@code null} if none.
     * @param  code       the identifier code, or {@code null} for no-operation.
     * @param  scope      whether the date applies to data, to metadata or to both.
     *
     * @see #addTitle(CharSequence)
     * @see #addTitleOrIdentifier(String, Scope)
     * @see #addIdentifier(Identifier, Scope)
     */
    public final void addIdentifier(final CharSequence authority, String code, final Scope scope) {
        code = Strings.trimOrNull(code);
        if (code != null) {
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
     * {@code identificationInfo/resourceFormat} node.
     *
     * @param  abbreviation  the format short name or abbreviation, or {@code null} for no-operation.
     * @param  listeners     where to report a failure to connect to the {@code jdbc/SpatialMetadata} database.
     * @param  fallback      whether to fallback on {@link #addFormatName(String)} if the description was not found.
     * @return whether the format description has been added.
     *
     * @see #addCompression(CharSequence)
     * @see #addFormatName(CharSequence)
     */
    public boolean setPredefinedFormat(final String abbreviation, final StoreListeners listeners, boolean fallback) {
        if (abbreviation != null && abbreviation.length() != 0) {
            if (format == null) try {
                format = MetadataSource.getProvided().lookup(Format.class, abbreviation);
                /*
                 * Additional step for converting deprecated "name" and "specification" into non-deprecated properties.
                 * This step is not required on SIS branches that depend on development branches of GeoAPI 3.1 or 4.0.
                 */
                format = DefaultFormat.castOrCopy(format);
                return true;
            } catch (MetadataStoreException e) {
                if (listeners != null) {
                    listeners.warning(Level.FINE, null, e);
                } else {
                    Logging.recoverableException(StoreUtilities.LOGGER, null, null, e);
                }
            }
            if (fallback) {
                addFormatName(abbreviation);
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a language used for documenting data and/or metadata.
     * Storage locations are:
     *
     * <ul>
     *   <li><b>Metadata:</b> {@code metadata/defaultLocale} or {@code metadata/otherLocale}</li>
     *   <li><b>Resource:</b> {@code metadata/identificationInfo/defaultLocale} or
     *                        {@code metadata/identificationInfo/otherLocale}</li>
     * </ul>
     *
     * @param  language  a language used for documenting data and/or metadata, or {@code null} for no-operation.
     * @param  encoding  the encoding associated to the locale, or {@code null} if unspecified.
     * @param  scope     whether the language applies to data, to metadata or to both.
     */
    public final void addLanguage(final Locale language, final Charset encoding, final Scope scope) {
        if (language != null) {
            if (scope != Scope.RESOURCE) metadata().getLocalesAndCharsets().put(language, encoding);
            if (scope != Scope.METADATA) identification().getLocalesAndCharsets().put(language, encoding);
        }
    }

    /**
     * Adds information about the scope of the resource.
     * The scope is typically (but not restricted to) {@code ScopeCode.COVERAGE},
     * {@link ScopeCode#FEATURE} or the more generic {@link ScopeCode#DATASET}.
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
     * except for {@code DateType.LAST_REVISION}, {@code DateType.LAST_UPDATE LAST_UPDATE} or any other date type
     * prefixed by {@code "LATE_"}, where only the latest date is kept.
     *
     * @param  date   the date to add, or {@code null} for no-operation..
     * @param  type   the type of the date to add, or {@code null} if none (not legal but tolerated).
     * @param  scope  whether the date applies to data, to metadata or to both.
     *
     * @see #addAcquisitionTime(Temporal)
     */
    public final void addCitationDate(final Temporal date, final DateType type, final Scope scope) {
        if (date != null) {
            final var cd = new DefaultCitationDate(date, type);
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
     * Returns the current citation title. This method is typically used for deciding
     * whether to use some fallback as title, because titles are mandatory in ISO 19115.
     *
     * @return the title defined in the current citation, or {@code null} if none.
     */
    public final InternationalString getTitle() {
        return (citation != null) ? citation.getTitle() : null;
    }

    /**
     * Sets the title, replacing any previous value.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/title}</li>
     * </ul>
     *
     * @param title  the resource title, or {@code null} for no-operation.
     */
    public final void setTitle(final CharSequence title) {
        final InternationalString i18n = trim(title);
        if (i18n != null) {
            citation().setTitle(i18n);
        }
    }

    /**
     * Adds a title or alternate title of the resource, if not already present.
     * This operation does nothing if the title is already defined and the given
     * title is already used as an identifier (this policy is a complement of the
     * {@link #addTitleOrIdentifier(String, Scope)} behavior).
     * Storage locations are:
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultCitation citation = citation();
            final InternationalString current = citation.getTitle();
            if (current == null) {
                citation.setTitle(i18n);
            } else if (!equals(current, i18n)) {
                for (final Identifier id : citation.getIdentifiers()) {
                    if (CharSequences.equalsFiltered(title, id.getCode(), Characters.Filter.LETTERS_AND_DIGITS, true)) {
                        return;
                    }
                }
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
        if (scope != Scope.METADATA) {
            if (getTitle() == null) {
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultCitation citation = citation();
            citation.setEdition(append(citation.getEdition(), i18n));
        }
    }

    /**
     * Adds the name of the series, or aggregate dataset, of which the dataset is a part.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/series/name}</li>
     * </ul>
     *
     * @param  name  name of the series, or {@code null} for no-operation.
     */
    public final void addSeries(final CharSequence name) {
        final InternationalString i18n = trim(name);
        if (i18n != null) {
            final DefaultSeries series = series();
            series.setName(append(series.getName(), i18n));
        }
    }

    /**
     * Adds details on which pages of the publication the article was published.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/series/page}</li>
     * </ul>
     *
     * @param  page  the page, or {@code null} for no-operation.
     */
    public final void addPage(final CharSequence page) {
        if (page != null) {
            final DefaultSeries series = series();
            series.setPage(page.toString());
        }
    }

    /**
     * Adds details on which pages of the publication the article was published.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/citation/series/page}</li>
     * </ul>
     *
     * @param  page   the page number, or 0 or negative for no-operation.
     * @param  total  the total number of pages, or 0 or negative if unknown.
     */
    public final void addPage(final int page, final int total) {
        if (page > 0) {
            addPage(Vocabulary.formatInternational(
                    (total > 0) ? Vocabulary.Keys.Page_2 : Vocabulary.Keys.Page_1, page, total));
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
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
            final DefaultCitation citation = citation();
            citation.setOtherCitationDetails(append(citation.getOtherCitationDetails(), i18n));
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
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
            // No need to use `addIfNotPresent(…)` for enumerations.
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
    public final void addCitedResponsibleParty(ResponsibleParty party, final Role role) {
        if (party != null) {
            if (role != null && !role.equals(party.getRole())) {
                party = new DefaultResponsibleParty(party);
                ((DefaultResponsibility) party).setRole(role);
            }
            addIfNotPresent(citation().getCitedResponsibleParties(), party);
        }
    }

    /**
     * Adds a means of communication with person(s) and organizations(s) associated with the resource(s).
     * This is often the same party as the above cited responsibly party, with only the role changed.
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
    public final void addPointOfContact(final ResponsibleParty contact, final Scope scope) {
        if (contact != null) {
            if (scope != Scope.RESOURCE) addIfNotPresent(metadata().getContacts(), contact);
            if (scope != Scope.METADATA) addIfNotPresent(identification().getPointOfContacts(), contact);
        }
    }

    /**
     * Adds a distributor. This is often the same as the above responsible party.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/distributionInfo/distributor/distributorContact}</li>
     * </ul>
     *
     * @param  distributor  the distributor, or {@code null} for no-operation.
     */
    public final void addDistributor(final ResponsibleParty distributor) {
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
        if (credit != null) {
            final String c = CharSequences.trimWhitespaces(credit).toString();
            if (!c.isEmpty()) {
                addIfNotPresent(identification().getCredits(), c);
            }
        }
    }

    /**
     * Parses the legal notice. The method expects a string of the form
     * “Copyright, John Smith, 1992. All rights reserved.”
     * The result of above example will be:
     *
     * <pre class="text">
     *   Metadata
     *     └─Identification info
     *         └─Resource constraints
     *             ├─Use constraints……………………………… Copyright
     *             └─Reference
     *                 ├─Title……………………………………………… Copyright (C), John Smith, 1992. All rights reserved.
     *                 ├─Date
     *                 │   ├─Date……………………………………… 1992
     *                 │   └─Date type………………………… In force
     *                 └─Cited responsible party
     *                     ├─Party
     *                     │   └─Name…………………………… John Smith
     *                     └─Role……………………………………… Owner</pre>
     *
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceConstraint}</li>
     * </ul>
     *
     * @param  locale  the language of the notice, or {@code null} if unspecified.
     * @param  notice  the legal notice, or {@code null} for no-operation.
     */
    public final void parseLegalNotice(final Locale locale, final String notice) {
        if (notice != null) {
            LegalSymbols.parse(locale, notice, constraints());
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
            // No need to use `addIfNotPresent(…)` for code lists.
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
     * @param  envelope   the extent to add in the metadata, or {@code null} for no-operation.
     * @param  listeners  the listeners to notify in case of warning, or {@code null} if none.
     */
    public final void addExtent(final Envelope envelope, final StoreListeners listeners) {
        if (envelope != null) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            addReferenceSystem(crs);
            if (!(envelope instanceof AbstractEnvelope && ((AbstractEnvelope) envelope).isAllNaN())) {
                if (crs != null) try {
                    extent().addElements(envelope);
                } catch (TransformException e) {
                    final boolean ignorable = (e instanceof NotSpatioTemporalException);
                    if (listeners != null) {
                        if (ignorable) {
                            listeners.warning(Level.FINE, null, e);
                        } else {
                            listeners.warning(e);
                        }
                    } else {
                        Logging.recoverableException(StoreUtilities.LOGGER, null, null, e);
                    }
                }
                // Future version could add as a geometry in unspecified CRS.
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
     * @param  coordinates  the geographic coordinates, or {@code null} for no-operation.
     * @param  index        index of the first value to use in the given array.
     */
    public final void addExtent(final double[] coordinates, int index) {
        if (coordinates != null) {
            final var bbox = new DefaultGeographicBoundingBox(
                        coordinates[index], coordinates[++index], coordinates[++index], coordinates[++index]);
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
     *
     * @see #addAcquisitionTime(Temporal)
     */
    public final void addTemporalExtent(final Temporal startTime, final Temporal endTime) {
        if (startTime != null || endTime != null) {
            final var t = new DefaultTemporalExtent(startTime, endTime);
            addIfNotPresent(extent().getTemporalElements(), t);
        }
    }

    /**
     * Adds descriptions for the given feature.
     * Storage locations are:
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
     * @param  occurrences  number of instances of the given feature type, or a negative value if unknown.
     *         Note that ISO-19115 considers 0 as an invalid value. Consequently, if 0, the feature is not added.
     * @return the name of the added feature (even if not added to the metadata), or {@code null} if none.
     *
     * @see FeatureCatalogBuilder#define(DefaultFeatureType)
     */
    public final GenericName addFeatureType(final DefaultFeatureType type, final long occurrences) {
        if (type == null) {
            return null;
        }
        final GenericName name = type.getName();
        addFeatureType(name, occurrences);
        return name;
    }

    /**
     * Adds descriptions for a feature of the given name.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/featureTypes/featureTypeName}</li>
     *   <li>{@code metadata/contentInfo/featureTypes/featureInstanceCount}</li>
     * </ul>
     *
     * @param  name         name of the feature type to add, or {@code null} for no-operation.
     * @param  occurrences  number of instances of the given feature type, or a negative value if unknown.
     *         Note that ISO-19115 considers 0 as an invalid value. Consequently, if 0, the feature is not added.
     */
    public final void addFeatureType(final GenericName name, final long occurrences) {
        if (name != null && occurrences != 0) {
            final var info = new DefaultFeatureTypeInfo(name);
            if (occurrences > 0) {
                info.setFeatureInstanceCount(shared((int) Math.min(occurrences, Integer.MAX_VALUE)));
            }
            addIfNotPresent(featureDescription().getFeatureTypeInfo(), info);
        }
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
            // No need to use `addIfNotPresent(…)` for code lists.
            identification().getSpatialRepresentationTypes().add(type);
        }
    }

    /**
     * Adds and populates a "spatial representation info" node using the given grid geometry.
     * This method invokes implicitly {@link #newGridRepresentation(GridType)}, unless this
     * method returns {@code false} in which case nothing has been done.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/spatialRepresentationInfo/transformationDimensionDescription}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/transformationParameterAvailability}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionName}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize}</li>
     *   <li>{@code metadata/spatialRepresentationInfo/axisDimensionProperties/resolution}</li>
     *   <li>{@code metadata/identificationInfo/spatialResolution/distance}</li>
     *   <li>{@code metadata/identificationInfo/temporalResolution}</li>
     *   <li>{@code metadata/identificationInfo/spatialRepresentationType}</li>
     *   <li>{@code metadata/referenceSystemInfo}</li>
     * </ul>
     *
     * This method does not add the envelope provided by {@link GridGeometry#getEnvelope()}.
     * That envelope appears in a separated node, which can be added by {@link #addExtent(Envelope, StoreListeners)}.
     * This separation is required by {@link AbstractGridCoverageResource} for instance.
     *
     * @param  description    a general description of the "grid to CRS" transformation, or {@code null} if none.
     *                        Can also be specified later by a call to {@link #setGridToCRS(CharSequence)}.
     * @param  grid           the grid extent, "grid to CRS" transform and target CRS, or {@code null} if none.
     * @param  addResolution  whether to declare the resolutions. Callers should set this argument to {@code false} if they intend
     *                        to provide the resolution themselves, or if grid axes are not in the same order as CRS axes.
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
                final GridExtent gex = grid.getExtent();
                final int dimension = gex.getDimension();
                for (int i=0; i<dimension; i++) {
                    final Optional<DimensionNameType> axisType = gex.getAxisType(i);
                    if (axisType.isPresent()) {
                        setAxisName(i, axisType.get());
                    }
                    setAxisSize(i, gex.getSize(i));
                }
            }
            if (addResolution && grid.isDefined(GridGeometry.RESOLUTION)) {
                final double[] resolution = grid.getResolution(false);
                for (int i=0; i<resolution.length; i++) {
                    setAxisResolution(i, resolution[i], (cs != null) ? cs.getAxis(i).getUnit() : null);
                }
                addSpatioTemporalResolution(resolution, cs);
            }
        }
        return true;
    }

    /**
     * Adds linear and temporal resolutions computed from an array of resolutions for each CRS axis.
     * This method tries to separate the horizontal, vertical and temporal components.
     * The horizontal components can be linear or angular.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/spatialResolution/distance}</li>
     *   <li>{@code metadata/identificationInfo/temporalResolution}</li>
     * </ul>
     *
     * @param  resolution  the resolution for each coordinate system axis, or {@code null} if unknown.
     * @param  cs          the coordinate system, or {@code null} if unknown.
     */
    public final void addSpatioTemporalResolution(final double[] resolution, final CoordinateSystem cs) {
        if (resolution != null && cs != null) try {
            final int dimension = Math.min(resolution.length, cs.getDimension());
            for (int i=0; i<dimension; i++) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                final Unit<?> unit = axis.getUnit();
                final Unit<?> targetUnit;
                final BiConsumer<DefaultResolution,Double> setter;
                if (Units.isLinear(unit)) {
                    targetUnit = Units.METRE;
                    if (AxisDirections.isVertical(axis.getDirection())) {
                        setter = DefaultResolution::setVertical;
                    } else {
                        setter = DefaultResolution::setDistance;
                    }
                } else if (Units.isAngular(unit)) {
                    targetUnit = Units.DEGREE;
                    setter = DefaultResolution::setAngularDistance;
                } else if (Units.isTemporal(unit) && AxisDirections.isTemporal(axis.getDirection())) {
                    targetUnit = Units.DAY;
                    setter = null;
                } else {
                    continue;
                }
                final double distance = unit.getConverterToAny(targetUnit).convert(resolution[i]);
                if (setter == null) {
                    addTemporalResolution(distance);
                } else if (Double.isFinite(distance)) {
                    var r = new DefaultResolution();
                    setter.accept(r, shared(distance));
                    addIfNotPresent(identification().getSpatialResolutions(), r);
                }
            }
        } catch (IncommensurableException e) {
            // Should never happen because we verified that the unit was linear or temporal.
            Logging.unexpectedException(StoreUtilities.LOGGER, MetadataBuilder.class, "addSpatioTemporalResolution", e);
        }
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
    public final void addLinearResolution(final double distance) {
        if (Double.isFinite(distance)) {
            final var r = new DefaultResolution();
            r.setDistance(shared(distance));
            addIfNotPresent(identification().getSpatialResolutions(), r);
        }
    }

    /**
     * Adds a temporal resolution in days.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/temporalResolution}</li>
     * </ul>
     *
     * @param  duration  the resolution in days, or {@code NaN} for no-operation.
     */
    public final void addTemporalResolution(final double duration) {
        if (Double.isFinite(duration)) {
            addIfNotPresent(identification().getTemporalResolutions(),
                    Duration.ofNanos(Math.round(duration * Constants.NANOSECONDS_PER_DAY)));
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            if (gridRepresentation instanceof DefaultGeorectified) {
                ((DefaultGeorectified) gridRepresentation).setPointInPixel(value);
            }
        }
    }

    /**
     * Sets whether parameters for transformation, control/check point(s) or orientation parameters are available.
     * Storage locations are:
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            if (gridRepresentation instanceof DefaultGeoreferenceable) {
                addIfNotPresent(((DefaultGeoreferenceable) gridRepresentation).getGeolocationInformation(), info);
            }
        }
    }

    /**
     * Adds <i>check points</i> (if georectified) or <i>ground control points</i> (if georeferenceable).
     * Ground control points (GCP) are large marked targets on the ground. GCP should not be used for storing the
     * localization grid (e.g. "model tie points" in a GeoTIFF file).
     * Storage locations are:
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultGridSpatialRepresentation gridRepresentation = gridRepresentation();
            final Collection<GCP> points;
            if (gridRepresentation instanceof DefaultGeorectified) {
                points = ((DefaultGeorectified) gridRepresentation).getCheckPoints();
            } else if (gridRepresentation instanceof DefaultGeoreferenceable) {
                points = groundControlPoints().getGCPs();
            } else {
                return;
            }
            final var gcp = new DefaultGCP();
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
            final DefaultGridSpatialRepresentation r = gridRepresentation();
            if (r instanceof DefaultGeorectified) {
                ((DefaultGeorectified) r).setTransformationDimensionDescription(i18n);
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
    public final void setAxisSize(final int dimension, final long length) {
        if (length >= 0) {
            axis(dimension).setDimensionSize(shared(length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length));
        }
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
     * Sets the sequence identifier, sample value ranges, transfer function and units of measurement
     * from the given sample dimension. This method dispatch its work to other methods in this class.
     * Before to set any value, this method starts a new band by calling {@link #newSampleDimension()}.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/sequenceIdentifier}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/minValue}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/maxValue}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/scale}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/offset}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/transferFunctionType}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/unit}</li>
     * </ul>
     *
     * @param  band  the sample dimension to describe in metadata, or {@code null} if none.
     */
    public final void addNewBand(final SampleDimension band) {
        if (band != null) {
            newSampleDimension();
            setBandIdentifier(band.getName());
            // Really `getMeasurementRange()`, not `getSampleRange()`.
            band.getMeasurementRange().ifPresent((range) -> {
                addMinimumSampleValue(range.getMinDouble());
                addMaximumSampleValue(range.getMaxDouble());
            });
            band.getTransferFunctionFormula().ifPresent((tr) -> {
                setTransferFunction(tr.getScale(), tr.getOffset());
                sampleDimension().setTransferFunctionType(tr.getType());
            });
            band.getUnits().ifPresent((unit) -> setSampleUnits(unit));
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
    public final void setBandIdentifier(final GenericName sequenceIdentifier) {
        if (sequenceIdentifier != null) {
            final MemberName name;
            if (sequenceIdentifier instanceof MemberName) {
                name = (MemberName) sequenceIdentifier;
            } else {
                name = Names.createMemberName(null, null, sequenceIdentifier.tip().toString(), Integer.class);
            }
            sampleDimension().setSequenceIdentifier(name);
        }
    }

    /**
     * Sets the number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     * This is a convenience method for {@link #setBandIdentifier(MemberName)} when the band is specified only
     * by a number. Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/sequenceIdentifier}</li>
     * </ul>
     *
     * @param  sequenceIdentifier  the band number, or 0 or negative if none.
     */
    public final void setBandIdentifier(final int sequenceIdentifier) {
        if (sequenceIdentifier > 0) {
            sampleDimension().setSequenceIdentifier(Names.createMemberName(null, null, sequenceIdentifier));
        }
    }

    /**
     * Adds an identifier for the current band.
     * These identifiers can be used to provide names for the attribute from a standard set of names.
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
        name = Strings.trimOrNull(name);
        if (name != null) {
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultSampleDimension sampleDimension = sampleDimension();
            sampleDimension.setDescription(append(sampleDimension.getDescription(), i18n));
        }
    }

    /**
     * Adds a description of a particular sample value.
     * ISO 19115 range elements are approximately equivalent to
     * {@code org.apache.sis.coverage.Category} in the {@code sis-coverage} module.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/rangeElementDescription}</li>
     * </ul>
     *
     * @param  name        designation associated with a set of range elements, or {@code null} if none.
     * @param  definition  description of a set of specific range elements, or {@code null} if none.
     */
    public void addSampleValueDescription(final CharSequence name, final CharSequence definition) {
        final InternationalString i18n = trim(name);
        final InternationalString def  = trim(definition);
        if (i18n != null && def != null) {
            final var element = new DefaultRangeElementDescription();
            element.setName(i18n);
            element.setDefinition(def);
            addIfNotPresent(coverageDescription().getRangeElementDescriptions(), element);
        }
    }

    /**
     * Adds a minimal value for the current sample dimension. The value should be in the unit of measurement
     * specified by {@link #setSampleUnits(Unit)}. If a minimal value was already defined, then the new value
     * will be set only if it is smaller than the existing one. {@code NaN} values are ignored.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/minValue}</li>
     * </ul>
     *
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     *
     * @param value  the minimal value to add to the existing range of sample values, or {@code NaN} for no-operation.
     */
    public final void addMinimumSampleValue(final double value) {
        if (!Double.isNaN(value)) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultSampleDimension sampleDimension = sampleDimension();
            final Double current = sampleDimension.getMinValue();
            if (current == null || value < current) {
                sampleDimension.setMinValue(shared(value));
            }
        }
    }

    /**
     * Adds a maximal value for the current sample dimension. The value should be in the unit of measurement
     * specified by {@link #setSampleUnits(Unit)}. If a maximal value was already defined, then the new value
     * will be set only if it is greater than the existing one. {@code NaN} values are ignored.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/maxValue}</li>
     * </ul>
     *
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
     *
     * @param value  the maximal value to add to the existing range of sample values, or {@code NaN} for no-operation.
     */
    public final void addMaximumSampleValue(final double value) {
        if (!Double.isNaN(value)) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultSampleDimension sampleDimension = sampleDimension();
            final Double current = sampleDimension.getMaxValue();
            if (current == null || value > current) {
                sampleDimension.setMaxValue(shared(value));
            }
        }
    }

    /**
     * Returns {@code true} if current band has the minimum or maximum value defined.
     *
     * @return whether minimum or maximum value is defined for current band.
     */
    public final boolean hasSampleValueRange() {
        return (sampleDimension != null)
                && (sampleDimension.getMinValue() != null || sampleDimension.getMaxValue() != null);
    }

    /**
     * Sets the units of data in the current band.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/unit}</li>
     * </ul>
     *
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
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
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/scale}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/offset}</li>
     *   <li>{@code metadata/contentInfo/attributeGroup/attribute/transferFunctionType}</li>
     * </ul>
     *
     * If a coverage contains more than one band, additional bands can be created by calling
     * {@link #newSampleDimension()} before to call this method.
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
        processingLevel = Strings.trimOrNull(processingLevel);
        if (processingLevel != null) {
            coverageDescription().setProcessingLevelCode(sharedIdentifier(authority, processingLevel));
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
        identifier = Strings.trimOrNull(identifier);
        if (identifier != null) {
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
        identifier = Strings.trimOrNull(identifier);
        if (identifier != null) {
            final var instrument = new DefaultInstrument();
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
     * @see #addTemporalExtent(Temporal, Temporal)
     */
    public final void addAcquisitionTime(final Temporal time) {
        if (time != null) {
            final var event = new DefaultEvent();
            event.setContext(Context.ACQUISITION);
            event.setDateOfOccurrence(time);
            final var op = new DefaultOperation();
            op.setSignificantEvents(Collections.singleton(event));
            op.setType(OperationType.REAL);
            op.setStatus(Progress.COMPLETED);
            addIfNotPresent(acquisition().getOperations(), op);
        }
    }

    /**
     * Adds an event that describe the range of time at which data were acquired.
     * Current implementation computes the average of given instants.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/acquisitionInformation/operation/significantEvent/time}</li>
     * </ul>
     *
     * @param  startTime  start time, or {@code null} if unknown.
     * @param  endTime    end time, or {@code null} if unknown.
     */
    public final void addAcquisitionTime(final Instant startTime, final Instant endTime) {
        final Temporal time;
        if (startTime == null) {
            if (endTime == null) return;
            time = endTime;
        } else if (endTime == null) {
            time = startTime;
        } else {
            // Divide by 2 before to add in order to avoid overflow.
            time = Instant.ofEpochMilli((startTime.toEpochMilli() >> 1) + (endTime.toEpochMilli() >> 1));
        }
        addAcquisitionTime(time);
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
        identifier = Strings.trimOrNull(identifier);
        if (identifier != null) {
            final var r = new DefaultOperation();
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
        identifier = Strings.trimOrNull(identifier);
        if (identifier != null) {
            final var r = new DefaultRequirement();
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
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultLineage lineage = lineage();
            lineage.setStatement(append(lineage.getStatement(), i18n));
        }
    }

    /**
     * Adds a source described by the given metadata.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/source/description}</li>
     *   <li>{@code metadata/resourceLineage/source/citation}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/level}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/extent}</li>
     *   <li>{@code metadata/resourceLineage/source/sourceReferenceSystem}</li>
     *   <li>{@code metadata/resourceLineage/source/sourceSpatialResolution}</li>
     * </ul>
     *
     * @param  source  metadata about a source of the resource for which to describe the lineage.
     *
     * @see #addLineage(CharSequence)
     * @see #addProcessDescription(CharSequence)
     */
    public final void addSource(final Metadata source) {
        if (source != null) {
            final ResourceLineage r = new ResourceLineage(source);
            if (!r.isEmpty()) {
                addIfNotPresent(lineage().getSources(), r.build());
            }
        }
    }

    /**
     * Adds information about a source of data used for producing the resource.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/source/description}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/level}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/levelDescription/features}</li>
     * </ul>
     *
     * <h4>Example</h4>
     * If a Landsat image uses the "GTOPO30" digital elevation model, then it can declare the source
     * with "GTOPO30" description, {@link ScopeCode#MODEL} and feature "Digital Elevation Model".
     *
     * @param  description  a detailed description of the level of the source data, or {@code null} if none.
     * @param  level        hierarchical level of the source (e.g. model), or {@code null} if unspecified.
     * @param  feature      more detailed name for {@code level}, or {@code null} if none.
     *
     * @see #addSource(Metadata)
     * @see #addProcessing(CharSequence, String)
     * @see #addProcessDescription(CharSequence)
     */
    @SuppressWarnings("deprecation")
    public final void addSource(final CharSequence description, final ScopeCode level, final CharSequence feature) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final var source = new DefaultSource(description);
            if (level != null || feature != null) {
                final var scope = new DefaultScope(level);
                if (feature != null) {
                    final var sd = new DefaultScopeDescription();
                    sd.getFeatures().add(new org.apache.sis.metadata.iso.maintenance.LegacyFeatureType(feature));
                    scope.getLevelDescription().add(sd);
                }
            }
            addIfNotPresent(lineage().getSources(), source);
        }
    }

    /**
     * Adds information about a source of data used for producing the resource.
     * Storage locations are:
     *
     * <ul>
     *   <li>{@code metadata/resourceLineage/source/scope/level}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/extent}</li>
     *   <li>{@code metadata/resourceLineage/source/scope/levelDescription/*}</li>
     *   <li>{@code metadata/resourceLineage/source/citation}</li>
     *   <li>{@code metadata/resourceLineage/source/sourceReferenceSystem}</li>
     *   <li>{@code metadata/resourceLineage/source/sourceSpatialResolution}</li>
     * </ul>
     *
     * <h4>Example</h4>
     * If a {@code FeatureSet} is the aggregation of two other {@code FeatureSet} resources,
     * then this method can be invoked twice with the metadata of each source {@code FeatureSet}.
     * If the aggregated data are features, then {@code level} should be {@link ScopeCode#FEATURE}.
     *
     * @param  metadata  the metadata of the source, or {@code null} if none.
     * @param  level     hierarchical level of the source (e.g. feature). Should not be null.
     * @param  features  names of dataset, features or attributes used in the source.
     *
     * @see #addSource(Metadata)
     */
    public final void addSource(final Metadata metadata, final ScopeCode level, final CharSequence... features) {
        if (metadata != null) {
            final var source = new DefaultSource();
            final var scope  = new DefaultScope(level);
            source.setSourceReferenceSystem(CollectionsExt.first(metadata.getReferenceSystemInfo()));
            for (final Identification id : metadata.getIdentificationInfo()) {
                source.setSourceCitation(id.getCitation());
                if (id instanceof AbstractIdentification) {
                    final AbstractIdentification aid = (AbstractIdentification) id;
                    source.setSourceSpatialResolution(CollectionsExt.first(aid.getSpatialResolutions()));
                    scope.setExtents(aid.getExtents());
                }
                if (features != null && features.length != 0) {
                    /*
                     * Note: the same ScopeDescription may be shared by many Source instances
                     * in the common case where many sources contain features of the same type.
                     */
                    final var sd = new DefaultScopeDescription();
                    sd.setLevelDescription(level, new LinkedHashSet<>(Arrays.asList(features)));
                    scope.getLevelDescription().add(shared(DefaultScopeDescription.class, sd));
                }
                source.setScope(scope.isEmpty() ? null : scope);
                if (!source.isEmpty()) {
                    addIfNotPresent(lineage().getSources(), source);
                    break;
                }
            }
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
        identifier = Strings.trimOrNull(identifier);
        if (identifier != null) {
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
     * Adds a name to the resource format. If no format citation has been created yet,
     * then the given value is used as the format title. Otherwise, the given value is
     * used as an alternative name of the current formaT. Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceFormat/formatSpecificationCitation/alternateTitle}</li>
     * </ul>
     *
     * If this method is used together with {@link #setPredefinedFormat setPredefinedFormat(…)},
     * then the predefined format should be set <strong>before</strong> this method.
     *
     * @param value  the format name, or {@code null} for no-operation.
     *
     * @see #setFormatEdition(CharSequence)
     * @see #addCompression(CharSequence)
     */
    public final void addFormatName(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultFormat format = format();
            DefaultCitation c = DefaultCitation.castOrCopy(format.getFormatSpecificationCitation());
            if (c == null) {
                c = new DefaultCitation(i18n);
            } else {
                addIfNotPresent(c.getAlternateTitles(), i18n);
            }
            format.setFormatSpecificationCitation(c);
        }
    }

    /**
     * Returns the citation of the format as a modifiable object for allowing the caller to set properties.
     */
    private DefaultCitation getFormatCitation() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final DefaultFormat format = format();
        DefaultCitation c = DefaultCitation.castOrCopy(format.getFormatSpecificationCitation());
        if (c == null) {
            c = new DefaultCitation();
        }
        format.setFormatSpecificationCitation(c);   // Unconditional because may replace a proxy.
        return c;
    }

    /**
     * Sets a version number for the resource format. Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceFormat/formatSpecificationCitation/edition}</li>
     * </ul>
     *
     * If this method is used together with {@link #setPredefinedFormat setPredefinedFormat(…)},
     * then the predefined format should be set <strong>before</strong> this method.
     *
     * @param value  the format edition, or {@code null} for no-operation.
     *
     * @see #addFormatName(CharSequence)
     */
    public final void setFormatEdition(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            getFormatCitation().setEdition(i18n);
        }
    }

    /**
     * Adds a note about which reader is used. This method should not be invoked before
     * the {@linkplain #addFormatName format name} has been set. Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/identificationInfo/resourceFormat/formatSpecificationCitation/identifier}</li>
     *   <li>{@code metadata/identificationInfo/resourceFormat/formatSpecificationCitation/otherCitationDetails}</li>
     * </ul>
     *
     * If this method is used together with {@link #setPredefinedFormat setPredefinedFormat(…)},
     * then the predefined format should be set <strong>before</strong> this method.
     *
     * @param driver   library-specific way to identify the format (mandatory).
     * @param version  the library version, or {@code null} if unknown.
     */
    public final void addFormatReader(final ReferenceIdentifier driver, final Version version) {
        CharSequence title = null;
        Citation authority = driver.getAuthority();
        if (authority != null) {
            title = authority.getTitle();
            if (title != null) {
                for (CharSequence t : authority.getAlternateTitles()) {
                    if (t.length() < title.length()) {
                        title = t;      // Alternate titles are often abbreviations.
                    }
                }
            }
        }
        final DefaultCitation c = getFormatCitation();
        c.setOtherCitationDetails(
                Resources.formatInternational(Resources.Keys.ReadBy_2, (title != null) ? title : driver.getCodeSpace(),
                        (version != null) ? version : Vocabulary.formatInternational(Vocabulary.Keys.Unspecified)));
    }

    /**
     * Adds a note saying that Apache <abbr>SIS</abbr> has been used for decoding the format.
     * This method should not be invoked before the {@linkplain #addFormatName format name} has been set.
     *
     * @param  name  the format name, or {@code null} if unspecified.
     */
    public void addFormatReaderSIS(final String name) {
        if (name != null) {
            addFormatReader(new ImmutableIdentifier(Citations.SIS, Constants.SIS, name), Version.SIS);
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
     * If this method is used together with {@link #setPredefinedFormat setPredefinedFormat(…)},
     * then the predefined format should be set <strong>before</strong> this method.
     *
     * @param value  the compression name, or {@code null} for no-operation.
     *
     * @see #addFormatName(CharSequence)
     */
    public final void addCompression(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     *     with {@code function} set to {@code OnLineFunction.COMPLETE_METADATA}</li>
     * </ul>
     *
     * @param  link  URL to a more complete description of the metadata, or {@code null}.
     */
    public final void addCompleteMetadata(final URI link) {
        if (link != null) {
            final var ln = new DefaultOnlineResource(link);
            ln.setFunction(OnLineFunction.valueOf("COMPLETE_METADATA"));
            ln.setProtocol(link.getScheme());
            addIfNotPresent(metadata().getMetadataLinkages(), ln);
        }
    }

    /**
     * Sets the metadata standards to ISO 19115-1, and optionally to ISO 19115-2 too.
     * Those metadata citations are added only if the metadata object is otherwise non-empty.
     * Storage location is:
     *
     * <ul>
     *   <li>{@code metadata/metadataStandards}</li>
     * </ul>
     *
     * @param  part2  whether to set ISO 19115-2 in addition to ISO 19115-1.
     */
    public final void setISOStandards(final boolean part2) {
        standardISO = part2 ? (byte) 2 : (byte) 1;
    }

    /**
     * Appends information from the metadata of a component.
     * This is a helper method for building the metadata of an aggregate.
     * Aggregate metadata should be set before to invoke this method, in particular:
     *
     * <ul>
     *   <li>The aggregated resource {@linkplain #addTitle(CharSequence) title}.</li>
     *   <li>The {@linkplain #addFormatName format} (may not be the same as component format).</li>
     * </ul>
     *
     * This method applies the following heuristic rules (may change in any future version).
     * Those rules assume that the component metadata was built with {@code MetadataBuilder}
     * (this assumption determines which metadata elements are inspected).
     *
     * <ul>
     *   <li>Content information is added verbatim. There is usually one instance per component.</li>
     *   <li>Extents are added as one {@link Extent} per component, but without duplicated values.</li>
     *   <li>All Coordinate Reference System information are added without duplicated values.</li>
     *   <li>Some citation information are merged in a single citation.
     *       The following information are ignored because considered too specific to the component:<ul>
     *         <li>titles (except if no title has been set, in which case the first title is used)</li>
     *         <li>identifiers</li>
     *         <li>series (includes page numbers).</li>
     *       </ul></li>
     *   <li>{@linkplain #addCompression Compression} are added (without duplicated value) but not the
     *       other format information (because the aggregate is assumed to have its own format name).</li>
     *   <li>Distributor names, but not the other distribution information because the aggregated resource
     *       may not be distributed in the same way then the components.</li>
     * </ul>
     *
     * @param  component  the component from which to append metadata.
     */
    public final void addFromComponent(final Metadata component) {
        /*
         * Note: this method contains many loops like below:
         *
         *     for (Foo r : info.getFoos()) {
         *         addIfNotPresent(bla().getFoos(), r);
         *     }
         *
         * We could easily factor out the above pattern in a method, but we don't do that because
         * it would invoke `bla().getFoos()` before the loop. We want that call to happen only if
         * the collection contains at least one element. Usually, there is only 0 or 1 element.
         */
        for (final Identification info : component.getIdentificationInfo()) {
            final Citation c = info.getCitation();
            if (c != null) {
                // Title (except first one), identifiers and series are assumed to not apply (see Javadoc).
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final DefaultCitation citation = citation();
                if (citation.getTitle() == null) {
                    citation.setTitle(c.getTitle());
                }
                for (ResponsibleParty r : c.getCitedResponsibleParties()) {
                    addIfNotPresent(citation.getCitedResponsibleParties(), r);
                }
                citation.getPresentationForms().addAll(c.getPresentationForms());
            }
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final DefaultDataIdentification identification = identification();
            for (Format r : info.getResourceFormats()) {
                addCompression(r.getFileDecompressionTechnique());
                // Ignore format name (see Javadoc).
            }
            for (Constraints r : info.getResourceConstraints()) {
                addIfNotPresent(identification.getResourceConstraints(), r);
            }
            if (info instanceof DataIdentification) {
                final var di = (DataIdentification) info;
                for (Extent e : di.getExtents()) {
                    addIfNotPresent(identification.getExtents(), e);
                }
                for (Resolution r : di.getSpatialResolutions()) {
                    addIfNotPresent(identification.getSpatialResolutions(), r);
                }
                identification.getTopicCategories().addAll(di.getTopicCategories());
                identification.getSpatialRepresentationTypes().addAll(di.getSpatialRepresentationTypes());
            }
            if (info instanceof AbstractIdentification) {
                final var di = (AbstractIdentification) info;
                for (TemporalAmount r : di.getTemporalResolutions()) {
                    addIfNotPresent(identification.getTemporalResolutions(), r);
                }
            }
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final DefaultMetadata metadata = metadata();
        for (ContentInformation info : component.getContentInfo()) {
            addIfNotPresent(metadata.getContentInfo(), info);
        }
        for (final ReferenceSystem crs : component.getReferenceSystemInfo()) {
            addReferenceSystem(crs);
        }
        for (SpatialRepresentation info : component.getSpatialRepresentationInfo()) {
            addIfNotPresent(metadata.getSpatialRepresentationInfo(), info);
        }
        for (AcquisitionInformation info : component.getAcquisitionInformation()) {
            addIfNotPresent(metadata.getAcquisitionInformation(), info);
        }
        Distribution di = component.getDistributionInfo();
        if (di != null) {
            // See Javadoc about why we copy only the distributors.
            for (Distributor r : di.getDistributors()) {
                addIfNotPresent(distribution().getDistributors(), r);
            }
        }
    }

    /**
     * Merges the given metadata into the metadata created by this builder.
     * The given source should be an instance of {@link Metadata},
     * but some types of metadata components are accepted as well.
     *
     * <p>This method should be invoked last, just before the call to {@link #build()}.
     * Any identification information, responsible party, extent, coverage description, <i>etc.</i>
     * added after this method call will be stored in new metadata object (not merged).</p>
     *
     * @param  source  the source metadata to merge. Will never be modified.
     * @param  locale  the locale to use for error message in exceptions, or {@code null} for the default locale.
     * @return {@code true} if the given source has been merged,
     *         or {@code false} if its type is not managed by this builder.
     * @throws RuntimeException if the merge failed (may be {@link IllegalArgumentException},
     *         {@link ClassCastException}, {@link org.apache.sis.metadata.InvalidMetadataException}…)
     *
     * @see Merger
     */
    public boolean mergeMetadata(final Object source, final Locale locale) {
        flush();
        final ModifiableMetadata target;
        /*
         * In the following `instanceof` checks, objects closer to root should be tested first.
         * For example, we should finish the checks of all `Metadata` elements before to check
         * if the object is a sub-element of a `Metadata` element. This ordering is because an
         * implementation may implement many interfaces: the main element together with some of
         * its sub-elements. We want to use the object with most information. Furthermore, the
         * main object may not use a type (e.g. `Citation`) for the same sub-element than what
         * the code below assumes.
         */
             if (source instanceof Metadata)                    target = metadata();
        else if (source instanceof DataIdentification)          target = identification();
        else if (source instanceof GridSpatialRepresentation)   target = gridRepresentation();
        else if (source instanceof CoverageDescription)         target = coverageDescription();
        else if (source instanceof FeatureCatalogueDescription) target = featureDescription();
        else if (source instanceof AcquisitionInformation)      target = acquisition();
        else if (source instanceof Lineage)                     target = lineage();
        else if (source instanceof Distribution)                target = distribution();
        else if (source instanceof Citation)                    target = citation();
        else if (source instanceof Extent)                      target = extent();
        else if (source instanceof LegalConstraints)            target = constraints();
        else if (source instanceof Series)                      target = series();
        else if (source instanceof DefaultResponsibleParty)     target = responsibility();
        else if (source instanceof AbstractParty)               target = party();
        else if (source instanceof DefaultAttributeGroup)       target = attributeGroup();
        else if (source instanceof SampleDimension)             target = sampleDimension();
        else if (source instanceof GCPCollection)               target = groundControlPoints();
        else if (source instanceof Format)                      target = format();
        else if (source instanceof Platform)                    target = platform();
        else if (source instanceof ProcessStep)                 target = processStep();
        else if (source instanceof Processing)                  target = processing();
        else if (source instanceof ReferenceSystem) {
            addReferenceSystem((ReferenceSystem) source);
            return true;
        } else {
            return false;
        }
        final Merger merger = new Merger(locale);
        merger.copy(source, target);
        useParentElements();
        return true;
    }

    /**
     * Replaces any null metadata element by the last element from the parent.
     * This is used for continuing the edition of an existing metadata.
     */
    private void useParentElements() {
        if (identification      == null) identification      = last (DefaultDataIdentification.class,          metadata,            DefaultMetadata::getIdentificationInfo);
        if (gridRepresentation  == null) gridRepresentation  = last (DefaultGridSpatialRepresentation.class,   metadata,            DefaultMetadata::getSpatialRepresentationInfo);
        if (coverageDescription == null) coverageDescription = last (DefaultCoverageDescription.class,         metadata,            DefaultMetadata::getContentInfo);
        if (featureDescription  == null) featureDescription  = last (DefaultFeatureCatalogueDescription.class, metadata,            DefaultMetadata::getContentInfo);
        if (acquisition         == null) acquisition         = last (DefaultAcquisitionInformation.class,      metadata,            DefaultMetadata::getAcquisitionInformation);
        if (lineage             == null) lineage             = last (DefaultLineage.class,                     metadata,            DefaultMetadata::getResourceLineages);
        if (distribution        == null) distribution        = fetch(DefaultDistribution.class,                metadata,            DefaultMetadata::getDistributionInfo);
        if (citation            == null) citation            = fetch(DefaultCitation.class,                    identification,      AbstractIdentification::getCitation);
        if (extent              == null) extent              = last (DefaultExtent.class,                      identification,      AbstractIdentification::getExtents);
        if (constraints         == null) constraints         = last (DefaultLegalConstraints.class,            identification,      AbstractIdentification::getResourceConstraints);
        if (responsibility      == null) responsibility      = last (DefaultResponsibleParty.class,            citation,            DefaultCitation::getCitedResponsibleParties);
        if (party               == null) party               = last (AbstractParty.class,                      responsibility,      DefaultResponsibility::getParties);
        if (attributeGroup      == null) attributeGroup      = last (DefaultAttributeGroup.class,              coverageDescription, DefaultCoverageDescription::getAttributeGroups);
        if (sampleDimension     == null) sampleDimension     = last (DefaultSampleDimension.class,             attributeGroup,      DefaultAttributeGroup::getAttributes);
        if (format              == null) format              = last (DefaultFormat.class,                      distribution,        DefaultDistribution::getDistributionFormats);
        if (platform            == null) platform            = last (DefaultPlatform.class,                    acquisition,         DefaultAcquisitionInformation::getPlatforms);
        if (processStep         == null) processStep         = last (DefaultProcessStep.class,                 lineage,             DefaultLineage::getProcessSteps);
        if (processing          == null) processing          = fetch(DefaultProcessing.class,                  processStep,         DefaultProcessStep::getProcessingInformation);
    }

    /**
     * Returns the element of the given source metadata if it is of the desired class.
     * This method is equivalent to {@link #last(Class, Object, Function)} but for a singleton.
     *
     * @param  <S>     the type of the source metadata.
     * @param  <E>     the type of metadata element provided by the source.
     * @param  <T>     the type of the desired metadata element.
     * @param  target  the type of the desired metadata element.
     * @param  source  the source metadata, or {@code null} if none.
     * @param  getter  the getter to use for fetching elements from the source metadata.
     * @return the metadata element from the source, or {@code null} if none.
     */
    private static <S extends ISOMetadata, E, T extends E> T fetch(final Class<T> target, final S source,
            final Function<S,E> getter)
    {
        if (source != null) {
            final E last = getter.apply(source);
            if (target.isInstance(last)) {
                return target.cast(last);
            }
        }
        return null;
    }

    /**
     * Returns the element of the given source metadata if it is of the desired class.
     * This method is equivalent to {@link #fetch(Class, Object, Function)} but for a collection.
     *
     * @param  <S>     the type of the source metadata.
     * @param  <E>     the type of metadata element provided by the source.
     * @param  <T>     the type of the desired metadata element.
     * @param  target  the type of the desired metadata element.
     * @param  source  the source metadata, or {@code null} if none.
     * @param  getter  the getter to use for fetching elements from the source metadata.
     * @return the metadata element from the source, or {@code null} if none.
     */
    private static <S extends ISOMetadata, E, T extends E> T last(final Class<T> target, final S source,
            final Function<S,Collection<E>> getter)
    {
        if (source != null) {
            // If not a sequenced collection, the iteration may be in any order.
            for (final E last : JDK21.reversed(getter.apply(source))) {
                if (target.isInstance(last)) {
                    return target.cast(last);
                }
            }
        }
        return null;
    }

    /**
     * Writes all pending metadata objects into the {@link DefaultMetadata} root class.
     * Then all {@link #identification}, {@link #gridRepresentation}, <i>etc.</i> fields
     * except {@link #metadata} are set to {@code null}.
     */
    private void flush() {
        newIdentification();
        newGridRepresentation(GridType.UNSPECIFIED);
        newFeatureTypes();
        newCoverage(false);
        newAcquisition();
        newDistribution();
        newLineage();
    }

    /**
     * Returns the metadata as a modifiable object.
     *
     * @return the metadata (never {@code null}).
     */
    public final DefaultMetadata build() {
        flush();
        final DefaultMetadata md = metadata();
        if (standardISO != 0) {
            List<Citation> c = Citations.ISO_19115;
            if (standardISO == 1) {
                c = Collections.singletonList(c.get(0));
            }
            md.setMetadataStandards(c);
        }
        return md;
    }

    /**
     * Returns the metadata as an unmodifiable object.
     *
     * @return the metadata (never {@code null}).
     */
    public final DefaultMetadata buildAndFreeze() {
        final DefaultMetadata md = build();
        md.transitionTo(DefaultMetadata.State.FINAL);
        return md;
    }

    /**
     * Returns a shared instance of the given object if it already exists.
     * If the given object is new, then it is added to the cache and returned.
     *
     * <p>It is caller's responsibility to ensure that the type given in argument
     * does not conflict with one of the type documented in {@link #sharedValues}.</p>
     */
    private <T> T shared(final Class<T> type, final T value) {
        final T existing = type.cast(sharedValues.putIfAbsent(value, value));
        return (existing != null) ? existing : value;
    }

    /**
     * Returns a shared instance of the given value.
     * This is a helper method for callers who want to set themselves some additional
     * metadata values on the instance returned by {@link #build()}.
     *
     * @param   value  a double value.
     * @return  the given value, but as an existing instance if possible.
     */
    protected final Double shared(final double value) {
        final Double n = value;
        final Object existing = sharedValues.putIfAbsent(n, n);
        return (existing != null) ? (Double) existing : n;
    }

    /**
     * Returns a shared instance of the given value.
     * This is a helper method for callers who want to set themselves some additional
     * metadata values on the instance returned by {@link #build()}.
     *
     * @param   value  an integer value.
     * @return  the same value, but as an existing instance if possible.
     */
    protected final Integer shared(final int value) {
        final Integer n = value;
        final Object existing = sharedValues.putIfAbsent(n, n);
        return (existing != null) ? (Integer) existing : n;
    }
}
