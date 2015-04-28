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
import javax.xml.bind.annotation.XmlType;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Scope;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the events or source data used in constructing the data specified by
 * the scope or lack of knowledge about lineage.
 *
 * <div class="section">Relationship between properties</div>
 * At least one of {@linkplain #getStatement statement}, {@linkplain #getProcessSteps() process steps}
 * and {@link #getSources() sources} shall be provided.
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
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "LI_Lineage_Type", propOrder = {
    "statement",
/// "scope",
/// "additionalResource",
    "processSteps",
    "sources"
})
@XmlRootElement(name = "LI_Lineage")
public class DefaultLineage extends ISOMetadata implements Lineage {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6214461492323186254L;

    /**
     * General explanation of the data producer's knowledge about the lineage of a dataset.
     * Should be provided only if {@linkplain DefaultScope#getLevel scope level} is
     * {@linkplain ScopeCode#DATASET dataset} or {@linkplain ScopeCode#SERIES series}.
     */
    private InternationalString statement;

    /**
     * Type of resource and / or extent to which the lineage information applies.
     */
    private Scope scope;

    /**
     * A resources (for example publication) that describes the whole
     * process to generate this resource (for example a dataset).
     */
    private Collection<Citation> additionalDocumentation;

    /**
     * Information about an event in the creation process for the data specified by the scope.
     */
    private Collection<ProcessStep> processSteps;

    /**
     * Information about the source data used in creating the data specified by the scope.
     */
    private Collection<Source> sources;

    /**
     * Constructs an initially empty lineage.
     */
    public DefaultLineage() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Lineage)
     */
    public DefaultLineage(final Lineage object) {
        super(object);
        if (object != null) {
            statement               = object.getStatement();
            processSteps            = copyCollection(object.getProcessSteps(), ProcessStep.class);
            sources                 = copyCollection(object.getSources(), Source.class);
            if (object instanceof DefaultLineage) {
                scope                   = ((DefaultLineage) object).getScope();
                additionalDocumentation = copyCollection(((DefaultLineage) object).getAdditionalDocumentation(), Citation.class);
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
     *       {@code DefaultLineage}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultLineage} instance is created using the
     *       {@linkplain #DefaultLineage(Lineage) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultLineage castOrCopy(final Lineage object) {
        if (object == null || object instanceof DefaultLineage) {
            return (DefaultLineage) object;
        }
        return new DefaultLineage(object);
    }

    /**
     * Returns the general explanation of the data producer's knowledge about the lineage of a dataset.
     * Can be provided only if {@linkplain DefaultScope#getLevel scope level}
     * is {@link ScopeCode#DATASET DATASET} or {@link ScopeCode#SERIES SERIES}.
     *
     * @return Explanation of the data producer's knowledge about the lineage, or {@code null}.
     */
    @Override
    @XmlElement(name = "statement")
    public InternationalString getStatement() {
        return statement;
    }

    /**
     * Sets the general explanation of the data producers knowledge about the lineage of a dataset.
     *
     * @param newValue The new statement.
     */
    public void setStatement(final InternationalString newValue) {
        checkWritePermission();
        statement = newValue;
    }

    /**
     * Returns the type of resource and / or extents to which the lineage information applies.
     *
     * @return Type of resource and / or extents to which the lineage information applies.
     *
     * @since 0.5
     */
/// @XmlElement(name = "scope")
    @UML(identifier="scope", obligation=OPTIONAL, specification=ISO_19115)
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the type of resource and / or extents to which the lineage information applies.
     *
     * @param newValue The new type of resource.
     *
     * @since 0.5
     */
    public void setScope(final Scope newValue) {
        checkWritePermission();
        scope = newValue;
    }

    /**
     * Returns additional documentation.
     *
     * @return Additional documentation.
     *
     * @since 0.5
     */
/// @XmlElement(name = "additionalDocumentation")
    @UML(identifier="additionalDocumentation", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getAdditionalDocumentation() {
        return additionalDocumentation = nonNullCollection(additionalDocumentation, Citation.class);
    }

    /**
     * Sets additional documentation.
     *
     * @param newValues Additional documentation.
     *
     * @since 0.5
     */
    public void setAdditionalDocumentation(final Collection<? extends Citation> newValues)  {
        additionalDocumentation = writeCollection(newValues, additionalDocumentation, Citation.class);
    }

    /**
     * Returns the information about about events in the life of a resource specified by the scope.
     *
     * @return Information about events in the life of a resource.
     */
    @Override
    @XmlElement(name = "processStep")
    public Collection<ProcessStep> getProcessSteps() {
        return processSteps = nonNullCollection(processSteps, ProcessStep.class);
    }

    /**
     * Sets information about events in the life of a resource specified by the scope.
     *
     * @param newValues The new process steps.
     */
    public void setProcessSteps(final Collection<? extends ProcessStep> newValues)  {
        processSteps = writeCollection(newValues, processSteps, ProcessStep.class);
    }

    /**
     * Returns information about the source data used in creating the data specified by the scope.
     *
     * @return Information about the source data.
     */
    @Override
    @XmlElement(name = "source")
    public Collection<Source> getSources() {
        return sources = nonNullCollection(sources, Source.class);
    }

    /**
     * Sets information about the source data used in creating the data specified by the scope.
     *
     * @param newValues The new sources.
     */
    public void setSources(final Collection<? extends Source> newValues) {
        sources = writeCollection(newValues, sources, Source.class);
    }
}
