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

import java.util.Date;
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.quality.Scope;
import org.opengis.metadata.lineage.Source;
import org.opengis.metadata.lineage.Processing;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.lineage.ProcessStepReport;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.util.iso.Types;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.jaxb.gml.TM_Primitive;
import org.apache.sis.internal.jaxb.metadata.MD_Scope;
import org.apache.sis.internal.util.TemporalUtilities;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about an event or transformation in the life of a resource.
 * Includes the process used to maintain the resource.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code LI_ProcessStep}
 * {@code   └─description……} Description of the event, including related parameters or tolerances.</div>
 *
 * <p><b>Limitations:</b></p>
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
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@TitleProperty(name = "description")
@XmlType(name = "LI_ProcessStep_Type", propOrder = {
    "description",
    "rationale",
    "stepDateTime",             // New in ISO 19115:2014
    "date",                     // Legacy ISO 19115:2003
    "processors",
    "reference",                // New in ISO 19115:2014
    "scope",                    // New in ISO 19115:2014
    "sources",
    "outputs",                  // ISO 19115-2 extension
    "processingInformation",    // Ibid.
    "reports"                   // Ibid.
})
@XmlRootElement(name = "LI_ProcessStep")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.LE_ProcessStep.class)
public class DefaultProcessStep extends ISOMetadata implements ProcessStep {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2338712901907082970L;

    /**
     * Description of the event, including related parameters or tolerances.
     */
    private InternationalString description;

    /**
     * Requirement or purpose for the process step.
     */
    private InternationalString rationale;

    /**
     * Date, time or range of date and time over which the process step occurred.
     */
    private TemporalPrimitive stepDateTime;

    /**
     * Identification of, and means of communication with, person(s) and
     * organization(s) associated with the process step.
     */
    private Collection<ResponsibleParty> processors;

    /**
     * Process step documentation.
     */
    private Collection<Citation> references;

    /**
     * Type of resource and / or extent to which the process step applies.
     */
    private Scope scope;

    /**
     * Information about the source data used in creating the data specified by the scope.
     */
    private Collection<Source> sources;

    /**
     * Description of the product generated as a result of the process step.
     */
    private Collection<Source> outputs;

    /**
     * Comprehensive information about the procedure by which the algorithm was applied
     * to derive geographic data from the raw instrument measurements, such as datasets,
     * software used, and the processing environment.
     */
    private Processing processingInformation;

    /**
     * Report generated by the process step.
     */
    private Collection<ProcessStepReport> reports;

    /**
     * Creates an initially empty process step.
     */
    public DefaultProcessStep() {
    }

    /**
     * Creates a process step initialized to the given description.
     *
     * @param description  description of the event, including related parameters or tolerances.
     */
    public DefaultProcessStep(final CharSequence description) {
        this.description = Types.toInternationalString(description);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ProcessStep)
     */
    public DefaultProcessStep(final ProcessStep object) {
        super(object);
        if (object != null) {
            description           = object.getDescription();
            rationale             = object.getRationale();
            stepDateTime          = TemporalUtilities.createInstant(object.getDate());
            processors            = copyCollection(object.getProcessors(), ResponsibleParty.class);
            sources               = copyCollection(object.getSources(), Source.class);
            outputs               = copyCollection(object.getOutputs(), Source.class);
            processingInformation = object.getProcessingInformation();
            reports               = copyCollection(object.getReports(), ProcessStepReport.class);
            if (object instanceof DefaultProcessStep) {
                references = copyCollection(((DefaultProcessStep) object).getReferences(), Citation.class);
                scope      = ((DefaultProcessStep) object).getScope();
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
     *       {@code DefaultProcessStep}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultProcessStep} instance is created using the
     *       {@linkplain #DefaultProcessStep(ProcessStep) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultProcessStep castOrCopy(final ProcessStep object) {
        if (object == null || object instanceof DefaultProcessStep) {
            return (DefaultProcessStep) object;
        }
        return new DefaultProcessStep(object);
    }

    /**
     * Returns the description of the event, including related parameters or tolerances.
     *
     * @return description of the event, or {@code null}.
     */
    @Override
    @XmlElement(name = "description", required = true)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the description of the event, including related parameters or tolerances.
     *
     * @param  newValue  the new description.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the requirement or purpose for the process step.
     *
     * @return requirement or purpose for the process step, or {@code null}.
     */
    @Override
    @XmlElement(name = "rationale")
    public InternationalString getRationale() {
        return rationale;
    }

    /**
     * Sets the requirement or purpose for the process step.
     *
     * @param  newValue  the new rationale.
     */
    public void setRationale(final InternationalString newValue) {
        checkWritePermission();
        rationale = newValue;
    }

    /**
     * Returns the date, time or range of date and time over which the process step occurred.
     *
     * @return date, time or period over which the process step occurred, or {@code null}.
     *
     * @since 1.0
     */
    @XmlElement(name = "stepDateTime")
    @XmlJavaTypeAdapter(TM_Primitive.Since2014.class)
    public TemporalPrimitive getStepDateTime() {
        return stepDateTime;
    }

    /**
     * Sets the date, time or range of date and time over which the process step occurred.
     *
     * @param  newValue  the new date, time or period.
     *
     * @since 1.0
     */
    public void setStepDateTime(final TemporalPrimitive newValue) {
        checkWritePermission();
        stepDateTime = newValue;
    }

    /**
     * Returns the date and time or range of date and time on or over which the process step occurred.
     *
     * @return date on or over which the process step occurred, or {@code null}.
     *
     * @deprecated As of ISO 19115-1:2014, replaced by {@link #getStepDateTime()}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "dateTime", namespace = LegacyNamespaces.GMD)
    public Date getDate() {
        return FilterByVersion.LEGACY_METADATA.accept() ? TemporalUtilities.getDate(getStepDateTime()) : null;
    }

    /**
     * Sets the date and time or range of date and time on or over which the process step occurred.
     *
     * @param  newValue  the new date.
     *
     * @deprecated As of ISO 19115-1:2014, replaced by {@link #setStepDateTime(TemporalPrimitive)}.
     */
    @Deprecated
    public void setDate(final Date newValue) {
        setStepDateTime(TemporalUtilities.createInstant(newValue));
    }

    /**
     * Returns the identification of, and means of communication with, person(s) and
     * organization(s) associated with the process step.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return means of communication with person(s) and organization(s) associated with the process step.
     */
    @Override
    @XmlElement(name = "processor")
    public Collection<ResponsibleParty> getProcessors() {
        return processors = nonNullCollection(processors, ResponsibleParty.class);
    }

    /**
     * Identification of, and means of communication with, person(s) and
     * organization(s) associated with the process step.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code ResponsibleParty} is replaced by the {@code Responsibility} parent interface.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValues  the new processors.
     */
    public void setProcessors(final Collection<? extends ResponsibleParty> newValues) {
        processors = writeCollection(newValues, processors, ResponsibleParty.class);
    }

    /**
     * Returns the process step documentation.
     *
     * @return process step documentation.
     *
     * @since 0.5
     */
    // @XmlElement at the end of this class.
    @UML(identifier="reference", obligation=OPTIONAL, specification=ISO_19115)
    public Collection<Citation> getReferences() {
        return references = nonNullCollection(references, Citation.class);
    }

    /**
     * Sets the process step documentation.
     *
     * @param  newValues  the new documentation.
     *
     * @since 0.5
     */
    public void setReferences(final Collection<? extends Citation> newValues){
        references = writeCollection(newValues, references, Citation.class);
    }

    /**
     * Returns the type of resource and / or extent to which the process step applies.
     *
     * @return type of resource, or {@code null} if none.
     *
     * @since 0.5
     */
    @XmlElement(name = "scope")
    @XmlJavaTypeAdapter(MD_Scope.Since2014.class)
    @UML(identifier="scope", obligation=OPTIONAL, specification=ISO_19115)
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the type of resource and / or extent to which the process step applies.
     *
     * @param  newValue  the new type of resource.
     *
     * @since 0.5
     */
    public void setScope(final Scope newValue) {
        checkWritePermission();
        scope = newValue;
    }

    /**
     * Returns the information about the source data used in creating the data specified by the scope.
     *
     * @return information about the source data used in creating the data.
     */
    @Override
    @XmlElement(name = "source")
    public Collection<Source> getSources() {
        return sources = nonNullCollection(sources, Source.class);
    }

    /**
     * Information about the source data used in creating the data specified by the scope.
     *
     * @param  newValues  the new sources.
     */
    public void setSources(final Collection<? extends Source> newValues) {
        sources = writeCollection(newValues, sources, Source.class);
    }

    /**
     * Returns the description of the product generated as a result of the process step.
     *
     * @return product generated as a result of the process step.
     */
    @Override
    @XmlElement(name = "output")
    public Collection<Source> getOutputs() {
        return outputs = nonNullCollection(outputs, Source.class);
    }

    /**
     * Sets the description of the product generated as a result of the process step.
     *
     * @param  newValues  the new output values.
     */
    public void setOutputs(final Collection<? extends Source> newValues) {
        outputs = writeCollection(newValues, outputs, Source.class);
    }

    /**
     * Returns the comprehensive information about the procedure by which the algorithm
     * was applied to derive geographic data from the raw instrument measurements, such
     * as datasets, software used, and the processing environment.
     *
     * @return procedure by which the algorithm was applied to derive geographic data, or {@code null}.
     */
    @Override
    @XmlElement(name = "processingInformation")
    public Processing getProcessingInformation() {
        return processingInformation;
    }

    /**
     * Sets the comprehensive information about the procedure by which the algorithm was
     * applied to derive geographic data from the raw instrument measurements, such as
     * datasets, software used, and the processing environment.
     *
     * @param  newValue  the new processing information value.
     */
    public void setProcessingInformation(final Processing newValue) {
        checkWritePermission();
        processingInformation = newValue;
    }

    /**
     * Returns the report generated by the process step.
     *
     * @return report generated by the process step.
     */
    @Override
    @XmlElement(name = "report")
    public Collection<ProcessStepReport> getReports() {
        return reports = nonNullCollection(reports, ProcessStepReport.class);
    }

    /**
     * Sets the report generated by the process step.
     *
     * @param  newValues  the new process step report values.
     */
    public void setReports(final Collection<? extends ProcessStepReport> newValues) {
        reports = writeCollection(newValues, reports, ProcessStepReport.class);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "reference")
    private Collection<Citation> getReference() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getReferences() : null;
    }
}
