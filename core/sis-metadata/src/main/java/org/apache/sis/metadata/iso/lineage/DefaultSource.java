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
package org.apache.sis.metadata.iso.lineage;

import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.lineage.NominalResolution;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.opengis.metadata.quality.Scope;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.internal.metadata.Dependencies;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.Namespaces;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the source data used in creating the data specified by the scope.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code LI_Source}
 * {@code   ├─description……………………………………………} Detailed description of the level of the source data.
 * {@code   └─scope……………………………………………………………} Type and / or extent of the source.
 * {@code       ├─level…………………………………………………} Hierarchical level of the data specified by the scope.
 * {@code       └─levelDescription……………………} Detailed description about the level of the data specified by the scope.
 * {@code           ├─attributeInstances……} Attribute instances to which the information applies.
 * {@code           ├─attributes…………………………} Attributes to which the information applies.
 * {@code           ├─dataset…………………………………} Dataset to which the information applies.
 * {@code           ├─featureInstances…………} Feature instances to which the information applies.
 * {@code           ├─features………………………………} Features to which the information applies.
 * {@code           └─other………………………………………} Class of information that does not fall into the other categories to which the information applies.</div>
 *
 * According ISO 19115, at least one of {@linkplain #getDescription() description} and
 * {@linkplain #getSourceExtents() source extents} shall be provided.
 *
 * <div class="section">Limitations</div>
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
 * @author  Rémi Maréchal (Geomatys)
 * @version 0.5
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@TitleProperty(name = "description")
@XmlType(name = "LI_Source_Type", propOrder = {
    "description",
    "scaleDenominator",
    "sourceCitation",
    "sourceExtents",
    "sourceSteps",
    "processedLevel",
    "resolution"
})
@XmlRootElement(name = "LI_Source")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.LE_Source.class)
public class DefaultSource extends ISOMetadata implements Source {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8444238043227180224L;

    /**
     * Detailed description of the level of the source data.
     */
    private InternationalString description;

    /**
     * Spatial resolution expressed as a scale factor, an angle or a level of detail.
     */
    private Resolution sourceSpatialResolution;

    /**
     * Spatial reference system used by the source data.
     */
    private ReferenceSystem sourceReferenceSystem;

    /**
     * Recommended reference to be used for the source data.
     */
    private Citation sourceCitation;

    /**
     * Reference to metadata for the source.
     */
    private Collection<Citation> sourceMetadata;

    /**
     * Type and / or extent of the source.
     */
    private Scope scope;

    /**
     * Information about an event in the creation process for the source data.
     */
    private Collection<ProcessStep> sourceSteps;

    /**
     * Processing level of the source data.
     */
    private Identifier processedLevel;

    /**
     * Distance between consistent parts (centre, left side, right side) of two adjacent pixels.
     */
    private NominalResolution resolution;

    /**
     * Creates an initially empty source.
     */
    public DefaultSource() {
    }

    /**
     * Creates a source initialized with the given description.
     *
     * @param description  a detailed description of the level of the source data, or {@code null}.
     */
    public DefaultSource(final CharSequence description) {
        this.description = Types.toInternationalString(description);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Source)
     */
    public DefaultSource(final Source object) {
        super(object);
        if (object != null) {
            description             = object.getDescription();
            sourceReferenceSystem   = object.getSourceReferenceSystem();
            sourceCitation          = object.getSourceCitation();
            sourceSteps             = copyCollection(object.getSourceSteps(), ProcessStep.class);
            processedLevel          = object.getProcessedLevel();
            resolution              = object.getResolution();
            if (object instanceof DefaultSource) {
                sourceSpatialResolution = ((DefaultSource) object).getSourceSpatialResolution();
                sourceMetadata          = copyCollection(((DefaultSource) object).getSourceMetadata(), Citation.class);
                scope                   = ((DefaultSource) object).getScope();
            } else {
                setScaleDenominator(object.getScaleDenominator());
                setSourceExtents(object.getSourceExtents());
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
     *       {@code DefaultSource}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultSource} instance is created using the
     *       {@linkplain #DefaultSource(Source) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSource castOrCopy(final Source object) {
        if (object == null || object instanceof DefaultSource) {
            return (DefaultSource) object;
        }
        return new DefaultSource(object);
    }

    /**
     * Returns a detailed description of the level of the source data.
     *
     * @return description of the level of the source data, or {@code null}.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets a detailed description of the level of the source data.
     *
     * @param  newValue  the new description.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the spatial resolution expressed as a scale factor, an angle or a level of detail.
     *
     * @return spatial resolution expressed as a scale factor, an angle or a level of detail, or {@code null} if none.
     *
     * @since 0.5
     */
/// @XmlElement(name = "sourceSpatialResolution")
    @UML(identifier="sourceSpatialResolution", obligation=OPTIONAL, specification=ISO_19115)
    public Resolution getSourceSpatialResolution() {
        return sourceSpatialResolution;
    }

    /**
     * Sets the spatial resolution expressed as a scale factor, an angle or a level of detail.
     *
     * @param  newValue  the new spatial resolution.
     *
     * @since 0.5
     */
    public void setSourceSpatialResolution(final Resolution newValue) {
        checkWritePermission();
        sourceSpatialResolution = newValue;
    }

    /**
     * Returns the denominator of the representative fraction on a source map.
     * This method fetches the value from the
     * {@linkplain #getSourceSpatialResolution() source spatial resolution}.
     *
     * @return representative fraction on a source map, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultResolution#getEquivalentScale()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "scaleDenominator")
    @Dependencies("getSourceSpatialResolution")
    public RepresentativeFraction getScaleDenominator() {
        final Resolution resolution = getSourceSpatialResolution();
        return (resolution != null) ? resolution.getEquivalentScale() : null;
    }

    /**
     * Sets the denominator of the representative fraction on a source map.
     * This method stores the value in the
     * {@linkplain #setSourceSpatialResolution(Resolution) source spatial resolution}.
     *
     * @param  newValue  the new scale denominator.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultResolution#setEquivalentScale(RepresentativeFraction)}.
     */
    @Deprecated
    public void setScaleDenominator(final RepresentativeFraction newValue)  {
        checkWritePermission();
        Resolution resolution = null;
        if (newValue != null) {
            resolution = sourceSpatialResolution;
            if (resolution instanceof DefaultResolution) {
                ((DefaultResolution) resolution).setEquivalentScale(newValue);
            } else {
                resolution = new DefaultResolution(newValue);
            }
        }
        /*
         * Invoke the non-deprecated setter method only if the reference changed,
         * for consistency with other deprecated setter methods in metadata module.
         */
        if (resolution != sourceSpatialResolution) {
            setSourceSpatialResolution(resolution);
        }
    }

    /**
     * Returns the spatial reference system used by the source data.
     *
     * @return spatial reference system used by the source data, or {@code null}.
     *
     * @todo We need to annotate the referencing module before we can annotate this method.
     */
    @Override
/// @XmlElement(name = "sourceReferenceSystem")
    public ReferenceSystem getSourceReferenceSystem()  {
        return sourceReferenceSystem;
    }

    /**
     * Sets the spatial reference system used by the source data.
     *
     * @param  newValue  the new reference system.
     */
    public void setSourceReferenceSystem(final ReferenceSystem newValue) {
        checkWritePermission();
        sourceReferenceSystem = newValue;
    }

    /**
     * Returns the recommended reference to be used for the source data.
     *
     * @return recommended reference to be used for the source data, or {@code null}.
     */
    @Override
    @XmlElement(name = "sourceCitation")
    public Citation getSourceCitation() {
        return sourceCitation;
    }

    /**
     * Sets the recommended reference to be used for the source data.
     *
     * @param  newValue  the new source citation.
     */
    public void setSourceCitation(final Citation newValue) {
        checkWritePermission();
        sourceCitation = newValue;
    }

    /**
     * Returns the references to metadata for the source.
     *
     * @return references to metadata for the source.
     *
     * @since 0.5
     */
/// @XmlElement(name = "sourceMetadata")
    @UML(identifier="sourceMetadata", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getSourceMetadata() {
        return sourceMetadata = nonNullCollection(sourceMetadata, Citation.class);
    }

    /**
     * Sets the references to metadata for the source.
     *
     * @param  newValues  the new references.
     *
     * @since 0.5
     */
    public void setSourceMetadata(final Collection<? extends Citation> newValues) {
        sourceMetadata = writeCollection(newValues, sourceMetadata, Citation.class);
    }

    /**
     * Return the type and / or extent of the source.
     * This information should be provided if the {@linkplain #getDescription() description} is not provided.
     *
     * @return type and / or extent of the source, or {@code null} if none.
     *
     * @since 0.5
     */
/// @XmlElement(name = "scope")
    @UML(identifier="scope", obligation=CONDITIONAL, specification=ISO_19115)
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the type and / or extent of the source.
     *
     * @param  newValue  the new type and / or extent of the source.
     *
     * @since 0.5
     */
    public void setScope(final Scope newValue){
        checkWritePermission();
        scope = newValue;
    }

    /**
     * Returns the information about the spatial, vertical and temporal extent of the source data.
     * This method fetches the values from the {@linkplain #getScope() scope}.
     *
     * @return information about the extent of the source data.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultScope#getExtents()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "sourceExtent")
    @Dependencies("getScope")
    public Collection<Extent> getSourceExtents() {
        Scope scope = getScope();
        if (!(scope instanceof DefaultScope)) {
            if (isModifiable()) {
                scope = new DefaultScope(scope);
                this.scope = scope;
            } else {
                return Collections.singleton(scope.getExtent());
            }
        }
        return ((DefaultScope) scope).getExtents();
    }

    /**
     * Information about the spatial, vertical and temporal extent of the source data.
     * This method stores the values in the {@linkplain #setScope(Scope) scope}.
     *
     * @param  newValues  the new source extents.
     *
     * @deprecated As of ISO 19115:2014, moved to {@link DefaultScope#setExtents(Collection)}.
     */
    @Deprecated
    public void setSourceExtents(final Collection<? extends Extent> newValues) {
        checkWritePermission();
        Scope scope = this.scope;
        if (!(scope instanceof DefaultScope)) {
            scope = new DefaultScope(scope);
            setScope(scope);
        }
        ((DefaultScope) scope).setExtents(newValues);
    }

    /**
     * Returns information about process steps in which this source was used.
     *
     * @return information about process steps in which this source was used.
     */
    @Override
    @XmlElement(name = "sourceStep")
    public Collection<ProcessStep> getSourceSteps() {
        return sourceSteps = nonNullCollection(sourceSteps, ProcessStep.class);
    }

    /**
     * Sets information about process steps in which this source was used.
     *
     * @param  newValues  the new process steps.
     */
    public void setSourceSteps(final Collection<? extends ProcessStep> newValues) {
        sourceSteps = writeCollection(newValues, sourceSteps, ProcessStep.class);
    }

    /**
     * Returns the processing level of the source data. {@code null} if unspecified.
     *
     * @return processing level of the source data, or {@code null}.
     */
    @Override
    @XmlElement(name = "processedLevel", namespace = Namespaces.GMI)
    public Identifier getProcessedLevel() {
        return processedLevel;
    }

    /**
     * Sets the processing level of the source data.
     *
     * @param  newValue  the new processed level value.
     */
    public void setProcessedLevel(final Identifier newValue) {
        checkWritePermission();
        processedLevel = newValue;
    }

    /**
     * Returns the distance between consistent parts (centre, left side, right side) of two adjacent pixels.
     *
     * @return distance between consistent parts of two adjacent pixels, or {@code null}.
     */
    @Override
    @XmlElement(name = "resolution", namespace = Namespaces.GMI)
    public NominalResolution getResolution() {
        return resolution;
    }

    /**
     * Sets the distance between consistent parts (centre, left side, right side) of two adjacent pixels.
     *
     * @param  newValue  the new nominal resolution value.
     */
    public void setResolution(final NominalResolution newValue) {
        checkWritePermission();
        resolution = newValue;
    }
}
