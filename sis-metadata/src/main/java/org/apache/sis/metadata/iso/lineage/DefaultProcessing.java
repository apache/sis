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
 * Comprehensive information about the procedure(s), process(es) and algorithm(s) applied
 * in the process step.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
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
    private static final long serialVersionUID = -8032712379901591272L;

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
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultProcessing castOrCopy(final Processing object) {
        if (object == null || object instanceof DefaultProcessing) {
            return (DefaultProcessing) object;
        }
        final DefaultProcessing copy = new DefaultProcessing();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the information to identify the processing package that produced the data.
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
    public synchronized void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }

    /**
     * Returns the reference to document describing processing software.
     */
    @Override
    @XmlElement(name = "softwareReference", namespace = Namespaces.GMI)
    public synchronized Collection<Citation> getSoftwareReferences() {
        return softwareReferences = nonNullCollection(softwareReferences, Citation.class);
    }

    /**
     * Sets the reference to document describing processing software.
     *
     * @param newValues The new software references values.
     */
    public synchronized void setSoftwareReferences(final Collection<? extends Citation> newValues) {
        softwareReferences = copyCollection(newValues, softwareReferences, Citation.class);
    }

    /**
     * Returns the additional details about the processing procedures. {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "procedureDescription", namespace = Namespaces.GMI)
    public synchronized InternationalString getProcedureDescription() {
        return procedureDescription;
    }

    /**
     * Sets the additional details about the processing procedures.
     *
     * @param newValue The new procedure description value.
     */
    public synchronized void setProcedureDescription(final InternationalString newValue) {
        checkWritePermission();
        procedureDescription = newValue;
    }

    /**
     * Returns the reference to documentation describing the processing.
     */
    @Override
    @XmlElement(name = "documentation", namespace = Namespaces.GMI)
    public synchronized Collection<Citation> getDocumentations() {
        return documentations = nonNullCollection(documentations, Citation.class);
    }

    /**
     * Sets the reference to documentation describing the processing.
     *
     * @param newValues The new documentations values.
     */
    public synchronized void setDocumentations(final Collection<? extends Citation> newValues) {
        documentations = copyCollection(newValues, documentations, Citation.class);
    }

    /**
     * Returns the parameters to control the processing operations, entered at run time.
     * {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "runTimeParameters", namespace = Namespaces.GMI)
    public synchronized InternationalString getRunTimeParameters() {
        return runTimeParameters;
    }

    /**
     * Sets the parameters to control the processing operations, entered at run time.
     *
     * @param newValue The new runtime parameter value.
     */
    public synchronized void setRunTimeParameters(final InternationalString newValue) {
        checkWritePermission();
        runTimeParameters = newValue;
    }

    /**
     * Returns the details of the methodology by which geographic information was derived from the
     * instrument readings.
     */
    @Override
    @XmlElement(name = "algorithm", namespace = Namespaces.GMI)
    public synchronized Collection<Algorithm> getAlgorithms() {
        return algorithms = nonNullCollection(algorithms, Algorithm.class);
    }

    /**
     * Sets the details of the methodology by which geographic information was derived from the
     * instrument readings.
     *
     * @param newValues The new algorithms values.
     */
    public synchronized void setAlgorithms(final Collection<? extends Algorithm> newValues) {
        algorithms = copyCollection(newValues, algorithms, Algorithm.class);
    }
}
