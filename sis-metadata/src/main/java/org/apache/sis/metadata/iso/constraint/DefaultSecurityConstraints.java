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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
 */
package org.apache.sis.metadata.iso.constraint;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.constraint.Classification;
import org.opengis.metadata.constraint.SecurityConstraints;


/**
 * Handling restrictions imposed on the resource for national security or similar security concerns.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_SecurityConstraints_Type", propOrder = {
    "classification",
    "userNote",
    "classificationSystem",
    "handlingDescription"
})
@XmlRootElement(name = "MD_SecurityConstraints")
public class DefaultSecurityConstraints extends DefaultConstraints implements SecurityConstraints {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6412833018607679734L;;

    /**
     * Name of the handling restrictions on the resource.
     */
    private Classification classification;

    /**
     * Explanation of the application of the legal constraints or other restrictions and legal
     * prerequisites for obtaining and using the resource.
     */
    private InternationalString userNote;

    /**
     * Name of the classification system.
     */
    private InternationalString classificationSystem;

    /**
     * Additional information about the restrictions on handling the resource.
     */
    private InternationalString handlingDescription;

    /**
     * Creates an initially empty security constraints.
     */
    public DefaultSecurityConstraints() {
    }

    /**
     * Creates a security constraints initialized with the specified classification.
     *
     * @param classification The name of the handling restrictions on the resource.
     */
    public DefaultSecurityConstraints(final Classification classification) {
        this.classification = classification;
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
    public static DefaultSecurityConstraints castOrCopy(final SecurityConstraints object) {
        if (object == null || object instanceof DefaultSecurityConstraints) {
            return (DefaultSecurityConstraints) object;
        }
        final DefaultSecurityConstraints copy = new DefaultSecurityConstraints();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the name of the handling restrictions on the resource.
     */
    @Override
    @XmlElement(name = "classification", required = true)
    public synchronized Classification getClassification() {
        return classification;
    }

    /**
     * Sets the name of the handling restrictions on the resource.
     *
     * @param newValue The new classification.
     */
    public synchronized void setClassification(final Classification newValue) {
        checkWritePermission();
        classification = newValue;
    }

    /**
     * Returns the explanation of the application of the legal constraints or other restrictions and legal
     * prerequisites for obtaining and using the resource.
     */
    @Override
    @XmlElement(name = "userNote")
    public synchronized InternationalString getUserNote() {
        return userNote;
    }

    /**
     * Sets the explanation of the application of the legal constraints or other restrictions and legal
     * prerequisites for obtaining and using the resource.
     *
     * @param newValue The new user note.
     */
    public synchronized void setUserNote(final InternationalString newValue) {
        checkWritePermission();
        userNote = newValue;
    }

    /**
     * Returns the name of the classification system.
     */
    @Override
    @XmlElement(name = "classificationSystem")
    public synchronized InternationalString getClassificationSystem() {
        return classificationSystem;
    }

    /**
     * Sets the name of the classification system.
     *
     * @param newValue The new classification system.
     */
    public synchronized void setClassificationSystem(final InternationalString newValue) {
        checkWritePermission();
        classificationSystem = newValue;
    }

    /**
     * Returns the additional information about the restrictions on handling the resource.
     */
    @Override
    @XmlElement(name = "handlingDescription")
    public synchronized InternationalString getHandlingDescription() {
        return handlingDescription;
    }

    /**
     * Sets the additional information about the restrictions on handling the resource.
     *
     * @param newValue The new handling description.
     */
    public synchronized void setHandlingDescription(final InternationalString newValue) {
        checkWritePermission();
        handlingDescription = newValue;
    }
}
