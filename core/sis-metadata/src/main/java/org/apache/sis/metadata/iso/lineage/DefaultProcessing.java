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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.lineage.Algorithm;
import org.opengis.metadata.lineage.Processing;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;


/**
 * Comprehensive information about the procedure(s), process(es) and algorithm(s) applied in the process step.
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "LE_Processing_Type", propOrder = {
    "identifier",
    "softwareReferences",
    "procedureDescription",
    "documentations",
    "runTimeParameters",
    "algorithms"
})
@XmlRootElement(name = "LE_Processing", namespace = Namespaces.GMI)
public class DefaultProcessing extends ISOMetadata implements Processing {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5698533358975632857L;

    /**
     * Reference to document describing processing software.
     */
    private Collection<Citation> softwareReferences;

    /**
     * Additional details about the processing procedures.
     */
    private InternationalString procedureDescription;

    /**
     * Reference to documentation describing the processing.
     */
    private Collection<Citation> documentations;

    /**
     * Parameters to control the processing operations, entered at run time.
     */
    private InternationalString runTimeParameters;

    /**
     * Details of the methodology by which geographic information was derived from the
     * instrument readings.
     */
    private Collection<Algorithm> algorithms;

    /**
     * Constructs an initially empty range element description.
     */
    public DefaultProcessing() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Processing)
     */
    public DefaultProcessing(final Processing object) {
        super(object);
        if (object != null) {
            identifiers          = singleton(object.getIdentifier(), Identifier.class);
            softwareReferences   = copyCollection(object.getSoftwareReferences(), Citation.class);
            procedureDescription = object.getProcedureDescription();
            documentations       = copyCollection(object.getDocumentations(), Citation.class);
            runTimeParameters    = object.getRunTimeParameters();
            algorithms           = copyCollection(object.getAlgorithms(), Algorithm.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultProcessing}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultProcessing} instance is created using the
     *       {@linkplain #DefaultProcessing(Processing) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultProcessing castOrCopy(final Processing object) {
        if (object == null || object instanceof DefaultProcessing) {
            return (DefaultProcessing) object;
        }
        return new DefaultProcessing(object);
    }

    /**
     * Returns the information to identify the processing package that produced the data.
     *
     * @return Identifier of the processing package that produced the data, or {@code null}.
     */
    @Override
    @XmlElement(name = "identifier", namespace = Namespaces.GMI, required = true)
    public Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets the information to identify the processing package that produced the data.
     *
     * @param newValue The new identifier value.
     */
    public void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Returns the reference to document describing processing software.
     *
     * @return Document describing processing software.
     */
    @Override
    @XmlElement(name = "softwareReference", namespace = Namespaces.GMI)
    public Collection<Citation> getSoftwareReferences() {
        return softwareReferences = nonNullCollection(softwareReferences, Citation.class);
    }

    /**
     * Sets the reference to document describing processing software.
     *
     * @param newValues The new software references values.
     */
    public void setSoftwareReferences(final Collection<? extends Citation> newValues) {
        softwareReferences = writeCollection(newValues, softwareReferences, Citation.class);
    }

    /**
     * Returns the additional details about the processing procedures. {@code null} if unspecified.
     *
     * @return Processing procedures, or {@code null}.
     */
    @Override
    @XmlElement(name = "procedureDescription", namespace = Namespaces.GMI)
    public InternationalString getProcedureDescription() {
        return procedureDescription;
    }

    /**
     * Sets the additional details about the processing procedures.
     *
     * @param newValue The new procedure description value.
     */
    public void setProcedureDescription(final InternationalString newValue) {
        checkWritePermission();
        procedureDescription = newValue;
    }

    /**
     * Returns the reference to documentation describing the processing.
     *
     * @return Documentation describing the processing.
     */
    @Override
    @XmlElement(name = "documentation", namespace = Namespaces.GMI)
    public Collection<Citation> getDocumentations() {
        return documentations = nonNullCollection(documentations, Citation.class);
    }

    /**
     * Sets the reference to documentation describing the processing.
     *
     * @param newValues The new documentations values.
     */
    public void setDocumentations(final Collection<? extends Citation> newValues) {
        documentations = writeCollection(newValues, documentations, Citation.class);
    }

    /**
     * Returns the parameters to control the processing operations, entered at run time.
     *
     * @return Parameters to control the processing operations, or {@code null}.
     */
    @Override
    @XmlElement(name = "runTimeParameters", namespace = Namespaces.GMI)
    public InternationalString getRunTimeParameters() {
        return runTimeParameters;
    }

    /**
     * Sets the parameters to control the processing operations, entered at run time.
     *
     * @param newValue The new runtime parameter value.
     */
    public void setRunTimeParameters(final InternationalString newValue) {
        checkWritePermission();
        runTimeParameters = newValue;
    }

    /**
     * Returns the details of the methodology by which geographic information was derived from the
     * instrument readings.
     *
     * @return Methodology by which geographic information was derived from the instrument readings.
     */
    @Override
    @XmlElement(name = "algorithm", namespace = Namespaces.GMI)
    public Collection<Algorithm> getAlgorithms() {
        return algorithms = nonNullCollection(algorithms, Algorithm.class);
    }

    /**
     * Sets the details of the methodology by which geographic information was derived from the
     * instrument readings.
     *
     * @param newValues The new algorithms values.
     */
    public void setAlgorithms(final Collection<? extends Algorithm> newValues) {
        algorithms = writeCollection(newValues, algorithms, Algorithm.class);
    }
}
