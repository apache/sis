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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.lineage.NominalResolution;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.Namespaces;


/**
 * Information about the source data used in creating the data specified by the scope.
 *
 * {@section Relationship between properties}
 * According ISO 19115, at least one of {@linkplain #getDescription() description} and
 * {@linkplain #getSourceExtents() source extents} shall be provided.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
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
    private static final long serialVersionUID = -398526682785377249L;

    /**
     * Detailed description of the level of the source data.
     */
    private InternationalString description;

    /**
     * Denominator of the representative fraction on a source map.
     */
    private RepresentativeFraction scaleDenominator;

    /**
     * Spatial reference system used by the source data.
     */
    private ReferenceSystem sourceReferenceSystem;

    /**
     * Recommended reference to be used for the source data.
     */
    private Citation sourceCitation;

    /**
     * Information about the spatial, vertical and temporal extent of the source data.
     */
    private Collection<Extent> sourceExtents;

    /**
     * Information about an event in the creation process for the source data.
     */
    private Collection<ProcessStep> sourceSteps;

    /**
     * Processing level of the source data.
     */
    private Identifier processedLevel;

    /**
     * Distance between consistent parts (centre, left side, right side) of two adjacent
     * pixels.
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
     * @param description A detailed description of the level of the source data, or {@code null}.
     */
    public DefaultSource(final CharSequence description) {
        this.description = Types.toInternationalString(description);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Source)
     */
    public DefaultSource(final Source object) {
        super(object);
        description           = object.getDescription();
        scaleDenominator      = object.getScaleDenominator();
        sourceCitation        = object.getSourceCitation();
        sourceExtents         = copyCollection(object.getSourceExtents(), Extent.class);
        sourceSteps           = copyCollection(object.getSourceSteps(), ProcessStep.class);
        processedLevel        = object.getProcessedLevel();
        resolution            = object.getResolution();
        sourceReferenceSystem = object.getSourceReferenceSystem();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets a detailed description of the level of the source data.
     *
     * @param newValue The new description.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the denominator of the representative fraction on a source map.
     */
    @Override
    @XmlElement(name = "scaleDenominator")
    public RepresentativeFraction getScaleDenominator()  {
        return scaleDenominator;
    }

    /**
     * Sets the denominator of the representative fraction on a source map.
     *
     * @param newValue The new scale denominator.
     */
    public void setScaleDenominator(final RepresentativeFraction newValue)  {
        checkWritePermission();
        scaleDenominator = newValue;
    }

    /**
     * Returns the spatial reference system used by the source data.
     *
     * @todo needs to annotate the referencing module before.
     */
    @Override
    public ReferenceSystem getSourceReferenceSystem()  {
        return sourceReferenceSystem;
    }

    /**
     * Sets the spatial reference system used by the source data.
     *
     * @param newValue The new reference system.
     */
    public void setSourceReferenceSystem(final ReferenceSystem newValue) {
        checkWritePermission();
        sourceReferenceSystem = newValue;
    }

    /**
     * Returns the recommended reference to be used for the source data.
     */
    @Override
    @XmlElement(name = "sourceCitation")
    public Citation getSourceCitation() {
        return sourceCitation;
    }

    /**
     * Sets the recommended reference to be used for the source data.
     *
     * @param newValue The new source citation.
     */
    public void setSourceCitation(final Citation newValue) {
        checkWritePermission();
        sourceCitation = newValue;
    }

    /**
     * Returns the information about the spatial, vertical and temporal extent of the source data.
     */
    @Override
    @XmlElement(name = "sourceExtent")
    public Collection<Extent> getSourceExtents()  {
        return sourceExtents = nonNullCollection(sourceExtents, Extent.class);
    }

    /**
     * Information about the spatial, vertical and temporal extent of the source data.
     *
     * @param newValues The new source extents.
     */
    public void setSourceExtents(final Collection<? extends Extent> newValues) {
        sourceExtents = writeCollection(newValues, sourceExtents, Extent.class);
    }

    /**
     * Returns information about an event in the creation process for the source data.
     */
    @Override
    @XmlElement(name = "sourceStep")
    public Collection<ProcessStep> getSourceSteps() {
        return sourceSteps = nonNullCollection(sourceSteps, ProcessStep.class);
    }

    /**
     * Sets information about an event in the creation process for the source data.
     *
     * @param newValues The new source steps.
     */
    public void setSourceSteps(final Collection<? extends ProcessStep> newValues) {
        sourceSteps = writeCollection(newValues, sourceSteps, ProcessStep.class);
    }

    /**
     * Returns the processing level of the source data. {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "processedLevel", namespace = Namespaces.GMI)
    public Identifier getProcessedLevel() {
        return processedLevel;
    }

    /**
     * Sets the processing level of the source data.
     *
     * @param newValue The new processed level value.
     */
    public void setProcessedLevel(final Identifier newValue) {
        checkWritePermission();
        processedLevel = newValue;
    }

    /**
     * Returns the distance between consistent parts (centre, left side, right side) of
     * two adjacent pixels. {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "resolution", namespace = Namespaces.GMI)
    public NominalResolution getResolution() {
        return resolution;
    }

    /**
     * Sets the distance between consistent parts (centre, left side, right side) of
     * two adjacent pixels.
     *
     * @param newValue The new nominal resolution value.
     */
    public void setResolution(final NominalResolution newValue) {
        checkWritePermission();
        resolution = newValue;
    }
}
