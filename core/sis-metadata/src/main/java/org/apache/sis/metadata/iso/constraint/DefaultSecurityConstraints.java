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
package org.apache.sis.metadata.iso.constraint;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.constraint.Classification;
import org.opengis.metadata.constraint.SecurityConstraints;


/**
 * Handling restrictions imposed on the resource for national security or similar security concerns.
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
 * @since   0.3
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
    private static final long serialVersionUID = 621767670847345848L;

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
     * Constructs a new constraints with the given {@linkplain #getUseLimitations() use limitation}.
     *
     * @param useLimitation The use limitation, or {@code null} if none.
     */
    public DefaultSecurityConstraints(final CharSequence useLimitation) {
        super(useLimitation);
    }

    /**
     * Creates a security constraints initialized with the specified classification.
     *
     * @param classification The name of the handling restrictions on the resource, or {@code null}.
     */
    public DefaultSecurityConstraints(final Classification classification) {
        this.classification = classification;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(SecurityConstraints)
     */
    public DefaultSecurityConstraints(final SecurityConstraints object) {
        super(object);
        if (object != null) {
            classification       = object.getClassification();
            userNote             = object.getUserNote();
            classificationSystem = object.getClassificationSystem();
            handlingDescription  = object.getHandlingDescription();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultSecurityConstraints}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultSecurityConstraints} instance is created using the
     *       {@linkplain #DefaultSecurityConstraints(SecurityConstraints) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSecurityConstraints castOrCopy(final SecurityConstraints object) {
        if (object == null || object instanceof DefaultSecurityConstraints) {
            return (DefaultSecurityConstraints) object;
        }
        return new DefaultSecurityConstraints(object);
    }

    /**
     * Returns the name of the handling restrictions on the resource.
     *
     * @return Name of the handling restrictions on the resource, or {@code null}.
     */
    @Override
    @XmlElement(name = "classification", required = true)
    public Classification getClassification() {
        return classification;
    }

    /**
     * Sets the name of the handling restrictions on the resource.
     *
     * @param newValue The new classification.
     */
    public void setClassification(final Classification newValue) {
        checkWritePermission();
        classification = newValue;
    }

    /**
     * Returns the explanation of the application of the legal constraints or other restrictions and legal
     * prerequisites for obtaining and using the resource.
     *
     * @return Explanation of the application of the legal constraints, or {@code null}.
     */
    @Override
    @XmlElement(name = "userNote")
    public InternationalString getUserNote() {
        return userNote;
    }

    /**
     * Sets the explanation of the application of the legal constraints or other restrictions and legal
     * prerequisites for obtaining and using the resource.
     *
     * @param newValue The new user note.
     */
    public void setUserNote(final InternationalString newValue) {
        checkWritePermission();
        userNote = newValue;
    }

    /**
     * Returns the name of the classification system.
     *
     * @return Name of the classification system, or {@code null}.
     */
    @Override
    @XmlElement(name = "classificationSystem")
    public InternationalString getClassificationSystem() {
        return classificationSystem;
    }

    /**
     * Sets the name of the classification system.
     *
     * @param newValue The new classification system.
     */
    public void setClassificationSystem(final InternationalString newValue) {
        checkWritePermission();
        classificationSystem = newValue;
    }

    /**
     * Returns the additional information about the restrictions on handling the resource.
     *
     * @return Additional information about the restrictions, or {@code null}.
     */
    @Override
    @XmlElement(name = "handlingDescription")
    public InternationalString getHandlingDescription() {
        return handlingDescription;
    }

    /**
     * Sets the additional information about the restrictions on handling the resource.
     *
     * @param newValue The new handling description.
     */
    public void setHandlingDescription(final InternationalString newValue) {
        checkWritePermission();
        handlingDescription = newValue;
    }
}
