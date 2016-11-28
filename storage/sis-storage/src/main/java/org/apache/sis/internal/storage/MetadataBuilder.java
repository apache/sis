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

import java.util.Date;
import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.Charset;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.acquisition.Context;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.distribution.Format;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.spatial.DefaultDimension;
import org.apache.sis.metadata.iso.spatial.DefaultGeorectified;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultSampleDimension;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription;
import org.apache.sis.metadata.iso.content.DefaultImageDescription;
import org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo;
import org.apache.sis.metadata.iso.citation.AbstractParty;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.acquisition.DefaultEvent;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultOperation;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultRequirement;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.Types;

import static java.util.Collections.singleton;
import static org.apache.sis.internal.util.StandardDateFormat.MILLISECONDS_PER_DAY;

// Branch-dependent imports
import java.time.LocalDate;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.lineage.DefaultProcessStep;
import org.apache.sis.metadata.iso.lineage.DefaultProcessing;
import org.opengis.feature.FeatureType;


/**
 * Helper methods for the metadata created by {@code DataStore} implementations.
 * This is not yet a general-purpose builder suitable for public API, since the
 * methods provided in this class are tailored for Apache SIS data store needs.
 * API of this class may change in any future SIS versions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Marechal (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class MetadataBuilder {
    /**
     * Instructs {@link #newParty(byte)} that the next party to create should be an instance of
     * {@link DefaultIndividual}.
     *
     * @see #partyType
     * @see #newParty(byte)
     */
    public static final byte INDIVIDUAL = 1;

    /**
     * Instructs {@link #newParty(byte)} that the next party to create should be an instance of
     * {@link DefaultOrganisation}.
     *
     * @see #partyType
     * @see #newParty(byte)
     */
    public static final byte ORGANISATION = 2;

    /**
     * The metadata created by this reader, or {@code null} if none.
     */
    private DefaultMetadata metadata;

    /**
     * The identification information that are part of {@linkplain #metadata}, or {@code null} if none.
     */
    private DefaultDataIdentification identification;

    /**
     * The citation of data {@linkplain #identification}, or {@code null} if none.
     */
    private DefaultCitation citation;

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private DefaultResponsibility responsibility;

    /**
     * Part of the responsible party of the {@linkplain #citation}, or {@code null} if none.
     */
    private AbstractParty party;

    /**
     * Copyright information, or {@code null} if none.
     */
    private DefaultLegalConstraints constraints;

    /**
     * The extent information that are part of {@linkplain #identification}, or {@code null} if none.
     */
    private DefaultExtent extent;

    /**
     * Information about the platforms and sensors that collected the data, or {@code null} if none.
     */
    private DefaultAcquisitionInformation acquisition;

    /**
     * Platform where are installed the sensors that collected the data, or {@code null} if none.
     */
    private DefaultPlatform platform;

    /**
     * Information about the grid shape, or {@code null} if none.
     */
    private DefaultGeorectified gridRepresentation;

    /**
     * Information about the content of a grid data cell, or {@code null} if none.
     * May also be an instance of {@link DefaultImageDescription} if {@link #electromagnetic} is {@code true}.
     */
    private DefaultCoverageDescription coverageDescription;

    /**
     * Information about the feature types, or {@code null} if none.
     */
    private DefaultFeatureCatalogueDescription featureDescription;

    /**
     * Information about content type for groups of attributes for a specific range dimension, or {@code null} if none.
     */
    private DefaultAttributeGroup attributeGroup;

    /**
     * The characteristic of each dimension (layer) included in the resource, or {@code null} if none.
     */
    private DefaultSampleDimension sampleDimension;

    /**
     * The distribution format, or {@code null} if none.
     * This is part of the resource {@linkplain #identification}.
     */
    private Format format;

    /**
     * Information about the events or source data used in constructing the data specified by the
     * {@linkplain DefaultLineage#getScope() scope}.
     */
    private DefaultLineage lineage;

    /**
     * Information about an event or transformation in the life of a resource.
     * This is part of {@link #lineage}.
     */
    private DefaultProcessStep processStep;

    /**
     * Information about the procedures, processes and algorithms applied in the process step.
     * This is part of {@link #processStep}.
     */
    private DefaultProcessing processing;

    /**
     * Whether the next party to create should be an instance of {@link DefaultIndividual} or {@link DefaultOrganisation}.
     * Value can be {@link #INDIVIDUAL}, {@link #ORGANISATION} or 0 if unknown, in which case an {@link AbstractParty}
     * will be created.
     *
     * @see #INDIVIDUAL
     * @see #ORGANISATION
     * @see #newParty(byte)
     */
    private byte partyType;

    /**
     * {@code true} if the next {@code CoverageDescription} to create will be a description of measurements
     * in the electromagnetic spectrum. In that case, the coverage description will actually be an instance
     * of {@code ImageDescription}.
     */
    private boolean electromagnetic;

    /**
     * For using the same instance of {@code Double} when the value is the same.
     * We use this map because the same values appear many time in a Landsat file.
     *
     * @see #parseDouble(String)
     */
    private final Map<Number,Number> sharedNumbers = new HashMap<>();

    /**
     * Creates a new metadata reader.
     */
    public MetadataBuilder() {
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
     * Commits all pending information under the "responsible party" node (author, address, <i>etc</i>).
     * If there is no pending party information, then invoking this method has no effect
     * except setting the {@code type} flag.
     * If new party information are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "responsible party" node,
     * separated from the previous one, is desired.</p>
     *
     * @param  type  {@link #INDIVIDUAL}, {@link #ORGANISATION} or 0 if unknown.
     */
    public final void newParty(final byte type) {
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
        newParty((byte) 0);
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
     * Commits all pending information under the metadata "spatial representation" node (dimensions, <i>etc</i>).
     * If there is no pending spatial representation information, then invoking this method has no effect.
     * If new spatial representation info are added after this method call, they will be stored in a new element.
     *
     * <p>This method does not need to be invoked unless a new "spatial representation info" node,
     * separated from the previous one, is desired.</p>
     */
    public final void newGridRepresentation() {
        if (gridRepresentation != null) {
            final int n = gridRepresentation.getAxisDimensionProperties().size();
            if (n != 0) {
                gridRepresentation.setNumberOfDimensions(shared(n));
            }
            addIfNotPresent(metadata.getSpatialRepresentationInfo(), gridRepresentation);
            gridRepresentation = null;
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
        if (sampleDimension != null) {
            addIfNotPresent(attributGroup().getAttributes(), sampleDimension);
            sampleDimension = null;
        }
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
     * Creates the metadata object if it does not already exists, then returns it.
     *
     * @return the metadata (never {@code null}).
     */
    private DefaultMetadata metadata() {
        if (metadata == null) {
            metadata = new DefaultMetadata();
        }
        return metadata;
    }

    /**
     * Creates the identification information object if it does not already exists, then returns it.
     *
     * @return the identification information (never {@code null}).
     */
    private DefaultDataIdentification identification() {
        if (identification == null) {
            identification = new DefaultDataIdentification();
        }
        return identification;
    }

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
     * Creates the individual or organization information object if it does not already exists, then returns it.
     *
     * <p><b>Limitation:</b> if the party type is unknown, then this method creates an {@code AbstractParty} instead
     * than one of the subtypes. This is not valid, but we currently have no way to guess if a party is an individual
     * or an organization. For now we prefer to let users know that the type is unknown rather than to pick a
     * potentially wrong type.</p>
     *
     * @return the individual or organization information (never {@code null}).
     */
    private AbstractParty party() {
        if (party == null) {
            switch (partyType) {
                case INDIVIDUAL:   party = new DefaultIndividual();   break;
                case ORGANISATION: party = new DefaultOrganisation(); break;
                default:           party = new AbstractParty();       break;
            }
        }
        return party;
    }

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
     * Creates the acquisition information object if it does not already exists, then returns it.
     *
     * @return the acquisition information (never {@code null}).
     */
    private DefaultAcquisitionInformation acquisition() {
        if (acquisition == null) {
            acquisition = new DefaultAcquisitionInformation();
        }
        return acquisition;
    }

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
     * Creates a grid representation object if it does not already exists, then returns it.
     *
     * @return the grid representation object (never {@code null}).
     */
    private DefaultGeorectified gridRepresentation() {
        if (gridRepresentation == null) {
            gridRepresentation = new DefaultGeorectified();
        }
        return gridRepresentation;
    }

    /**
     * Creates the sample dimension object if it does not already exists, then returns it.
     *
     * @return the sample dimension (never {@code null}).
     */
    private DefaultSampleDimension sampleDimension() {
        if (sampleDimension == null) {
            sampleDimension = new DefaultSampleDimension();
        }
        return sampleDimension;
    }

    /**
     * Creates the attribut group object if it does not already exists, then returns it.
     *
     * @return the attribut group (never {@code null}).
     */
    private DefaultAttributeGroup attributGroup() {
        if (attributeGroup == null) {
            attributeGroup = new DefaultAttributeGroup();
        }
        return attributeGroup;
    }

    /**
     * Creates the coverage description object if it does not already exists, then returns it.
     *
     * @return the coverage description (never {@code null}).
     */
    private DefaultCoverageDescription coverageDescription() {
        if (coverageDescription == null) {
            coverageDescription = electromagnetic ? new DefaultImageDescription() : new DefaultCoverageDescription();
        }
        return coverageDescription;
    }

    /**
     * Creates the feature descriptions object if it does not already exists, then returns it.
     * This method sets the {@code includedWithDataset} property to {@code true} because the
     * metadata built by this helper class are typically encoded together with the data.
     *
     * @return the feature descriptions (never {@code null}).
     */
    private DefaultFeatureCatalogueDescription featureDescription() {
        if (featureDescription == null) {
            featureDescription = new DefaultFeatureCatalogueDescription();
            featureDescription.setIncludedWithDataset(true);
        }
        return featureDescription;
    }

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
     * Creates the lineage object if it does not already exists, then returns it.
     *
     * @return the lineage (never {@code null}).
     */
    private DefaultLineage lineage() {
        if (lineage == null) {
            lineage = new DefaultLineage();
        }
        return lineage;
    }

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
     * Adds a language used for documenting metadata.
     * Storage location is:
     *
     * <pre>metadata/language</pre>
     *
     * @param  language  a language used for documenting metadata.
     */
    public final void add(final Locale language) {
        if (language != null) {
            // No need to use 'addIfNotPresent(…)' because Locale collection is a Set by default.
            metadata().getLanguages().add(language);
        }
    }

    /**
     * Adds the given character encoding to the metadata.
     * Storage location is:
     *
     * <pre>metadata/characterSet</pre>
     *
     * @param  encoding  the character encoding to add.
     */
    public final void add(final Charset encoding) {
        if (encoding != null) {
            // No need to use 'addIfNotPresent(…)' because Charset collection is a Set by default.
            metadata().getCharacterSets().add(encoding);
        }
    }

    /**
     * Adds information about the scope of the resource.
     * The scope is typically {@link ScopeCode#DATASET}.
     * Storage location is:
     *
     * <pre>metadata/metadataScope/resourceScope</pre>
     *
     * @param  scope  the scope of the resource, or {@code null} if none.
     */
    public final void add(final ScopeCode scope) {
        if (scope != null) {
            addIfNotPresent(metadata().getMetadataScopes(), new DefaultMetadataScope(scope, null));
        }
    }

    /**
     * Adds descriptions for the given feature.
     * Storage location is:
     *
     * <pre>metadata/contentInfo/featureTypes/featureTypeName</pre>
     *
     * @param  type         the feature type to add, or {@code null}.
     * @param  occurrences  number of instances of the given features, or {@code null} if unknown.
     */
    public final void add(final FeatureType type, final Integer occurrences) {
        if (type != null) {
            final DefaultFeatureTypeInfo info = new DefaultFeatureTypeInfo(type.getName());
            if (occurrences != null) {
                info.setFeatureInstanceCount(shared(occurrences));
            }
            addIfNotPresent(featureDescription().getFeatureTypeInfo(), info);
        }
    }

    /**
     * Adds the given coordinate reference system to metadata, if it does not already exists.
     * This method ensures that there is no duplicated values.
     * Storage location is:
     *
     * <pre>metadata/referenceSystemInfo</pre>
     *
     * @param  crs  the coordinate reference system to add to the metadata, or {@code null} if none.
     */
    public final void add(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            addIfNotPresent(metadata().getReferenceSystemInfo(), crs);
        }
    }

    /**
     * Adds the given envelope, including its CRS, to the metadata. If the metadata already contains a geographic
     * bounding box, then a new bounding box is added; this method does not compute the union of the two boxes.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/extent/geographicElement</pre>
     *
     * @param  envelope  the extent to add in the metadata, or {@code null} if none.
     * @throws TransformException if an error occurred while converting the given envelope to extents.
     */
    public final void addExtent(final AbstractEnvelope envelope) throws TransformException {
        if (envelope != null) {
            add(envelope.getCoordinateReferenceSystem());
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
     * <pre>metadata/identificationInfo/extent/geographicElement</pre>
     *
     * @param  ordinates  the geographic coordinates.
     * @param  index      index of the first value to use in the given array.
     */
    public final void addExtent(final double[] ordinates, int index) {
        final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(
                    ordinates[index], ordinates[++index], ordinates[++index], ordinates[++index]);
        if (!bbox.isEmpty()) {
            addIfNotPresent(extent().getGeographicElements(), bbox);
        }
    }

    /**
     * Adds a temporal extent covered by the data.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/extent/temporalElement</pre>
     *
     * @param  startTime  when the data begins, or {@code null}.
     * @param  endTime    when the data ends, or {@code null}.
     * @throws UnsupportedOperationException if the temporal module is not on the classpath.
     *
     * @see #addAcquisitionTime(Date)
     */
    public final void addExtent(final Date startTime, final Date endTime) {
        if (startTime != null || endTime != null) {
            final DefaultTemporalExtent t = new DefaultTemporalExtent();
            t.setBounds(startTime, endTime);
            addIfNotPresent(extent().getTemporalElements(), t);
        }
    }

    /**
     * Adds a date of the given type. This is not the data acquisition time,
     * but rather the metadata creation or last update time.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/citation/date/date</pre>
     *
     * @param date  the date to add, or {@code null}.
     * @param type  the type of the date to add, or {@code null}.
     */
    public final void add(final Date date, final DateType type) {
        if (date != null) {
            addIfNotPresent(citation().getDates(), new DefaultCitationDate(date, type));
        }
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
     * Returns the concatenation of the given string. The previous string may be {@code null}.
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
     * Returns {@code true} if a title has been specified for the current identification information.
     * This method is provided because titles are mandatory in ISO 19115 metadata, so data stores may
     * want to provide a fallback if no title has been found. Titles are specified by calls to
     * {@link #addTitle(CharSequence)} and needs to be specified again if {@link #newIdentification()}
     * has been invoked.
     *
     * @return whether a title exists for the current identification information.
     */
    public final boolean hasTitle() {
        return (citation != null) && citation.getTitle() != null;
    }

    /**
     * Adds a title or alternate title of the resource.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/citation/title</pre>
     *
     * @param title  the resource title or alternate title, or {@code null} if none.
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
     * Adds a brief narrative summary of the resource(s).
     * If a summary already existed, the new one will be appended after a new line.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/abstract</pre>
     *
     * @param description  the summary of resource(s), or {@code null} if none.
     */
    public final void addAbstract(final CharSequence description) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultDataIdentification identification = identification();
            identification.setAbstract(append(identification.getAbstract(), i18n));
        }
    }

    /**
     * Adds an author name. If an author was already defined with a different name,
     * then a new party instance is created.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/citation/party/name</pre>
     *
     * @param  name  the name of the author or publisher, or {@code null} if none.
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
     * Adds recognition of those who contributed to the resource(s).
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/credit</pre>
     *
     * @param  credit  recognition of those who contributed to the resource(s).
     */
    public final void addCredits(final CharSequence credit) {
        final InternationalString i18n = trim(credit);
        if (i18n != null) {
            addIfNotPresent(identification().getCredits(), i18n);
        }
    }

    /**
     * Adds a data identifier (not necessarily the same as the metadata identifier).
     * Empty strings (ignoring spaces) are ignored.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/citation/identifier</pre>
     *
     * @param  code  the identifier code, or {@code null} if none.
     */
    public final void addIdentifier(String code) {
        if (code != null && !(code = code.trim()).isEmpty()) {
            addIfNotPresent(citation().getIdentifiers(), new DefaultIdentifier(code));
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
                     * We require the year to be surrounded by punctuations in order to reduce the risk of confusion
                     * with postal addresses. So this block should accept "John, 1992." but not "1992 Nowhere road".
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
                        if (endOfToken >= length || isSpaceOrPunctuation(notice.codePointAt(endOfToken))) try {
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
     * <pre>metadata/identificationInfo/resourceConstraint</pre>
     *
     * @param  notice  the legal notice, or {@code null} if none.
     */
    public final void parseLegalNotice(final String notice) {
        if (notice != null) {
            LegalSymbols.parse(notice, constraints());
        }
    }

    /**
     * Adds a platform on which instrument are installed. If a platform was already defined
     * with a different identifier, then a new platform instance will be created.
     * Storage location is:
     *
     * <pre>metadata/acquisitionInformation/platform/identifier</pre>
     *
     * @param  identifier  identifier of the platform to add, or {@code null}.
     */
    public final void addPlatform(String identifier) {
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
            platform().setIdentifier(new DefaultIdentifier(identifier));
        }
    }

    /**
     * Adds an instrument or sensor on the platform.
     * Storage location is:
     *
     * <pre>metadata/acquisitionInformation/platform/instrument/identifier</pre>
     *
     * @param  identifier  identifier of the sensor to add, or {@code null}.
     */
    public final void addInstrument(String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            final DefaultInstrument instrument = new DefaultInstrument();
            instrument.setIdentifier(new DefaultIdentifier(identifier));
            addIfNotPresent(platform().getInstruments(), instrument);
        }
    }

    /**
     * Adds an event that describe the time at which data were acquired.
     * Storage location is:
     *
     * <pre>metadata/acquisitionInformation/operation/significantEvent/time</pre>
     *
     * @param  time  the acquisition time, or {@code null}.
     *
     * @see #addExtent(Date, Date)
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
     * Adds the identifier of the requirement to be satisfied by data acquisition.
     * Storage location is:
     *
     * <pre>metadata/acquisitionInformation/acquisitionRequirement/identifier</pre>
     *
     * @param  identifier  unique name or code for the requirement, or {@code null}.
     */
    public final void addAcquisitionRequirement(String identifier) {
        if (identifier != null && !(identifier = identifier.trim()).isEmpty()) {
            final DefaultRequirement r = new DefaultRequirement();
            r.setIdentifier(new DefaultIdentifier(identifier));
            addIfNotPresent(acquisition().getAcquisitionRequirements(), r);
        }
    }

    /**
     * Adds information about the procedure, process and algorithm applied in a process step.
     * If a processing was already defined with a different identifier, then a new processing
     * instance will be created. Storage location is:
     *
     * <pre>metadata/resourceLineage/processStep/processingInformation/identifier</pre>
     *
     * @param  identifier  identification of the processing package that produced the data, or {@code null}.
     *
     * @see #addSoftwareReference(CharSequence)
     * @see #addHostComputer(CharSequence)
     */
    public final void addProcessing(String identifier) {
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
            processing().setIdentifier(new DefaultIdentifier(identifier));
        }
    }

    /**
     * Adds a reference to document describing processing software.
     * This is added to the processing identified by last call to {@link #addProcessing(String)}.
     * Storage location is:
     *
     * <pre>metadata/resourceLineage/processStep/processingInformation/softwareReference/title</pre>
     *
     * @param  title  title of the document that describe the software, or {@code null} if none.
     */
    public final void addSoftwareReference(final CharSequence title) {
        final InternationalString i18n = trim(title);
        if (i18n != null) {
            addIfNotPresent(processing().getSoftwareReferences(), new DefaultCitation(i18n));
        }
    }

    /**
     * Adds information about the computer and/or operating system in use at the processing time.
     * This is added to the processing identified by last call to {@link #addProcessing(String)}.
     * Storage location is:
     *
     * <pre>metadata/resourceLineage/processStep/processingInformation/procedureDescription</pre>
     *
     * @param  platform  name of the computer or operation system on which the processing has been executed.
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
     * <pre>metadata/resourceLineage/processStep/description</pre>
     *
     * @param  description  additional details about the process step, or {@code null}.
     */
    public final void addProcessDescription(final CharSequence description) {
        final InternationalString i18n = trim(description);
        if (i18n != null) {
            final DefaultProcessStep ps = processStep();
            ps.setDescription(append(ps.getDescription(), i18n));
        }
    }

    /**
     * Sets the area of the dataset obscured by clouds, expressed as a percentage of the spatial extent.
     * This method does nothing if the given value is {@link Double#NaN}.
     *
     * <p>This method is available only if {@link #commitCoverageDescription(boolean)}
     * has been invoked with the {@code electromagnetic} parameter set to {@code true}.
     * Storage location is:</p>
     *
     * <pre>metadata/contentInfo/cloudCoverPercentage</pre>
     *
     * @param  value  the new cloud percentage.
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
     * <p>This method is available only if {@link #commitCoverageDescription(boolean)}
     * has been invoked with the {@code electromagnetic} parameter set to {@code true}.
     * Storage location is:</p>
     *
     * <pre>metadata/contentInfo/illuminationAzimuthAngle</pre>
     *
     * @param  value  the new illumination azimuth angle, or {@code null}.
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
     * <p>This method is available only if {@link #commitCoverageDescription(boolean)}
     * has been invoked with the {@code electromagnetic} parameter set to {@code true}.
     * Storage location is:</p>
     *
     * <pre>metadata/contentInfo/illuminationElevationAngle</pre>
     *
     * @param  value  the new illumination azimuth angle, or {@code null}.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public final void setIlluminationElevationAngle(final double value) {
        if (!Double.isNaN(value)) {
            ((DefaultImageDescription) coverageDescription()).setIlluminationElevationAngle(shared(value));
        }
    }

    /**
     * Adds a linear resolution in metres.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/spatialResolution/distance</pre>
     *
     * @param  distance  the resolution in metres, or {@code NaN} if none.
     */
    public final void addResolution(final double distance) {
        if (!Double.isNaN(distance)) {
            final DefaultResolution r = new DefaultResolution();
            r.setDistance(shared(distance));
            addIfNotPresent(identification().getSpatialResolutions(), r);
        }
    }

    /**
     * Sets the file format. The given name should be a short name like "GeoTIFF".
     * The long name will be inferred from the given short name, if possible.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/resourceFormat/formatSpecificationCitation/alternateTitle</pre>
     *
     * @param  abbreviation  the format short name or abbreviation, or {@code null}.
     * @throws MetadataStoreException  if this method can not connect to the {@code jdbc/SpatialMetadata} database.
     *         Callers should generally handle this exception as a recoverable one (i.e. log a warning and continue).
     */
    public final void setFormat(final String abbreviation) throws MetadataStoreException {
        if (abbreviation != null && abbreviation.length() != 0) {
            if (format == null) {
                format = MetadataSource.getProvided().lookup(Format.class, abbreviation);
            }
        }
    }

    /**
     * Sets identification of grid data as point or cell.
     * Storage location is:
     *
     * <pre>metadata/spatialRepresentationInfo/cellGeometry</pre>
     *
     * @param  value   whether the data represent point or area, or {@code null} if unknown.
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
     * <pre>metadata/spatialRepresentationInfo/pointInPixel</pre>
     *
     * @param  value   whether the data represent point or area, or {@code null} if unknown.
     */
    public final void setPointInPixel(final PixelOrientation value) {
        if (value != null) {
            gridRepresentation().setPointInPixel(value);
        }
    }

    /**
     * Sets a general description of the transformation form grid coordinates to "real world" coordinates.
     * Storage location is:
     *
     * <pre>metadata/spatialRepresentationInfo/transformationDimensionDescription</pre>
     *
     * @param  value  a general description of the "grid to CRS" transformation, or {@code null} if unknown.
     */
    public final void setGridToCRS(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            gridRepresentation().setTransformationDimensionDescription(i18n);
        }
    }

    /**
     * Returns the axis at the given dimension index. All previous dimensions are created if needed.
     *
     * @param  index  index of the desired dimension.
     * @return dimension at the given index.
     */
    private DefaultDimension axis(final short index) {
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
     * <pre>metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionName</pre>
     *
     * @param  dimension  the axis dimension, as a {@code short} for avoiding excessive values.
     * @param  name       the name to set for the given dimension.
     */
    public final void setAxisName(final short dimension, final DimensionNameType name) {
        axis(dimension).setDimensionName(name);
    }

    /**
     * Sets the number of cells along the given dimension.
     * Storage location is:
     *
     * <pre>metadata/spatialRepresentationInfo/axisDimensionProperties/dimensionSize</pre>
     *
     * @param  dimension  the axis dimension, as a {@code short} for avoiding excessive values.
     * @param  length     number of cell values along the given dimension.
     */
    public final void setAxisLength(final short dimension, final int length) {
        axis(dimension).setDimensionSize(shared(length));
    }

    /**
     * Adds a minimal value for the current sample dimension. If a minimal value was already defined, then
     * the new value will be set only if it is smaller than the existing one. {@code NaN} values are ignored.
     * Storage location is:
     *
     * <pre>metadata/contentInfo/attributeGroup/attribute/minValue</pre>
     *
     * @param value  the minimal value to add to the existing range of sample values, or {@code NaN}.
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
     * Storage location is:
     *
     * <pre>metadata/contentInfo/attributeGroup/attribute/maxValue</pre>
     *
     * @param value  the maximal value to add to the existing range of sample values, or {@code NaN}.
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
     * Adds a compression name.
     * Storage location is:
     *
     * <pre>metadata/identificationInfo/resourceFormat/fileDecompressionTechnique</pre>
     *
     * @param value  the compression name, or {@code null}.
     */
    public final void addCompression(final CharSequence value) {
        final InternationalString i18n = trim(value);
        if (i18n != null) {
            final DefaultFormat format = format();
            format.setFileDecompressionTechnique(append(format.getFileDecompressionTechnique(), i18n));
        }
    }

    /**
     * Returns the metadata (optionally as an unmodifiable object), or {@code null} if none.
     * If {@code freeze} is {@code true}, then the returned metadata instance can not be modified.
     *
     * @param  freeze  {@code true} if this method should {@linkplain DefaultMetadata#freeze() freeze}
     *         the metadata instance before to return it.
     * @return the metadata, or {@code null} if none.
     */
    public final DefaultMetadata build(final boolean freeze) {
        newIdentification();
        newGridRepresentation();
        newFeatureTypes();
        newCoverage(false);
        newAcquisition();
        final DefaultMetadata md = metadata;
        metadata = null;
        if (freeze && md != null) {
            md.freeze();
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
        final Number existing = sharedNumbers.putIfAbsent(value, value);
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
        final Number existing = sharedNumbers.putIfAbsent(value, value);
        return (existing != null) ? (Integer) existing : value;
    }
}
